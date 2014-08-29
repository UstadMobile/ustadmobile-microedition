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

import com.toughra.mlearnplayer.datatx.ContentDownloader;
import com.toughra.mlearnplayer.datatx.MLCloudConnector;
import com.toughra.mlearnplayer.datatx.MLCloudRequest;
import com.toughra.mlearnplayer.datatx.MLCloudSimpleRequest;
import com.toughra.mlearnplayer.xml.GenericXmlParser;
import com.toughra.mlearnplayer.xml.XmlNode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 *
 * @author mike
 */

public class ContentDownloadThread extends Thread{
    
    /** The directory url the content comes from (containing index xml) */
    public String baseURL;
    
    /** The cloud connector object */
    private MLCloudConnector cloudConnector;
    
    /**
     * The destination directory that we are going to save the content to: 
     * in a form that can be provided to FileConnection e.g. file://localhost/some/dir
     */
    public String destDir;
    
    /**
     * Create a new ContentDownloadthread for the given content baseURL
     * and using the given cloudConnector object
     * 
     * @param cloudConnector MLCloudConnector object that connects to server
     * @param baseURL URL of directory e.g. http://server[:port]/dir/contentdir/
     */
    public ContentDownloadThread(MLCloudConnector cloudConnector, String baseURL, String destDir) {
        this.baseURL = baseURL;
        
        //make sure the last character is a trailing slash
        if(this.baseURL.charAt(this.baseURL.length()-1) != '/') {
            this.baseURL += "/";
        }
        
        this.cloudConnector = cloudConnector;
        this.destDir = destDir;
    }
    
    public void run() {
        try {
            runDownload(); //runs the runDownload in the thread.
            
        }catch(Exception e) {
            EXEStrMgr.lg(50, "Error whilst running download thread", e);
        }
        
    }
    
    public void runDownload() {
        try {
            XmlNode indexFile = getMicroEdIndex();
            Vector filesToDownload = indexFile.findChildrenByTagName("file", 
                    true);
            
            //make sure that destDir has a trailing /
            String destDirBase = MLearnUtils.checkTrailingSlash(destDir);
            
            //generate the base directory
            FileConnection fcon = (FileConnection)Connector.open(destDir);
            if(!(fcon.exists() && fcon.isDirectory())) {
                //get rid of it first if a non directory object is in our way
                if(fcon.exists()) {
                    fcon.delete();
                }
                
                fcon.mkdir();
            }
            fcon.close();
            fcon = null;
            
            int numFiles = filesToDownload.size();
            
            for(int i = 0; i < numFiles; i++) {
                XmlNode currentFileNode = (XmlNode)filesToDownload.elementAt(i);
                String filename = ContentDownloader.getFileVersionToDownload(
                        currentFileNode);
                
                String fileURL = baseURL + filename;
                System.out.println("Attempt to download " + fileURL);
                MLCloudSimpleRequest.downloadURLToFile(cloudConnector, fileURL, 
                        destDirBase + filename, 5);
            }
            //Checking if all files of the course has been downloaded.
            //checkAllFilesDownloaded()..
        }catch(Exception e) {
            //we try and do soemthing...
            e.printStackTrace();
        }
        int x = 0;
    }
    
    /**
     * Retrieve the XML Listing manifest from the server
     * 
     * @return XMLNode representing the Manifest XML
     */
    public XmlNode getMicroEdIndex() throws UnsupportedEncodingException, IOException, XmlPullParserException, Exception{
        String indexURL = this.baseURL + "ustadpkg_me.xml";
        
        MLCloudSimpleRequest indexRequest = new MLCloudSimpleRequest(
                cloudConnector, indexURL, new Hashtable());
        
        //temp: put this in an array
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        int responseStatus = cloudConnector.doRequest(indexRequest, bout, 
                new Hashtable());
        if(responseStatus == 200) {
            String respStr = new String(bout.toByteArray());
            ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
            InputStreamReader reader = new InputStreamReader(bin, "UTF-8");
            KXmlParser kParser = new KXmlParser();
            kParser.setInput(reader);
            GenericXmlParser gParser = new GenericXmlParser();
            XmlNode contentIndex = gParser.parseXML(kParser, true);
            return contentIndex;
        }
        
        
        return null;
    }
    
}