/*
 * Ustad Mobile (Micro Edition App)
 * 
 * Copyright 2011-2014 UstadMobile Inc. All rights reserved.
 * www.ustadmobile.com
 *
 * Ustad Mobile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version with the following additional terms:
 * 
 * All names, links, and logos of Ustad Mobile and Toughra Technologies FZ
 * LLC must be kept as they are in the original distribution.  If any new
 * screens are added you must include the Ustad Mobile logo as it has been
 * used in the original distribution.  You may not create any new
 * functionality whose purpose is to diminish or remove the Ustad Mobile
 * Logo.  You must leave the Ustad Mobile logo as the logo for the
 * application to be used with any launcher (e.g. the mobile app launcher).
 * 
 * If you want a commercial license to remove the above restriction you must
 * contact us and purchase a license without these restrictions.
 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 
 * Ustad Mobile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
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

