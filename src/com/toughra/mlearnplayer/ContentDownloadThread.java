/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.toughra.mlearnplayer;

import com.toughra.mlearnplayer.datatx.MLCloudConnector;
import com.toughra.mlearnplayer.datatx.MLCloudSimpleRequest;
import com.toughra.mlearnplayer.xml.GenericXmlParser;
import com.toughra.mlearnplayer.xml.XmlNode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
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
     * Create a new ContentDownloadthread for the given content baseURL
     * and using the given cloudConnector object
     * 
     * @param cloudConnector MLCloudConnector object that connects to server
     * @param baseURL URL of directory e.g. http://server[:port]/dir/contentdir/
     */
    public ContentDownloadThread(MLCloudConnector cloudConnector, String baseURL) {
        this.baseURL = baseURL;
        
        //make sure the last character is a trailing slash
        if(this.baseURL.charAt(this.baseURL.length()-1) != '/') {
            this.baseURL += "/";
        }
        
        this.cloudConnector = cloudConnector;
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
        }catch(Exception e) {
            //we try and do soemthing...
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