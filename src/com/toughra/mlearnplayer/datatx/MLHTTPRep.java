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
                EXEStrMgr.getInstance().swap(EXEStrMgr.SWAP_TOBUF);
                doSwap = true;
            }

            System.out.println("need to send: " + cFname);

            String fileURL = EXEStrMgr.getInstance().getPref("basefolder") +
                    "/" + cFname;

            try {
                EXEStrMgr.lg(25, "Attempt to send " + fileURL + " as " + cFname + " from " + alreadySent);
                long sizeSentTo = sendLog(fileURL, cFname, true, alreadySent);
                if(sizeSentTo > 0) {
                    repStatusHT.put(cFname, String.valueOf(sizeSentTo));
                }
            }catch(Exception e) {
                EXEStrMgr.lg(124, "Exception attempting to send log directly", e);
            }

            if(doSwap) {
                EXEStrMgr.getInstance().swap(EXEStrMgr.SWAP_TOFILE);
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
            EXEStrMgr.lg(25, "Starting to send own files");
            sendOwnLogs(pusher);
            EXEStrMgr.lg(25, "Finished sending own files");
            
            EXEStrMgr.lg(25, "Starting to look for files from other devices to replicate");
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

                        EXEStrMgr.lg(25, "Updated sent file " + sentFileURI);
                        EXEStrMgr.lg(25, "Sent " + bname);
                    }catch(IOException e) {
                        EXEStrMgr.lg(123, " Exception sending " + bname, e);
                    }
                }
            }
        }catch(Exception e) {
            EXEStrMgr.lg(125, "Exception sending logs ", e);
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
            EXEStrMgr.lg(26, "Already sent " + fileURI + " : " + alreadySent + " bytes");
            
            //check and see if the log was fully sent and is not for today... delete it
            String logBasename = fileURI.substring(fileURI.lastIndexOf('/') + 1);
            String logNameToday = EXEStrMgr.getInstance().sessionActivityLogName;
            if(!logBasename.equals(logNameToday)) {
                //time to delete it 
                FileConnection fConDel = null;
                try {
                    fConDel = (FileConnection)Connector.open(fileURI);
                    fConDel.delete();
                }catch(IOException e) {
                    e.printStackTrace();
                } finally {
                    fConDel.close();
                }
            }
            return STATUS_ALREADYSENT;
        }
        
        if(alreadySent >= 0) {
            EXEStrMgr.lg(27, "Already sent " + alreadySent + " bytes out of " + fileSize);
        }
        
        
        String url = MLearnPlayerMidlet.masterServer + MLCloudConnector.CLOUD_LOGSUBMIT_PATH;
        
        //if we are sending our own logs - rename this as the server expects it
        if(isOwnLog) {
            basename = EXEStrMgr.getInstance().getPref("uuid") + "-" + basename;
        }
        
        
        int logBytesSent = MLCloudConnector.getInstance().sendLogFile(url, params, "filecontent", 
                basename, "text/plain", fileURI, alreadySent);
        
        
        if(logBytesSent != -1) {
            return (alreadySent + logBytesSent);
        }else {
            EXEStrMgr.lg(126, "Could not send logs... ");
        }
        
        //this means that we did not get to a response - so something went wrong
        return -1;
    }
    
}


