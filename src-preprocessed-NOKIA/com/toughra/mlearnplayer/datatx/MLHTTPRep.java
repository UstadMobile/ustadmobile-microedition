/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.toughra.mlearnplayer.datatx;

import com.sun.lwuit.io.ConnectionRequest;
import com.sun.lwuit.io.NetworkManager;
import com.sun.lwuit.io.util.Util;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.file.FileConnection;
import com.toughra.mlearnplayer.EXEStrMgr;
import com.toughra.mlearnplayer.MLearnUtils;

/**
 *
 * @author mike
 */
public class MLHTTPRep {
     
    public void sendLogs() {
        try {
            //make sure that the server knows that we exist
            checkCallHome();
            
            String url = EXEStrMgr.getInstance().getPref("httptx.url");
            //if the url is null it means no data transmission
            if(url == null) {
                return;
            }
            String logBaseDir =EXEStrMgr.getInstance().getPref("basefolder")
                    + "/logrx";
            FileConnection dirCon = (FileConnection)Connector.open(logBaseDir);
            Vector fileList = MLearnUtils.enumToVector(dirCon.list("*activity.log", true));
            dirCon.close();
                        
            for(int i = 0; i < fileList.size(); i++) {
                String bname = fileList.elementAt(i).toString();
                try {
                    sendLog(logBaseDir + "/" + bname, bname);
                    EXEStrMgr.po("Sent " + bname, EXEStrMgr.DEBUG);
                }catch(IOException e) {
                    EXEStrMgr.po(e, " Exception sending " + bname);
                }
                
            }
        }catch(Exception e) {
            EXEStrMgr.po(e, "Exception sending logs ");
        }
    }
    
    public void checkCallHome() {
        String calledHome = EXEStrMgr.getInstance().getPref("player.calledhome");
        long lastCallHomeTime = 0L;
        if(calledHome != null) {
            lastCallHomeTime = Long.parseLong(calledHome);
        }
        long timeNow = System.currentTimeMillis() / 1000;
        
        //has it been more than an hour since we called home?
        if((timeNow - lastCallHomeTime) > 3600) {
            //do the call home to make sure that the phone is talking before it goes out
            String url = EXEStrMgr.getInstance().getPref("httptx.url");
            
            Hashtable params = new Hashtable();
            String uuid = EXEStrMgr.getInstance().getPref("uuid");
            params.put("action", "callhome");
            params.put("uuid", uuid);
            params.put("learnername", EXEStrMgr.getInstance().getPref("learnername"));
            
            try {
                HttpMultipartRequest req = new HttpMultipartRequest(
                    url, params, "filecontent", uuid, "text/plain", null);
                byte[] response = req.send();
                String respStr  = new String(response);
                if(req.respCode == 200) {
                    long utime = System.currentTimeMillis() / 1000;
                    EXEStrMgr.getInstance().setPref(
                            "player.calledhome", String.valueOf(utime));
                }
            }catch(Exception e) {
                EXEStrMgr.po(e, "Error running callhome");
            }
            
        }
    }
    
    public void sendLog(String fileURI, String basename) throws Exception{
        FileConnection fcon = (FileConnection)Connector.open(fileURI, Connector.READ, true);
        String sentFileURI = fileURI + ".sent";
        int alreadySent = MLearnUtils.getIntFromFile(sentFileURI);
        Hashtable params = new Hashtable();
        
        String studentUUID = basename.substring(0, basename.indexOf('-'));
        Hashtable stdNames = MLServerThread.getInstance().getStudentNames();
        String studentName = stdNames.get(studentUUID) != null ? 
                stdNames.get(studentUUID).toString() : "unknown";
        params.put("student_uuid", studentUUID);
        params.put("student_learnername", studentName);
        
        String dataUsername = EXEStrMgr.getInstance().getPref("httptx.username");
        String dataPass = EXEStrMgr.getInstance().getPref("httptx.password");
        
        params.put("txuser", dataUsername);
        params.put("txpass", dataPass);
        
        int fileSize = (int)fcon.fileSize();
        fcon.close();
        
        if(alreadySent >= fileSize) {
            EXEStrMgr.po("Already sent " + fileURI + " : " + alreadySent + " bytes", EXEStrMgr.DEBUG);
            return;
        }
        
        //TODO: Need to skip alreadySent bytes
        
        /*
        ByteArrayOutputStream bout= new ByteArrayOutputStream(fileSize);
        InputStream fin = fcon.openInputStream();
        MLearnUtils.copyStrm(fin, bout);
        fin.close();
        
        byte[] fileBytes = bout.toByteArray();
        bout.close();
        bout = null;
        */
        
        
        String url = EXEStrMgr.getInstance().getPref("httptx.url");
        HttpMultipartRequest req = new HttpMultipartRequest(
                url, params, "filecontent", basename, "text/plain", fileURI);
        if(alreadySent > 0) {
            req.skipBytes = alreadySent;
        }
        
        byte[] response = req.send();
        if(response != null) {
            String responseStr = new String(response);
            int respCode = req.respCode;
            
            if(respCode == 200) {
                //everything OK - write a log of how much we have sent
                MLearnUtils.writeIntToFile(fileSize, sentFileURI);
                EXEStrMgr.po("Updated sent file " + sentFileURI, EXEStrMgr.DEBUG);
            }
            
            EXEStrMgr.po("Server says : " + respCode + " : " + responseStr, EXEStrMgr.DEBUG);
        }else {
            EXEStrMgr.po("Null came back ... ", EXEStrMgr.WARN);
        }
    }
    
}
