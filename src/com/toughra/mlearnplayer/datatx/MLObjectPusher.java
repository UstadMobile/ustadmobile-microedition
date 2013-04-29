/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
 * This will push out logs to the next bluetooth or http hop
 * 
 * @author mike
 */
public class MLObjectPusher extends Thread{
    
    public MLObjectPusher() {
        Random r = new Random(System.currentTimeMillis());
        countDown = r.nextInt(REPDELAY);
    }
    
    Hashtable repStatus;
    
    //sync every 5mins
    public static final int REPDELAY = 300000;
    
    public static final int TICK = 1000;
    
    public static int countDown = REPDELAY;
    
    public static boolean enabled = true;
    
    public void run() {
        while(enabled) {
            countDown -= TICK;
            if(countDown <= 0) {
                String conURL = EXEStrMgr.getInstance().getPref("server.bt.url");
                if(conURL != null) {
                    checkRepFiles();
                    sendData(conURL);
                }
                
                //also - send the logs through if that has been set
                new MLHTTPRep().sendLogs();
                
                countDown = REPDELAY;
            }
            
            try { Thread.sleep(TICK); }
            catch(InterruptedException e) {}
        }
    }
    
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
    
    public Vector checkRepFiles() {
        //check files to be replicated
        
        String baseDir = EXEStrMgr.getInstance().getPref("basefolder");
        Vector repList = new Vector();
        
        try {
            
            FileConnection dirCon = (FileConnection)Connector.open(baseDir);
            Enumeration e= dirCon.list();
            while(e.hasMoreElements()) {
                String name = e.nextElement().toString();
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
    
    public void sendStudentInfoOverBt(ClientSession cs, HeaderSet hs) throws IOException{
        Hashtable ht = new Hashtable();
        ht.put("learnername", EXEStrMgr.getInstance().getPref("learnername"));
        sendStudentInfoOverBt(ht, cs, hs);
    }
    
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

        repStatus.put(fileBaseName, String.valueOf(fileLen));
        EXEStrMgr.po("Saved repstatus " + fileBaseName + "sent " + fileLen, EXEStrMgr.DEBUG);
    }
    
    
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
            EXEStrMgr.po("Attempting to get rep status ", EXEStrMgr.DEBUG);
            repStatus = getRepStatus();
            EXEStrMgr.po("Opened rep status ", EXEStrMgr.DEBUG);
            
            
            try {
                sendStudentInfoOverBt(cs, hs);
            }catch(IOException e) {
                EXEStrMgr.po("Exception sending student info " + e.toString(), EXEStrMgr.DEBUG);
            }
            
            
            for(int i = 0; i < numFiles; i++) {
                String cFname = repList.elementAt(i).toString();
                
                boolean doSwap = false;
                if(EXEStrMgr.getInstance().logFileOpen(cFname)) {
                    EXEStrMgr.getInstance().l(null, null, EXEStrMgr.SWAP_TOBUF);
                    doSwap = true;
                }
                
                
                EXEStrMgr.getInstance().po("Checking status for " + cFname, EXEStrMgr.DEBUG);
                //check rep status
                Object sentSizeObj = repStatus.get(cFname);
                long alreadySent = -1;
                if(sentSizeObj != null) {
                    alreadySent = Long.parseLong(sentSizeObj.toString());
                }

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
