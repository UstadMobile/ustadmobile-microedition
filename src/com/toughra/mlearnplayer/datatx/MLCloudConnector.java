/*
 * Ustad Mobil.  
 * Copyright 2011-2013 Toughra Technologies FZ LLC.
 * www.toughra.com
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.toughra.mlearnplayer.datatx;

import com.toughra.mlearnplayer.EXEStrMgr;
import com.toughra.mlearnplayer.MLearnPlayerMidlet;
import com.toughra.mlearnplayer.MLearnUtils;
import javax.microedition.io.*;
import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.microedition.io.file.FileConnection;

/**
 *
 * @author mike
 */
public class MLCloudConnector {
    
    /** Instance of self */
    private static MLCloudConnector instance;
    
    /** Socket connection to the server*/
    private SocketConnection  conn;
    
    /** InputStream going to the server */
    private InputStream in;
    
    /** OutputStream coming from the server*/
    private OutputStream out;
    
    /** The boundary for HTTP Post requests */
    static final String BOUNDARY = "----------V2ymHFg03ehbqgZCaKO6jy";
    
    /** HTTP response code of last request */
    private int lastResponseCode;
    
    /** The string to append to the server URL for login*/
    public static String CLOUD_LOGIN_PATH="/login.php";
    
    /** The String to append to the server URL for log submission */
    public static String CLOUD_LOGSUBMIT_PATH="/umobile/datarxdummy.php";
    
    /** The String to append to the server URL for callhome submission */
    public static String CLOUD_CALLHOME_PATH="/umobile/datarxdummy.php";
    
    public static final MLCloudConnector getInstance() {
        if(instance == null) {
            instance = new MLCloudConnector();
        }
        
        return instance;
    }
    
    /**
     * This will open a connection to the server.  We are doing this with manual
     * sockets because:
     *  1. We need keep-alive working across all devices.  Without keepalive we
     *     would have to repeat SSL handshakes all the time. 4KB every 5 mins.
     * 
     *  2. We need to use GZIP on all devices to minimize the amount of data
     *     being used.
     */
    private void openConnection() {
        if(conn != null) {
            return;
        }
        
        try {
            conn = (SocketConnection)Connector.open("socket://" + MLearnPlayerMidlet.masterServer
                    + ":80");
            
            conn.setSocketOption(SocketConnection.DELAY, 1);
            conn.setSocketOption(SocketConnection.LINGER, 0);
            conn.setSocketOption(SocketConnection.RCVBUF, 8192);
            conn.setSocketOption(SocketConnection.SNDBUF, 512);
            conn.setSocketOption(SocketConnection.KEEPALIVE, 1);
            
            in = conn.openInputStream();
            out = conn.openOutputStream();
            System.out.println("Opened up connection to cloud server");
        }catch(Exception e) {
            e.printStackTrace();
            EXEStrMgr.po(e, "Exception getting connection to server going");
        }
    }
    
