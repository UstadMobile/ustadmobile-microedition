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
import java.io.*;
import java.util.Hashtable;
import javax.obex.*;
import javax.bluetooth.*;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import com.toughra.mlearnplayer.EXEStrMgr;
import com.toughra.mlearnplayer.MLearnPreferences;
import com.toughra.mlearnplayer.MLearnUtils;

/**
 * MLServerThread runs to accept bluetooth client sessions and then receive
 * activity log data from them.  It will save the received activity logs to
 * the directory logrx under the main umobiledata folder.
 * 
 * It will also save student_names to umobiledata/studentnames.ht .  This table
 * is used to report the student names to the master server online
 * 
 * @author mike
 */
public class MLServerThread extends ServerRequestHandler implements Runnable {
   
    /**
     * If the server enabled preference is set - this becomes true.  Set to false and 
     * server will exit when next client request is received
     */
    public boolean enabled = false;
    
    /** Static instance of self*/
    private static MLServerThread instance;
    
    /** Runs - yes in a thread...*/
    Thread serverThread;
    
    /** (unused?) */
    boolean finished = false;
    
    /** The UUID for this service */
    public static final String UUIDSTR = "8851";
    
    /** The service type name - FTP */
    public static final String SERVNAME = "FTP";
    
    /** Custom Bluetooth Header for Student UUID */
    public static final int HEADER_UUID = 0x30;
    
    /** Custom Bluetooth header to indicate where data has started from for partial uploads*/
    public static final int HEADER_STARTFROM = 0x31;
    
    /** Custom Bluetooth header for student name */
    public static final int HEADER_STUDENTNAME = 0x32;
    
    /** Customer Bluetooth header for the operation name - can be 'logxmit' or 'learnerdata'*/
    public static final int HEADER_OPNAME = 0x33;
    
    /** Indicates if the incoming log dir has been checked or not - e.g. make sure it exists*/
    boolean checkedLogDir = false;
    
    /** the log receiving directory URI */
    String logRxDir;
    
    /** Hashtable of student names mapped StudentUUID -&gt; Student learnername */
    Hashtable studentNames;
    
    
    //ugly hack for testing purposes
    int pCount = 0;
    
    /**
     * There should only ever be one instance of MLServerThread
     * 
     * @return instance of MLServerThread
     */
    public static MLServerThread getInstance() {
        if(instance == null) {
            instance = new MLServerThread();
        }
        
        return instance;
    }
    
    /**
     * Constructor
     */
    public MLServerThread() {
        logRxDir = null;
        getStudentNames();
        
    }
    
    /**
     * Loads the studentNames hashtable
     * @return Hashtable mapped StudentUUID -&gt; Learnername
     */
    public Hashtable getStudentNames() {
        if(studentNames == null) {
            String fileName = EXEStrMgr.getInstance().getPref("basefolder") + 
                    "/studentnames.ht";
            try {
                FileConnection con = (FileConnection)Connector.open(fileName);
                if(con.exists()) {
                    ByteArrayOutputStream bout = new ByteArrayOutputStream((int)con.fileSize());
                    InputStream fin = con.openInputStream();
                    Util.copy(fin, bout);
                    fin.close();
                    con.close();
                    
                    studentNames = MLearnPreferences.fromByteArray(bout.toByteArray());
                    EXEStrMgr.po("Loaded student names", EXEStrMgr.DEBUG);
                }else {
                    studentNames = new Hashtable();
                }
            }catch(Exception e) {
                EXEStrMgr.po("Excception loading student names " + e.toString(), EXEStrMgr.DEBUG);
            }
        }
        
        return studentNames;
    }
    
    /**
     * Save the student names hashtable
     * 
     */
    public void saveStudentNames() {
        String fileName = EXEStrMgr.getInstance().getPref("basefolder") + 
                    "/studentnames.ht";
        if(studentNames != null) {
            try {
                FileConnection fcon = (FileConnection)Connector.open(fileName);
                if(!fcon.exists()) {
                    fcon.create();
                }
                OutputStream fout = fcon.openOutputStream();
                fout.write(MLearnPreferences.toByteArray(studentNames));
                fout.close();
                fcon.close();
            }catch(Exception e) {
                EXEStrMgr.po("Exception loading student names " + e.toString(),
                        EXEStrMgr.WARN);
            }
        }
    }
    
