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
import javax.microedition.io.*;
import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;
import javax.microedition.io.file.FileConnection;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import com.toughra.mlearnplayer.EXEStrMgr;
import com.toughra.mlearnplayer.MLearnPreferences;

/**
 *
 * This class is used to push activity logs over Bluetooth to the teacher's phone.
 * 
 * It will save replication status to make sure that if logs are updated only
 * the remaining part of the log will be transmitted (e.g. no duplication)
 * 
 * @author mike
 */
public class MLObjectPusher extends Thread{
    
    /**
     * Empty constructor
     */
    public MLObjectPusher() {
        Random r = new Random(System.currentTimeMillis());
        countDown = r.nextInt(REPDELAY);
    }
    
    /**
     * Hashtable in the form of :
     * log base name (e.g. studentid-date-activity.log) -> Number bytes sent
     */
    Hashtable repStatus;
    
    /** The delay in ms after which to try to replicate (default: 5mins) */
    public static final int REPDELAY = 300000;
    
    /** Thread sleep tick time in ms */
    public static final int TICK = 1000;
    
    /** The countDown tracker, when this reaches zero, the replication attempt will run*/
    public static int countDown = REPDELAY;
    
    /** true if enabled and replication runs (e.g. there is a teachers phone) false otherwise*/
    public static boolean enabled = true;
    
    /**
     * Main thread method that will call checkRepFiles to do the replication and 
     * sendData
     */
    public void run() {
        while(enabled) {
            countDown -= TICK;
            if(countDown <= 0) {
                //String logSendMethod = EXEStrMgr.getInstance().getPref("logsend.method");
                
                //see if we need to send logs over bluetooth...
                
                /* This is no longer really supported for the moment
                if(logSendMethod == null && logSendMethod.equals("bluetooth")) {
                   String conURL = EXEStrMgr.getInstance().getPref("server.bt.url");
                    if(conURL != null) {
                        checkRepFiles();
                        sendData(conURL);
                    }
                }*/
                
                //check and see if we need to send updated preferences
                if(EXEStrMgr.getInstance().getCloudUser() != null) {
                    MLCloudConnector.getInstance().sendPreferences();

                    //also - send the logs through if that has been set
                    new MLHTTPRep().sendLogs(this);
                }
                
                
                countDown = REPDELAY;
            }
            
            try { Thread.sleep(TICK); }
            catch(InterruptedException e) {}
        }
    }
    
    /**
     * Returns the replication status hashtable
     * 
     * @return replication status hashtable mapped file base name -&gt; to
     * number of bytes sent successfully so far.
     */
    public Hashtable getRepStatus() {
        Hashtable ht = null;
        //check and see if it exists
        String fileName = EXEStrMgr.getInstance().getPref("basefolder")  
                + "/repstatus.ht";
        
        FileConnection fCon = null;
        try {
            fCon = (FileConnection)Connector.open(fileName);
            if(fCon.exists()) {
                ByteArrayOutputStream bout = new ByteArrayOutputStream((int)fCon.fileSize());
                InputStream fin = fCon.openInputStream();
                Util.copy(fin, bout);
                fin.close();
                ht = MLearnPreferences.fromByteArray(bout.toByteArray());
            }else  {
                ht = new Hashtable();
            }
        }catch(Exception e) {
            EXEStrMgr.lg(316, "Error trying to get rep status", e);
        }finally {
            if(fCon != null) {
                try { fCon.close(); }
                catch(Exception e){}
            }
        }
        return ht;
    }
    
    /**
     * Save replication status - put the hashtable into a file called repstatus.ht
     * 
     * @param ht 
     */
    public void saveRepStatus(Hashtable ht)  {
        FileConnection fCon = null;
        String fileName = EXEStrMgr.getInstance().getPref("basefolder")  
                + "/repstatus.ht";
        EXEStrMgr.lg(28, "Attempting to save rep status to " + fileName);
        try {
            fCon = (FileConnection)Connector.open(fileName);
            if(!fCon.exists()) {
                fCon.create();
            }
            OutputStream out = fCon.openOutputStream();
            out.write(MLearnPreferences.toByteArray(ht));
            out.close();
            fCon.close();
        }catch(IOException e) {
            EXEStrMgr.lg(321, "Exception saving rep status ",e);
        }finally {
            if(fCon != null) {
                try { fCon.close(); }
                catch(Exception e) {}
            }
        }
    }
    
