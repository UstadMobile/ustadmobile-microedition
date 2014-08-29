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

import com.sun.lwuit.io.util.Util;
import com.toughra.mlearnplayer.EXEStrMgr;
import com.toughra.mlearnplayer.MLearnPlayerMidlet;
import com.toughra.mlearnplayer.MLearnUtils;
import javax.microedition.io.*;
import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import net.sf.jazzlib.*;

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
    
    /** 
     * Server to connect to (Default: MLearnPlayerMidlet.masterServer 
     * In the form of servername:port (do not prefix http://)
     */
    public String serverName;
    
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
    
    /** The maximum amount of log content to send in one POST request (in bytes)*/
    public static final int MAX_LOGSEND = 100000;
    
    /** The string to append to the server URL for login*/
    //public static String CLOUD_LOGIN_PATH="/umcloud/app/login.xhtml";
    public static String CLOUD_LOGIN_PATH="/xAPI/statements?limit=1";
    
    /** The string to append to the server URL for checking course link by id*/
    //public static String CLOUD_LOGIN_PATH="/umcloud/app/login.xhtml";
    public static String CLOUD_GETCOURSEBYID_PATH="/getcourse/";
    
    /** The string to append to the server URL for sending preferences to cloud */
    public static String CLOUD_SETPREF_PATH="/umcloud/app/setPreference.xhtml";
    
    /** The string to append to the server URL for getting preferences from cloud as XML*/
    public static String CLOUD_GETPREF_PATH="/umcloud/app/getPreference.xhtml";
    
    /** The String to append to the server URL for log submission */
    public static String CLOUD_LOGSUBMIT_PATH="/xAPI/statements";
    
    /** The String to append to the server URL for callhome submission */
    public static String CLOUD_CALLHOME_PATH="/umobile/datarxdummy.php";
    
    /** Create a new MLCloudConnector object for the given server
     * 
     * @param serverName Servername as address:port (no protocol: prefix)
     */
    public MLCloudConnector(String serverName) {
        this.serverName = serverName;
    }
    
    /**
     * Create a new MLCloudConnector object for the default server
     */
    public MLCloudConnector() {
        this(MLearnPlayerMidlet.masterServer);
    }
    
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
            conn = (SocketConnection)Connector.open("socket://" + this.serverName);
            
            //conn.setSocketOption(SocketConnection.DELAY, 0);
            //conn.setSocketOption(SocketConnection.LINGER, 0);
            //conn.setSocketOption(SocketConnection.RCVBUF, 8192);
            conn.setSocketOption(SocketConnection.RCVBUF, 0);
            //conn.setSocketOption(SocketConnection.SNDBUF, 512);
            conn.setSocketOption(SocketConnection.SNDBUF, 0);
            
            conn.setSocketOption(SocketConnection.KEEPALIVE, 1);
            
            conn.setSocketOption(SocketConnection.DELAY, 0);
            //conn.setSocketOption(SocketConnection.KEEPALIVE, 0);
            
            boolean delayOff = conn.getSocketOption(SocketConnection.DELAY) == 0;
            boolean keepAliveOff = conn.getSocketOption(
                    SocketConnection.KEEPALIVE) == 0;
            
            in = conn.openInputStream();
            out = conn.openOutputStream();
            System.out.println("Opened up connection to cloud server : "+
                this.serverName);
            EXEStrMgr.lg(114, "Connecting to server: " + this.serverName);
        }catch(Exception e) {
            EXEStrMgr.lg(124, "Exception getting connection to server going", e);
        }
    }
    
    /**
    *   Custom split method for J2ME
    * 
    * */
    public static String[] Split(String splitStr, String delimiter) {
        StringBuffer token = new StringBuffer();
        Vector tokens = new Vector();
        // split
        char[] chars = splitStr.toCharArray();
        for (int i=0; i < chars.length; i++) {
            if (delimiter.indexOf(chars[i]) != -1) {
                // we bumbed into a delimiter
                if (token.length() > 0) {
                    tokens.addElement(token.toString());
                    token.setLength(0);
                }
            } else {
                token.append(chars[i]);
            }
        }
        // don't forget the "tail"...
        if (token.length() > 0) {
            tokens.addElement(token.toString());
        }
        // convert the vector into an array
        String[] splitArray = new String[tokens.size()];
        for (int i=0; i < splitArray.length; i++) {
            splitArray[i] = (String)tokens.elementAt(i);
        }
        return splitArray;
 }
    
    
    /**
     * Reads the HTTP response of a server and give back the course id folder link
     * 
     * @param out The output stream in which to place the response
     * @param headers Hashtable in which http headers will be placed.  Important: headers field names will be made lower case
     * 
     * @return the course id folder link or -1 if failure
     * 
     */
    private String readCourseIDFolderLink(OutputStream out, Hashtable headers) throws Exception{
        int responseStatus = 0;

        // now reading the response
        EXEStrMgr.lg(22, "Request header sent, waiting for response...");

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

        String[] courseLinkHeaders = Split(hdrText, "\n");
        String courseLinkFolder = courseLinkHeaders[6].substring(8);

        responseStatus = Integer.parseInt(hdrText.substring(9, 12));
        lastResponseCode = responseStatus;
        
        if (responseStatus != 200){
            return "FAIL";
        }
       
        return courseLinkFolder;
    }
    
     
    /**
     * Reads the HTTP response of a server
     * 
     * @param out The output stream in which to place the response
     * @param headers Hashtable in which http headers will be placed.  Important: headers field names will be made lower case
     * 
     * @return the HTTP status code
     * 
     */
    private int readResponse(OutputStream out, Hashtable headers) throws Exception{
        int responseStatus = 0;

        // now reading the response
        EXEStrMgr.lg(22, "Request header sent, waiting for response...");

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
            
            if(b != -1) {
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
            }
        }while(inHeader && b != -1);

        String hdrText = new String(headerStream.toByteArray());
        String logStr = new String(hdrText);
        if(hdrText.length() > 64) {
            logStr = logStr.substring(0, 64);
        }
        EXEStrMgr.lg(150, "HTTP response header starts: " + logStr);
        responseStatus = Integer.parseInt(hdrText.substring(9, 12));
        lastResponseCode = responseStatus;
        
        //we need to go through the string line by line
        String[] lines = MLearnUtils.getLines(hdrText);
        for(int i = 1; i < lines.length; i++) {
            int colonPos = lines[i].indexOf(':');
            String headerName = lines[i].substring(0, colonPos).toLowerCase();
            String headerValue = lines[i].substring(colonPos+2);
            headers.put(headerName, headerValue);
        }
        
        //check and see if its gzipped
        Object encodingObj = headers.get("content-encoding");
        boolean isGzipped = false;
        if(encodingObj != null) {
            if(encodingObj.equals("gzip")) {
                isGzipped = true;
            }
        }
        
        
        int clPos = hdrText.toLowerCase().indexOf("content-length: ");
        byte[] buf = new byte[1024];
        
        /*
         * Check if there is a content-length header - if so set number bytes
         * remaining and keep alive can work.
         */
        if (clPos != -1) {
            contentLength = Integer.parseInt(
                    hdrText.substring(clPos + 16,
                    hdrText.indexOf("\r\n", clPos + 17)));
            if(contentLength > 0) {
                /*
                 * some servers wrongly say content-length 0 - flag this
                 * and mark as not really having a set content-length, 
                 * no keepalive, as though content length was never reported
                 */
                clPos = -1;
            }
        }

        EXEStrMgr.lg(127, "Response: Is GZIPPED: " + isGzipped + 
                " / Content length: " + contentLength);
        ByteArrayOutputStream gzipBuf = null;
        OutputStream outToUse = out;
        gzipBuf = new ByteArrayOutputStream();
        if(isGzipped) {
            outToUse = gzipBuf;
        }
                
        int count = 0;
        
        while(bytesToGo > 0 || clPos == -1) {
            int bytesToRead = buf.length;
            
            //to support keep alive - read only up to bytesToGo bytes as per
            //content-length header
            if(clPos != -1) {
                bytesToRead = Math.min(bytesToGo, buf.length);
            }
            
            count = in.read(buf, 0, bytesToRead);
            if(count == -1 || (count < bytesToRead && clPos == -1)) {
                break;//end of stream
            }
            outToUse.write(buf, 0, count);
            bytesToGo -= count;
        }
        
        outToUse.flush();
        
        //if it's gzipped - now gunzip it
        if(isGzipped) {
            //just for debug
            byte[] gzippedBytes = gzipBuf.toByteArray();            
            ByteArrayInputStream gzipIn = new ByteArrayInputStream(gzippedBytes);
            
            GZIPInputStream gin = new GZIPInputStream(gzipIn);
            Util.copy(gin, out);
        }
        
        Object conObj = headers.get("connection");
        if(conObj != null && conObj.toString().toLowerCase().equals("close")) {
            System.out.println("Server demands connection closed");
            closeConnections();
        }

        return responseStatus;
    }
    
    /**
     * Make an HTTP 1.1 Request header
     * 
     * Will default to GET method
     * 
     * @param targetURL URL to request e.g. http://server[:port]/dir/file.html
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
        buf.append("Cache-control: no-cache\n");
        if(MLearnUtils.KEEPALIVE_ENABLED) {
            buf.append("Connection: Keep-Alive\n");
        }else {
            buf.append("Connection: close\n");
        }
        
        //disable - something is screwed up here
        //buf.append("Accept-Encoding: gzip\n");
        
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
                    InputStream reqIn = request.getInputStream();
                    byte[] buf = new byte[1024];
                    int count = 0;
                    /* in case of wanting to examine a request closely*/
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                            
                    while((count = reqIn.read(buf)) != -1) {
                        out.write(buf, 0, count);
                        bout.write(buf,0,count);
                    }
                    
                    out.flush();
                    
                    String requestStr = new String(bout.toByteArray());
                    
                    reqIn.close();
                    respCode = readResponse(respOut, respHeaders);
                }
                if(respCode == 400) {
                    //we did something bad - close connections
                    closeConnections();
                }
                
                return respCode;
            }catch(Exception e) {
                EXEStrMgr.lg(125, "Something went wrong with cloud request "
                        + " HTTP Code: " + respCode + " : ", e);
                closeConnections();
            }
            
            try { Thread.sleep(RETRY_WAIT); }
            catch(InterruptedException i) {}
        }
        return respCode;
    }

    /**
     * This will take a MLCloudRequest object, perform the request, retry if
     * required.
     * 
     * @param request The request object
     * @param respOut Output stream into which response will be sent
     * @param respHeaders Hashtable in which response headers will be placed (not implemented)
     * 
     * @return the course folder link for that id HTTP response code or -1 if there's a total failure
     */
    public String doCourseIDRequest(MLCloudRequest request, OutputStream respOut, Hashtable respHeaders) {
        int respCode = -1;
        String courseIDLink="FAIL";
        for(int tryCount = 0; tryCount < RETRY_ATTEMPTS; tryCount++) {
            try {
                synchronized(this) {
                    openConnection();
                    InputStream reqIn = request.getInputStream();
                    byte[] buf = new byte[1024];
                    int count = 0;
                    /* in case of wanting to examine a request closely*/
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                            
                    while((count = reqIn.read(buf)) != -1) {
                        out.write(buf, 0, count);
                        bout.write(buf,0,count);
                    }
                    
                    String requestStr = new String(bout.toByteArray());
                    
                    reqIn.close();
                    courseIDLink = readCourseIDFolderLink(respOut, respHeaders);
                }
                if(courseIDLink == "FAIL") {
                    //we did something bad - close connections
                    closeConnections();
                }else{
                    courseIDLink = "/media/eXeExport/" + courseIDLink;
                }
                
                return courseIDLink;
            }catch(Exception e) {
                EXEStrMgr.lg(125, "Something went wrong with cloud request", e);
                closeConnections();
            }
            
            try { Thread.sleep(RETRY_WAIT); }
            catch(InterruptedException i) {}
        }
        return courseIDLink;
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
            EXEStrMgr.lg(126, "Error attempting to close cloud sockets etc:" + eStr, lastE);
        }
    }
    
    /**
     * Make the Header value for HTTP Basic auth e.g. Base userid:pass 
     * base64 encoded;
     * 
     * @param userId - http user id
     * @param userPass - http password
     */
    public static String makeHttpAuthString(String userId, String userPass) {
        String authString = userId+":"+userPass;
        String authEncBytes = Base64.encode(authString);
        String authStringEnc = "Basic " + authEncBytes;
        
        return authStringEnc;
    }
    
    public int checkLogin(String userID, String userPass) {
        int result = -1;
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
                        
            StringBuffer url = new StringBuffer(
                    MLearnPlayerMidlet.masterServer + CLOUD_LOGIN_PATH);
            
            //Basic Authentication credential string.
            //Format: username:password
            String authStringEnc = makeHttpAuthString(userID, userPass);
            
            //Headers for TINCAN auth check
            Hashtable tcAuthHeaders = new Hashtable();
            tcAuthHeaders.put("X-Experience-API-Version", "1.0.1");
            tcAuthHeaders.put("Authorization", authStringEnc);
            
            
            MLCloudRequest loginRequest = new MLCloudSimpleRequest(this, 
                    url.toString(), tcAuthHeaders);
            EXEStrMgr.lg(23, "MLCloudConnect: Connection opened");
            
            result = doRequest(loginRequest, bout, new Hashtable());
            if(result == 200) {
                EXEStrMgr.getInstance().setCloudUser(userID);
                EXEStrMgr.getInstance().setCloudPass(userPass);
            }

            String serverSays = new String(bout.toByteArray());
        }catch(Exception e) {
            EXEStrMgr.lg(121, "Something bad with checklogin", e);
        }
        
        return result;
    }
    
   public String getCourseLinkByID(String courseID) {
        int result = -1;
        String courseFolderLink = "FAIL";
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            
           String cloudServer = "svr2.ustadmobile.com:8010";
            StringBuffer url = new StringBuffer(
                    cloudServer + CLOUD_GETCOURSEBYID_PATH)
                    .append("?id=").append(courseID);
                    
            System.out.println(url.toString());
            
            MLCloudRequest getCourseLinkByIDRequest = new MLCloudSimpleRequest(this, 
                    url.toString(),null);
            EXEStrMgr.lg(23, "MLCloudConnect: Connection opened");
            
            //result = doRequest(getCourseLinkByIDRequest, bout, new Hashtable());
            courseFolderLink = doCourseIDRequest(getCourseLinkByIDRequest, bout, new Hashtable());
            
            if(courseFolderLink != "FAIL") {
                System.out.println("Success!");
                System.out.println("The folder to download is: "+ courseFolderLink);
            }

            //String serverSays = new String(bout.toByteArray());
            
        }catch(Exception e) {
            EXEStrMgr.lg(121, "Something bad with getCourseLinkByID", e);
        }
        
        return courseFolderLink;
    }
    
    public String getBoundaryString() {
        return BOUNDARY;
    }
    
    public StringBuffer appendFormFieldStart(StringBuffer res, String paramName) {
        res.append("Content-Disposition: form-data; name=\"").append(paramName).append("\"\r\n")
                    .append("\r\n");
        return res;
    }
    
    public StringBuffer appendFormFieldEnd(StringBuffer res, String boundary) {
        res.append("\r\n").append("--").append(boundary).append("\r\n");
        return res;
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
    public String getBoundaryMessage(String boundary, Hashtable params, String fileField, String fileName, String fileType) {
        StringBuffer res = new StringBuffer("--").append(boundary).append("\r\n");

        Enumeration keys = params.keys();

        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            String value = (String) params.get(key);
            appendFormFieldStart(res, key).append(value);
            appendFormFieldEnd(res, boundary);
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
     * @return number of log bytes sent -1 if there was an error (e.g. non 200 HTTP response code)
     * @throws Exception 
     */
    public int sendLogFile(String url, Hashtable params, String fileField, String fileName, String fileType, String fileConURI, long skipBytes) throws Exception{
        openConnection();
        
        int retVal = -1;
        
        //check and see if we have no username and password, in which case, just return
        if(EXEStrMgr.getInstance().getCloudUser() == null) {
            return retVal;
        }
 
        try{
            Hashtable headersToAdd = new Hashtable();
            headersToAdd.put("X-Experience-API-Version", "1.0.0");
            headersToAdd.put("Content-Type", 
                    "application/json; charset=UTF-8");
            headersToAdd.put("Authorization", makeHttpAuthString(
                    EXEStrMgr.getInstance().getCloudUser(),
                    EXEStrMgr.getInstance().getCloudPass()));
                    
            MLCloudFileRequest request = new MLCloudFileRequest(this, url, params, 
                    fileField, fileConURI, skipBytes, false, headersToAdd);
            if(request.logBytesToSend > 1) {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                Hashtable respHeaders = new Hashtable();
                int respCode = doRequest(request, bout, respHeaders);
                String serverSays = new String(bout.toByteArray());
                if(respCode == 200) {
                    retVal = request.logBytesProcessed;
                }
            }else {
                //there are no actual TINCAN statements in it
                retVal = request.logBytesProcessed;
            }
        }catch(Exception e) {
            System.err.println("bad whilst attempting to send file");
            e.printStackTrace();
        }
        
        return retVal;
        
    }
    
    /**
     * Gets preferences from the cloud.  They are returned by the server in the 
     * form of:
     * 
     * OUT OF USE UNTIL IMPLEMENTATION OF TINCAN STATE API
     * 
     */
    public void getPreferences() {
        
    }
    
    /**
     * Sends the preferences that have been changed to the cloud
     * 
     * OUT OF USE UNTIL IMPLEMENTATION OF TINCAN STATE API
     * 
     * @return true if everything was OK, false if any error happened
     */
    public boolean sendPreferences() {
        boolean doneOK = false;
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



