/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.toughra.mlearnplayer.datatx;

import com.toughra.mlearnplayer.EXEStrMgr;
import com.toughra.mlearnplayer.MLearnUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

/**
 * Simple get request wrapper
 * 
 * @author mike
 */
public class MLCloudSimpleRequest implements MLCloudRequest  {
    
    byte[] reqBytes;
    int pos = 0;
    int reqBytesLen = 0;

    /**
     * Makes a new request object representing a request to the given server
     * for a given url
     * 
     * @param connector MLCloudConnector that controls access to the server
     * @param url HTTP URL to request: e.g. http://server[:port]/dir/file.html
     * @param headers - Hashtable where response headers are stored - null for none
     */
    public MLCloudSimpleRequest(MLCloudConnector connector, String url, Hashtable headers) {
        this.reqBytes = connector.getRequestHeader(url, 
                "GET", headers).getBytes();
        this.reqBytesLen = reqBytes.length;
    }
    
    public void retry() {
        this.pos = 0;
    }
    
    
    public InputStream getInputStream() {
        return new ByteArrayInputStream(reqBytes);
    }
    
    public int read() throws IOException {
        int retVal = -1;
        if(pos < reqBytesLen) {
            retVal = reqBytes[pos];
            pos++;
        }
        
        return retVal;
    }
    
    public byte[] getRequestBytes(MLCloudConnector connector) {
        return reqBytes;
    }
    
    /**
     * Download a given http URL to a given file URI, with numRetries number of attempts
     * 
     * @param cloudConnector MLCloudConnector we are using for the connection
     * @param httpURL HTTP URL to download
     * @param destFileURI File URI destination
     * @param numRetries Number of retry attempts for that file
     * 
     * @throws Exception - the exception from the last attempt if it fails
     */
    public static int downloadURLToFile(MLCloudConnector cloudConnector, String httpURL, String destFileURI, int numRetries) throws Exception{
        boolean downloadFinishedOK = false;
        Exception lastE = null;
        int tryCount = 0;
        for(tryCount = 0; (tryCount < numRetries) && !downloadFinishedOK; tryCount++) {
            FileConnection fCon = null;
            OutputStream fout = null;
            MLCloudRequest req;
            try {
                req = new MLCloudSimpleRequest(
                    cloudConnector, httpURL, null);
                fCon = (FileConnection)Connector.open(destFileURI);

                //J2ME files must be created first
                if(!fCon.exists()) {
                    fCon.create();
                }
                fout = fCon.openOutputStream();
                cloudConnector.doRequest(req, fout, new Hashtable());
                downloadFinishedOK = true;

            }catch(Exception e) {
                lastE = e;
                EXEStrMgr.lg(130, 
                    "Something wrong downloading file from " +httpURL, e);
            }finally {
                MLearnUtils.closeFileCon(fCon, fout, "Download file " + httpURL);
                fCon = null;
                fout = null;
            }
        }
        if(downloadFinishedOK == false) {
            EXEStrMgr.lg(135, "Failed to download " + httpURL + " after "
                    + tryCount + " attempts", lastE);
            throw lastE;
        }
        
        return 0;
    }
}

