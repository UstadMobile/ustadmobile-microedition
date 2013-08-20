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
    
    /** The waiting time (in ms) before retrying a request */
    public static final int RETRY_WAIT = 5000;
    
    /** The number of times to retry */
    public static final int RETRY_ATTEMPTS = 3;
    
    /** The string to append to the server URL for login*/
    public static String CLOUD_LOGIN_PATH="/UMCloud-18586311361890088819.0/app/login.xhtml";
    
    /** The string to append to the server URL for sending preferences to cloud */
    public static String CLOUD_SETPREF_PATH="/UMCloud-18586311361890088819.0/app/setPreference.xhtml";
    
    /** The string to append to the server URL for getting preferences from cloud as XML*/
    public static String CLOUD_GETPREF_PATH="/UMCloud-18586311361890088819.0/app/getPreference.xhtml";
    
    /** The String to append to the server URL for log submission */
    public static String CLOUD_LOGSUBMIT_PATH="/UMCloud-18586311361890088819.0/app/uploadLog.xhtml";
    
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
            conn = (SocketConnection)Connector.open("socket://" + MLearnPlayerMidlet.masterServer);
            
            conn.setSocketOption(SocketConnection.DELAY, 1);
            conn.setSocketOption(SocketConnection.LINGER, 0);
            //conn.setSocketOption(SocketConnection.RCVBUF, 8192);
            //conn.setSocketOption(SocketConnection.RCVBUF, 0);
            //conn.setSocketOption(SocketConnection.SNDBUF, 512);
            //conn.setSocketOption(SocketConnection.SNDBUF, 0);
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
    private int readResponse(OutputStream out, Hashtable params) throws Exception{
        int responseStatus = 0;

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
        //TODO: check for connection: close
        int clPos = hdrText.toLowerCase().indexOf("content-length: ");
        if (clPos == -1) {
            // reading till the end of stream
            throw new IllegalArgumentException("MLCloudConnector: Content-length was not specified on response.");
        } else {
            contentLength = Integer.parseInt(
                    hdrText.substring(clPos + 16,
                    hdrText.indexOf("\r\n", clPos + 17)));
            bytesToGo = (int)contentLength;
            //temp just to see what the hell happens
            //bytesToGo = bytesToGo -1;
        }

        //imagine one byte
        byte[] buf = new byte[1024];
        int count = 0;
        while(bytesToGo > 0) {
            count = in.read(buf, 0, Math.min(bytesToGo, buf.length));
            if(count == -1) {
                break;//end of stream
            }
            out.write(buf, 0, count);
            bytesToGo -= count;
        }
        
        out.flush();

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
    
    /**
     * This will take a MLCloudRequest object, perform the request, retry if
     * required.
     * 
     * @param request The request object
     * @param respOut Output stream into which response will be sent
     * @param respHeaders Hashtable in which response headers will be placed (not implemented)
     * 
     * @return the HTTP response code or -1 if there's a total failure
     */
    public int doRequest(MLCloudRequest request, OutputStream respOut, Hashtable respHeaders) {
        int respCode = -1;
        for(int tryCount = 0; tryCount < RETRY_ATTEMPTS; tryCount++) {
            try {
                synchronized(this) {
                    openConnection();
                    byte[] reqBytes = request.getRequestBytes(this);
                    String requestStr = new String(reqBytes);
                    out.write(reqBytes);
                    respCode = readResponse(respOut, respHeaders);
                }
                return respCode;
            }catch(Exception e) {
                e.printStackTrace();
                EXEStrMgr.po(e, "Something went wrong with request");
                closeConnections();
            }
            try { Thread.sleep(RETRY_WAIT); }
            catch(InterruptedException i) {}
        }
        return respCode;
    }

    private void closeConnections() {
        Exception lastE = null;
        String eStr = "";
        if(in != null) {
            try {in.close();}
            catch(IOException e1) { lastE = e1; eStr  += e1.toString();}
            in = null;
        }
        if(out != null) {
            try {out.close(); }
            catch(IOException e2) {lastE = e2; eStr  += e2.toString();}
            out = null;
        }
        if(conn != null) {
            try { conn.close(); }
            catch(IOException e3) {lastE = e3; eStr  += e3.toString();}
            conn = null;
        }
        
        System.gc();

        if(lastE != null) {
            EXEStrMgr.po(lastE, "Error attempting to close cloud sockets etc:" + eStr);
        }
    }
    
    public int checkLogin(String userID, String userPass) {
        int result = -1;
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            
            StringBuffer url = new StringBuffer(
                    MLearnPlayerMidlet.masterServer + CLOUD_LOGIN_PATH).append('?');
            appendCredentialsToURL(url, userID, userPass);
            MLCloudRequest loginRequest = new MLCloudSimpleRequest(url.toString());
            EXEStrMgr.po("MLCloudConnect: Connection opened", EXEStrMgr.DEBUG);
            /*
            String urlStr = url.toString();
            out.write(getRequestHeader(urlStr).getBytes());
            EXEStrMgr.po("Connected OK - request sent", EXEStrMgr.DEBUG);


            result = readResponse(bout, new Hashtable());
            */
            
            result = doRequest(loginRequest, bout, new Hashtable());
            if(result == 200) {
                EXEStrMgr.getInstance().setCloudUser(userID);
                EXEStrMgr.getInstance().setCloudPass(userPass);
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
    * @param fileField the field to use for the file (can be null for no file)
    * @param fileName The name of the file to present to the remote server (can be null for no file)
    * @param fileType The mime type of the file to present to the remote server (can be null for no file)
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
        
        if(fileField != null) {
            res.append("Content-Disposition: form-data; name=\"").append(fileField).append("\"; filename=\"").append(fileName).append("\"\r\n")
                    .append("Content-Type: ").append(fileType).append("\r\n\r\n");
        }

        return res.toString();
    }
    
    /**
     * 
     * sendLogFile method makes a multipart post request to the server.  This used to
     * send as a file attachment; but this confuses JSF servers.   So this is sent
     * as a very long field.
     * 
     * @param url full HTTP URL that we are talking to
     * @param params Hashtable of key/value pairs that will be sent as form fields
     * @param fileField The fieldname for the file field that will be used (eg logcontent)
     * @param fileName Filename to present to the server -eg. studentname.log
     * @param fileType Type of file being sent - e.g. text/plain
     * @param fileConURI The URI to use with the FileConnection method (source data)
     * @param skipBytes Number of bytes of the file to skip (e.g. data already sent before)
     * 
     * @return byte array of the response sent by the server
     * @throws Exception 
     */
    public byte[] sendLogFile(String url, Hashtable params, String fileField, String fileName, String fileType, String fileConURI, long skipBytes) throws Exception{
        openConnection();
        
        byte[] responseBytes = null;
 
        try{
            String boundary = getBoundaryString();
        
            FileConnection fcon = null;
            InputStream is = null;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            InputStream fin = null;
            
            if(fileConURI != null) {
                fcon = (FileConnection)Connector.open(fileConURI);
                fin = fcon.openInputStream();
            }else {
                String msg = "No File Loaded";
                fin = new ByteArrayInputStream(msg.getBytes());
            }
            
            if(skipBytes > 0) {
                EXEStrMgr.po("HTTP Rep skipping " + skipBytes + " bytes already sent", EXEStrMgr.DEBUG);
                fin.skip(skipBytes);
            }

            int count = 0;
            byte[] buf = new byte[1024];

            while((count = fin.read(buf)) != -1) {
                bos.write(buf, 0, count);
            }

            fin.close();
            if(fcon != null) {
                fcon.close();
            }
            bos.flush();
            String fileContents = new String(bos.toByteArray());
            bos.close();
            bos = null;
            
            params.put(fileField, fileContents);
            
            String boundaryMessage = getBoundaryMessage(boundary, params, null, null, null);

            String endBoundary = "\r\n--" + boundary + "--\r\n";

            EXEStrMgr.po("Opening Connection to " + url, EXEStrMgr.DEBUG);
                    
            Hashtable requestHeaders = new Hashtable();
            requestHeaders.put("Content-Type", "multipart/form-data; boundary=" + getBoundaryString());
            
            
            
            //long sizeAvailable = fcon.fileSize();
            //long fileSizeToSend = sizeAvailable - skipBytes;
            byte[] boundaryMessageBytes = boundaryMessage.getBytes();
            byte[] endBoundaryBytes = endBoundary.getBytes();
            long contentLength = boundaryMessageBytes.length + endBoundaryBytes.length;
            
            requestHeaders.put("Content-Length", String.valueOf(contentLength));
            
            String requestHeader = getRequestHeader(url, HttpConnection.POST, requestHeaders);
            synchronized(this) {
                out.write(requestHeader.getBytes());
                out.write(boundaryMessageBytes);

                
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
            StringBuffer url = new StringBuffer(MLearnPlayerMidlet.masterServer)
                    .append(CLOUD_GETPREF_PATH).append('?');
            appendCredentialsToURL(url);
            String urlStr = url.toString();
            MLCloudRequest request = new MLCloudSimpleRequest(urlStr);
            url = null;
            
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            int responseCode = doRequest(request, bout, new Hashtable());
            //TODO: add the userid and password to this
            
            //String responseStr = new String(bout.toByteArray());
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
     * Simply append userid=..&password= to the url
     * @param url 
     */
    private void appendCredentialsToURL(StringBuffer url, String cloudUser, String cloudPass) {
        url.append("userid=").append(Util.encodeUrl(cloudUser)).append('&');
        url.append("password=").append(Util.encodeUrl(cloudPass));
    }
    
    private void appendCredentialsToURL(StringBuffer url) {
        appendCredentialsToURL(url, EXEStrMgr.getInstance().getPref(EXEStrMgr.KEY_CLOUDUSER),
                EXEStrMgr.getInstance().getPref(EXEStrMgr.KEY_CLOUDPASS));
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
            appendCredentialsToURL(url);
            url.append('&');
            
            for(int i = 0; i < prefNames.length; i++) {
                url.append("param_");
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


class MLCloudSimpleRequest implements MLCloudRequest {
    String url;
    
    MLCloudSimpleRequest(String url) {
        this.url = url;
    }
    
    public byte[] getRequestBytes(MLCloudConnector connector) {
        String request = connector.getRequestHeader(url);
        return request.getBytes();
    }
}

/**
 * A request containing a file upload
 * 
 * @author mike
 */
class MLCloudFileRequest  implements  MLCloudRequest{
    
    String url;
    Hashtable params;
    String fileField;
    
    public MLCloudFileRequest(String url, Hashtable params, String fileField) {
        this.url = url;
        this.params = params;
        this.fileField = fileField;
    }
    
    public byte[] getRequestBytes(MLCloudConnector connector) {
        return null;
    }
}