    /**
     * Reads the HTTP response of a server
     * 
     * @param out The output stream in which to place the response
     * @param params Hashtable in which http headers will be placed
     * 
     * @return the HTTP status code
     * 
     */
    private int readResponse(OutputStream out, Hashtable params) {
        int responseStatus = 0;
        try {
            // now reading the response
            EXEStrMgr.po("Request header sent, waiting for response...", EXEStrMgr.DEBUG);
            
            ByteArrayOutputStream headerStream = new ByteArrayOutputStream(800);
            long contentLength = -1;
            
            //first read all of the header
            boolean inHeader = true;
            int b = -1;
            byte numCRs = 0;
            byte numLFs = 0;
            int bytesToGo = 1;
            do {
                b = in.read();
                if(b == '\n') {
                    numLFs++;
                    if(numCRs == 2 && numLFs == 2) {
                        inHeader = false;
                        break;//header is over
                    }
                }else if(b == '\r') {
                    numCRs++;
                }else {
                    numLFs = 0;
                    numCRs = 0;
                }
                headerStream.write(b);
            }while(inHeader && b != -1);
            
            String hdrText = new String(headerStream.toByteArray());
            responseStatus = Integer.parseInt(hdrText.substring(9, 12));
            lastResponseCode = responseStatus;
            int clPos = hdrText.toLowerCase().indexOf("content-length: ");
            if (clPos == -1) {
                // reading till the end of stream
                throw new IllegalArgumentException("MLCloudConnector: Content-length was not specified on response.");
            } else {
                contentLength = Integer.parseInt(
                        hdrText.substring(clPos + 16,
                        hdrText.indexOf("\r\n", clPos + 17)));
                bytesToGo = (int)contentLength;
            }
            
            //imagine one byte
            b = 0;
            while(bytesToGo > 0) {
                b = in.read();
                if(b == -1) {
                    break;//end of stream..
                }
                out.write(b);
                bytesToGo--;
            }
            out.flush();
        }catch(Exception e) {
            EXEStrMgr.po(e, "bad whilst attempting to read from cloud connection");
        }
        
        
        
        return responseStatus;
    }
    
    /**
     * Make an HTTP 1.1 Request header
     * 
     * Will default to GET method
     * 
     * @param targetURL URL to request
     * 
     * @return The header as a string
     */
    public String getRequestHeader(String targetURL) {
        return getRequestHeader(targetURL, HttpConnection.GET, null);
    }
    
    /**
     * Makes an HTTP 1.1 Request header
     * 
     * @param endRequest - if set true (normal) will end with a \n so request goes through.  False so you can add your own stuff
     */ 
    public String getRequestHeader(String targetURL, String method, Hashtable headers) {
        StringBuffer buf = new StringBuffer(512);
        
        String[] urlParts = MLearnUtils.getURLParts(targetURL);
        
        buf.append(method + " " + urlParts[MLearnUtils.URLPART_RESOURCE] + " HTTP/1.1\n");
        
        buf.append("Host: ");
        buf.append(urlParts[MLearnUtils.URLPART_HOST]);
        buf.append(':');
        buf.append(urlParts[MLearnUtils.URLPART_PORT]);
        buf.append('\n');
        buf.append("Accept: */*\n");
        buf.append("Cache-control: no-transform\n");
        buf.append("Connection: Keep-Alive\n");
        if(headers != null) {
            Enumeration e = headers.keys();
            while(e.hasMoreElements()) {
                String headerName = e.nextElement().toString();
                buf.append(headerName).append(": ");
                buf.append(headers.get(headerName).toString()).append("\n");
            }
        }
        
        // if you want keep-alive - remove the following line
        //buf.append("Connection: close\n");
        buf.append("User-Agent: MIDP2.0\n");
        buf.append('\n');

        return buf.toString();
    }

    
    public int checkLogin(String userID, String userPass) {
        int result = -1;
        try {
            ByteArrayOutputStream bout = null;
            synchronized(this) { 
                String url = "http://" + MLearnPlayerMidlet.masterServer + CLOUD_LOGSUBMIT_PATH;
                openConnection();
                EXEStrMgr.po("MLCloudConnect: Connection opened", EXEStrMgr.DEBUG);
                out.write(getRequestHeader(url).getBytes());
                EXEStrMgr.po("Connected OK - request sent", EXEStrMgr.DEBUG);
                bout = new ByteArrayOutputStream();
                result = readResponse(bout, new Hashtable());
            }
            String serverSays = new String(bout.toByteArray());
            EXEStrMgr.po("Server says " + serverSays, EXEStrMgr.DEBUG);
        }catch(Exception e) {
            EXEStrMgr.po(e, "Something bad with checklogin");
        }
        
        return result;
    }
    
    String getBoundaryString() {
        return BOUNDARY;
    }
    
