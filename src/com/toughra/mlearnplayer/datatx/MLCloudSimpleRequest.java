/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.toughra.mlearnplayer.datatx;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Simple get request wrapper
 * 
 * @author mike
 */
public class MLCloudSimpleRequest implements MLCloudRequest  {
    
    byte[] reqBytes;
    int pos = 0;
    int reqBytesLen = 0;

    /**
     * Makes a new request object representing a request to the given server
     * for a given url
     * 
     * @param connector MLCloudConnector that controls access to the server
     * @param url HTTP URL to request: e.g. http://server[:port]/dir/file.html
     */
    public MLCloudSimpleRequest(MLCloudConnector connector, String url) {
        this.reqBytes = connector.getRequestHeader(url).getBytes();
        this.reqBytesLen = reqBytes.length;
    }

    public void retry() {
        this.pos = 0;
    }
    
    
    public InputStream getInputStream() {
        return new ByteArrayInputStream(reqBytes);
    }
    
    public int read() throws IOException {
        int retVal = -1;
        if(pos < reqBytesLen) {
            retVal = reqBytes[pos];
            pos++;
        }
        
        return retVal;
    }
    
    public byte[] getRequestBytes(MLCloudConnector connector) {
        return reqBytes;
    }
}

