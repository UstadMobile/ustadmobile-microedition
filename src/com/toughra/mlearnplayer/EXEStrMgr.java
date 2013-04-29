/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
 * @author mike
 */
public class EXEStrMgr {
    
    MLearnPlayerMidlet host;
    
    static EXEStrMgr instance;
    
    static String storageName = "net.paiwastoon.ustadmobile";
    
    public MLearnPreferences prefs;
    
    final String pStrFname = "preferences";
    
    public static final String localeResName = "localize";
    
    PrintStream debugStrm;
    
    PrintStream logStrm;
    
    String baseFolder;
    
    public static final int INFO = 0;
    
    public static final int WARN = 1;
    
    public static final int DEBUG = 2;
    
    public String uuid;
    
    public Hashtable openLogFCs;
    
    /** no swap operation required */
    public static final int SWAP_NONE = 0;
    
    /** swap to using a buffer */
    public static final int SWAP_TOBUF = 1;
    
    /** swap back to using files */
    public static final int SWAP_TOFILE = 2;
    
    /** used when we are buffering as a file is being transmitted */
    ByteArrayOutputStream bufOut;
    
    public EXEStrMgr(MLearnPlayerMidlet host) {
        this.host = host;
        
        Storage.init(storageName);
        
        openLogFCs = new Hashtable();
        
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
                
                uuid = prefs.getPref("uuid");
                
                bout.close();
            }catch(IOException e) {
                e.printStackTrace();
            }
        }else {
            prefs = new MLearnPreferences();
            Random r = new Random(System.currentTimeMillis());
            uuid = String.valueOf(Math.abs(r.nextLong()));
            prefs.setPref("uuid", uuid);            
        }
        if(prefs.getPref("basefolder") == null) {
            setupBaseFolder();
        }
        baseFolder = prefs.getPref("basefolder");
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
    
    
    public String getLocale() {
        return getPref("locale");
    }
    
    /**
     * Will return yyyy-mm-dd
     */ 
    public String getDateLogStr() {
        Calendar cal = Calendar.getInstance();
        return  cal.get(Calendar.YEAR) + "-" +
                    pad1(cal.get(Calendar.MONTH)) + "-" + pad1(cal.get(Calendar.DAY_OF_MONTH));
    }
    
    /**
     * Utility function that will check if a log already exists
     * should make this essentially auto append
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
    
    public static void po(Exception e, String str) {
        getInstance().p(str + " : " + e.toString(), WARN);
        e.printStackTrace();
    }
    
    public static void po(String str, int level) {
        getInstance().p(str, level);
    }
    
    public synchronized void p(String str, int level) {
        if(debugStrm == null && baseFolder != null) {
            debugStrm = openLogStream("debug");
        }
        
        if(debugStrm != null) {
            debugStrm.println("[" + new Date().toString() + "] " + str);
        }
    }
    
    public synchronized static void lg(String str, Idevice device) {
        getInstance().l(str, device);
    }
    
    
    public void l(String str, Idevice device) {
        l(str, device, SWAP_NONE);
    }
    
    /**
     * Makes a log of the learner's activity
     * 
     * 
     * 
     * @param str
     * @param device 
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
     * with the maximum amount of space
     */
    public void setupBaseFolder() {
        Enumeration e = FileSystemRegistry.listRoots();
        String biggestRoot = null;
        long spaceFound = 0;
        
        while(e.hasMoreElements()) {
            String thisRoot = "file://localhost" + "/" + e.nextElement().toString();
            try {
                FileConnection fc = (FileConnection)Connector.open(thisRoot);
                long spaceFoundHr = fc.availableSize();
                fc.close();
                
                if(spaceFoundHr > spaceFound) {
                    spaceFound = spaceFoundHr;
                    biggestRoot = thisRoot;
                }
            }catch(Exception e2) {
                e2.printStackTrace();
            }
            
        }
        
        String baseFolder = biggestRoot + "umobiledata";
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
    
    public static EXEStrMgr getInstance() {
        return getInstance(MLearnPlayerMidlet.getInstance());
    }
    
    public static EXEStrMgr getInstance(MLearnPlayerMidlet host) {
        if(instance == null) {
            instance = new EXEStrMgr(host);
        }
        
        return instance;
    }
    
    public void setPref(String key, String val){
        prefs.setPref(key, val);
    }
    
    public String getPref(String key) {
        return prefs.getPref(key);
    }
    
    public void delPref(String key) {
        prefs.prefs.remove(key);
    }
    
    public void saveAll() {
        savePrefs();
        if(logStrm != null) { logStrm.flush(); }
        if(debugStrm != null) { debugStrm.flush(); }
    }
    
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
