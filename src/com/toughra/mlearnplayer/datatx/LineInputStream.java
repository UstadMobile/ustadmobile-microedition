/*
 * Ustad Mobile (Micro Edition App)
 * 
 * Copyright 2011-2014 UstadMobile Inc. All rights reserved.
 * www.ustadmobile.com
 *
 * Ustad Mobile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version with the following additional terms:
 * 
 * All names, links, and logos of Ustad Mobile and Toughra Technologies FZ
 * LLC must be kept as they are in the original distribution.  If any new
 * screens are added you must include the Ustad Mobile logo as it has been
 * used in the original distribution.  You may not create any new
 * functionality whose purpose is to diminish or remove the Ustad Mobile
 * Logo.  You must leave the Ustad Mobile logo as the logo for the
 * application to be used with any launcher (e.g. the mobile app launcher).
 * 
 * If you want a commercial license to remove the above restriction you must
 * contact us and purchase a license without these restrictions.
 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 
 * Ustad Mobile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
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
