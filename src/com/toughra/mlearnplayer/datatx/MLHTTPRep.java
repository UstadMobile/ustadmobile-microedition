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

import com.sun.lwuit.io.ConnectionRequest;
import com.sun.lwuit.io.NetworkManager;
import com.sun.lwuit.io.util.Util;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.file.FileConnection;
import com.toughra.mlearnplayer.EXEStrMgr;
import com.toughra.mlearnplayer.MLearnUtils;

/**
 * This class takes care of sending student logs collected from other devices
 * and uploading them to an http address as a file in the post field.
 * 
 * It is capable of partial uploads.  When a file is uploaded it will use
 * DataOutputStream to write an int value to a  file called (LogName).sent 
 * 
 * If a file has been modified after the most recent .sent file, then only the 
 * remaining data is transferred - the number of bytes specified in (LogName).sent
 * will be skipped.
 * 
 * Uses HttpMultipartRequest to actually do the HTTP Post operation
 * 
 * @author mike
 */
public class MLHTTPRep {
     
    /**
     * the main logic of the class - will pick up settings from the preferences
     * and look for any logs that need sent (e.g. modified since .sent file was
     * last modified)
     */
    public void sendLogs() {
        try {
            //make sure that the server knows that we exist
            checkCallHome();
            
            String url = EXEStrMgr.getInstance().getPref("httptx.url");
            //if the url is null it means no data transmission
            if(url == null) {
                return;
            }
            String logBaseDir =EXEStrMgr.getInstance().getPref("basefolder")
                    + "/logrx";
            FileConnection dirCon = (FileConnection)Connector.open(logBaseDir);
            Vector fileList = MLearnUtils.enumToVector(dirCon.list("*activity.log", true));
            dirCon.close();
                        
            //go through all the files in the list
            for(int i = 0; i < fileList.size(); i++) {
                String bname = fileList.elementAt(i).toString();
                try {
                    sendLog(logBaseDir + "/" + bname, bname);
                    EXEStrMgr.po("Sent " + bname, EXEStrMgr.DEBUG);
                }catch(IOException e) {
                    EXEStrMgr.po(e, " Exception sending " + bname);
                }
                
            }
        }catch(Exception e) {
            EXEStrMgr.po(e, "Exception sending logs ");
        }
    }
    
    /**
     * A callhome message is a utility so that you can see from an http server
     * if a phone sent out has been active or not.  Send a callhome every hour
     * whilst running
     * 
     */
    public void checkCallHome() {
        String calledHome = EXEStrMgr.getInstance().getPref("player.calledhome");
        long lastCallHomeTime = 0L;
        if(calledHome != null) {
            lastCallHomeTime = Long.parseLong(calledHome);
        }
        long timeNow = System.currentTimeMillis() / 1000;
        
        //has it been more than an hour since we called home?
        if((timeNow - lastCallHomeTime) > 3600) {
            //do the call home to make sure that the phone is talking before it goes out
            String url = EXEStrMgr.getInstance().getPref("httptx.url");
            
            Hashtable params = new Hashtable();
            String uuid = EXEStrMgr.getInstance().getPref("uuid");
            params.put("action", "callhome");
            params.put("uuid", uuid);
            params.put("learnername", EXEStrMgr.getInstance().getPref("learnername"));
            
            try {
                HttpMultipartRequest req = new HttpMultipartRequest(
                    url, params, "filecontent", uuid, "text/plain", null);
                byte[] response = req.send();
                String respStr  = new String(response);
                if(req.respCode == 200) {
                    long utime = System.currentTimeMillis() / 1000;
                    EXEStrMgr.getInstance().setPref(
                            "player.calledhome", String.valueOf(utime));
                }
            }catch(Exception e) {
                EXEStrMgr.po(e, "Error running callhome");
            }
            
        }
    }
    
    /**
     * Sends a log file to the server.  It will look up the student information
     * that is known about this user and set student_uuid and student_learnername
     * with the http request as post form parameters.  It will then use 
     * HttpMultipartRequest to send the log as an post file field
     * 
     * @param fileURI The complete URI to pass to Connector
     * @param basename The basename of the file which is used to set the filename in the HTTP request
     * @throws Exception if something goes wrong
     */
    public void sendLog(String fileURI, String basename) throws Exception{
        FileConnection fcon = (FileConnection)Connector.open(fileURI, Connector.READ, true);
        String sentFileURI = fileURI + ".sent";
        int alreadySent = MLearnUtils.getIntFromFile(sentFileURI);
        Hashtable params = new Hashtable();
        
        String studentUUID = basename.substring(0, basename.indexOf('-'));
        Hashtable stdNames = MLServerThread.getInstance().getStudentNames();
        String studentName = stdNames.get(studentUUID) != null ? 
                stdNames.get(studentUUID).toString() : "unknown";
        params.put("student_uuid", studentUUID);
        params.put("student_learnername", studentName);
        
        String dataUsername = EXEStrMgr.getInstance().getPref("httptx.username");
        String dataPass = EXEStrMgr.getInstance().getPref("httptx.password");
        
        params.put("txuser", dataUsername);
        params.put("txpass", dataPass);
        
        int fileSize = (int)fcon.fileSize();
        fcon.close();
        
        if(alreadySent >= fileSize) {
            EXEStrMgr.po("Already sent " + fileURI + " : " + alreadySent + " bytes", EXEStrMgr.DEBUG);
            return;
        }
        
        
        String url = EXEStrMgr.getInstance().getPref("httptx.url");
        HttpMultipartRequest req = new HttpMultipartRequest(
                url, params, "filecontent", basename, "text/plain", fileURI);
        
        //used for partial uploads
        if(alreadySent > 0) {
            req.skipBytes = alreadySent;
        }
        
        byte[] response = req.send();
        if(response != null) {
            String responseStr = new String(response);
            int respCode = req.respCode;
            
            if(respCode == 200) {
                //everything OK - write a log of how much we have sent
                MLearnUtils.writeIntToFile(fileSize, sentFileURI);
                EXEStrMgr.po("Updated sent file " + sentFileURI, EXEStrMgr.DEBUG);
            }
            
            EXEStrMgr.po("Server says : " + respCode + " : " + responseStr, EXEStrMgr.DEBUG);
        }else {
            EXEStrMgr.po("Null came back ... ", EXEStrMgr.WARN);
        }
    }
    
}
