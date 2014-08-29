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

import com.toughra.mlearnplayer.ContentDownloadThread;
import com.toughra.mlearnplayer.EXEStrMgr;
import com.toughra.mlearnplayer.MLearnPlayerMidlet;
import com.toughra.mlearnplayer.MLearnUtils;
import com.toughra.mlearnplayer.xml.XmlNode;
import java.io.ByteArrayOutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

/**
 *
 * @author mike
 */
public class ContentDownloader {
    
    /** Main instance of the class */
    private static ContentDownloader mainInstance;
    
    /** Download Threads ongoing */
    private ContentDownloadThread[] downloadThreads;
    
    /** MLCloudConnector object to use for content download */
    private MLCloudConnector connector;
    
    /**
     * Returns instance of ContentDownloader
     */
    public static ContentDownloader getInstance() {
        if(mainInstance == null) {
            mainInstance = new ContentDownloader();
        }
        return mainInstance;
    }
    
    public MLCloudConnector getConnector() {
        if(connector == null) {
            if(connector == null) {
                String hostname = "svr2.ustadmobile.com";
                String port = "8010";
                String serverString = hostname+":"+port;
                try {
                    connector = new MLCloudConnector(serverString);
                }catch(Exception e) {
                    EXEStrMgr.lg(121, "Something bad with opening connection to cloud server", e);
                }
            }
        
        }
        return connector;
    }
    
    public int getCourseLinkByID(String courseID) {
        int result = -1;
        try {
            
            /**
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            
           String cloudServer = "svr2.ustadmobile.com:8010";
            StringBuffer url = new StringBuffer(
                    cloudServer + CLOUD_GETCOURSEBYID_PATH)
                    .append("?id=").append(courseID);
                    
            System.out.println(url.toString());
            
            MLCloudRequest getCourseLinkByIDRequest = new MLCloudSimpleRequest(this, 
                    url.toString(),null);
            EXEStrMgr.lg(23, "MLCloudConnect: Connection opened");
            
            result = doRequest(getCourseLinkByIDRequest, bout, new Hashtable());
            if(result == 200) {
                System.out.println("Success!");
            }

            String serverSays = new String(bout.toByteArray());
            **/
            
        }catch(Exception e) {
            EXEStrMgr.lg(121, "Something bad with getCourseLinkByID", e);
        }
        
        return result;
    }
    
    
    
    /**
     * Will start the download of a piece of content from a given URL
     * 
     * @param baseURL - base http url eg. http://servername[:port]/some/dir/ (can be with or without trailing / )
     * @param destDir - destination directory in a format that can be understood by FileConnection (eg: file://localhost/root1/ustadmobileContent/courseFolder)
     */
    public ContentDownloadThread makeDownloadThread(String baseURL, String destDir) {
        String[] urlParts = MLearnUtils.getURLParts(baseURL);

        return new ContentDownloadThread(getConnector(), baseURL, destDir);
    }
    
    /**
     * For a given XmlNode representing a <file> object - figure out which version
     * should be downloaded for this device.
     * 
     * @param fileNode XmlNode representing <file> tag
     * @return String of filename to download (adjusted for image resolution, media format, etc) if required
     */
    public static String getFileVersionToDownload(XmlNode fileNode) {
        String filename = fileNode.getTextChildContent(0);
        boolean isAudioFile = fileNode.hasAttribute("audioformats");
        boolean isVideoFile = fileNode.hasAttribute("videoformats");
        
        if(fileNode.hasAttribute("resolutions")) {
            String resolutions = fileNode.getAttribute("resolutions");
            int screenIndex = MLearnUtils.getScreenIndex(resolutions);
            filename = MLearnUtils.getImageNameForSize(screenIndex, filename);
        }else if(isAudioFile || isVideoFile) {
            String attrName = null;
            String[] formatNames = null;
            if(isAudioFile) {
                attrName = "audioformats";
                formatNames= MLearnUtils.AUDIOFORMAT_NAMES;
            }else if(isVideoFile) {
                attrName = "videoformats";
                formatNames = MLearnUtils.VIDEOFORMAT_NAMES;
            }
            
            String mediaFormats = fileNode.getAttribute(attrName);
            String[] supportedFormats = MLearnUtils.getSupportedMediaCached();
            boolean[] availableFormats = MLearnUtils.formatListToBooleans(
                    formatNames, mediaFormats);
            boolean[] formatsToUse = MLearnUtils.cutUnsupportedFormats(
                    supportedFormats, formatNames, availableFormats);
            int mediaFormatToUse = MLearnUtils.getPreferredFormat(formatsToUse);
            filename = MLearnUtils.reworkMediaURI(mediaFormatToUse, 
                            formatNames, filename);
        }
        
        return filename;
    }
    
    /**
     * Given the manifest - check to make sure that all files required have been
     * downloaded OK
     * 
     * @param manifest XmlNode representing the content manifest
     * @param destDir Destination dir on the filesystem e.g. file://localhost/root/downloaded-dir
     * 
     * @return "OK" if directory exists, and all files from manifest downloaded, error messages otherwise
     * @throws Exception 
     */
    public static String checkAllFilesDownloaded(XmlNode manifest, String destDir) throws Exception{
        String errMsg = null;
        boolean allFilesDownloaded = false;
        Vector fileElements = manifest.findChildrenByTagName("file", true);
        FileConnection dirCon = (FileConnection)Connector.open(destDir);
        
        if(!dirCon.exists()) {
            errMsg = "Directory " + destDir + " does not exist";
        }
        
        if(errMsg == null && !dirCon.isDirectory()) {
            errMsg = "" + destDir + " is not a directory";
        }
        
        
        try {
            if(errMsg == null) {
                //get a list of the directory
                Enumeration dirList = dirCon.list();
                Vector dirContentVector = new Vector();
                while(dirList.hasMoreElements()) {
                    dirContentVector.addElement(dirList.nextElement());
                }

                for(int i = 0; i < fileElements.size(); i++) {
                    String fileNameToFind = 
                            ContentDownloader.getFileVersionToDownload(
                            ((XmlNode)fileElements.elementAt(i)));
                    if(!dirContentVector.contains(fileNameToFind)) {
                        //File is missing
                        errMsg += "File " + fileNameToFind + " is missing";
                    }
                }
                if(errMsg == null) {
                    allFilesDownloaded = true;
                }
            }
        }catch(Exception e) {
            EXEStrMgr.lg(140, "Failed to actually download all required files", e);
        }finally {
            dirCon.close();
        }
        
        if(allFilesDownloaded == true) {
            return "OK";
        }else {
            return errMsg;
        }
        
    }
    
}

