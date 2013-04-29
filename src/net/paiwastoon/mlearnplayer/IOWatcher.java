/*
 * Checks and finds out what has not been properly closed etc.
 */
package net.paiwastoon.mlearnplayer;

import java.io.*;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;


/**
 *
 * @author mike
 */
public class IOWatcher  {
    
    
    /**
     * 
     * Provides a ByteArrayInputStream for a given file - ensuring that the file
     * itself is actually closed.
     * 
     * @param fileURL
     * @return
     * @throws IOException 
     */
    public static InputStream makeWatchedInputStream(String fileURL) throws IOException {
        FileConnection con = (FileConnection)Connector.open(fileURL);
        if(!(con.exists() && con.canRead())) {
            con.close();
            throw new IOException("Watched input Stream File not found or cannot read: " + fileURL);
        }
        int fileSize = (int)con.fileSize();
        byte[] buf = new byte[fileSize];
        
        InputStream in = con.openInputStream();
        in.read(buf, 0, fileSize);
        in.close();
        con.close();
        
        WatchedByteInputStream ret = new WatchedByteInputStream(buf);
        return ret;
    }
    
}
