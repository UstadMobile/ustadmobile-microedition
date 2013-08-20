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
import com.toughra.mlearnplayer.MLearnPlayerMidlet;
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
        String url = MLearnPlayerMidlet.masterServer + MLCloudConnector.CLOUD_LOGSUBMIT_PATH;
        
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

            System.out.println("need to send: " + cFname);

            String fileURL = EXEStrMgr.getInstance().getPref("basefolder") +
                    "/" + cFname;

            try {
                EXEStrMgr.po("Attempt to send " + fileURL + " as " + cFname + " from " + alreadySent, EXEStrMgr.DEBUG);
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
    
    /**
     * the main logic of the class - will pick up settings from the preferences
     * and look for any logs that need sent (e.g. modified since .sent file was
     * last modified)
     */
    public void sendLogs(MLObjectPusher pusher) {
        try {
            String url = MLearnPlayerMidlet.masterServer + MLCloudConnector.CLOUD_LOGSUBMIT_PATH;
            
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
        
        long fileSize = fcon.fileSize();
        fcon.close();
        
        if(alreadySent >= fileSize) {
            EXEStrMgr.po("Already sent " + fileURI + " : " + alreadySent + " bytes", EXEStrMgr.DEBUG);
            return STATUS_ALREADYSENT;
        }
        
        if(alreadySent >= 0) {
            EXEStrMgr.po("Already sent " + alreadySent + " bytes out of " + fileSize, EXEStrMgr.DEBUG);
        }
        
        
        String url = MLearnPlayerMidlet.masterServer + MLCloudConnector.CLOUD_LOGSUBMIT_PATH;
        
        //if we are sending our own logs - rename this as the server expects it
        if(isOwnLog) {
            basename = EXEStrMgr.getInstance().getPref("uuid") + "-" + basename;
        }
        
        byte[] response = MLCloudConnector.getInstance().sendLogFile(url, params, "filecontent", 
                basename, "text/plain", fileURI, alreadySent);
        
        
        if(response != null) {
            String responseStr = new String(response);
            int respCode = MLCloudConnector.getInstance().getLastResponseCode();
            EXEStrMgr.po("Cloud Server says : " + respCode + " : " + responseStr, EXEStrMgr.DEBUG);
            
            if(respCode == 200) {
                //everything OK - write a log of how much we have sent
                return fileSize;
            }
            
        }else {
            EXEStrMgr.po("Null came back ... ", EXEStrMgr.WARN);
        }
        
        //this means that we did not get to a response - so something went wrong
        return -1;
    }
    
}


