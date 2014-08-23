/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.toughra.mlearnplayer.test;

import com.toughra.mlearnplayer.EXEStrMgr;
import com.toughra.mlearnplayer.MLearnUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import org.json.me.JSONException;
import org.json.me.JSONObject;

/**
 * @author mike
 */
public class UstadMobileUnitTest extends j2meunit.midletui.TestRunner {
    
    public static JSONObject testSettings;
    
    public void startApp() {
        com.sun.lwuit.Display.init(this);
        
        try {
            InputStream in = getClass().getResourceAsStream(
                    "/res/test/test-properties.json");
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            MLearnUtils.copyStrm(in, bout);
            String jsonStr = new String(bout.toByteArray());
            UstadMobileUnitTest.testSettings = new JSONObject(jsonStr);
        }catch(IOException ioe) {
            EXEStrMgr.lg(152, "IOException getting test settings", ioe);
        }catch(JSONException je) {
            EXEStrMgr.lg(151, "Exception getting test setting", je);
        }
               
        start(new String[] {"com.toughra.mlearnplayer.test.UstadMobileBaseTest",
            "com.toughra.mlearnplayer.test.UstadMobileTinCanTest"});
    }

    public static String getTestSetting(String key) {
        String retVal = null;
        try {
            if(testSettings != null) {
                retVal = testSettings.getString(key);
            }
        }catch(JSONException e) {
            EXEStrMgr.lg(150, "Exception getting test setting", e);
        }
        return retVal;
    }
    
    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
    }
}
