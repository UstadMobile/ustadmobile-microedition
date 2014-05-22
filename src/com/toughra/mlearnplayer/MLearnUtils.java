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

import com.sun.lwuit.Button;
import com.sun.lwuit.Command;
import com.sun.lwuit.Display;
import com.sun.lwuit.Form;
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
import javax.microedition.media.Manager;

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
    
    /**
     * The height at the top to leave (effects videos etc)
     */
    public static final int TOPHEIGHT=10;
    
    /**
     * The height of the menu bar at the bottom (for menu bar etc)
     */
    public static final int BOTTOMHEIGHT=22;
    
    
    static long memFree = 0;
    
    /** Position of Host in return value of getURLParts */
    public static final int URLPART_HOST = 0;
    
    /** Position of Port in return value of getURLParts */
    public static final int URLPART_PORT = 1;
    
    /** Position of Resource in return value of getURLParts */
    public static final int URLPART_RESOURCE = 2;
    
    /** for utilities re screen size 
     * this should match those defined in exelearning
     */
    public static final int[][] SCREENSIZES = new int[][]{
        {100, 100},
        {240, 320},
        {320, 240},
        {480, 640},
        {640, 480}
    };
    
    public static String[] SCREENSIZE_NAMES = new String[] {"100x100", "240x320", "320x240",
        "480x640", "640x480"
    };
    
    /*
     * Audio format names - as we use from EXElearning
     */
    public static final String[] AUDIOFORMAT_NAMES = new String[] {"mp3", "wav", "ogg"};
    
    /**
     * Video format names - as we use from EXElearning
     */
    public static final String[] VIDEOFORMAT_NAMES = new String[] {"mp4", "3gp", "mpg", "ogv"};
    
    /**
     * Hard coded known mime types and the extensions that go to them
     */
    public static Hashtable formatNamesToMime = null;
    
    static {
        formatNamesToMime = new Hashtable();
        formatNamesToMime.put("mp3", "audio/mpeg");
        formatNamesToMime.put("wav", "audio/x-wav");
        formatNamesToMime.put("ogg", "audio/ogg");
        formatNamesToMime.put("mp4", "video/mp4");
        formatNamesToMime.put("3gp", "video/3gp");
        formatNamesToMime.put("ogv", "video/ogg");
        formatNamesToMime.put("mpg", "video/mpeg");
    }
    
    /** The prefix to add when using Connector to open FileConnection object - before the root*/
    public static String FILECON_LOCALPREFIX = "file://localhost/";
    
    /** Cached results from querying for supported media */
    public static String[] supportedMediaFormats = null;
    
    /** The File seperator character */
    public static char FILE_SEP = '/';
    
    /**
     * Will return true if this looks like an image (ends .png .jpg .jpeg
     * .gif)
     * 
     * @param filename
     * @return true if it's an image false otherwise
     */
    public static boolean fileNameIsImage(String filename) {
        if(filename == null) {
            return false;
        }
        
        filename = filename.toLowerCase();
        
        boolean isImg = filename.endsWith(".jpg")  || filename.endsWith(".png")
                || filename.endsWith(".jpeg") || filename.endsWith(".gif");
        return isImg;
    }
    
    /**
     * Will figure out what should be the name for an image according to the device
     * size index.  e.g. change imagename.jpg to imagename-100x100.jpg
     * 
     * 
     * @param deviceSize the index as per SCREENSIZE
     * @param imageBaseName e.g. imagename.jpg
     * @return combined named - e.g. imagename-100x100.jpg
     */
    public static String getImageNameForSize(int deviceSize, String imageBaseName) {
        if(deviceSize == -1) {
            return imageBaseName;
        }
        
        int extDotPos = imageBaseName.lastIndexOf('.');
        String baseName = imageBaseName.substring(0, extDotPos);
        String extension = imageBaseName.substring(extDotPos);
        String newName = baseName + "-" + SCREENSIZE_NAMES[deviceSize] + extension;
        
        return newName;
    }
    
    
    /**
     * Find the most suitable base size format to be using for images for this screen
     * 
     * @param availableSizes - Boolean array of available sizes
     */
    public static int getScreenIndex(boolean[] availableSizes) {
        return getScreenIndex(Display.getInstance().getDisplayWidth(), Display.getInstance().getDisplayHeight(), availableSizes);
    }
    
    /**
     * Find the most suitable base size format to be using for images for this screen
     * 
     * @param resolutions- Boolean array of available sizes
     */
    public static int getScreenIndex(String resolutions) {
        boolean[] supportedSizes = MLearnUtils.getSupportedScreenSizes(resolutions);
        return getScreenIndex(supportedSizes);
    }
    
    /**
     * Find the most suitable base size format to be using for images etc.
     * @param deviceWidth - width of device screen
     * @param deviceHeight - height of device screen
     * @param availableSizes - boolean array indexed as per SCREENSIZES
     */
    public static int getScreenIndex(int deviceWidth, int deviceHeight, boolean[] availableSizes) {
        float lowestDiffFound = -1;
        int bestMatch = -1;
        for(int i = 0; i < SCREENSIZES.length; i++) {
            if(availableSizes[i]) {
                float diffPercentX = Math.abs(((float)deviceWidth/(float)SCREENSIZES[i][0]) -1);
                float diffPercentY = Math.abs(((float)deviceHeight/(float)SCREENSIZES[i][1]) -1);
                float totalDiff = diffPercentX + diffPercentY;
                if(lowestDiffFound == -1 || totalDiff < lowestDiffFound) {
                    bestMatch = i;
                    lowestDiffFound = totalDiff;
                }
            }
        }
        
        
        return bestMatch;
    }
    
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
        EXEStrMgr.lg(20, msg);
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
            int randomOffset = MLearnUtils.nextRandom(r,length);
            int rIndex = randomOffset + start;
            Object tmp = arrObj[rIndex];
            arrObj[rIndex] = arrObj[i];
            arrObj[i] = tmp;
        }
    }
    
    /**
     * Drop in replacement for using Random with CLDC-1.0
     * 
     * @param r
     * @param max
     * @return 
     */
    public static int nextRandom(Random r, int max) {
        return Math.abs(r.nextInt() % max);
    }
    
    /**
     * Replacement for equalsIgnoreCase
     */
    public static boolean equalsIgnoreCase(String str1, String str2) {
        if(str1 == str2 ){
            return true;
        }
        
        if(str1 == null || str2 == null) {
            return false;
        }
        
        return (str1.toLowerCase().equals(str2.toLowerCase()));
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
            EXEStrMgr.lg(322, "Error getting file content id for " + path);
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
            EXEStrMgr.lg(316, " Exception loading hashtable from " + fileURL, e);
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
     * Utility method to extract the host and port from an HTTP(s) URL.
     * 
     * Static constants represent elements in the array: URLPART_HOST,
     * URLPART_PORT and URLPART_RESOURCE
     * 
     * @param targetURL
     * 
     * @return String array in the form of {URLPART_HOST, URLPART_PORT, URLPART_RESOURCE}
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
    
    /**
     * Splits a string up line by line - removes the \n or \r symbols
     * 
     * @param str
     * @return 
     */
    public static String[] getLines(String str) {
        Vector lines = new Vector();
        int lastLineStart = 0;
        for(int i = 0; i < str.length(); i++) {
            char currentChar = str.charAt(i);
            boolean isNewLineChar = (currentChar == '\n' || currentChar == '\r');
            if(isNewLineChar && i > lastLineStart) {
                //we have found the end of the line
                String lineContent = str.substring(lastLineStart, i);
                lines.addElement(lineContent);
            }
            
            if(isNewLineChar) {
                lastLineStart = i+1;
            }
            
        }
        
        String[] linesStr = new String[lines.size()];
        lines.copyInto(linesStr);
        return linesStr;
    }
    
    /**
     * Finds the start position of attribute in string, and the end of it (e.g. termination
     * of it's quotes)
     * 
     * WARNING: expects attribName= NO WHITE SPACE
     * 
     */
    public static int[] getAttribStartEndPos(String tagHTML, String attribName) {
        String tagHTMLLower = tagHTML.toLowerCase();
        int startPos = tagHTML.indexOf(attribName + "=");
        char quoteChar = tagHTML.charAt(startPos + attribName.length() + 1);
        String endingStr = tagHTML.substring(attribName.length()+3);
        int endPos = tagHTMLLower.indexOf(quoteChar, startPos + attribName.length()+2);
        return new int[] {startPos, endPos};
    }
    
    /**
     * 
     * @param htmlStr
     * @param tagName
     * @param searchFrom
     * @param isLowerCase 
     * @return 
     */
    public static int[] getSingleTagStartAndEndPos(String htmlStr, String tagName, int searchFrom, boolean isLowerCase){
        if(!isLowerCase){
            htmlStr = htmlStr.toLowerCase();
        }
        int startPos = htmlStr.indexOf("<" + tagName.toLowerCase(), searchFrom);
        if(startPos == -1) {
            //it's not here
            return new int[]{-1, -1};
        }
        int endPos = htmlStr.indexOf(">", startPos);
        return new int[] {startPos, endPos};
    }
    
    /**
     * Go through the list of supported media on this device
     */
    public static String[] getSupportedMedia() {
        String[] result = Manager.getSupportedContentTypes(null);
        return result;
    }
    
    /**
     * Go through list of supported media on this device
     * cached OK
     * 
     * @return String array of supported media formats
     */
    public static String[] getSupportedMediaCached() {
        if(MLearnUtils.supportedMediaFormats == null) {
            MLearnUtils.supportedMediaFormats = MLearnUtils.getSupportedMedia();
        }
        
        return MLearnUtils.supportedMediaFormats;
    }
    
    /**
     * Given a list of supported formats (string as exelearning makes) will make
     * an array of booleans.
     * 
     * @param formatNames
     * @param supportedList
     * @return 
     */
    public static boolean[] formatListToBooleans(String[] formatNames, String supportedList) {
        boolean[] retVal = new boolean[formatNames.length];
        supportedList = supportedList.toLowerCase();
        
        //make sure there is a trailing , - we use it for easy searching
        if(supportedList.charAt(supportedList.length()-1) != ',') {
            supportedList += ",";
        }
        for(int i = 0; i < formatNames.length; i++) {
            if(supportedList.indexOf(formatNames[i].toLowerCase()+",") != -1) {
                retVal[i] = true;
            }
        }
        
        return retVal;
    }
    
    /**
     * Eliminate formats that are not supported by the device from the list of supported
     * formats.  Results in boolean list of formats that the package has AND 
     * are supported by the device
     * 
     * @param supportedMimeTypes String array of supported mime types as from getSupportedMedia
     * @param formatNames String array of format names that we are using
     * @param contentFormatList boolean array of content types present in this package
     */
    public static boolean[] cutUnsupportedFormats(String[] deviceSupportedMimeTypes, String[] formatNames, boolean[] contentFormatList) {
        for(int i = 0; i < contentFormatList.length; i++) {
            if(contentFormatList[i]) {//make sure this is a type present in package
                //find out if it's on the list
                Object mimeTypeObj = formatNamesToMime.get(formatNames[i]);
                if(mimeTypeObj == null) {
                    //can't find the mime type = not supported
                    contentFormatList[i] = false;
                }else {
                    boolean mimeIsSupported = false;
                    for(int j = 0; j < deviceSupportedMimeTypes.length; j++) {
                        if(deviceSupportedMimeTypes[j].equals(mimeTypeObj)) {
                            mimeIsSupported = true;
                            j = deviceSupportedMimeTypes.length;//end the loop this way
                        }
                    }
                    if(!mimeIsSupported) {
                        //Sorry - looks like we have to cancel out this format
                        contentFormatList[i] = false;
                    }
                }
            }
        }
        
        return contentFormatList;
    }
    
    /**
     * Returns the first supported format (eg the first index of true in the array)
     * @param formatList list of formats supported
     * @return first index of true in the list, -1 if nothing is supported
     */
    public static final int getPreferredFormat(boolean[] formatList) {
        for(int i = 0; i < formatList.length; i++) {
            if(formatList[i]) {
                return i;
            }
        }
        
        return -1;
    }
    
    /**
     * Determines which media file should actually be used
     * 
     * @param preferredForamt
     * @param formatNames 
     * @param mediaURI 
     */
    public static String reworkMediaURI(int preferredFormat, String[] formatNames, String mediaURI) {
        if(preferredFormat == -1) {
            return null;
        }
        
        String formatName = formatNames[preferredFormat];
        int dotPosMediaURI = mediaURI.lastIndexOf('.');
        String existingExtension = mediaURI.substring(dotPosMediaURI+1);
        if(existingExtension.equals(formatName)) {
            //it's all good now
            return mediaURI;
        }else {
            String newMediaURI = mediaURI.substring(0, dotPosMediaURI) + "."
                    + formatName;
            return newMediaURI;
        }
        
    }
    
    /**
     * Returns the utilizable screen height
     */
    public static final int getUsableScreenHeight() {
        return com.sun.lwuit.Display.getInstance().getDisplayHeight() - BOTTOMHEIGHT - TOPHEIGHT;
    }
    
    /**
     * Make sure the String ends with /
     * @param str - string to add trailing slash to
     */
    public static final String checkTrailingSlash(String str) {
        if(str.charAt(str.length()-1) == '/') {
            return str;
        }else {
            return str + "/";
        }
    }
    
    /**
     * Safely closes a FileConnection and OutputStream
     * @param fCon FileConnection representing the file
     * @param fout OutputStream being used
     * @param message String to add when logging any error that occurs
     */
    public static final int closeFileCon(FileConnection fCon, OutputStream fout, String message) {
        int retStatus = 0;
        if(fout  != null) {
            try {
                fout.close();
            }catch(Exception e1) {
                EXEStrMgr.lg(131, 
                        "Exception closing fileoutput stream ("+message+")", e1);
                retStatus = 1;
            }
        }
        
        if(fCon != null) {
            try {
                fCon.close();
            }catch(Exception e2) {
                EXEStrMgr.lg(131, 
                        "Exception closing fileconnection ("+message+")", e2);
                retStatus = 1;
            }
        }
        
        return retStatus;
    }
    
    /**
     * Looks through the list of screen sizes that are included in this package
     * and returns a boolean array representing which ones are included and 
     * which ones are not
     * 
     * @param screenSizeStr
     * @return boolean array with true for those that are supported
     */
    public static boolean[] getSupportedScreenSizes(String screenSizeStr) {
        boolean[] retVal = new boolean[MLearnUtils.SCREENSIZE_NAMES.length];
        for(int i = 0; i < retVal.length; i++) {
            if(screenSizeStr.indexOf(MLearnUtils.SCREENSIZE_NAMES[i]) != -1) {
                retVal[i] = true;
            }
        }
        
        return retVal;
    }
    
    /**
     * Join multiple paths together - do not allow multiple file separators
     * together - this could lead to malformed uri exceptions
     * 
     * @param paths
     * @return 
     */
    public static String joinPaths(String[] paths) {
        String retVal = new String(paths[0]);
        for(int i = 1; i < paths.length; i++) {
            if(retVal.charAt(retVal.length()-1) != FILE_SEP) {
                retVal += FILE_SEP;
            }
            
            String nextPath = paths[i];
            if(paths[i].length() > 0 && paths[i].charAt(0) == FILE_SEP) {
                nextPath = nextPath.substring(1);
            }
            retVal += nextPath;
        }
        return retVal;
    }
    
    
    /**
     * Join two paths - make sure there is just one FILE_SEP character 
     * between them
     */
    public static String joinPath(String path1, String path2) {
        if(path1.charAt(path1.length()-1) != FILE_SEP) {
            path1 += FILE_SEP;
        }
        
        if(path2.length() > 0 && path2.charAt(0) == FILE_SEP) {
            path2 = path2.substring(1);
        }
        
        return path1 + path2;
    }
    
    /**
     * Check if a file actually exists
     * 
     * @param fileConURI - URI to pass to Connector
     */
    public static boolean fileExists(String fileConURI) {
        boolean result = false;
        FileConnection fCon = null;
        try {
            fCon = (FileConnection)Connector.open(fileConURI, Connector.READ);
            result = fCon.exists();
        }catch(Exception e) {
            EXEStrMgr.lg(70, "Error checking if " + fileConURI + "exists", e);
        }finally {
            closeConnector(fCon, "Exception closing " + fileConURI);
        }
        
        return result;
    }
    
    /**
     * Safely close a file connector object - do not throw exception - this is 
     * used in finally clauses
     */
    public static void closeConnector(FileConnection con, String logMsg) {
        if(con != null) {
            try {
                con.close();
            }catch(Exception e) {
                EXEStrMgr.lg(71, logMsg, e);
            }
        }
    }
    
    
    /**
     * For each string in an array of strings, make a button, set action listener
     * 
     * Each button will be assigned a command - id will be the integer of its
     * index in the label array
     * 
     * @param form : Form to add to (should implement actionListener)
     * @param iconPaths : paths to icon as would be used with Class.getResourceAsStream 
     * - if null follow a pattern to look for iconPathPrefix<index>.png
     * @param iconPathPrefix : Prefix to index of the path to most icons for this menu
     * @param buttonLabels : Array of strings to use for button text
     */
    public static void addButtonsToForm(Form form, String[] buttonLabels, String[] iconPaths, String iconPathPrefix) {
        ActionListener listener = (ActionListener)form;
        for(int i = 0; i < buttonLabels.length; i++) {
            Image iconImg = null;
            InputStream imgIn = null;
            if(iconPaths != null || iconPathPrefix != null) {
                String thisPath = null;
                try {
                    if(iconPaths != null && iconPaths.length > i && iconPaths[i] != null) {
                        thisPath = iconPaths[i];
                    }else {
                        thisPath = iconPathPrefix + i + ".png";
                    }
                    imgIn = form.getClass().getResourceAsStream(thisPath);
                    if(imgIn != null) {
                        iconImg = Image.createImage(imgIn);
                    }
                }catch(Exception e) {
                    EXEStrMgr.lg(160, "Error attempting to load icon path: " 
                            + thisPath, e);
                }finally {
                    if(imgIn != null) {
                        try { imgIn.close(); }
                        catch(IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            
            Command thisCmd = (iconImg == null) ? 
                    new Command(buttonLabels[i], i) :
                    new Command(buttonLabels[i], iconImg, i);
            
            Button thisButton = new Button(thisCmd);
            thisButton.addActionListener(listener);
            form.addComponent(thisButton);
        }
    }
}
