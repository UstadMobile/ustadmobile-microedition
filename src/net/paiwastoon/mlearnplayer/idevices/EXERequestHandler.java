/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.paiwastoon.mlearnplayer.idevices;

import com.sun.lwuit.html.DocumentInfo;
import com.sun.lwuit.html.DocumentRequestHandler;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import net.paiwastoon.mlearnplayer.MLearnPlayerMidlet;

/**
 *
 * @author mike
 */
public class EXERequestHandler implements DocumentRequestHandler{

    private MLearnPlayerMidlet hostMidlet;
    public String htmlStr;
    
    private Vector subHandlers;
    private Vector subHandlerPrefixes;
    private int numSubHandlers = 0;
    
    public EXERequestHandler(MLearnPlayerMidlet hostMidlet, String htmlStr) {
        this.hostMidlet = hostMidlet;
        this.htmlStr = htmlStr;
    }
    
    public void addSubHandler(String prefix, EXERequestSubHandler handler) {
        if(subHandlers == null) {
            subHandlers = new Vector(2);
            subHandlerPrefixes = new Vector(2);
        }
        
        subHandlerPrefixes.addElement(prefix);
        subHandlers.addElement(handler);
        numSubHandlers++;
    }
    
    public InputStream resourceRequested(DocumentInfo di) {
        String url = di.getUrl();
        boolean matchesSubHandler = false;
        
        if(url.equalsIgnoreCase("idevice://current/")) {
            String htmlCompleteStr = "<html><body style='direction: rtl'>" + htmlStr + "</body></html>";
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
