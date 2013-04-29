/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.paiwastoon.mlearnplayer;

import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * 
 * 
 * @author mike
 */
public class MLearnPreferences {
    
    public Hashtable prefs;
    
    public MLearnPreferences() {
        prefs = new Hashtable();
    }
    
    public static byte[] toByteArray(Hashtable ht) {
        int numElements = ht.size();
        byte[] b = null;
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream dout = new DataOutputStream(bout);
            dout.writeInt(numElements);
            Enumeration e = ht.keys();
            while(e.hasMoreElements()) {
                Object key = e.nextElement();
                Object val = ht.get(key);
                dout.writeUTF(key.toString());
                dout.writeUTF(val.toString());
            }
            b = bout.toByteArray();
            bout.close();
            dout.close();
        }catch(IOException e) {
            e.printStackTrace();
        }
        
        return b;
    }
    
    public static Hashtable fromByteArray(byte[] b) {
        Hashtable ht = new Hashtable();
        try {
            ByteArrayInputStream bin = new ByteArrayInputStream(b);
            DataInputStream din = new DataInputStream(bin);
            int numElements = din.readInt();
            for(int i = 0; i < numElements; i++) {
                String key = din.readUTF();
                String val = din.readUTF();
                ht.put(key, val);
            }
            
            din.close();
            bin.close();
        }catch(IOException e) {
            e.printStackTrace();
        }
        return ht;
    }
    
    //load the hashtable from an array of bytes
    public void load(byte[] b) {
        prefs = fromByteArray(b);
    }
    
    public byte[] save() {
        return toByteArray(prefs);
    }
    
    public String getPref(String key) {
        Object val = prefs.get(key);
        if(val == null) {
            return null;
        }else {
            return val.toString();
        }
    }
    
    public void setPref(String key, String value) {
        prefs.put(key, value);
    }
    
}
