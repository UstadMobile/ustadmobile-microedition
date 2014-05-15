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

/**
 * Utility child class of ByteArrayInputStream.
 * 
 * When the stream is closed will make sure that the byte array is set to
 * null to free memory
 * 
 * @author mike
 */
public class WatchedByteInputStream extends ByteArrayInputStream{
    
    /**
     * Constructs an input stream given a byte buffer
     * @param buf 
     */
    public WatchedByteInputStream(byte[] buf) {
        super(buf);
    }
    
    /**
     * Constructs an input stream given the buffer, offset and length
     * @param buf
     * @param offset
     * @param length 
     */
    public WatchedByteInputStream(byte[] buf, int offset, int length) {
        super(buf, offset, length);
    }

    /**
     * Closes stream and sets to byte buffer to null
     * @throws IOException 
     */
    public synchronized void close() throws IOException {
        super.close();
        super.buf = null;
    }
    
    
}
