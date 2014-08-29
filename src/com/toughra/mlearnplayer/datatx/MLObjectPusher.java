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
package com.toughra.mlearnplayer.datatx;

import com.sun.lwuit.io.util.Util;
import javax.microedition.io.*;
import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;
import javax.microedition.io.file.FileConnection;
import com.toughra.mlearnplayer.EXEStrMgr;
import com.toughra.mlearnplayer.MLearnPreferences;
import com.toughra.mlearnplayer.MLearnUtils;

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
        countDown = MLearnUtils.nextRandom(r, REPDELAY);
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
                //check and see if we need to send updated preferences
                if(EXEStrMgr.getInstance().getCloudUser() != null) {
                    //also - send the logs through if that has been set
                    new MLHTTPRep().sendOwnLogs(this);
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
    
    
    
    
}
