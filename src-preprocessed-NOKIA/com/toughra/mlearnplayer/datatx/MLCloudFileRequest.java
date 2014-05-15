
package com.toughra.mlearnplayer.datatx;

import com.toughra.mlearnplayer.EXEStrMgr;
import com.toughra.mlearnplayer.datatx.Base64;
import com.toughra.mlearnplayer.datatx.MLCloudConnector;
import com.toughra.mlearnplayer.datatx.MLCloudRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.file.FileConnection;
import net.sf.jazzlib.GZIPOutputStream;

/**
 * A request containing a file upload for the cloud server.  
 * 
 * Regrettably Java Enterprise Edition makes it difficult to capture 
 * multipart file fields that would normally be binary safe and the best
 * way to do things.  We also want to zip this up as much as possible
 * to minimize data transfer usage.
 * 
 * Therefor a field named as per the variable fileField will be sent containing
 * data that is gzipped and then encoded in Base64.
 * 
 * @author mike
 */
public class MLCloudFileRequest  implements  MLCloudRequest{
    
    /** Any file greater than this size in bytes will be cached into a file, smaller will work in memory*/
    static long MAXMEMFILESIZE = 200000;
        
    static final int COMP_BOUNDARYMESSAGE= 0;
    static final int COMP_STARTFILEFIELD = 1;
    static final int COMP_ENDREQ = 2;
    
    boolean zipIt = true;
    
    int zippedLength;
    
    byte[] wholeRequestBytes;
    
    /** The amount of log bytes that we intend to send with this request from
     *  the file.  It will be less than MLCloudConnector.MAX_LOGSEND and will terminate
     *  the last new line character (inclusive) before MLCloudConnector.MAX_LOGSEND bytes
     *  after skipping skipBytes (for those bytes already sent)
     */
    public int logBytesToSend = -1;
    
    public MLCloudFileRequest(MLCloudConnector connector, String url, Hashtable params, String fileField, String fileConURI, long skipBytes) {
        //figure out this request details
        
        FileConnection fCon = null;
        InputStream fin = null;
        
        byte[][] contentBytes = null;
        
        byte[] zippedBytes = null;
        
        long filesize = 0;
        
        try {
            fCon = (FileConnection)Connector.open(fileConURI);
            filesize = fCon.fileSize();
            
            String[] reqCompStr = new String[3];
            String boundary = connector.getBoundaryString();

            reqCompStr[COMP_BOUNDARYMESSAGE] = connector.getBoundaryMessage(boundary, params, null, null, null);
            reqCompStr[COMP_STARTFILEFIELD] = connector.appendFormFieldStart(new StringBuffer(), fileField).toString();

            //end of the message is the end of that field and then the end boundary message
            String endBoundary =  "\r\n--" + boundary + "--\r\n";
            reqCompStr[COMP_ENDREQ] = /*connector.appendFormFieldEnd(new StringBuffer(), boundary).toString()
                    + */endBoundary;

            contentBytes = new byte[3][0];
            long contentByteCount = 0;

            for(int i = 0; i < reqCompStr.length; i++) {
                contentBytes[i] = reqCompStr[i].getBytes();
                contentByteCount += contentBytes[i].length;
            }
            OutputStream gzOutDest = new ByteArrayOutputStream();
            ByteArrayOutputStream contentZipped = new ByteArrayOutputStream();


            GZIPOutputStream gzOut = new GZIPOutputStream(contentZipped);
            gzOutDest.write(contentBytes[COMP_BOUNDARYMESSAGE]);
            gzOutDest.write(contentBytes[COMP_STARTFILEFIELD]);
            //now the file itself
            fin = fCon.openInputStream();
            if(skipBytes > 0) {
                fin.skip(skipBytes);
            }
            int count = 0;
            byte[] buf = new byte[1024];
            int pos = 0;
            
            ByteArrayOutputStream logContentOut = new ByteArrayOutputStream();
            while((count = fin.read(buf)) != -1 && pos < MLCloudConnector.MAX_LOGSEND) {
                logContentOut.write(buf, 0, count);
                pos += count;
            }
            
            logContentOut.flush();
            
            /*
             * Find the last newline character by working backwards through
             * the byte array
             */
            byte[] logContentBytes = logContentOut.toByteArray();
            int lastNewLinePos = -1;
            for(int i = logContentBytes.length - 1; i > 0; i--) {
                if((char)logContentBytes[i] == '\n') {
                    lastNewLinePos = i;
                    break;
                }
            }
            
            logBytesToSend = lastNewLinePos + 1;
            
            //gzout write here...
            gzOut.write(logContentBytes, 0, logBytesToSend);
            
            gzOut.flush();
            gzOut.close();
            
            

            //now encode this in base64
            byte[] logBytes = contentZipped.toByteArray();
            String gzBase64 = Base64.encode(logBytes);
            byte[] gzBase64Bytes = gzBase64.getBytes();
            gzOutDest.write(gzBase64Bytes);

            gzOutDest.write(contentBytes[COMP_ENDREQ]);
            gzOutDest.flush();

            zippedBytes = ((ByteArrayOutputStream)gzOutDest).toByteArray();
            zippedLength = zippedBytes.length;
        }catch(IOException e) {
            EXEStrMgr.lg(128, "Exception preparing cloud file request", e);
        }finally {
            if(fin != null) {
                try { fin.close(); }
                catch(IOException e2) { EXEStrMgr.lg(129, "Exception closing fin", e2); }
            }
            fin = null;
            if(fCon != null) {
                try { fCon.close(); }
                catch(IOException e3) { EXEStrMgr.lg(129, "Exception closing fCon", e3); }
            }
        }

        
        Hashtable requestHeaders = new Hashtable();
        
        requestHeaders.put("Content-Length", String.valueOf(zippedLength));
        requestHeaders.put("Content-Type", "multipart/form-data; boundary=" + connector.getBoundaryString());
        
        String requestStr = connector.getRequestHeader(url, HttpConnection.POST, requestHeaders);
        byte[] requestBytes = requestStr.getBytes();
        
        
        //build a byte[] array of what we do before the file field comes
        this.wholeRequestBytes = new byte[requestBytes.length + zippedLength];
        System.arraycopy(requestBytes, 0, wholeRequestBytes,0, requestBytes.length);
        System.arraycopy(zippedBytes, 0, wholeRequestBytes, requestBytes.length, zippedBytes.length);
    }
    
    public InputStream getInputStream() {
        return new ByteArrayInputStream(wholeRequestBytes);
    }
    
}
