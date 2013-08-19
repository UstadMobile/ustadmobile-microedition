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

import com.sun.lwuit.io.util.Util;
import com.toughra.mlearnplayer.EXEStrMgr;
import com.toughra.mlearnplayer.MLearnPlayerMidlet;
import com.toughra.mlearnplayer.MLearnUtils;
import com.toughra.mlearnplayer.xml.GenericXmlParser;
import com.toughra.mlearnplayer.xml.XmlNode;
import javax.microedition.io.*;
import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.io.file.FileConnection;
import org.kxml2.io.KXmlParser;

/**
 * MLCloudConnector manages maintaining a connection between the app and the
 * Ustad Mobile cloud server.  
 * 
 * This is done using a raw socket level connection because J2ME does not provide
 * guaranteed support of keep-alive persistent connections which are needed
 * to avoid wasting bandwidth on TCP and SSL handshakes.
 * 
 * In accordance with the HTTP spec server responses and requests MUST have a
 * content-length header.  Only that number of bytes specified by the header
 * must be read or written.
 * 
 * To maintain thread safety all I/O is in a synchronized(this) block
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
    
    /** The string to append to the server URL for sending preferences to cloud */
    public static String CLOUD_SETPREF_PATH="/echoparams.php";
    
    /** The string to append to the server URL for getting preferences from cloud as XML*/
    public static String CLOUD_GETPREF_PATH="/umobile/dummypreferences.php";
    
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
        
        buf.append("User-Agent: UstadMobile(J2ME)\n");
        buf.append('\n');

        return buf.toString();
    }

    
    public int checkLogin(String userID, String userPass) {
        int result = -1;
        try {
            ByteArrayOutputStream bout = null;
            synchronized(this) { 
                String url = "http://" + MLearnPlayerMidlet.masterServer + CLOUD_LOGIN_PATH;
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
     * sendFile method makes a multipart post request to the server (used to send
     * logs)
     * 
     * @param url full HTTP URL that we are talking to
     * @param params Hashtable of key/value pairs that will be sent as form fields
     * @param fileField The fieldname for the file field that will be used
     * @param fileName Filename to present to the server -eg. studentname.log
     * @param fileType Type of file being sent - e.g. text/plain
     * @param fileConURI The URI to use with the FileConnection method (source data)
     * @param skipBytes Number of bytes of the file to skip (e.g. data already sent before)
     * 
     * @return byte array of the response sent by the server
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
            synchronized(this) {
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
            }
        }catch(Exception e) {
            System.err.println("bad whilst attempting to send file");
            e.printStackTrace();
        }
        
        return responseBytes;
    }
    
    /**
     * Gets preferences from the cloud.  They are returned by the server in the 
     * form of:
     */
    public void getPreferences() {
        try {
            openConnection();
            String url = MLearnPlayerMidlet.masterServer + CLOUD_GETPREF_PATH;
            String requestStr = getRequestHeader(url);
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            int responseCode = 0;
            synchronized(this) {
                //TODO: add the userid and password to this
                out.write(requestStr.getBytes());
                responseCode = readResponse(bout, new Hashtable());
            }
            
            String responseStr = new String(bout.toByteArray());
            if(responseCode == 200) {
                byte[] xmlDocByte = bout.toByteArray();
                GenericXmlParser gParser = new GenericXmlParser();
                KXmlParser parser = new KXmlParser();
                InputStreamReader reader = new InputStreamReader(
                        new ByteArrayInputStream(xmlDocByte), "UTF-8");
                parser.setInput(reader);
                XmlNode node = gParser.parseXML(parser, true);
                Vector prefVector = node.findChildrenByTagName("pref", true);
                for(int i = 0; i < prefVector.size(); i++) { 
                    XmlNode currentPref = (XmlNode)prefVector.elementAt(i);
                    String prefKey = currentPref.getAttribute("key");
                    String prefVal = currentPref.getAttribute("value");
                    EXEStrMgr.getInstance().setPrefDirect(prefKey, prefVal);
                }
            }
        }catch(Exception e) {
            EXEStrMgr.po("Exception attempting to set preferences from cloud", lastResponseCode);
        }
    }
    
    /**
     * Sends the preferences that have been changed to the cloud
     * 
     * @return true if everything was OK, false if any error happened
     */
    public boolean sendPreferences() {
        boolean doneOK = false;
        try {
            String[] prefNames = EXEStrMgr.getInstance().getReplicateList();
            if(prefNames.length == 0) {
                return true;//nothing to do
            }
            
            openConnection();
            
            StringBuffer url = new StringBuffer(MLearnPlayerMidlet.masterServer)
                .append(MLCloudConnector.CLOUD_SETPREF_PATH).append('?');
            
            url.append("userid=").append(EXEStrMgr.getInstance().getPref(EXEStrMgr.KEY_CLOUDUSER)).append('&');
            url.append("password=").append(EXEStrMgr.getInstance().getPref(EXEStrMgr.KEY_CLOUDPASS)).append('&');
            
            for(int i = 0; i < prefNames.length; i++) {
                url.append(prefNames[i]).append('=');
                url.append(Util.encodeUrl(EXEStrMgr.getInstance().getPref(prefNames[i])));
                if(i < prefNames.length - 1) {
                    url.append('&');
                }
            }
            String urlStr = url.toString();
            
            //now send the request
            String requestStr = getRequestHeader(urlStr);
            out.write(requestStr.getBytes());
            Hashtable headers = new Hashtable();
            ByteArrayOutputStream respBytes = new ByteArrayOutputStream();
            int respCode = readResponse(respBytes, headers);
            if(respCode == 200) {
                doneOK = true;
                EXEStrMgr.getInstance().delPref(EXEStrMgr.KEY_REPLIST);
            }
        }catch(Exception e) {
            EXEStrMgr.po(e, "Exception attempting to send preferences to cloud");
        }
        
        return doneOK;
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