    /**
     * This is called during the startup routine.  If the server is enabled
     * then it will start the Bluetooth OBEX server
     */
    public void checkServer() {
        String prefVal = EXEStrMgr.getInstance().getPref("server.enabled");
        EXEStrMgr.po("Server enabled is " + prefVal, EXEStrMgr.DEBUG);
        if(prefVal != null && prefVal.equals("true")) {
            enabled = true;
            if(serverThread == null) {
                serverThread = new Thread(this);
                serverThread.start();
            }
        }else {
            enabled = false;
        }
    }
    
    /**
     * Will start the server running and then accept connections from clients.
     * 
     */
    public void startServer() {
        UUID uuid = new UUID(UUIDSTR, true);
        String url = "btgoep://localhost:" + uuid 
            + ";name=" + SERVNAME +";authenticate=false;master=false;encrypt=false";
            
        try {
            SessionNotifier sn = (SessionNotifier)Connector.open(url);
            EXEStrMgr.getInstance().p("Opened BT session for " + url, EXEStrMgr.DEBUG);
            while(enabled) {
                sn.acceptAndOpen(this);
                EXEStrMgr.getInstance().p("BT Client Connected", EXEStrMgr.DEBUG);
            }
        }catch(Exception e) {
            e.printStackTrace();
            EXEStrMgr.getInstance().p("Error opening bt session for : " + url + " : " + e.toString(), 
                    EXEStrMgr.WARN);
        }
    }

    /**
     * onConnect listener method
     * 
     * @param request HeaderSet of request
     * @param reply HeaderSet of reply
     * 
     * @return ResponseCodes.OBEX_HTTP_OK if we can accept requests, ResponseCodes.OBEX_HTTP_UNAVAILABLE otherwise
     */
    public int onConnect(HeaderSet request, HeaderSet reply) {
        if(enabled) {
            EXEStrMgr.getInstance().p("BT Client requests session", EXEStrMgr.DEBUG);
            return ResponseCodes.OBEX_HTTP_OK;
        }else {
            EXEStrMgr.getInstance().p("BT Server has been disabled since", EXEStrMgr.DEBUG);
            return ResponseCodes.OBEX_HTTP_UNAVAILABLE;
        }
        
    }
    
    /**
     * Check that our log reception folder exists and can be written to
     * then set checkedLogDir to true so that we don't have to do any more
     * file operations thereafter
     * 
     */
    public void checkLogFolder() {
        if(checkedLogDir) {
            return;//everything should  be ok
        }
        String folderName = EXEStrMgr.getInstance().getPref("basefolder") + "/logrx";
        try {
            FileConnection folderCon = (FileConnection)Connector.open(folderName);
            if(!folderCon.isDirectory()) {
                folderCon.mkdir();
            }
            checkedLogDir = true;
            folderCon.close();
            logRxDir = folderName;
        }catch(IOException e) {
            EXEStrMgr.po("Error creating log rx directory " + e.toString(), EXEStrMgr.WARN);
        }
    }
    
    /**
     * This will receive a hashtable serialized into bytes using DataInputStream
     * /MLearnPreferences format.  Right now it will just take the key learnername
     * and then update the studentNames hashtable.
     * 
     * @param op - put operation with the input stream
     */
    public void updateStudentData(Operation op) throws IOException{
        long contentSize = ((Long)op.getReceivedHeaders().getHeader(HeaderSet.LENGTH)).longValue();
        byte[] contentData = null;
        String userUUID = op.getReceivedHeaders().getHeader(HEADER_UUID).toString();
        InputStream in = op.openInputStream();        
        ByteArrayOutputStream bout = new ByteArrayOutputStream((int)contentSize);        
        Util.copy(in, bout);
        
        contentData = bout.toByteArray();
        
        bout.close();
        bout = null;
        
        Hashtable userHt = MLearnPreferences.fromByteArray(contentData);
        String studentName = userHt.get("learnername").toString();;
        studentNames.put(userUUID, studentName);
        try {
            saveStudentNames();
        }catch(Exception e) {
            EXEStrMgr.po("Exception saving student names " + e.toString(), EXEStrMgr.DEBUG);
        }
    }
    
