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
                
                //also - send the logs through if that has been set
                new MLHTTPRep().sendLogs(this);
                
                //check and see if we need to send updated preferences
                MLCloudConnector.getInstance().sendPreferences();
                
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
            EXEStrMgr.po("Error trying to get rep status"+ e.toString(), EXEStrMgr.WARN);
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
        EXEStrMgr.po("Attempting to save rep status to " + fileName, EXEStrMgr.DEBUG);
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
            EXEStrMgr.getInstance().po("Exception saving rep status " + e.toString(), EXEStrMgr.WARN);
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
        EXEStrMgr.po("checkRepFiles() looking for logs to send ",EXEStrMgr.DEBUG);
        
        
        String baseDir = EXEStrMgr.getInstance().getPref("basefolder");
        Vector repList = new Vector();
        
        try {
            
            FileConnection dirCon = (FileConnection)Connector.open(baseDir);
            Enumeration e= dirCon.list();
            while(e.hasMoreElements()) {
                String name = e.nextElement().toString();
                EXEStrMgr.po("checkRepFiles() found file : " + name ,EXEStrMgr.DEBUG);
                if(name.endsWith("-activity.log")) {
                    repList.addElement(name);
                    EXEStrMgr.po("Should replicate "+ name, EXEStrMgr.DEBUG);
                }
            }
            
            dirCon.close();
        }catch(Exception e) {
            EXEStrMgr.po("Exception prepping to replicate files", EXEStrMgr.DEBUG);
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
        
        EXEStrMgr.po("Starting put process for student data", EXEStrMgr.DEBUG);
        OutputStream out = op.openOutputStream();
        
        EXEStrMgr.po("Opened output stream - sending", EXEStrMgr.DEBUG);
        out.write(htBytes);
        out.close();
        op.close();
        EXEStrMgr.po("Sent student data hashtable", EXEStrMgr.DEBUG);
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
        EXEStrMgr.po("Attempting to open for reading " + fileName, EXEStrMgr.DEBUG);

        FileConnection fCon = (FileConnection)Connector.open(fileName, Connector.READ);

        //check already sent
        if(alreadySent > 0) {
            long fSize = fCon.fileSize();
            if(alreadySent >= fSize) {
                //it's already there...
                EXEStrMgr.po(fileName + " has already been sent actually", EXEStrMgr.DEBUG);
                fCon.close();
                return;
            }
        }
        
        long startPoint = (alreadySent >= 0) ? alreadySent : 0;

        InputStream in = fCon.openInputStream();
        if(startPoint > 0) {
            long skipped = in.skip(startPoint);
            EXEStrMgr.po("Skipped " + skipped + " bytes already sent", EXEStrMgr.DEBUG);
        }

        EXEStrMgr.po("Read the debug log file", EXEStrMgr.DEBUG);

        //byte[] toSend = logBytes; //strToSend.getBytes();
        long fileLen = fCon.fileSize();
        Long lengthObj = new Long(fileLen);

        EXEStrMgr.po("Sending " + lengthObj.toString() + " bytes ", EXEStrMgr.DEBUG);

        String fileBaseName = fileName.substring(fileName.lastIndexOf('/')+1);

        //Set heads which are recognized by MLServerThread
        hs.setHeader(HeaderSet.NAME, fileBaseName);
        hs.setHeader(HeaderSet.TYPE, "text/plain");
        hs.setHeader(HeaderSet.LENGTH, lengthObj);
        hs.setHeader(MLServerThread.HEADER_OPNAME, "logxmit");
        hs.setHeader(MLServerThread.HEADER_STUDENTNAME, EXEStrMgr.getInstance().getPref("learnername"));
        hs.setHeader(MLServerThread.HEADER_UUID, EXEStrMgr.getInstance().getPref("uuid"));

        Operation op = cs.put(hs);
        EXEStrMgr.po("Starting put process", EXEStrMgr.DEBUG);
        OutputStream out = op.openOutputStream();
        EXEStrMgr.po("Opened output stream - sending", EXEStrMgr.DEBUG);

        Util.copy(in, out);

        in.close();
        fCon.close();

        EXEStrMgr.po("File Sent 4.1 ", EXEStrMgr.DEBUG);

        out.close();
        EXEStrMgr.po("Closed out stream", EXEStrMgr.DEBUG);

        op.close();
        EXEStrMgr.po("Closed op", EXEStrMgr.DEBUG);

        //if we get here without any exception - we have been successful (hoorah)
        repStatus.put(fileBaseName, String.valueOf(fileLen));
        EXEStrMgr.po("Saved repstatus " + fileBaseName + "sent " + fileLen, EXEStrMgr.DEBUG);
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
            EXEStrMgr.po("Attempting to open for put: " + conURL, EXEStrMgr.DEBUG);
            con = (Connection)Connector.open(conURL);
            EXEStrMgr.po("Established connection to " + conURL, EXEStrMgr.DEBUG);
            
            
            ClientSession cs = (ClientSession)con;
            HeaderSet hs = cs.createHeaderSet();
            EXEStrMgr.po("Created header set ", EXEStrMgr.DEBUG);
            
            cs.connect(hs);
            
            Vector repList = checkRepFiles();
            int numFiles = repList.size();
            
            //open the rpelication status hashtable
            EXEStrMgr.po("Attempting to get rep status ", EXEStrMgr.DEBUG);
            repStatus = getRepStatus();
            EXEStrMgr.po("Opened rep status ", EXEStrMgr.DEBUG);
            
            //send the learnername etc. over the connection
            try {
                sendStudentInfoOverBt(cs, hs);
            }catch(IOException e) {
                EXEStrMgr.po("Exception sending student info " + e.toString(), EXEStrMgr.DEBUG);
            }
            
            //go through all the files 
            for(int i = 0; i < numFiles; i++) {
                String cFname = repList.elementAt(i).toString();
                
                boolean doSwap = false;
                if(EXEStrMgr.getInstance().logFileOpen(cFname)) {
                    EXEStrMgr.getInstance().l(null, null, EXEStrMgr.SWAP_TOBUF);
                    doSwap = true;
                }
                
                
                EXEStrMgr.getInstance().po("Checking status for " + cFname, EXEStrMgr.DEBUG);
                //check rep status
                long alreadySent = getReplicationSent(repStatus, cFname);
                
                
                EXEStrMgr.getInstance().po("Found we have sent " + alreadySent + " for " + cFname,
                        EXEStrMgr.DEBUG);

                String fileURL = EXEStrMgr.getInstance().getPref("basefolder") +
                        "/" + cFname;
    
                try {
                    sendFileOverBT(fileURL, cs, hs, alreadySent);
                    EXEStrMgr.po("Sent " + fileURL, EXEStrMgr.DEBUG);
                }catch(IOException e) {
                    EXEStrMgr.po("Exception sending " + fileURL + " : " + e.toString(), 
                            EXEStrMgr.WARN);
                }
                
                if(doSwap) {
                    EXEStrMgr.getInstance().l(null, null, EXEStrMgr.SWAP_TOFILE);
                }
            }
            
            cs.disconnect(null);
            EXEStrMgr.po("Disconnected and done ", EXEStrMgr.DEBUG);
            
            con.close();
            EXEStrMgr.po("BT Connection object closed", EXEStrMgr.DEBUG);
            
            saveRepStatus(repStatus);
            EXEStrMgr.po("Saved replication status", EXEStrMgr.DEBUG);
        }catch(IOException e) {
            EXEStrMgr.po("Error attempting to put data " + e.toString() + " : "
                    + e.getMessage(), EXEStrMgr.WARN);
        }
    }
    
    
}
