
package com.toughra.mlearnplayer.datatx;

import com.toughra.mlearnplayer.EXEStrMgr;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.file.FileConnection;

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
    
    /**
     * The amount of log bytes that we have gone through, because we are removing
     * those that we don't intend to send, this indicates how much of the original
     * contents we filtered through
     */
    public int logBytesProcessed = -1;
    
    
    
    public MLCloudFileRequest(MLCloudConnector connector, String url, Hashtable params, String fileField, String fileConURI, long skipBytes, boolean useBoundary, Hashtable headersToAdd) {
        //figure out this request details
        
        FileConnection fCon = null;
        InputStream fin = null;
        
        byte[][] contentBytes = null;
        
        byte[] zippedBytes = null;
        
        try {
            fCon = (FileConnection)Connector.open(fileConURI);
            
            String[] reqCompStr = new String[3];
            if(useBoundary) {
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
            }
            
            OutputStream gzOutDest = new ByteArrayOutputStream();
            ByteArrayOutputStream requestBodyContent = new ByteArrayOutputStream();

            //Disable GZIP for Now
            //GZIPOutputStream gzOut = new GZIPOutputStream(contentZipped);
            OutputStream outToUse = requestBodyContent;
            if(useBoundary) {
                outToUse.write(contentBytes[COMP_BOUNDARYMESSAGE]);
                outToUse.write(contentBytes[COMP_STARTFILEFIELD]);
            }
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
            
            String unfiltered = new String(logContentBytes);
            
            StringBuffer sbFilterBuffer = new StringBuffer();
            int lineStart = 0;
            int tLineCount = 0;
            for(int posCounter = 0; posCounter < lastNewLinePos; posCounter++) {
                char thisByte = (char)logContentBytes[posCounter];
                if(thisByte == '\n') {
                    String thisLine = new String(logContentBytes,
                            lineStart, (posCounter - lineStart));
                    if(thisLine.startsWith("T:")) {
                        int pipePos = thisLine.indexOf("|");
                        //get only the tincan statement itself
                        String filteredLine = thisLine.substring(
                                pipePos+1);
                        if(tLineCount > 0) {
                            sbFilterBuffer.append(",");
                        }
                        if(tLineCount == 1) {
                            //now we know - must add square bracket to start
                            sbFilterBuffer = new StringBuffer(
                                    '[' + sbFilterBuffer.toString());
                        }
                        tLineCount++;
                        sbFilterBuffer.append(filteredLine);
                    }
                    lineStart = posCounter + 1;
                    
                }
            }
            
            if(tLineCount > 1) {
                sbFilterBuffer.append(']');
            }
            
            String filteredRequest= sbFilterBuffer.toString();
            logBytesProcessed = lastNewLinePos + 1;
            
            byte[] filteredLogBytes = filteredRequest.getBytes();
            
            logBytesToSend = filteredLogBytes.length + 1;
            
            //gzout write here...
            outToUse.write(filteredLogBytes, 0,
                    filteredLogBytes.length);
            
            outToUse.flush();
            outToUse.close();
            
            

            //now encode this in base64
            byte[] logBytes = requestBodyContent.toByteArray();
            //String gzBase64 = Base64.encode(logBytes);
            //byte[] gzBase64Bytes = gzBase64.getBytes();
            gzOutDest.write(logBytes);

            if(useBoundary) {
                gzOutDest.write(contentBytes[COMP_ENDREQ]);
            }
            
            gzOutDest.flush();

            zippedBytes = ((ByteArrayOutputStream)gzOutDest).toByteArray();
            String actualRequest = new String(zippedBytes);
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
        if(useBoundary) {
            requestHeaders.put("Content-Type", "multipart/form-data; boundary=" + connector.getBoundaryString());
        }
        
        Enumeration e = headersToAdd.keys();
        while(e.hasMoreElements()) {
            Object key = e.nextElement();
            requestHeaders.put(key, headersToAdd.get(key));
        }
        
        String requestStr = connector.getRequestHeader(url, HttpConnection.POST, 
                requestHeaders);
        byte[] requestBytes = requestStr.getBytes();
        
        
        //build a byte[] array of what we do before the file field comes
        this.wholeRequestBytes = new byte[requestBytes.length + zippedLength];
        System.arraycopy(requestBytes, 0, wholeRequestBytes,0, requestBytes.length);
        System.arraycopy(zippedBytes, 0, wholeRequestBytes, requestBytes.length, zippedBytes.length);
        
        String wholeRequestStr = new String(this.wholeRequestBytes);
        int x = 0;
    }
    
    public InputStream getInputStream() {
        return new ByteArrayInputStream(wholeRequestBytes);
    }
    
}