    /**
     * This will receive a log from a students phone and put it in the logrx 
     * directory.
     * 
     * It can receive partial transmissions (e.g. when a log file has been updated)
     * and will open the log file already here in append mode.
     * 
     * @param op Operation object
     * @throws IOException 
     */
    public void receiveLog(Operation op) throws IOException {
        InputStream in = op.openInputStream();


        String fileName = op.getReceivedHeaders().getHeader(HeaderSet.NAME).toString();

        EXEStrMgr.po("Got input stream for " + fileName, EXEStrMgr.DEBUG);

        String userUUID = op.getReceivedHeaders().getHeader(HEADER_UUID).toString();

        Long headerSize = (Long)op.getReceivedHeaders().getHeader(HeaderSet.LENGTH);
        EXEStrMgr.po("Header size says " + headerSize + " size " + headerSize.getClass().getName(), EXEStrMgr.DEBUG);

        String saveFname = logRxDir + "/" + userUUID + "-" + fileName;
        FileConnection fCon = (FileConnection)Connector.open(saveFname);

        long sizeHere = 0;
        if(fCon.exists()) {
            sizeHere = fCon.fileSize();
        }

        OutputStream fout = MLearnUtils.openFileOutputAppendMode(fCon);

        Util.copy(in, fout);

        fout.flush();
        fout.close();
        fCon.close();

        EXEStrMgr.po("Wrote bytes to " + saveFname + " ... flushing... ", EXEStrMgr.DEBUG);

        //TODO: Should we add a in.close here?
        EXEStrMgr.po("Done receiving file - closing it all... ", EXEStrMgr.DEBUG);
    }
    
    /**
     * The main Bluetooth onPut method handler.  Assuming the server is still enabled
     * we will get the log and put it into the logrx directory
     * 
     * @param op Operation object
     * @return ResponseCodes.OBEX_HTTP_UNAVAILABLE if we cant do this now, ResponseCodes.OBEX_HTTP_OK when everything went OK
     */
    public int onPut(Operation op) {
        if(!enabled) {
            try {
                EXEStrMgr.po("Closing session - disabled server", EXEStrMgr.DEBUG);
                op.close(); 
            }catch(IOException e) {
                EXEStrMgr.po("exception closing session - disabled server", EXEStrMgr.WARN);
            }
            
            return ResponseCodes.OBEX_HTTP_UNAVAILABLE;
        }
        pCount++;
        try {
            //check the HEADER_OPNAME header and see what the client is requesting
            EXEStrMgr.getInstance().p("BT Client 4.1 starts onput", EXEStrMgr.DEBUG);
            String exeOpName = op.getReceivedHeaders().getHeader(HEADER_OPNAME).toString();
            
            if(exeOpName.equals("logxmit")) {
                //wants to send a log
                EXEStrMgr.po("Attempting to receive log data", EXEStrMgr.DEBUG);
                checkLogFolder();
                receiveLog(op);
            }else if(exeOpName.equals("learnerdata")) {
                //wants to send student data
                EXEStrMgr.po("Attempting to save learner data", EXEStrMgr.DEBUG);
                updateStudentData(op);
            }
            op.close();
            //op is done now
            EXEStrMgr.po("Closed operation", EXEStrMgr.DEBUG);
        }catch(IOException e) {
            EXEStrMgr.getInstance().p("Error receiving file 2 : " + e.toString(), EXEStrMgr.WARN);
        }
        //write student names to disk
        saveStudentNames();
        return ResponseCodes.OBEX_HTTP_OK;
    }
    
    
    /**
     * run method simply calls startServer
     */
    public void run() {
        startServer();
    }
    
}
