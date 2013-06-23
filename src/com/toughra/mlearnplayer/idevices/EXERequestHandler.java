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
package com.toughra.mlearnplayer.idevices;

import com.sun.lwuit.html.DocumentInfo;
import com.sun.lwuit.html.DocumentRequestHandler;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import com.toughra.mlearnplayer.MLearnPlayerMidlet;

/**
 * Implements DocumentRequestHandler and takes an HTML string as an argument.
 * When it receives a request for idevice://current/ it will return HTMLStr
 * wrapped with &lt;html&gt;&lt;body&gt; etc. 
 * 
 * When it receives any other request (e.g. for an image resources in a page) it
 * will use the MIDLet's stream resource requestor - which then refers to the 
 * currently in use package directory.
 * 
 * If the package is known to be in a right to left language it will add a 
 * style='direction: rtl' attribute to the body tag to make text run right to left.
 * 
 * @author mike
 */
public class EXERequestHandler implements DocumentRequestHandler{

    /** Our host midlet*/
    private MLearnPlayerMidlet hostMidlet;
    
    /** the string of the HTML body */
    public String htmlStr;
    
    /** subhandlers as required */
    private Vector subHandlers;
    /** subhandler prefixes to look through */
    private Vector subHandlerPrefixes;
    
    /** num sub handlers operational*/
    private int numSubHandlers = 0;
    
    /**
     * Constructor
     * @param hostMidlet our host midlet
     * @param htmlStr the html of the body that be displayed for idevice://current
     */
    public EXERequestHandler(MLearnPlayerMidlet hostMidlet, String htmlStr) {
        this.hostMidlet = hostMidlet;
        this.htmlStr = htmlStr;
    }
    
    /**
     * Adds a sub handler
     * @param prefix The prefix to of the URL to match against (eg idevice://something
     * @param handler EXERequestSubHandler to pass to
     */
    public void addSubHandler(String prefix, EXERequestSubHandler handler) {
        if(subHandlers == null) {
            subHandlers = new Vector(2);
            subHandlerPrefixes = new Vector(2);
        }
        
        subHandlerPrefixes.addElement(prefix);
        subHandlers.addElement(handler);
        numSubHandlers++;
    }
    
    /**
     * The DocumentRequestHandler main method.   Given a DocumentInfo with a URL
     * returns the htmlStr wrapped by html and body tags if the URL is 
     * idevice://current/ , then looks to see if any sub handlers claim this
     * URL, then finally passes the URL to the host midlet so that resources
     * (e.g. images etc) get found
     * 
     * @param di DocumentInfo containing a URL
     * @return InputStream for the resource requested as above
     */
    public InputStream resourceRequested(DocumentInfo di) {
        String url = di.getUrl();
        boolean matchesSubHandler = false;
        
        if(url.equalsIgnoreCase("idevice://current/")) {
            String dirSubStr = "";
            if(hostMidlet.myTOC.currentIsRTL) {
                 dirSubStr = " style='direction: rtl'";
            }
            String htmlCompleteStr = "<html><body" + dirSubStr + ">" + htmlStr + "</body></html>";
            di.setEncoding("UTF-8");
            try {return new ByteArrayInputStream(htmlCompleteStr.getBytes("UTF-8")); }
            catch(IOException e) {e.printStackTrace();}
        }
        
        for(int i = 0; i < numSubHandlers; i++) {
            if(url.startsWith(subHandlerPrefixes.elementAt(i).toString())) {
                EXERequestSubHandler handler = (EXERequestSubHandler)subHandlers.elementAt(i);
                try {
                    return handler.handleSubRequest(di);
                }catch(IOException e) {
                    System.err.println("Exception with subhandler for " + url + " : " + e.toString());
                    return null;
                }
            }
        }
        
        if (!matchesSubHandler) {
            String resURL = url.substring(18);//chop off idevice:// etc
            try {
                InputStream stream = hostMidlet.getInputStreamReaderByURL(resURL);
                
                return stream;
            }catch(Exception e) {
                e.printStackTrace();
            }
        }
        
        return null;
    }
    
}
