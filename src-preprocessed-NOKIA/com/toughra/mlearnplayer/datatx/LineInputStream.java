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
package com.toughra.mlearnplayer.datatx;

import java.io.*;

/**
 * Simple utility to read a file line by line
 * 
 * @author mike
 */
public class LineInputStream extends InputStream{
    
    InputStream src;
    
    static int nline = (int)'\n';
    
    static int cret = (int)'\r';
    
    public LineInputStream(InputStream src) {
        this.src = src;
    }

    public int read() throws IOException {
        return src.read();
    }

    public int available() throws IOException {
        return src.available();
    }

    public void close() throws IOException {
        src.close();
    }

    public synchronized void mark(int readlimit) {
        src.mark(readlimit);
    }

    public boolean markSupported() {
        return src.markSupported();
    }

    public int read(byte[] b) throws IOException {
        return src.read(b);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return src.read(b, off, len);
    }

    public synchronized void reset() throws IOException {
        src.reset();
    }

    public long skip(long n) throws IOException {
        return src.skip(n);
    }
    
    public String readLine() throws IOException{
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        int b = src.read();
        
        //we have reached the end of the file already if we get -1 now
        if(b == -1) {
            return null;
        }
        
        do{
            if(b == nline || b == cret) {
                break;
            }
            bout.write(b);
        }while((b = src.read()) != -1);
        
        return new String(bout.toByteArray());
    }
    
    
}
