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

import com.sun.lwuit.Graphics;
import com.sun.lwuit.Image;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.io.util.Util;
import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import com.toughra.mlearnplayer.xml.XmlNode;

/**
 * Assortment of utility methods
 * 
 * @author mike
 */
public class MLearnUtils {
    
    public static final int  RESIZE_MAXPROPORTION = 0;
    
    public static final int  RESIZE_BOUND_X = 1;
    
    public static final int  RESIZE_BOUND_Y = 2;
    
    public static final int  RESIZE_FILL = 3;
    
    static long memFree = 0;
    
    /** Position of Host in return value of getURLParts */
    public static final int URLPART_HOST = 0;
    
    /** Position of Port in return value of getURLParts */
    public static final int URLPART_PORT = 1;
    
    /** Position of Resource in return value of getURLParts */
    public static final int URLPART_RESOURCE = 2;
    
    
    public static long checkFreeMem() {
        System.gc();
        
        memFree = Runtime.getRuntime().freeMemory();
        return memFree;
    }
    
    
    /**
     * Converts file://localhost/e:/blah to remove localhost e.g.
     * for use with media components etc.
     * 
     * @param str
     * @return 
     */
    public static String connectorToFile(String str) {
        String retVal = new String(str);
        int lhostPos = retVal.indexOf("localhost");
        if(lhostPos != -1) {
            retVal = retVal.substring(0, lhostPos) + retVal.substring(lhostPos + 9);
        }
        
        return retVal;
    }
   
    /**
     * Prints out the free memory, and checks vs. the last time free printFreeMem
     * was called
     * 
     * @param midlet host midlet
     * @param opMessage message to show alongside (e.g. what is going on before)
     */
    public static void printFreeMem(MLearnPlayerMidlet midlet, String opMessage) {
        long memFreeBefore = memFree;
        long memFreeNow = checkFreeMem();
        long diff = memFreeNow - memFreeBefore;
        String msg = "Memory +/- \t" + diff + " (" + memFreeNow + ") (" + opMessage + ")";
        midlet.logMsg(msg);
        EXEStrMgr.po(msg, EXEStrMgr.DEBUG);
    }
    
    /**
     * Gets the text of the first node to match a given name - useful for
     * &lt;propname&gt;Text val&lt;/propname&gt;
     * @param parent the parent node to search through children of
     * @param nodeName the name of the tag e.g. propname
     * @return the value of text inside the &lt;propname&gt; e.g. Text val
     */
    public static String getTextProperty(XmlNode parent, String nodeName) {
        XmlNode node = (XmlNode)parent.findChildrenByTagName(nodeName, true).elementAt(0);
        return node.getTextChildContent(0);
    }
    
    
    /**
     * Will fire an action event to all listeners in the vector
     * Every element should be an action listener
     * 
     * @param listeners 
     */
    public static void fireActionEventToAll(Vector listeners, ActionEvent evt) {
        if(listeners == null) {
            return;
        }
        
        int numListeners = listeners.size();
        for(int i = 0; i < numListeners; i++) {
            ((ActionListener)listeners.elementAt(i)).actionPerformed(evt);
        }
    }
    
    /**
     * Gets the index in the array of a given item.  Uses strict (object) 
     * equality not .equals - e.g. must refer to exact same object
     * 
     * @param arr Array to look in - single dimensional array
     * @param obj
     * @return 
     */
    public static int indexInArray(Object arr, Object obj) {
        Object[] objArr = (Object[])arr;
        int numItems = objArr.length;
        for(int i = 0; i < numItems; i++) {
            if(objArr[i] == obj) {
                return i;
            }
        }
        
        return -1;
    }
    
    /**
     * Shuffle an array (because java.util.Collections is not in j2me)
     * 
     * @param array Array to shuffle
     * @param start starting pos
     * @param length length to shuffle through
     */
    public static void shuffleArray(Object[] array, int start, int length) {
        Object[] arrObj = (Object[])array;
        Random r = new Random();
        
        for(int i = start; i < (start + length); i++) {
            int randomOffset = r.nextInt(length);
            int rIndex = randomOffset + start;
            Object tmp = arrObj[rIndex];
            arrObj[rIndex] = arrObj[i];
            arrObj[i] = tmp;
        }
    }
    
    
    /**
     * Shuffle the entire array
     * 
     * @param array Array to shuffle
     */
    public static void shuffleArray(Object array) {
        Object[] arrObj = (Object[])array;
        shuffleArray(arrObj, 0, arrObj.length);
    }
    
