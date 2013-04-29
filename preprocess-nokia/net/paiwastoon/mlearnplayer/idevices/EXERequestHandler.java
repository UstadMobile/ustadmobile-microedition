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
import net.paiwastoon.mlearnplayer.MLearnPlayerMidlet;

/**
 *
 * @author mike
 */
public class EXERequestHandler implements DocumentRequestHandler{

    private MLearnPlayerMidlet hostMidlet;
    public String htmlStr;
    
    
    public EXERequestHandler(MLearnPlayerMidlet hostMidlet, String htmlStr) {
        this.hostMidlet = hostMidlet;
        this.htmlStr = htmlStr;
    }
    
    public InputStream resourceRequested(DocumentInfo di) {
        String url = di.getUrl();
        
        if(url.equalsIgnoreCase("idevice://current/")) {
            String htmlCompleteStr = "<html><body>" + htmlStr + "</body></html>";
            di.setEncoding("UTF-8");
            try {return new ByteArrayInputStream(htmlCompleteStr.getBytes("UTF-8")); }
            catch(IOException e) {e.printStackTrace();}
        }else {
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
