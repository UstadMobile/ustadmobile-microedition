/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.paiwastoon.mlearnplayer.datatx;

//Taken from http://www.developer.nokia.com/Community/Wiki/HTTP_Post_multipart_file_upload_in_Java_ME

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
 
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.file.FileConnection;
import net.paiwastoon.mlearnplayer.EXEStrMgr;
 
public class HttpMultipartRequest
{
	static final String BOUNDARY = "----------V2ymHFg03ehbqgZCaKO6jy";
 
	//byte[] postBytes = null;
	String url = null;
        
        int respCode = -1;
        
        //this is the file that will be read for the file field
        String fileConURI;
        
        String boundaryMessage;
        
        String endBoundary;
        
        //if we need to skip bytes that were already sent
        public long skipBytes = 0L;
 
	public HttpMultipartRequest(String url, Hashtable params, String fileField, String fileName, String fileType, String fileConURI) throws Exception
	{
		this.url = url;
 
		String boundary = getBoundaryString();
 
		boundaryMessage = getBoundaryMessage(boundary, params, fileField, fileName, fileType);
 
		endBoundary = "\r\n--" + boundary + "--\r\n";
                
                this.fileConURI = fileConURI;
	}
 
	String getBoundaryString()
	{
		return BOUNDARY;
	}
 
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
 
                
                    /*
                    bos.write(boundaryMessage.getBytes());
                    bos.write(fileBytes);
                    bos.write(endBoundary.getBytes());
 
		this.postBytes = bos.toByteArray();
                *                     dout.write(postBytes);

                    */
                    
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