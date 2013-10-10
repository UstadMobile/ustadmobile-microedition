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
public class MLServerThread {
   
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
                    EXEStrMgr.lg(36, "Loaded student names");
                }else {
                    studentNames = new Hashtable();
                }
            }catch(Exception e) {
                EXEStrMgr.lg(318, "Excception loading student names ",e);
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
                EXEStrMgr.lg(318, "Exception loading student names ", e);
            }
        }
    }
    
    /**
     * This is called during the startup routine.  If the server is enabled
     * then it will start the Bluetooth OBEX server
     */
    public void checkServer() {
        String prefVal = EXEStrMgr.getInstance().getPref("server.enabled");
        EXEStrMgr.lg(37, "Server enabled is " + prefVal);
        if(prefVal != null && prefVal.equals("true")) {
            enabled = true;
        }else {
            enabled = false;
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
            EXEStrMgr.lg(319, "Error creating log rx directory ", e);
        }
    }
        
}