    /**
     * Make a vector list of those files that need to be replicated (e.g. all
     * -activity.log) files.
     * 
     * @return 
     */
    public Vector checkRepFiles() {
        //check files to be replicated
        EXEStrMgr.lg(29, "checkRepFiles() looking for logs to send ");
        
        
        String baseDir = EXEStrMgr.getInstance().getPref("basefolder");
        Vector repList = new Vector();
        
        try {
            
            FileConnection dirCon = (FileConnection)Connector.open(baseDir);
            Enumeration e= dirCon.list();
            while(e.hasMoreElements()) {
                String name = e.nextElement().toString();
                EXEStrMgr.lg(30, "checkRepFiles() found file : " + name);
                if(name.endsWith("-activity.log")) {
                    repList.addElement(name);
                    EXEStrMgr.lg(30, "Should replicate "+ name);
                }
            }
            
            dirCon.close();
        }catch(Exception e) {
            EXEStrMgr.lg(317, "Exception prepping to replicate files", e);
        }
        return repList;
    }
    
    /**
     * Send a hashtable of student info over bluetooth
     */
    public void sendStudentInfoOverBt(Hashtable info, ClientSession cs, HeaderSet hs) throws IOException{
        byte[] htBytes = MLearnPreferences.toByteArray(info);
        
        hs.setHeader(HeaderSet.NAME, "learnerdata");
        hs.setHeader(HeaderSet.TYPE, "text/plain");
        hs.setHeader(HeaderSet.LENGTH, new Long(htBytes.length));
        hs.setHeader(MLServerThread.HEADER_UUID, EXEStrMgr.getInstance().getPref("uuid"));
        hs.setHeader(MLServerThread.HEADER_OPNAME, "learnerdata");

        Operation op = cs.put(hs);
        
        EXEStrMgr.lg(31, "Starting put process for student data");
        OutputStream out = op.openOutputStream();
        
        EXEStrMgr.lg(31, "Opened output stream - sending");
        out.write(htBytes);
        out.close();
        op.close();
        EXEStrMgr.lg(31, "Sent student data hashtable");
    }
    
    /**
     * Sets the correct learnername and passes to the overloaded method
     * 
     * @param cs ClientSession of active session
     * @param hs HeaderSet of active session
     * @throws IOException 
     */
    public void sendStudentInfoOverBt(ClientSession cs, HeaderSet hs) throws IOException{
        Hashtable ht = new Hashtable();
        ht.put("learnername", EXEStrMgr.getInstance().getPref("learnername"));
        sendStudentInfoOverBt(ht, cs, hs);
    }
    
    /**
     * Sends a file over the Bluetooth connection to the server
     * @param fileName File URI as it will be passed to Connector.open
     * @param cs current ClientSession
     * @param hs current HeaderSet
     * @param alreadySent num bytes already sent from this file to skip.  If alreadySent &gt;= file size - do nothing.
     * 
     * @throws IOException 
     */
    public void sendFileOverBT(String fileName, ClientSession cs, HeaderSet hs, long alreadySent) throws IOException{            
        EXEStrMgr.lg(32, "Attempting to open for reading " + fileName);

        FileConnection fCon = (FileConnection)Connector.open(fileName, Connector.READ);

        //check already sent
        if(alreadySent > 0) {
            long fSize = fCon.fileSize();
            if(alreadySent >= fSize) {
                //it's already there...
                EXEStrMgr.lg(33, fileName + " has already been sent actually");
                fCon.close();
                return;
            }
        }
        
        long startPoint = (alreadySent >= 0) ? alreadySent : 0;

        InputStream in = fCon.openInputStream();
        if(startPoint > 0) {
            long skipped = in.skip(startPoint);
            EXEStrMgr.lg(34, "Skipped " + skipped + " bytes already sent");
        }


        //byte[] toSend = logBytes; //strToSend.getBytes();
        long fileLen = fCon.fileSize();
        Long lengthObj = new Long(fileLen);

        EXEStrMgr.lg(35, "Sending " + lengthObj.toString() + " bytes ");

        String fileBaseName = fileName.substring(fileName.lastIndexOf('/')+1);

        //Set heads which are recognized by MLServerThread
        hs.setHeader(HeaderSet.NAME, fileBaseName);
        hs.setHeader(HeaderSet.TYPE, "text/plain");
        hs.setHeader(HeaderSet.LENGTH, lengthObj);
        hs.setHeader(MLServerThread.HEADER_OPNAME, "logxmit");
        hs.setHeader(MLServerThread.HEADER_STUDENTNAME, EXEStrMgr.getInstance().getPref("learnername"));
        hs.setHeader(MLServerThread.HEADER_UUID, EXEStrMgr.getInstance().getPref("uuid"));

        Operation op = cs.put(hs);
        EXEStrMgr.lg(35, "Starting put process");
        OutputStream out = op.openOutputStream();
        EXEStrMgr.lg(35, "Opened output stream - sending");

        Util.copy(in, out);

        in.close();
        fCon.close();

        EXEStrMgr.lg(35, "File Sent 4.1 ");

        out.close();
        EXEStrMgr.lg(35, "Closed out stream");

        op.close();
        EXEStrMgr.lg(35, "Closed op");

        //if we get here without any exception - we have been successful (hoorah)
        repStatus.put(fileBaseName, String.valueOf(fileLen));
        EXEStrMgr.lg(35, "Saved repstatus " + fileBaseName + "sent " + fileLen);
    }
    
