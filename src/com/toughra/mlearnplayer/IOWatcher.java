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

package com.toughra.mlearnplayer;

import java.io.*;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;


/**
 * Checks and finds out what has not been properly closed etc.
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
