/*
 * 
 */
package net.paiwastoon.mlearnplayer.idevices;

import com.sun.lwuit.html.DocumentInfo;
import java.io.InputStream;
import java.io.IOException;

/**
 *
 * @author mike
 */
public interface EXERequestSubHandler {
    
    /*
     * When a request matches this sub handler this method will be called
     */
    public InputStream handleSubRequest(DocumentInfo di) throws IOException;
}
