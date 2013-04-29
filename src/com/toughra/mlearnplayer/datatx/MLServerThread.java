/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
 *
 * @author mike
 */
public class MLServerThread extends ServerRequestHandler implements Runnable {
    
    public boolean enabled = false;
    
    private static MLServerThread instance;
    
    Thread serverThread;
    
    boolean finished = false;
    
    public static final String UUIDSTR = "8851";
    
    public static final String SERVNAME = "FTP";
    
    public static final int HEADER_UUID = 0x30;
    
    public static final int HEADER_STARTFROM = 0x31;
    
    public static final int HEADER_STUDENTNAME = 0x32;
    
    public static final int HEADER_OPNAME = 0x33;
    
    boolean checkedLogDir = false;
    
    String logRxDir;
    
    Hashtable studentNames;
    
    
    //ugly hack for testing purposes
    int pCount = 0;
    
    public static MLServerThread getInstance() {
        if(instance == null) {
            instance = new MLServerThread();
        }
        
        return instance;
    }
    
    public MLServerThread() {
        logRxDir = null;
        getStudentNames();
        
    }
    
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

    public int onConnect(HeaderSet request, HeaderSet reply) {
        if(enabled) {
            EXEStrMgr.getInstance().p("BT Client requests session", EXEStrMgr.DEBUG);
            return ResponseCodes.OBEX_HTTP_OK;
        }else {
            EXEStrMgr.getInstance().p("BT Server has been disabled since", EXEStrMgr.DEBUG);
            return ResponseCodes.OBEX_HTTP_UNAVAILABLE;
        }
        
    }
    
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
     * /MLearnPreferences format
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
            EXEStrMgr.getInstance().p("BT Client 4.1 starts onput", EXEStrMgr.DEBUG);
            String exeOpName = op.getReceivedHeaders().getHeader(HEADER_OPNAME).toString();
            if(exeOpName.equals("logxmit")) {
                EXEStrMgr.po("Attempting to receive log data", EXEStrMgr.DEBUG);
                checkLogFolder();
                receiveLog(op);
            }else if(exeOpName.equals("learnerdata")) {
                EXEStrMgr.po("Attempting to save learner data", EXEStrMgr.DEBUG);
                updateStudentData(op);
            }
            op.close();
            EXEStrMgr.po("Closed operation", EXEStrMgr.DEBUG);
        }catch(IOException e) {
            EXEStrMgr.getInstance().p("Error receiving file 2 : " + e.toString(), EXEStrMgr.WARN);
        }
        saveStudentNames();
        return ResponseCodes.OBEX_HTTP_OK;
    }
    
    
    
    public void run() {
        startServer();
    }
    
}
