/*
 * Ustad Mobile (Micro Edition App)
 * 
 * Copyright 2011-2014 UstadMobile Inc. All rights reserved.
 * www.ustadmobile.com
 *
 * Ustad Mobile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version with the following additional terms:
 * 
 * All names, links, and logos of Ustad Mobile and Toughra Technologies FZ
 * LLC must be kept as they are in the original distribution.  If any new
 * screens are added you must include the Ustad Mobile logo as it has been
 * used in the original distribution.  You may not create any new
 * functionality whose purpose is to diminish or remove the Ustad Mobile
 * Logo.  You must leave the Ustad Mobile logo as the logo for the
 * application to be used with any launcher (e.g. the mobile app launcher).
 * 
 * If you want a commercial license to remove the above restriction you must
 * contact us and purchase a license without these restrictions.
 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 
 * Ustad Mobile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.toughra.mlearnplayer;

import com.sun.lwuit.io.Storage;
import com.sun.lwuit.util.Resources;
import java.io.*;
import java.util.*;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import org.json.me.JSONException;
import org.json.me.JSONObject;

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
 * Property replication change tracking works by having a key called replist
 * with property names with a : at each end - e.g. :propname1::propname2: 
 * 
 * When a property gets changed it is added to the list to be replicated.  Once
 * the change is done the property is reset.
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
    
    /** The standard email address string to turn user into mbox
     * 
     */
    static String MBOX_POSTFIX = "ustadmobile.com";
    
    /** Preferences storage that is made as a serialized hashtable*/
    public MLearnPreferences prefs;
    
    /**The file name that is used for the serialized hashtable */
    final String pStrFname = "preferences";
    
    /**The filename used for localized resources*/
    public static final String localeResName = "localize";
    
    /**PrintStream used to save debugging information*/
    PrintStream debugStrm;
    
    /**PrintStream used for activity logging (-activity.log)*/
    //PrintStream logStrm;
    OutputStream logOut;
    
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
    
    /**The key for setting the preference replicate list*/
    public static final String KEY_REPLIST = "replist";
    
    /** The key representing the current cloud user */
    public static final String KEY_CLOUDUSER = "clouduser";
    
    /** The key representing the current cloud password */
    public static final String KEY_CLOUDPASS = "cloudpass";
    
    /** The key representing the learner name */
    public static final String KEY_LEARNERNAME = "learnername";
    
    /** A list of preferences that should not be replicated to/from the cloud*/
    public static final String[] DONOTREPKEYS = {KEY_REPLIST, KEY_CLOUDUSER, KEY_CLOUDPASS};
    
    /** The deliminator being used */
    public static final char LOGDELIMINATOR = '|';
    
    //VERBs for logging
    public static final String VERB_ANSWERED = "answered";
    
    public static final String VERB_FAILED = "failed";
    
    public static final String VERB_SAW = "saw";
    
    
    /** the log name that is going to be in use during this session */
    public String sessionActivityLogName;
    
    /**
     * Minimum error level severity to trigger writing to log
     * 0-99 - common/expected
     * 100-199 - not nice but ok-ish
     * 300+ - nasty stuff that should not happen
     */
    //#ifndef ERRORLOGTHRESHOLD
    public static int ERROR_LOG_THRESHOLD = 0;
    //#endif
    
    //#ifdef ERRORLOGTHRESHOLD
    //#expand public static int ERROR_LOG_THRESHOLD = %ERRORLOGTHRESHOLD% ;
    //#endif
    
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
        sessionActivityLogName = getDateLogStr() + "-activity.log";
    }
    
    /**
     * Make a UUIID
     */
    public String makeUUID() {
        Random r = new Random(System.currentTimeMillis());
        String uuid = String.valueOf(Math.abs(r.nextLong()));
        return uuid;
    }
    
    public JSONObject getTinCanActor() {
        return getTinCanActor(getCloudUser());
    }
    
    /**
     * Make a JSONObject representing the TinCan actor for the user
     * 
     * @param cloudUser Username of user to make actor object for
     * 
     * @return Tin Can actor with mbox, name and objectType set
     */
    public JSONObject getTinCanActor(String cloudUser) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("mbox", "mailto:" +  cloudUser + "@" + MBOX_POSTFIX);
            obj.put("name", getCloudUser());
            obj.put("objectType", "Agent");
        }catch(JSONException e) {
            EXEStrMgr.lg(170, "Exception creating tin can actor json", e);
        }
        
        //String jsonText = obj.toString();
        
        return obj;
    }
    
    public String getCloudUser() {
        return prefs.getPref(KEY_CLOUDUSER);
    }
    
    public void setCloudUser(String cloudUser) {
        prefs.setPref(KEY_CLOUDUSER, cloudUser);
    }
    
    public String getCloudPass() {
        return prefs.getPref(KEY_CLOUDPASS);
    }
    
    public void setCloudPass(String cloudPass) {
        prefs.setPref(KEY_CLOUDPASS, cloudPass);
    }
    
    public void doCloudLogout() {
        prefs.deletePref(KEY_CLOUDUSER);
        prefs.deletePref(KEY_CLOUDPASS);
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
            
        System.out.println("Loaded localization res for " + localeToUse);
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
    public static String getDateLogStr() {
        Calendar cal = Calendar.getInstance();
        return  cal.get(Calendar.YEAR) + "-" +
                    pad1(cal.get(Calendar.MONTH)+1) + "-" + pad1(cal.get(Calendar.DAY_OF_MONTH));
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
    private OutputStream openLogStream(String logname) {
        OutputStream out = null;
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
            
            
        }catch (Exception e) {
            e.printStackTrace();
        }
        
        return out;
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
     * Logs a line to the activity log
     * @param device - Idevice object to log for
     * @param questionId - questionId this applies to (if applicable) -1 otherwise
     * @param timeOpen - ms it was open for
     * @param numCorrect - number of questions answered correctly
     * @param numCorrectFirst - number of questions answered correctly first attempt
     * @param numAnswered - number of questions answered (attempted)
     * @param verb - the verb for this action
     * @param score - score achieved by this interaction
     * @param maxScorePossible  - the maximum score that was possible from this...
     * @param answer - the answer the student provided
     * @param remarks - additional remarks from the device
     */
    public synchronized static void lg(Idevice device, int questionId, int timeOpen, int numCorrect, int numCorrectFirst, int numAnswered, String verb, int score, int maxScorePossible, String answer, String remarks) {
        getInstance().l('A', null, device, questionId, timeOpen, numCorrect, numCorrectFirst, numAnswered,  verb, score, maxScorePossible, answer, remarks);
    }
    
    /**
     * This is used to log error messages
     */
    public static void lg(int errCode, String msg) {
        lg(errCode, msg, null);
        
    }
    
  
    
    /**
     * This is used to log error messages
     */
    public static void lg(int errCode, String msg, Exception e) {
        if(e != null) {
            System.err.println(msg);
            e.printStackTrace();
            msg += "Exception: " + e.toString();
        }
        
        if(errCode > ERROR_LOG_THRESHOLD) {
            getInstance().l('D', errCode + ":" + msg, null, 0, 0, 0, 0, 0, null, 0, 0, null, null);
        }
        
        if(ERROR_LOG_THRESHOLD == 0) {
            //desperate debugging mode
            Runtime rt = Runtime.getRuntime();
            String memMsg = "Total Memory : " + rt.totalMemory() + " Free Memory " + rt.freeMemory();
            getInstance().l('D', memMsg, null, 0, 0, 0, 0, 0, null, 0, 0, null, null);
            
            try {getInstance().logOut.flush(); }
            catch(IOException e2) {
                System.err.println("exception flushing log output");
            }
        }
    }
    
    /**
     * Logs a line to the activity log
     * @param device - Idevice object to log for
     * @param questionId - questionId this applies to (if applicable) -1 otherwise
     * @param timeOpen - ms it was open for
     * @param numCorrect - number of questions answered correctly
     * @param numCorrectFirst - number of questions answered correctly first attempt
     * @param numAnswered - number of questions answered (attempted)
     * @param verb - the verb for this action
     * @param score - score achieved by this interaction
     * @param maxScorePossible  - the maximum score that was possible from this...
     * @param answer - the answer the student provided
     * @param remarks - additional remarks from the device
     */
    public void l(Idevice device, int questionId, int timeOpen, int numCorrect, int numCorrectFirst, int numAnswered, String verb, int score, int maxScorePossible, String answer, String remarks) {
        getInstance().l('A', null, device, questionId, timeOpen, numCorrect, numCorrectFirst, numAnswered,  verb, score, maxScorePossible, answer, remarks);
    }
    
    /**
     * Swaps between running the logging to a buffer or to it's normal file
     * 
     * This is used before/after sending log info
     * 
     * @param swapOp 
     */
    public void swap(int swapOp) {
        synchronized(this) {
            if(swapOp == SWAP_TOBUF) {
                //close the existing stream
                if(logOut != null) { 
                    try {
                        logOut.flush();
                        logOut.close();
                    }catch(IOException e) {
                        System.err.println("Something bad swapping to buffer");
                        e.printStackTrace();
                    }
                    
                    logOut = null;
                }

                try {
                    String logName = getDateLogStr() + "-activity.log";
                    FileConnection fileCon = (FileConnection)openLogFCs.get(logName);
                    fileCon.close();
                    openLogFCs.remove(logName);
                }catch(Exception e) {
                    e.printStackTrace();
                    System.out.println("Problem closing stream for rep swap " + e.toString());
                }

                //make logStrm as a bytearrayoutput stream for now
                logOut = new ByteArrayOutputStream();
                bufOut = (ByteArrayOutputStream)logOut;
                
                //remove this item from the files open hashtable
                return;
            }else if(swapOp == SWAP_TOFILE) {
                byte[] bufNow = null;
                try {
                    if(logOut != null) {
                        logOut.flush();
                        bufNow = bufOut.toByteArray();
                        logOut.close();
                    }
                }catch(IOException e) {
                    System.err.println("Something bad in swap back to file");
                    e.printStackTrace();
                }

                logOut = openLogStream("activity");
                try {
                    logOut.write(bufNow);
                }catch(IOException e) {
                    System.err.println("Error writing log buffer out... " + e.toString());
                }
                System.out.println("Swapped back to running on file");
                return;
            }
        }
    }
    
    /**
     * Removes chars that could be an issue in logs.  Replaces new line with \n
     * @param in
     * @return 
     */
    private String sanitizeLogString(String in) {
        if(in.length() == 0) {
            return Idevice.BLANK;
        }
        int charIdx = 0;
        char[][] replaceChars = { {'\n', 'n'}, {'\r', 'r'} };
        for(int i = 0; i < replaceChars.length; i++) {
            while((charIdx = in.indexOf(replaceChars[i][0])) != -1) {
                in = in.substring(0, charIdx) + "\\" + replaceChars[i][1]; 
                if(charIdx < in.length()-1) {
                    in += in.substring(charIdx+1);
                }
            }
        }
        
        in = in.replace('|', '/');
        
        return in;
    }
    
    /**
     * Queue a TinCan statement that should be sent to the TinCan server 
     * 
     * @param tinCanStatement - JSON object representing an entire tincan statement
     */
    public void queueTinCanStmt(JSONObject stmt) {
        l('T', stmt.toString(), null, 0, 0, 0, 0, 0, null, 0, 0, null, null);
    }
    
    /**
     * Makes a log of the learner's activity.  If required it can swap between
     * using the memory buffer and writing direct to the file (e.g. when the log
     * file itself is being transmitted to the teachers phone)
     * 
     * @param logType - Type of log (A activity D debug
     * @param debugStr - if this is a debug log - the whole string to log
     * @param device - Idevice object to log for
     * @param questionId - questionId this applies to (if applicable) -1 otherwise
     * @param timeOpen - ms it was open for
     * @param numCorrect - number of questions answered correctly
     * @param numCorrectFirst - number of questions answered correctly first attempt
     * @param numAnswered - number of questions answered (attempted)
     * @param verb - the verb for this action
     * @param score - score achieved by this interaction
     * @param maxScorePossible  - the maximum score that was possible from this...
     * @param answer - the answer the student provided
     * @param remarks - additional remarks from the device
     */
    public synchronized void l(char logType, String debugStr, Idevice device, int questionId, int timeOpen, int numCorrect, int numCorrectFirst, int numAnswered, String verb, int score, int maxScorePossible, String answer, String remarks) {
        StringBuffer logLine = new StringBuffer();
        logLine.append(logType).append(':');
        
        // Field 0 - PHONE TIMESTAMP (UNIXTIME)
        long timestamp = new Date().getTime() / 1000;
        logLine.append(timestamp).append(LOGDELIMINATOR);
        
        if(logOut == null && baseFolder != null) {
            System.out.println("Opening activity log append mode ");
            logOut = openLogStream("activity");
            System.out.println("Opened logstrm: ");
        }
        
        if(logType == 'A') {

            //Field 1 - COLLECTION ID
            if(device.hostMidlet.currentColId != null) {
                logLine.append(device.hostMidlet.currentColId);
            }else {
                logLine.append(" ");
            }

            logLine.append(LOGDELIMINATOR);

            //Field 2 - PACKAGE ID
            logLine.append(device.hostMidlet.currentPkgId).append(LOGDELIMINATOR);

            //Field 3 - PAGE NAME
            String pageHref = device.hostMidlet.currentHref;
            if(pageHref.endsWith(".xml")) {
                pageHref = pageHref.substring(0, pageHref.length()-4);
            }
            logLine.append(pageHref).append(LOGDELIMINATOR);

            //Field 4 - Idevice ID
            logLine.append(device.ideviceId).append(LOGDELIMINATOR);

            //Field 5 - Question ID
            logLine.append(questionId).append(LOGDELIMINATOR);

            //Field 6 - The idevice type
            logLine.append(device.getDeviceTypeName()).append(LOGDELIMINATOR);

            //Field 7 - Device Type (0=INFO 1=QUIZ)
            logLine.append(device.getLogType()).append(LOGDELIMINATOR);

            //Field 8 - Time open (in ms)
            logLine.append(timeOpen).append(LOGDELIMINATOR);

            //Field 9, 10, 11 - # correct answers, # answers correct first time, #questions attempted
            logLine.append(numCorrect).append(LOGDELIMINATOR).append(numCorrectFirst)
                    .append(LOGDELIMINATOR).append(numAnswered).append(LOGDELIMINATOR);

            ///Field 12 - the Verb
            logLine.append(verb).append(LOGDELIMINATOR);

            //Field 13,14 - Score and max score possible
            logLine.append(score).append(LOGDELIMINATOR).append(maxScorePossible).append(LOGDELIMINATOR);

            //Field 15 - The answer given
            logLine.append(sanitizeLogString(answer)).append(LOGDELIMINATOR);

            //Field 16 - the remarks
            logLine.append(sanitizeLogString(remarks));
        }else {
            //this is just a debug string
            logLine.append(sanitizeLogString(debugStr));
        }
            
        if(logOut != null) {
            String line = logLine.toString();
            try {
                logOut.write(line.getBytes("UTF-8"));
                logOut.write((int)'\n');
            }catch(IOException e) {
                System.err.println("Error writing to log");
                e.printStackTrace();
            }
        }else {
            System.err.println("Logstrm is null");
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
        String currentVal = prefs.getPref(key);
        if(currentVal != null) {
            if(!currentVal.equals(val) && isReplicatePref(key)) {
                //this preference has been changed and should be replicated
                addKeyToReplicate(key);
            }
        }
        prefs.setPref(key, val);
    }
    
    /**
     * Set a preference key as it has loaded from cloud (e.g. don't add to
     * replication list)
     * 
     * @param key
     * @param val
     */
    public void setPrefDirect(String key, String val) {
        prefs.setPref(key, val);
    }
    
    /**
     * This will check if the given key should be replicated
     * 
     * @param keyname Name of the key to check
     * 
     * @return true if it should be, false otherwise
     */
    private boolean isReplicatePref(String keyname) {
        for(int i = 0; i < DONOTREPKEYS.length; i++) {
            if(keyname.equals(DONOTREPKEYS[i])) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Adds a preference key to the list of those that need replicated
     * 
     * @param keyname
     * 
     */
    private void addKeyToReplicate(String keyname) {
        String repList = getPref(KEY_REPLIST);
        
        if(repList == null) {
            repList = "";
        }
        
        String keyNameCoded = ":" + keyname + ":";
        if(repList.indexOf(keyNameCoded) == -1) {
            repList += keyNameCoded;
            prefs.setPref(KEY_REPLIST, repList);
        }
    }
    
    /**
     * Gets a list of the keys that need replicated to the cloud
     */
    public String[] getReplicateList() {
        Vector repList = new Vector();
        String prefListStr = prefs.getPref(KEY_REPLIST);
        if(prefListStr == null) {
            return new String[] {};//zero length string means nothing to replicate
        }
        
        int pos = 0;
        while(pos < prefListStr.length()) {
            //search for the next property - all property names start with a :
            int nextPos = prefListStr.indexOf(':', pos+1);
            String propName = prefListStr.substring(pos+1, nextPos);
            repList.addElement(propName);
            pos = nextPos + 1;//e.g. after the ending : for that property
        }
        String[] repArr = new String[repList.size()];
        repList.copyInto(repArr);
        
        return repArr;
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
        try {
            if(logOut != null) { logOut.flush();}
        }catch(IOException e) {
            System.err.println("Error 101");
        }
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
            lg(301, e.toString());
        }
    }
    
    public static void writeDebugInfo() {
        lg(20, "Platform is: " + System.getProperty("microedition.platform"));
    }
    
}
