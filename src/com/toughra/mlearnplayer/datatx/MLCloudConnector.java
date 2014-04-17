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
    
    /** The string to append to the server URL for sending preferences to cloud */
    public static String CLOUD_SETPREF_PATH="/umcloud/app/setPreference.xhtml";
    
    /** The string to append to the server URL for getting preferences from cloud as XML*/
    public static String CLOUD_GETPREF_PATH="/umcloud/app/getPreference.xhtml";
    
    /** The String to append to the server URL for log submission */
    public static String CLOUD_LOGSUBMIT_PATH="/umcloud/app/uploadLog.xhtml";
    
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
            EXEStrMgr.lg(124, "Exception getting connection to server going", e);
        }
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
            bytesToGo = (int)contentLength;
        }

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
            if(count == -1) {
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
        buf.append("Cache-control: no-cache\n");
        buf.append("Connection: Keep-Alive\n");
        buf.append("Accept-Encoding: gzip\n");
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
                EXEStrMgr.lg(125, "Something went wrong with cloud request", e);
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
            EXEStrMgr.lg(126, "Error attempting to close cloud sockets etc:" + eStr, lastE);
        }
    }
    
    public int checkLogin(String userID, String userPass) {
        int result = -1;
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            
            /*StringBuffer url = new StringBuffer(
                    MLearnPlayerMidlet.masterServer + CLOUD_LOGIN_PATH).append('?');
            appendCredentialsToURL(url, userID, userPass);*/
            
            StringBuffer url = new StringBuffer(
                    MLearnPlayerMidlet.masterServer + CLOUD_LOGIN_PATH);
            
            //Basic Authentication credential string.
            //Format: username:password
            String authString = userID+":"+userPass;
            String authEncBytes = Base64.encode(authString);
            String authStringEnc = "Basic " + authEncBytes;
            
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
    
    String getBoundaryString() {
        return BOUNDARY;
    }
    
    StringBuffer appendFormFieldStart(StringBuffer res, String paramName) {
        res.append("Content-Disposition: form-data; name=\"").append(paramName).append("\"\r\n")
                    .append("\r\n");
        return res;
    }
    
    StringBuffer appendFormFieldEnd(StringBuffer res, String boundary) {
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
    String getBoundaryMessage(String boundary, Hashtable params, String fileField, String fileName, String fileType) {
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
        /* DO NOTHING RIGHT NOW UNTIL TINCAN COMES
        openConnection();
        
        int retVal = -1;
        
        //check and see if we have no username and password, in which case, just return
        if(EXEStrMgr.getInstance().getCloudUser() == null) {
            return retVal;
        }
 
        try{
            params.put("userid", EXEStrMgr.getInstance().getCloudUser());
            params.put("password", EXEStrMgr.getInstance().getCloudPass());
            MLCloudFileRequest request = new MLCloudFileRequest(this, url, params, fileField, fileConURI, skipBytes);
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            Hashtable respHeaders = new Hashtable();
            int respCode = doRequest(request, bout, respHeaders);
            if(respCode == 200) {
                retVal = request.logBytesToSend;
            }
        }catch(Exception e) {
            System.err.println("bad whilst attempting to send file");
            e.printStackTrace();
        }
        
        return retVal;
        */
        return -1;
    }
    
    /**
     * Gets preferences from the cloud.  They are returned by the server in the 
     * form of:
     */
    public void getPreferences() {
        /* DISABLED UNTIL TINCAN FOR PREFERENCES COMES
        try {
            StringBuffer url = new StringBuffer(MLearnPlayerMidlet.masterServer)
                    .append(CLOUD_GETPREF_PATH).append('?');
            appendCredentialsToURL(url);
            String urlStr = url.toString();
            MLCloudRequest request = new MLCloudSimpleRequest(this, urlStr);
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
            EXEStrMgr.lg(122, "Exception attempting to set preferences from cloud"+  lastResponseCode, e);
        }
        */
    }
    
    /**
     * Simply append userid=..&password= to the url
     * @param url 
     */
    private void appendCredentialsToURL(StringBuffer url, String cloudUser, String cloudPass) {
        url.append("userid=").append(Util.encodeUrl(cloudUser)).append('&');
        url.append("password=").append(Util.encodeUrl(cloudPass)).append('&');
        url.append("ts=").append(System.currentTimeMillis());//avoid caching issues
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
        
        /* GO AWAY UNTIL TINCAN
        //if we are not really logged in then skip this for now...
        if(EXEStrMgr.getInstance().getPref(EXEStrMgr.KEY_CLOUDUSER) == null) {
            return false;
        }
        
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
            MLCloudRequest request = new MLCloudSimpleRequest(this, urlStr);
            Hashtable headers = new Hashtable();
            ByteArrayOutputStream respBytes = new ByteArrayOutputStream();
            int respCode = doRequest(request, respBytes, headers);
            if(respCode == 200) {
                doneOK = true;
                EXEStrMgr.getInstance().delPref(EXEStrMgr.KEY_REPLIST);
            }
        }catch(Exception e) {
            EXEStrMgr.lg(127, "Exception attempting to send preferences to cloud",e);
        }
        
        return doneOK;
        */
        return false;
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




/**
 * Simple HTTP get request wrapper
 * 
 * @author mike
 */
class MLCloudSimpleRequest implements MLCloudRequest  {
    
    byte[] reqBytes;
    int pos = 0;
    int reqBytesLen = 0;
    Hashtable customHeaders;

    /**
     * 
     * @param connector - MLCloudConnector object
     * @param url url to issue request
     * @param customHeaders Hashtable of headers as key-value for HTTP
     */
    MLCloudSimpleRequest(MLCloudConnector connector, String url, Hashtable customHeaders) {
        this.reqBytes = connector.getRequestHeader(url, HttpConnection.GET, customHeaders).getBytes();
        this.reqBytesLen = reqBytes.length;
        this.customHeaders = customHeaders;
    }
    
    /**
     * 
     * @param connector - MLCloudConnector object
     * @param url url to issue request
     */
    MLCloudSimpleRequest(MLCloudConnector connector, String url) {
        this(connector, url, null);
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
}

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
class MLCloudFileRequest  implements  MLCloudRequest{
    
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

