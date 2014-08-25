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

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import com.toughra.mlearnplayer.EXEStrMgr;
import com.toughra.mlearnplayer.MLearnPlayerMidlet;


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
     * @param pusher MLObjectPusher object that is used to figure out which files need sent up
     * @return Total bytes processed in this run
     */
    public long sendOwnLogs(MLObjectPusher pusher) {
        String url = MLearnPlayerMidlet.masterServer + MLCloudConnector.CLOUD_LOGSUBMIT_PATH;
        
        Vector selfRepList = pusher.checkRepFiles();
        Hashtable repStatusHT = pusher.getRepStatus();
        
        long totalSent = 0;

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
                long sizeSentTo = sendLog(fileURL, cFname, alreadySent);
                totalSent += (sizeSentTo - alreadySent);
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
        return totalSent;
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
     * @return 
     */
    public long sendLog(String fileURI, String basename, long alreadySent) throws Exception{
        FileConnection fcon = (FileConnection)Connector.open(fileURI, Connector.READ, true);
        
        Hashtable params = new Hashtable();
        
        
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


