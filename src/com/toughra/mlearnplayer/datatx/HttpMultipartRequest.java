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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
 
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.file.FileConnection;
import com.toughra.mlearnplayer.EXEStrMgr;

/**
 * Utility class that will send a file attachment as an http form multipart request
 * 
 * Usage:
 * 
 * 1. Instantiate the class
 * 2. Call the send() method
 * 
 * Taken from http://www.developer.nokia.com/Community/Wiki/HTTP_Post_multipart_file_upload_in_Java_ME
 * 
 * @author mike
 */
public class HttpMultipartRequest
{
	static final String BOUNDARY = "----------V2ymHFg03ehbqgZCaKO6jy";
 
	//byte[] postBytes = null;
	String url = null;
        
        /**HTTP Response code from the Server - e.g. 200 for OK*/
        int respCode = -1;
        
        /**The URI of the file - will use FileConnection to get it's contents to send*/
        String fileConURI;
        
        /** Boundary message for http conversation*/
        String boundaryMessage;
        
        /** end of boundary message for http conversation*/
        String endBoundary;
        
        /** Bytes to skip from file (e.g. partial upload) */
        public long skipBytes = 0L;
 
        /**
         * 
         * @param url URL to post to
         * @param params HTTP fields to send as parameters to be accessed by script as key/value pairs
         * @param fileField The HTTP field to use for sending the file
         * @param fileName The name of the file to send as it will be presented to the server
         * @param fileType The mime type of the file as it will be presented to the server
         * @param fileConURI the URI of the file for FileConnection to get it's contents
         * @throws Exception 
         */
	public HttpMultipartRequest(String url, Hashtable params, String fileField, String fileName, String fileType, String fileConURI) throws Exception
	{
		this.url = url;
 
		String boundary = getBoundaryString();
 
		boundaryMessage = getBoundaryMessage(boundary, params, fileField, fileName, fileType);
 
		endBoundary = "\r\n--" + boundary + "--\r\n";
                
                this.fileConURI = fileConURI;
	}
 
        /**
         * Getter for boundary string
         * 
         * @return 
         */
	String getBoundaryString()
	{
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
	String getBoundaryMessage(String boundary, Hashtable params, String fileField, String fileName, String fileType)
	{
		StringBuffer res = new StringBuffer("--").append(boundary).append("\r\n");
 
		Enumeration keys = params.keys();
 
		while(keys.hasMoreElements())
		{
			String key = (String)keys.nextElement();
			String value = (String)params.get(key);
 
			res.append("Content-Disposition: form-data; name=\"").append(key).append("\"\r\n")    
				.append("\r\n").append(value).append("\r\n")
				.append("--").append(boundary).append("\r\n");
		}
		res.append("Content-Disposition: form-data; name=\"").append(fileField).append("\"; filename=\"").append(fileName).append("\"\r\n") 
			.append("Content-Type: ").append(fileType).append("\r\n\r\n");
 
		return res.toString();
	}
 
        /**
         * Send the message
         * 
         * @return The response of the server to the request
         * 
         * @throws Exception 
         */
	public byte[] send() throws Exception
	{
		HttpConnection hc = null;
                FileConnection fcon = null;
 		InputStream is = null;
 		ByteArrayOutputStream bos = new ByteArrayOutputStream();
                InputStream fin = null;
 
		byte[] res = null;
 
		try
		{
                    EXEStrMgr.po("Opening Connection to " + url, EXEStrMgr.DEBUG);
                    
                    hc = (HttpConnection) Connector.open(url, Connector.READ_WRITE);
                    
                    hc.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + getBoundaryString());
                    hc.setRequestMethod(HttpConnection.POST);
                    
                    //hc.setRequestProperty("Content-Length", ""+postBytes.length);
                    //hc.setRequestProperty("User-Agent", "UstadMobile/0.1");
                    
                    //ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    
                    
                    OutputStream dout = hc.openOutputStream();
                    dout.write(boundaryMessage.getBytes());
                    if(fileConURI != null) {
                        fcon = (FileConnection)Connector.open(fileConURI);
                        fin = fcon.openInputStream();
                    }else {
                        String msg = "No File Loaded";
                        fin = new ByteArrayInputStream(msg.getBytes());
                    }
                    
                    int b;
                    
                    if(this.skipBytes > 0) {
                        EXEStrMgr.po("HTTP Rep skipping " + skipBytes + " bytes already sent", EXEStrMgr.DEBUG);
                        fin.skip(skipBytes);
                    }
                    
                    while((b = fin.read()) != -1) {
                        dout.write(b);
                    }
                    fin.close();
                    if(fcon != null) {
                        fcon.close();
                    }
                    
                    dout.write(endBoundary.getBytes());
 
                    dout.flush();
                    dout.close();
                    
                    this.respCode = hc.getResponseCode();
                    
                    int ch;
                    is = hc.openInputStream();
                    
                    while ((ch = is.read()) != -1)
                    {
                            bos.write(ch);
                    }
                    
                    res = bos.toByteArray();
		}
		catch(Exception e)
		{
                    EXEStrMgr.po(e, "Exception sending http data");
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if(bos != null)
					bos.close();
 
				if(is != null)
					is.close();
 
				if(hc != null)
					hc.close();
			}
			catch(Exception e2)
			{
                            EXEStrMgr.po(e2, " exception closing stuff");
				e2.printStackTrace();
			}
		}
		return res;
	}
}