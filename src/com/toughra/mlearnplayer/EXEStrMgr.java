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
package com.toughra.mlearnplayer;

import com.sun.lwuit.io.Storage;
import com.sun.lwuit.util.Resources;
import java.io.*;
import java.util.*;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;

/**
 *
 * Class to manage data transmission, storage, etc for Ustad Mobile
 * 
 * For purposes of logging this class can close a FileConnector, switch to using
 * the buffer, flush the contents to the file, and then reopen a file connector
 * 
 * NOTE: Storage Manager automatically makes a folder on the root folder with
 * the greatest amount of space available.  This should normally mean in most
 * j2me environments the memory card.
 * 
 * Manages the preferences for Ustad Mobil.
 * 
 * @author mike
 */
public class EXEStrMgr {
    
    /**The Host midlet*/
    MLearnPlayerMidlet host;
    
    /**We only want one instance of this class*/
    static EXEStrMgr instance;
    
    /**The storage name used for j2me storage*/
    static String storageName = "net.paiwastoon.ustadmobile";
    
    /** Preferences storage that is made as a serialized hashtable*/
    public MLearnPreferences prefs;
    
    /**The file name that is used for the serialized hashtable */
    final String pStrFname = "preferences";
    
    /**The filename used for localized resources*/
    public static final String localeResName = "localize";
    
    /**PrintStream used to save debugging information*/
    PrintStream debugStrm;
    
    /**PrintStream used for activity logging (-activity.log)*/
    PrintStream logStrm;
    
    /**The base folder (umobiledata) where we save info*/
    String baseFolder;
    
    /**Log level info */
    public static final int INFO = 0;
    
    /**Log level warn*/
    public static final int WARN = 1;
    
    /**Log level debug*/
    public static final int DEBUG = 2;
    
    /**The uuid that has been randomly generated for this instance*/
    public String uuid;
    
    /**FileConnector objects that are currently open*/
    public Hashtable openLogFCs;
    
    /** no swap operation required */
    public static final int SWAP_NONE = 0;
    
    /** swap to using a buffer */
    public static final int SWAP_TOBUF = 1;
    
    /** swap back to using files */
    public static final int SWAP_TOFILE = 2;
    
    /** used when we are buffering as a file is being transmitted */
    ByteArrayOutputStream bufOut;
    
    public static String rootMessages;
    
    /*
     * Constructor - 
     * 
     * @param host - MLearnPlayerMidlet instance
     */
    public EXEStrMgr(MLearnPlayerMidlet host) {
        this.host = host;
        
        Storage.init(storageName);
        
        openLogFCs = new Hashtable();
        
        //Check and see if we have the preferences already saved in j2me file system
        if(Storage.getInstance().exists(pStrFname)) {
            try {
                InputStream in = Storage.getInstance().createInputStream(pStrFname);
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                int b;
                while((b = in.read()) != -1) {
                    bout.write(b);
                }
                in.close();
                in = null;
                
                prefs = new MLearnPreferences();
                prefs.load(bout.toByteArray());
                
                //avoid weird things - even though there is no way it should be
                //possible to load without a UUID
                uuid = prefs.getPref("uuid");
                if(uuid == null) {
                    uuid = makeUUID();
                    prefs.setPref("uuid", uuid);
                    prefs.setPref("learnername", "");
                }
                bout.close();
            }catch(IOException e) {
                e.printStackTrace();
            }
        }else {
            //this is our first run - create and generate a uuid
            prefs = new MLearnPreferences();
            uuid = makeUUID();
            prefs.setPref("uuid", uuid);            
        }
        
        if(prefs.getPref("basefolder") == null) {
            setupBaseFolder();
        }
        baseFolder = prefs.getPref("basefolder");
    }
    
    /**
     * Make a UUIID
     */
    public String makeUUID() {
        Random r = new Random(System.currentTimeMillis());
        String uuid = String.valueOf(Math.abs(r.nextLong()));
        return uuid;
    }
    
    public String getCloudUser() {
        return prefs.getPref("clouduser");
    }
    
    public void setCloudUser(String cloudUser) {
        prefs.setPref("clouduser", cloudUser);
    }
    
    public String getCloudPass() {
        return prefs.getPref("cloudpass");
    }
    
    public void setCloudPass(String cloudPass) {
        prefs.setPref("cloudpass", cloudPass);
    }
    
    public void doCloudLogout() {
        prefs.deletePref("clouduser");
        prefs.deletePref("cloudpass");
    }
    
    /**
     * Get a vector of the locales that are available
     * 
     * @param res
     * @param resName
     * @return 
     */
    public Vector getLocaleList(Resources res, String resName) {
        Vector retVal = new Vector();
        Enumeration localesAvail = host.localeRes.listL10NLocales(localeResName);
        while(localesAvail.hasMoreElements()) {
            retVal.addElement(localesAvail.nextElement());
        }
        
        return retVal;
    }
    