    /**
     * Resize an image
     * 
     * @param src
     * @param width
     * @param height
     * @param strategy - one of the strategy flags - right now we just run fill
     * @return 
     */
    public static Image resizeImage(Image src, int width, int height, int strategy) {
        Image res = Image.createImage(width, height);
        Graphics g = res.getGraphics();
        g.drawImage(src, 0, 0, width, width);
        return res;
    }
    
    /**
     * Gets the string contents of the file given by path - useful for flat files
     * 
     * @param path
     * @return Contents of the file as a String
     * 
     */
    public static String readFile(String path) {
        String str = null;
        try {
            //we'll use this method because it will read the whole thing adn close it
            ByteArrayInputStream bin = (ByteArrayInputStream)
                IOWatcher.makeWatchedInputStream(path);
            str = Util.readToString(bin);
            bin.close();
        }catch(IOException e) {
            EXEStrMgr.getInstance().p("Error getting file content id for " + path, EXEStrMgr.WARN);
        }
        
        return str;
    }
    /**
     * 
     * Used to find the collection id of a directory
     * 
     * @param dir
     * @return 
     */
    public static String getCollectionID(String dir) {
        String path = dir;
        if(!path.endsWith("/")) {
            path = path + "/";
        }
        String str = MLearnUtils.readFile(path + "execollectionid");
        str = str.trim();
        return str;
    }
    
    /**
     * 
     * Used to find the collection title of a directory
     * 
     * @param dir
     * @return 
     */
    public static String getCollectionTitle(String dir) {
        String path = dir;
        if(!path.endsWith("/")) {
            path = path + "/";
        }
        String str = MLearnUtils.readFile(path + "execollectiontitle");
        str = str.trim();
        return str;
    }
    
    /**
     * Make a boolean array and fill it with the given value
     * 
     * @param value default value to set each element to
     * @param size - length of the array to generate
     * @return boolean[] array filled set to the given value
     */
    public static boolean[] makeBooleanArray(boolean value, int size) {
        boolean[] retVal = new boolean[size];
        for (int i = 0; i < size; i++) {
            retVal[i] = value;
        }
        
        return retVal;
    }
    
