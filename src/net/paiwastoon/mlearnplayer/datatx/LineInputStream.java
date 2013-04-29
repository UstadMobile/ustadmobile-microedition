/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.paiwastoon.mlearnplayer.datatx;

import java.io.*;

/**
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
