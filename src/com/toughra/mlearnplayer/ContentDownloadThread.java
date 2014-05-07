/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
            
        }catch(Exception e) {
            //should do or say something
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