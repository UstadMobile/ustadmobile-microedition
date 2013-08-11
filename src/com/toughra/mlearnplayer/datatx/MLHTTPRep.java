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
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.file.FileConnection;
import com.toughra.mlearnplayer.EXEStrMgr;
import com.toughra.mlearnplayer.MLearnUtils;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

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
     
    /** Used to access the replication status of our own logs*/
    protected Hashtable repStatus;
    
    /** Return from sendLog indicates all data was sent already - nothing more to send*/
    public static final int STATUS_ALREADYSENT = 0;
    
    /** Return from send log indicating there was an error */
    public static final int STATUS_SEND_ERROR = -1;
    
    /**
     * This function shall send our own logs (e.g. those from this phone)
     * over the http connection (e.g. instead of using the bluetooth replication
     * functionality)
     * 
     */
    public void sendOwnLogs(MLObjectPusher pusher) {
        String logSendMethod = EXEStrMgr.getInstance().getPref("logsend.method");
        if(logSendMethod != null && logSendMethod.equals("http")) {
            String url = EXEStrMgr.getInstance().getPref("httptx.url");
            Vector selfRepList = pusher.checkRepFiles();
            Hashtable repStatusHT = pusher.getRepStatus();
            
            for(int i = 0; i < selfRepList.size(); i++) {
                String cFname = selfRepList.elementAt(i).toString();
                
                long alreadySent = pusher.getReplicationSent(repStatusHT, cFname);
                boolean doSwap = false;
                if(EXEStrMgr.getInstance().logFileOpen(cFname)) {
                    EXEStrMgr.getInstance().l(null, null, EXEStrMgr.SWAP_TOBUF);
                    doSwap = true;
                }
                
                System.out.println("need to send: ");
                
                String fileURL = EXEStrMgr.getInstance().getPref("basefolder") +
                        "/" + cFname;
                
                try {
                    long sizeSentTo = sendLog(fileURL, cFname, true, alreadySent);
                    if(sizeSentTo > 0) {
                        repStatusHT.put(cFname, String.valueOf(sizeSentTo));
                    }
                }catch(Exception e) {
                    EXEStrMgr.po(e, "Exception attempting to send log directly");
                }
                
                if(doSwap) {
                    EXEStrMgr.getInstance().l(null, null, EXEStrMgr.SWAP_TOFILE);
                }
                
            }
            
            pusher.saveRepStatus(repStatusHT);
        }
    }
    
    /**
     * the main logic of the class - will pick up settings from the preferences
     * and look for any logs that need sent (e.g. modified since .sent file was
     * last modified)
     */
    public void sendLogs(MLObjectPusher pusher) {
        try {
            //make sure that the server knows that we exist
            checkCallHome();
            
            String url = EXEStrMgr.getInstance().getPref("httptx.url");
            //if the url is null it means no data transmission
            if(url == null) {
                return;
            }
            
            //if we are set to do so - send our own logs directly to the server.
            EXEStrMgr.po("Starting to send own files", EXEStrMgr.DEBUG);
            sendOwnLogs(pusher);
            EXEStrMgr.po("Finished sending own files", EXEStrMgr.DEBUG);
            
            EXEStrMgr.po("Starting to look for files from other devices to replicate", EXEStrMgr.DEBUG);
            String logBaseDir =EXEStrMgr.getInstance().getPref("basefolder")
                    + "/logrx";
            FileConnection dirCon = (FileConnection)Connector.open(logBaseDir);
            
            if(dirCon.isDirectory()) {
                Vector fileList = MLearnUtils.enumToVector(dirCon.list("*activity.log", true));
                dirCon.close();

                //go through all the files in the list
                for(int i = 0; i < fileList.size(); i++) {
                    String bname = fileList.elementAt(i).toString();
                    try {
                        String fileURI = logBaseDir + "/" + bname;
                        String sentFileURI = fileURI + ".sent";
                        long alreadySent = MLearnUtils.getIntFromFile(sentFileURI);

                        long fileSizeSent = sendLog(fileURI, bname, false, alreadySent);

                        //TODO: potential for trouble as we are writing an int - this should be long
                        MLearnUtils.writeIntToFile((int)fileSizeSent, sentFileURI);

                        EXEStrMgr.po("Updated sent file " + sentFileURI, EXEStrMgr.DEBUG);
                        EXEStrMgr.po("Sent " + bname, EXEStrMgr.DEBUG);
                    }catch(IOException e) {
                        EXEStrMgr.po(e, " Exception sending " + bname);
                    }
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
        EXEStrMgr.po("Doing check call home", EXEStrMgr.DEBUG);
        if(calledHome != null) {
            lastCallHomeTime = Long.parseLong(calledHome);
        }
        long timeNow = System.currentTimeMillis() / 1000;
        
        //has it been more than an hour since we called home?
        long timeDiff = (timeNow - lastCallHomeTime);
        
        //just in case the date was set wrong
        if(timeDiff > 600 || timeDiff < 0) {
            //do the call home to make sure that the phone is talking before it goes out
            String uuid = EXEStrMgr.getInstance().getPref("uuid");
            String learnerName = EXEStrMgr.getInstance().getPref("learnername");
            EXEStrMgr.po("Preparing call home for " + uuid + " / " + learnerName,
                    EXEStrMgr.DEBUG);
            
            String url = EXEStrMgr.getInstance().getPref("httptx.url");
            
            Hashtable params = new Hashtable();
            
            params.put("action", "callhome");
            params.put("uuid", uuid);
            params.put("learnername", learnerName);
            
            HttpConnection hc = null;
            InputStream din = null;
            
            //TODO: Encode these values
            String requestStr = "action=callhome&uuid=" + uuid + "&learnername=" 
                    + Util.encodeUrl(learnerName) 
                    + "&utime=" + String.valueOf(System.currentTimeMillis());
            EXEStrMgr.po("Preparing call to with arg " + requestStr,
                    EXEStrMgr.DEBUG);
            try {
                String fullURL = url + "?" + requestStr;
                hc = (HttpConnection)Connector.open(fullURL);
                hc.setRequestMethod(HttpConnection.GET);
                
                din = hc.openInputStream();
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                int b;
                while((b = din.read()) != -1) {
                    bout.write(b);
                }
                din.close();
                din = null;
                
                bout.flush();
                String responseStr = new String(bout.toByteArray());
                bout.close();
                bout = null;
                
                if(hc.getResponseCode() == 200) {
                    long utime = System.currentTimeMillis()/1000;
                    EXEStrMgr.getInstance().setPref(
                            "player.calledhome", String.valueOf(utime));
                    EXEStrMgr.po("Sent callhome : got response: " + responseStr, 
                            EXEStrMgr.DEBUG);
                }else {
                    EXEStrMgr.po("Callhome error: " + hc.getResponseCode() + ": " +
                            responseStr, EXEStrMgr.DEBUG);
                }
            }catch(Exception e) {
                EXEStrMgr.po(e, "Error running callhome");
            }finally {
                try {
                    if(din != null) din.close();
                    if(hc != null) hc.close();
                }catch(IOException e) {
                    EXEStrMgr.po(e, "Error ending/closing callhome");
                }
            }
            
        }
        
        EXEStrMgr.po("Finished check call home", EXEStrMgr.DEBUG);
    }
    
    /**
     * Sends a log file to the server.  It will look up the student information
     * that is known about this user and set student_uuid and student_learnername
     * with the http request as post form parameters.  It will then use 
     * HttpMultipartRequest to send the log as an post file field
     * 
     * @param fileURI The complete URI to pass to Connector
     * @param basename The basename of the file which is used to set the filename in the HTTP request
     * @param alreadySent - the number of bytes already sent
     * @throws Exception if something goes wrong
     */
    public long sendLog(String fileURI, String basename, boolean isOwnLog, long alreadySent) throws Exception{
        FileConnection fcon = (FileConnection)Connector.open(fileURI, Connector.READ, true);
        
        
        
        Hashtable params = new Hashtable();
        
        String studentUUID = null;
        if(isOwnLog) {
            studentUUID = EXEStrMgr.getInstance().getPref("uuid");
        }else {
            studentUUID = basename.substring(0, basename.indexOf('-'));
        }
        
        if(isOwnLog) {
            params.put("student_uuid", EXEStrMgr.getInstance().getPref("uuid"));
            params.put("student_learnername", EXEStrMgr.getInstance().getPref("learnername"));
        }else {
            Hashtable stdNames = MLServerThread.getInstance().getStudentNames();
            String studentName = stdNames.get(studentUUID) != null ? 
                    stdNames.get(studentUUID).toString() : "unknown";
            params.put("student_uuid", studentUUID);
            params.put("student_learnername", studentName);
        }
        
        String dataUsername = EXEStrMgr.getInstance().getPref("httptx.username");
        String dataPass = EXEStrMgr.getInstance().getPref("httptx.password");
        
        params.put("txuser", dataUsername);
        params.put("txpass", dataPass);
        
        long fileSize = fcon.fileSize();
        fcon.close();
        
        if(alreadySent >= fileSize) {
            EXEStrMgr.po("Already sent " + fileURI + " : " + alreadySent + " bytes", EXEStrMgr.DEBUG);
            return STATUS_ALREADYSENT;
        }
        
        if(alreadySent >= 0) {
            EXEStrMgr.po("Already sent " + alreadySent + " bytes out of " + fileSize, EXEStrMgr.DEBUG);
        }
        
        
        String url = EXEStrMgr.getInstance().getPref("httptx.url");
        
        //if we are sending our own logs - rename this as the server expects it
        if(isOwnLog) {
            basename = EXEStrMgr.getInstance().getPref("uuid") + "-" + basename;
        }
        
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
                return fileSize;
            }
            
            EXEStrMgr.po("Server says : " + respCode + " : " + responseStr, EXEStrMgr.DEBUG);
        }else {
            EXEStrMgr.po("Null came back ... ", EXEStrMgr.WARN);
        }
        
        //this means that we did not get to a response - so something went wrong
        return -1;
    }
    
}
