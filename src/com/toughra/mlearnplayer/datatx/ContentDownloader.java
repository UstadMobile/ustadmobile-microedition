/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.toughra.mlearnplayer.datatx;

import com.toughra.mlearnplayer.ContentDownloadThread;
import com.toughra.mlearnplayer.EXEStrMgr;
import com.toughra.mlearnplayer.MLearnPlayerMidlet;
import com.toughra.mlearnplayer.MLearnUtils;
import com.toughra.mlearnplayer.xml.XmlNode;
import java.util.Enumeration;
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
        return mainInstance;
    }
    
    /**
     * Will start the download of a piece of content from a given URL
     * 
     * @param baseURL - base http url eg. http://servername[:port]/some/dir/ (can be with or without trailing / )
     * @param destDir - destination directory in a format that can be understood by FileConnection
     */
    public ContentDownloadThread makeDownloadThread(String baseURL, String destDir) {
        String[] urlParts = MLearnUtils.getURLParts(baseURL);
        if(connector == null) {
            connector = new MLCloudConnector(urlParts[MLearnUtils.URLPART_HOST]
                    + ":" + urlParts[MLearnUtils.URLPART_PORT]);
        }
        
        return new ContentDownloadThread(connector, baseURL, destDir);
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

