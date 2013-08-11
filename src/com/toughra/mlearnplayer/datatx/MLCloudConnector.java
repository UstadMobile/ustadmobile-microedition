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
import javax.microedition.io.*;
import java.io.*;
import java.util.Hashtable;

/**
 *
 * @author mike
 */
public class MLCloudConnector {
    
    private static MLCloudConnector instance;
    
    /** Socket connection to the server*/
    private SocketConnection  conn;
    
    private InputStream in;
    
    private OutputStream out;
    
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
            byte[] buf = new byte[1024];
            int contentLength = -1;
            
            ByteArrayOutputStream headerStream = new ByteArrayOutputStream(800);
            byte numCRs = 0;
            byte numLFs = 0;
            int bytesToGo = 1;
            while (bytesToGo > 0) {
                int count = in.read(buf);
                int headerCount = 0;
                
                if (count == -1) {
                    // end-of-stream
                    break;
                }
                if (headerStream != null) {
                    for (int i = 0; i < count; i++) {
                        headerCount++;
                        if (buf[i] == '\n') {
                            numLFs++;
                            if (numLFs == 2 && numCRs == 2) {
                                // end of HTTP 1.1 header (ascii)
                                String hdrText = new String(headerStream.toByteArray());
                                responseStatus = Integer.parseInt(hdrText.substring(9, 12));
                                int clPos = hdrText.toLowerCase().indexOf("content-length: ");
                                if (clPos == -1) {
                                    // reading till the end of stream
                                    bytesToGo = Integer.MAX_VALUE;
                                } else {
                                    contentLength = Integer.parseInt(
                                            hdrText.substring(clPos + 16,
                                            hdrText.indexOf("\r\n", clPos + 17)));
                                    bytesToGo = contentLength - (count - i - 1);
                                }

                                EXEStrMgr.po("Got header, receiving body...", EXEStrMgr.DEBUG);
                                headerStream = null;
                                
                                //write the remainder to the main output stream
                                out.write(buf, headerCount, count - headerCount);
                                
                                break;
                            }
                        } else if (buf[i] == '\r') {
                            numCRs++;
                        } else {
                            numLFs = 0;
                            numCRs = 0;
                        }
                        headerStream.write(buf[i]);
                    }
                }else {
                    out.write(buf);
                    bytesToGo -= count;
                    if (contentLength != -1) {
                        EXEStrMgr.po("Loading body, " + bytesToGo
                                + " more byte(s) to receive", EXEStrMgr.DEBUG);
                    }
                }
            }
            out.flush();
        }catch(Exception e) {
            EXEStrMgr.po(e, "bad whilst attempting to read from cloud connection");
        }
        
        
        
        return responseStatus;
    }
    
    /**
     * Makes an HTTP 1.1 Request header
     * 
     */ 
    private String getRequestHeader(String targetURL) {
        StringBuffer buf = new StringBuffer(512);
        buf.append("GET " + targetURL + " HTTP/1.1\n");
        // extracting the target host and port number
        int portDelPos = targetURL.indexOf(':');
        int firstSlashPos = targetURL.indexOf('/');
        String targetHost = targetURL;
        String targetPort = "80";
        if (portDelPos != -1 && portDelPos < firstSlashPos) {
            // has port specified
            targetHost = targetURL.substring(0, portDelPos);
            if (firstSlashPos == -1) {
                targetPort = targetURL.substring(portDelPos + 1);
            } else {
                targetPort = targetURL.substring(portDelPos + 1, firstSlashPos);
            }
        } else {
            // no port - using default
            if (firstSlashPos != -1) {
                targetHost = targetURL.substring(0, firstSlashPos);
            }
        }
        buf.append("Host: ");
        buf.append(targetHost);
        buf.append(':');
        buf.append(targetPort);
        buf.append('\n');
        buf.append("Accept: */*\n");
        buf.append("Cache-control: no-transform\n");
        // if you want keep-alive - remove the following line
        //buf.append("Connection: close\n");
        buf.append("User-Agent: MIDP2.0\n");
        buf.append('\n');

        return buf.toString();
    }

    
    public int checkLogin(String userID, String userPass) {
        int result = -1;
        try {
            String url = "http://" + MLearnPlayerMidlet.masterServer + "/login.php";
            openConnection();
            EXEStrMgr.po("MLCloudConnect: Connection opened", EXEStrMgr.DEBUG);
            out.write(getRequestHeader(url).getBytes());
            EXEStrMgr.po("Connected OK - request sent", EXEStrMgr.DEBUG);
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            result = readResponse(bout, new Hashtable());
            String serverSays = new String(bout.toByteArray());
            EXEStrMgr.po("Server says " + serverSays, EXEStrMgr.DEBUG);
        }catch(Exception e) {
            EXEStrMgr.po(e, "Something bad with checklogin");
        }
        
        return result;
    }
    
}
