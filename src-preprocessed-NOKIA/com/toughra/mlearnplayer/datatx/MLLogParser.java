/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.toughra.mlearnplayer.datatx;
import java.io.*;
import java.util.*;
import com.toughra.mlearnplayer.EXEStrMgr;

/**
 *
 * @author mike
 */
public class MLLogParser {
    
    //num of string elements on each line
    static final int LOGELEMENTS = 11;
    
    private static final int IDX_TSTAMP = 0;
    private static final int IDX_UUID = 1;
    private static final int IDX_COLID = 2;
    private static final int IDX_PKGID = 3;
    private static final int IDX_SLIDE = 4;
    private static final int IDX_IDEVICEID = 5;
    private static final int IDX_DEVTYPE = 6;
    private static final int IDX_TYPE = 7;
    private static final int IDX_TIME = 8;
    private static final int IDX_SCOREDATA = 9;
    
    private static final String TYPE_QUIZ = "quiz";
    private static final String TYPE_INFO = "info";
    
    public static final String[] STOREKEYS = {"correctfirst", "solved", "attempted"};
    
    public static void incrementKey(Hashtable ht, String keyName, int val) {
        Object valObj = ht.get(keyName);
        int curVal = 0;
        if(valObj != null) {
            curVal = ((Integer)valObj).intValue();
        }
        curVal += val;
        ht.put(keyName, new Integer(curVal));
    }
    
    public static void dumpHT(Hashtable ht) {
        Enumeration keys = ht.keys();
        String str = "";
        while(keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object val = ht.get(key);
            str += key + " = " + val + "\n";
        }
        EXEStrMgr.po(str, EXEStrMgr.DEBUG);
    }
    
    
    public static void parseLog(InputStream in, Hashtable valueStore, Hashtable pkgComboTbl) throws IOException {
        LineInputStream lin = new LineInputStream(in);
        String line;
        
        while((line = lin.readLine()) != null) {
            System.out.println(line);
            String[] parts = new String[LOGELEMENTS];
            int partCount = 0;
            int lastSpace = 0;
            boolean doneFinding = false;
            
            
            for(int nextSpace = line.indexOf(' ', lastSpace + 1); doneFinding == false; nextSpace = line.indexOf(' ', lastSpace + 1)) {
                if(nextSpace == -1) {
                    doneFinding = true;
                    nextSpace = line.length();
                }
                parts[partCount] = line.substring(lastSpace, nextSpace);
                partCount++;
                lastSpace = nextSpace + 1;
            }
            
            String prefix = parts[IDX_UUID] + "/"+ parts[IDX_COLID] + "/"
                    + parts[IDX_PKGID];
            
            incrementKey(valueStore, prefix + ":time", Integer.parseInt(parts[IDX_TIME]));
            
            if(parts[IDX_TYPE].equals(TYPE_QUIZ)) {
                int[] scores = new int[3];
                String sData = parts[IDX_SCOREDATA];
                int sPos1 = sData.indexOf('/');
                int sPos2 = sData.indexOf('/', sPos1+1);
                scores[0] = Integer.parseInt(sData.substring(0, sPos1));
                scores[1] = Integer.parseInt(sData.substring(sPos1 + 1, sPos2));
                scores[2] = Integer.parseInt(sData.substring(sPos2+1));
                
                for(int i = 0; i < STOREKEYS.length; i++) {
                    incrementKey(valueStore, prefix+":" + STOREKEYS[i], scores[i]);
                }
            }
            
            //do pkg combo cache
            Object studentTbl = pkgComboTbl.get(parts[IDX_UUID]);
            Hashtable studentHashtable = null;
            boolean mustAdd = false;
            if(studentTbl == null) {
                studentHashtable = new Hashtable();
                mustAdd = true;
            }else {
                studentHashtable = (Hashtable)studentTbl;
            }
            String colPkg = parts[IDX_COLID] + "/" + parts[IDX_PKGID];
            studentHashtable.put(colPkg, colPkg);
            if(mustAdd) {
                pkgComboTbl.put(parts[IDX_UUID], studentHashtable);
            }
        }
    }   
    
}