    /**
     * Initiate the locale management.  This will look for a preference
     * called 'locale'.  If this does not exist will refer to system property
     * microedition.locale (device locale) and trim to two letters.
     * 
     * If the device locale is available that will be set, otherwise will be
     * set to work in English (en)
     * 
     * @throws IOException 
     */
    public void initLocale() throws IOException{
        host.localeRes = Resources.open("/localization.res");
        String localeToUse = getPref("locale");
        if(localeToUse == null) {
            //locale not yet set - see if we have the system locale
            String sysLocal = System.getProperty("microedition.locale");
            if(sysLocal == null) {
                sysLocal = "en";
            }else if(sysLocal.length() > 2){
                sysLocal = sysLocal.substring(0, 2);
            }
            
            Vector localesAvail = getLocaleList(host.localeRes, localeResName);
            if(localesAvail.contains(sysLocal)) {
                localeToUse = sysLocal;
            }else {
                localeToUse = "en";
            }
            setPref("locale", localeToUse);
            savePrefs();
        }
        
        
                        
        host.localeHt = host.localeRes.getL10N("localize", localeToUse);
            
        EXEStrMgr.getInstance().p("Loaded localization res for " + localeToUse, 1);
    }
    
    /**
     * 
     * @return The locale code in use (e.g. en_US)
     */
    public String getLocale() {
        return getPref("locale");
    }
    
    /**
     * Format a string to be used in the filename for the activity and debug 
     * names.
     * 
     * @return String formatted for date log file names yyyy-mm-dd
     */ 
    public String getDateLogStr() {
        Calendar cal = Calendar.getInstance();
        return  cal.get(Calendar.YEAR) + "-" +
                    pad1(cal.get(Calendar.MONTH)) + "-" + pad1(cal.get(Calendar.DAY_OF_MONTH));
    }
    
    /**
     * Utility function that will check if a log already exists.  It essentially
     * opens the file in an auto append mode, and it will create the file if 
     * it does not already exist.
     * 
     * It works by opening the file, checking the file size, and then skipping
     * size bytes.
     * 
     * The filename will automatically be put in umobiledata/date-logname
     * 
     * @param path
     * @return 
     */
    private PrintStream openLogStream(String logname) {
        OutputStream out = null;
        PrintStream strm = null;
        FileConnection fileCon = null;
        try {
            String debugFileName = getDateLogStr() + "-" + logname + ".log";

            String printOutDest = baseFolder + "/" + debugFileName;
            System.out.println("Attempt to open for writing debug log: " + printOutDest);
            
            fileCon = (FileConnection)Connector.open(printOutDest, 
                    Connector.READ_WRITE);
            openLogFCs.put(debugFileName, fileCon);
            
            if(fileCon.exists()) {
                out = fileCon.openOutputStream(fileCon.fileSize());
            }else {
                fileCon.create();
                out = fileCon.openOutputStream();
            }
            
            openLogFCs.put(debugFileName, fileCon);
            
            strm = new PrintStream(out);
        }catch (Exception e) {
            e.printStackTrace();
        }
        
        return strm;
    }
    
    /**
     * Check and see if a log is open
     * 
     * @param basename (e.g. 'activity' or 'debug')
     * @return true if the given name is open, false otherwise. 
     */
    public boolean logFileOpen(String basename) {
        return openLogFCs.containsKey(basename);
    }
    
    /**
     * if num is less than ten add a preceding 0
     * @param num 
     */
    private static String pad1(int num) {
        if(num >= 10) {
            return String.valueOf(num);
        }else {
            return "0" + num;
        }
    }
    
    /**
     * Logs an exception to the debug log
     * @param e The exception 
     * @param str additional string info
     */
    public static void po(Exception e, String str) {
        getInstance().p(str + " : " + e.toString(), WARN);
        e.printStackTrace();
    }
    
    /**
     * Logs a message to the debug log
     * @param str
     * @param level 
     */
    public static void po(String str, int level) {
        getInstance().p(str, level);
    }
    
    /**
     * Logs a message to the debug log.  Checks to see if
     * @param str
     * @param level 
     */
    public synchronized void p(String str, int level) {
        if(debugStrm == null && baseFolder != null) {
            debugStrm = openLogStream("debug");
        }
        
        if(debugStrm != null) {
            debugStrm.println("[" + new Date().toString() + "] " + str);
        }
    }
    
    /**
     * Logs a line to the activity log
     * @param str message to write
     * @param device - idevice to get log info from
     */
    public synchronized static void lg(String str, Idevice device) {
        getInstance().l(str, device);
    }
    
    /**
     * Logs a line to the activity log
     * @param str
     * @param device 
     */
    public void l(String str, Idevice device) {
        l(str, device, SWAP_NONE);
    }
    