    /**
     * Given the filename of the activity log that you want to check - sees 
     * how much of that has been sent
     * 
     * @param repStatus Hashtable mapped size -> Long object
     * @param cFname filename
     * 
     * @return size in bytes of replication data already sent
     */
    public long getReplicationSent(Hashtable repStatus, String cFname) {
        Object sentSizeObj = repStatus.get(cFname);
        long alreadySent = 0;
        if(sentSizeObj != null) {
            alreadySent = Long.parseLong(sentSizeObj.toString());
        }
        return alreadySent;    
    }
    
    /**
     * Sends the log data files the Bluetooth connection - Look over all the
     * log files - see which ones have new data that has to be sent, and send those
     * to the server.
     * 
     * If a log file is currently in use and needs replicated the logging instance
     * will be told to switch to writing it in memory until the transfer operation
     * completes.
     * 
     * @param conURL The Bluetooth server URL (normally saved in the preferences from the discovery process)
     */
    public void sendData(String conURL) {
        
        String baseNameReopen = null;
        Connection con = null;
        
        try {
            EXEStrMgr.lg(35, "Attempting to open for put: " + conURL);
            con = (Connection)Connector.open(conURL);
            EXEStrMgr.lg(35, "Established connection to " + conURL);
            
            
            ClientSession cs = (ClientSession)con;
            HeaderSet hs = cs.createHeaderSet();
            EXEStrMgr.lg(35, "Created header set ");
            
            cs.connect(hs);
            
            Vector repList = checkRepFiles();
            int numFiles = repList.size();
            
            //open the rpelication status hashtable
            EXEStrMgr.lg(35, "Attempting to get rep status ");
            repStatus = getRepStatus();
            EXEStrMgr.lg(35, "Opened rep status ");
            
            //send the learnername etc. over the connection
            try {
                sendStudentInfoOverBt(cs, hs);
            }catch(IOException e) {
                EXEStrMgr.lg(127, "Exception sending student info ",e);
            }
            
            //go through all the files 
            for(int i = 0; i < numFiles; i++) {
                String cFname = repList.elementAt(i).toString();
                
                boolean doSwap = false;
                if(EXEStrMgr.getInstance().logFileOpen(cFname)) {
                    EXEStrMgr.getInstance().swap(EXEStrMgr.SWAP_TOBUF);
                    doSwap = true;
                }
                
                
                EXEStrMgr.lg(35,"Checking status for " + cFname);
                //check rep status
                long alreadySent = getReplicationSent(repStatus, cFname);
                
                
                EXEStrMgr.lg(35, "Found we have sent " + alreadySent + " for " + cFname);

                String fileURL = EXEStrMgr.getInstance().getPref("basefolder") +
                        "/" + cFname;
    
                try {
                    sendFileOverBT(fileURL, cs, hs, alreadySent);
                    EXEStrMgr.lg(35, "Sent " + fileURL);
                }catch(IOException e) {
                    EXEStrMgr.lg(128, "Exception sending " + fileURL + " : ", e);
                }
                
                if(doSwap) {
                    EXEStrMgr.getInstance().swap(EXEStrMgr.SWAP_TOFILE);
                }
            }
            
            cs.disconnect(null);
            EXEStrMgr.lg(35, "Disconnected and done ");
            
            con.close();
            EXEStrMgr.lg(35, "BT Connection object closed");
            
            saveRepStatus(repStatus);
            EXEStrMgr.lg(35, "Saved replication status");
        }catch(IOException e) {
            EXEStrMgr.lg(129, "Error attempting to put data ", e);
        }
    }
    
    
}
