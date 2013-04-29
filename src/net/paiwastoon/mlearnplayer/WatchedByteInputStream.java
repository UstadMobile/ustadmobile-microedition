/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.paiwastoon.mlearnplayer;
import java.io.*;

/**
 *
 * @author mike
 */
public class WatchedByteInputStream extends ByteArrayInputStream{
    public WatchedByteInputStream(byte[] buf) {
        super(buf);
    }
    
    public WatchedByteInputStream(byte[] buf, int offset, int length) {
        super(buf, offset, length);
    }

    public synchronized void close() throws IOException {
        super.close();
        super.buf = null;
    }
    
    
}
