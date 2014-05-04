/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.toughra.mlearnplayer.datatx;

import com.toughra.mlearnplayer.ContentDownloadThread;
import com.toughra.mlearnplayer.MLearnUtils;

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
     * @param baseURL 
     */
    public ContentDownloadThread makeDownloadThread(String baseURL) {
        String[] urlParts = MLearnUtils.getURLParts(baseURL);
        if(connector == null) {
            connector = new MLCloudConnector(urlParts[MLearnUtils.URLPART_HOST]
                    + ":" + urlParts[MLearnUtils.URLPART_PORT]);
        }
        
        return new ContentDownloadThread(connector, baseURL);
    }
    
}