    /**
     * Count the number of occurences of value (eg. true/false) in the array
     * 
     * @param arr Array to search through
     * @param val value to look for
     * @return number of times val occurs in arr
     */
    public static int countBoolValues(boolean[] arr, boolean val) {
        int count = 0;
        int size = arr.length;
        for(int i = 0; i < size; i++) {
            if(arr[i] == val) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Localization method to lookup a string from the locale currently in use
     * 
     * @param key string key
     * 
     * @return the translation of that key in the current language
     */
    public static String _(String key) {
        Hashtable ht = MLearnPlayerMidlet.getInstance().localeHt;
        if(ht != null) {
            Object objVal = ht.get(key);
            if(objVal != null) {
                return objVal.toString();
            }
        }
        return key;
    }
    
    /**
     * Puts all the remaining elements from an Enumeration into a vector
     * 
     * @param e Enumeration to use
     * @return Vector with all remaining elements from Enumeration e
     */
    public static Vector enumToVector(Enumeration e) {
        Vector v = new Vector();
        while(e.hasMoreElements()) {
            v.addElement(e.nextElement());
        }
        
        return v;
    }
    
    /**
     * Opens a file in 'append' mode.  If the file does not exist - it will create
     * the file.  If the file exists it will set the pointer to the file size
     * (e.g. append to the end of the file)
     * 
     * @param fileCon FileConnection object for the file
     * @return OutputStream for the file set to end of file
     * 
     * @throws IOException 
     */
    public static OutputStream openFileOutputAppendMode(FileConnection fileCon) throws IOException{
        OutputStream out = null;
        if(fileCon.exists()) {
            out = fileCon.openOutputStream(fileCon.fileSize());
        }else {
            fileCon.create();
            out = fileCon.openOutputStream();
        }
        
        return out;
    }

    /**
     * Simply copy from the inputstream to the outputstream
     * 
     * @param in InputStream 
     * @param out  OutputStream
     * @throws IOException 
     */
    public static void copyStrm(InputStream in, OutputStream out) throws IOException {
        Util.copy(in, out);
    }
    
    
    /**
     * Loads the Hashtable from the given fileURL
     * 
     * TODO: Point this to MLearnPreferences implementation - this is a duplicate
     * 
     * @param fileURL URL pointing to the serialized hashtable
     * @return Hashtable from the fileURL
     */
    public static Hashtable loadHTFile(String fileURL) {
        return loadHTFile(fileURL, false);
    }
    
    /**
     * Loads a datainputstream made ht table
     * 
     * TODO: Point this to MLearnPreferences implementation - this is a duplicate
     * 
     * @param fileURL - url to load (passed dir to connector.open)
     * @param autoBlank - if it does not exist - return a blank one
     * @return Hashtable from fileURL
     */
    public static Hashtable loadHTFile(String fileURL, boolean autoBlank) {
        FileConnection con = null;
        InputStream fin = null;
        Hashtable retVal = null;
        try {
            con = (FileConnection)Connector.open(fileURL);
            if(!con.exists()) {
                retVal = new Hashtable();
            }else {
                int fSize = (int)con.fileSize();
                fin = con.openInputStream();
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                copyStrm(fin, bout);
                retVal = MLearnPreferences.fromByteArray(bout.toByteArray());
                bout.close();
            }
        }catch(IOException e) {
            EXEStrMgr.po(e, " Exception loading hashtable from " + fileURL);
        }finally {
            try { fin.close();} catch(Exception e2) {};
            try { con.close();} catch(Exception e3) {};
        }
        
        return retVal;
    }
    
    /**
     * Saves an int to a file.  Util method for tracking replication
     * @param i
     * @param fileName
     * @return 
     */
    public static int writeIntToFile(int i, String fileName) {
        FileConnection fcon = null;
        DataOutputStream dout = null;
        try {
            fcon = (FileConnection)Connector.open(fileName);
            if(!fcon.exists()) {
                fcon.create();
            }
            dout = fcon.openDataOutputStream();
            dout.writeInt(i);
            dout.flush();
            dout.close();
            fcon.close();
        }catch(IOException e) {
            try { dout.close(); } catch(Exception ig) {}
            try { fcon.close(); } catch(Exception ig2) {}
            return -1;
        }
        
        return 0;
    }
    
    /**
     * Gets an int from a file.  Returns Integer.MIN_VALUE if file
     * does not exist
     * 
     * @param fileName
     * @return 
     */
    public static int getIntFromFile(String fileName) {
        FileConnection fcon = null;
        DataInputStream din = null;
        int retVal = 0;
        try {
            fcon = (FileConnection)Connector.open(fileName);
            if(!fcon.exists()) {
                retVal = Integer.MIN_VALUE;
            }else {
                din = fcon.openDataInputStream();
                retVal = din.readInt();
                din.close();
                fcon.close();
            }
        }catch(IOException e) {
            try { din.close(); } catch(Exception ig) {}
            try { fcon.close(); } catch(Exception ig2) {}
            return Integer.MIN_VALUE;
        }
        
        return retVal;
    }
    
    /**
     * Utility method to extract the host and port from an HTTP(s) URL
     * 
     * @param targetURL
     * @return 
     */
    public static String[] getURLParts(String targetURL) {
        //see if we have http at the start
        boolean startsHTTP = targetURL.toLowerCase().startsWith("http:");
        boolean startsHTTPS = targetURL.toLowerCase().startsWith("https:");
        
        if(startsHTTP || startsHTTPS) {
            int slashPos2 = targetURL.indexOf("/") + 1;// because it will be //
            targetURL = targetURL.substring(slashPos2+1);
        }
        
        int portDelPos = targetURL.indexOf(':');
        int firstSlashPos = targetURL.indexOf('/');
        
        String targetResource = targetURL.substring(firstSlashPos);
        String targetHost = targetURL;
        String targetPort = startsHTTPS ? "443" : "80";
        if (portDelPos != -1 && portDelPos < firstSlashPos) {
            // has port specified
            targetHost = targetURL.substring(0, portDelPos);
            if (firstSlashPos == -1) {
                targetPort = targetURL.substring(portDelPos + 1);
            } else {
                targetPort = targetURL.substring(portDelPos + 1, firstSlashPos);
            }
        } else {
            // no port - using default
            if (firstSlashPos != -1) {
                targetHost = targetURL.substring(0, firstSlashPos);
            }
        }
        
        return new String[] {targetHost, targetPort, targetResource};
    }
}