    /**
     * Makes a log of the learner's activity.  If required it can swap between
     * using the memory buffer and writing direct to the file (e.g. when the log
     * file itself is being transmitted to the teachers phone)
     * 
     * @param str - string to log
     * @param device - Idevice object to log for
     * @param swapOp - if an open log needs switched to being a buffer...
     */
    public synchronized void l(String str, Idevice device, int swapOp) {
        if(swapOp == SWAP_TOBUF) {
            //close the existing stream
            if(logStrm != null) { 
                logStrm.flush();
                logStrm.close();
                logStrm = null;
            }
            
            try {
                String logName = getDateLogStr() + "-activity.log";
                FileConnection fileCon = (FileConnection)openLogFCs.get(logName);
                fileCon.close();
                openLogFCs.remove(logName);
            }catch(Exception e) {
                EXEStrMgr.po("Problem closing stream for rep swap " + e.toString(), WARN);
            }

            //make logStrm as a bytearrayoutput stream for now
            bufOut = new ByteArrayOutputStream();
            logStrm = new PrintStream(bufOut);
            
            //remove this item from the files open hashtable
            po("Switched to running on buffer", DEBUG);
            return;
        }else if(swapOp == SWAP_TOFILE) {
            if(logStrm != null) {
                logStrm.flush();
            }
            
            byte[] bufNow = bufOut.toByteArray();
            logStrm.close();
            
            logStrm = openLogStream("activity");
            try {
                logStrm.write(bufNow);
            }catch(IOException e) {
                EXEStrMgr.po("Error writing log buffer out... " + e.toString(), EXEStrMgr.DEBUG);
            }
            po("Swapped back to running on file", DEBUG);
            return;
        }
        
        long timestamp = new Date().getTime();
        
        if(logStrm == null && baseFolder != null) {
            EXEStrMgr.po("Opening activity log append mode ", EXEStrMgr.DEBUG);
            logStrm = openLogStream("activity");
            EXEStrMgr.po("Opened logstrm: " + logStrm, EXEStrMgr.DEBUG);
        }
        
        if(logStrm != null) {
            String logLine = "" + timestamp + " " + uuid+ " ";;
            if(device.hostMidlet.currentColId != null) {
                logLine += device.hostMidlet.currentColId + " ";
            }else {
                logLine += "null ";
            }
            
            String pageHref = device.hostMidlet.currentHref;
            if(pageHref.endsWith(".xml")) {
                pageHref = pageHref.substring(0, pageHref.length()-4);
            }
            
            logLine += device.hostMidlet.currentPkgId + " " 
                    + pageHref + " ";
            
            logLine += device.getLogLine();
            
            logStrm.println(logLine);
            
        }else {
            EXEStrMgr.po("Logstrm is null", EXEStrMgr.WARN);
        }
    }
    
    /**
     * Find out where we should put the base folder by finding the root folder
     * with the maximum amount of space (this should be the memory card generally)
     */
    public void setupBaseFolder() {
        String baseFolder = System.getProperty("fileconn.dir.photos") + "umobiledata";
        
        try {
            FileConnection bCon = (FileConnection)Connector.open(baseFolder);
            if(!bCon.isDirectory()) {
                bCon.mkdir();
            }
            bCon.close();
        }catch(Exception e3) {
            e3.printStackTrace();
        }
        
        setPref("basefolder", baseFolder);
        savePrefs();
    }
    
    /**
     * Returns the current operating instance
     * 
     * @return operating instance of EXEStrMgr
     */
    public static EXEStrMgr getInstance() {
        return getInstance(MLearnPlayerMidlet.getInstance());
    }
    
    /**
     * Returns the current operating instance
     * 
     * @param host the operating mlearnplayermidlet
     * 
     * @return operating instance of EXEStrMgr
     */
    public static EXEStrMgr getInstance(MLearnPlayerMidlet host) {
        if(instance == null) {
            instance = new EXEStrMgr(host);
        }
        
        return instance;
    }
    
    /**
     * Set a general preference key
     * 
     * @param key
     * @param val 
     */
    public void setPref(String key, String val){
        prefs.setPref(key, val);
    }
    
    /**
     * Returns a preference key as a String.  Returns null if the key does
     * not exist
     * 
     * @param key The parameter key to find in the hashtable
     * @return Value of the key
     */
    public String getPref(String key) {
        return prefs.getPref(key);
    }
    
    /**
     * Removes the preference key given 
     * 
     * @param key Key to remove
     */
    public void delPref(String key) {
        prefs.prefs.remove(key);
    }
    
    /**
     * Saves the preferences and flushes all log output to disk
     */
    public void saveAll() {
        savePrefs();
        if(logStrm != null) { logStrm.flush(); }
        if(debugStrm != null) { debugStrm.flush(); }
    }
    
    /**
     * Writes the preferences to disk as according to the hard coded filename as
     * a serialized hashtable.
     */
    public void savePrefs() {
        try {
            OutputStream out = Storage.getInstance().createOutputStream(pStrFname);
            out.write(prefs.save());
            out.flush();
            out.close();
            Storage.getInstance().flushStorageCache();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    
}