    /**
    * Generates the boundary message
    * 
    * @param boundary boundary unique string
    * @param params Hashtable of key/value pairs
    * @param fileField the field to use for the file
    * @param fileName The name of the file to present to the remote server
    * @param fileType The mime type of the file to present to the remote server
    * @return Boundary message to use with http request
    */
    String getBoundaryMessage(String boundary, Hashtable params, String fileField, String fileName, String fileType) {
        StringBuffer res = new StringBuffer("--").append(boundary).append("\r\n");

        Enumeration keys = params.keys();

        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            String value = (String) params.get(key);

            res.append("Content-Disposition: form-data; name=\"").append(key).append("\"\r\n")
                    .append("\r\n").append(value).append("\r\n")
                    .append("--").append(boundary).append("\r\n");
        }
        res.append("Content-Disposition: form-data; name=\"").append(fileField).append("\"; filename=\"").append(fileName).append("\"\r\n")
                .append("Content-Type: ").append(fileType).append("\r\n\r\n");

        return res.toString();
    }
    
    /**
     * 
     * @param url
     * @param params
     * @param fileField
     * @param fileName
     * @param fileType
     * @param fileConURI
     * @param skipBytes
     * @return
     * @throws Exception 
     */
    public byte[] sendFile(String url, Hashtable params, String fileField, String fileName, String fileType, String fileConURI, long skipBytes) throws Exception{
        openConnection();
        
        String boundary = getBoundaryString();
 
        String boundaryMessage = getBoundaryMessage(boundary, params, fileField, fileName, fileType);
 
	String endBoundary = "\r\n--" + boundary + "--\r\n";
                
        FileConnection fcon = null;
        InputStream is = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        InputStream fin = null;
 
        byte[] responseBytes = null;
 
        try{
            EXEStrMgr.po("Opening Connection to " + url, EXEStrMgr.DEBUG);
                    
            Hashtable requestHeaders = new Hashtable();
            requestHeaders.put("Content-Type", "multipart/form-data; boundary=" + getBoundaryString());
            
            if(fileConURI != null) {
                fcon = (FileConnection)Connector.open(fileConURI);
                fin = fcon.openInputStream();
            }else {
                String msg = "No File Loaded";
                fin = new ByteArrayInputStream(msg.getBytes());
            }
            
            long sizeAvailable = fcon.fileSize();
            long fileSizeToSend = sizeAvailable - skipBytes;
            byte[] boundaryMessageBytes = boundaryMessage.getBytes();
            byte[] endBoundaryBytes = endBoundary.getBytes();
            long contentLength = boundaryMessageBytes.length
                    + fileSizeToSend + endBoundaryBytes.length;
            
            requestHeaders.put("Content-Length", String.valueOf(contentLength));
            
            String requestHeader = getRequestHeader(url, HttpConnection.POST, requestHeaders);
            
            out.write(requestHeader.getBytes());
            out.write(boundaryMessageBytes);
            
            if(skipBytes > 0) {
                EXEStrMgr.po("HTTP Rep skipping " + skipBytes + " bytes already sent", EXEStrMgr.DEBUG);
                fin.skip(skipBytes);
            }
            
            int count = 0;
            byte[] buf = new byte[1024];
            
            while((count = fin.read(buf)) != -1) {
                out.write(buf, 0, count);
            }
            
            fin.close();
            if(fcon != null) {
                fcon.close();
            }
            out.write(endBoundaryBytes);
            out.flush();
            
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            Hashtable respHeaders = new Hashtable();
            int result = readResponse(bout, respHeaders);
            responseBytes = bout.toByteArray();
            String responseStr = new String(responseBytes);
            int x = 0;
        }catch(Exception e) {
            System.err.println("bad whilst attempting to send file");
            e.printStackTrace();
        }
        
        return responseBytes;
    }
    
    /** 
     * The result of the most recent request
     * 
     * @return result of the most recent request
     */
    public int getLastResponseCode() {
        return lastResponseCode;
    }
    
}
