/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.toughra.mlearnplayer.test;

import com.toughra.mlearnplayer.EXEStrMgr;
import com.toughra.mlearnplayer.UMTinCan;
import com.toughra.mlearnplayer.datatx.MLCloudConnector;
import com.toughra.mlearnplayer.datatx.MLHTTPRep;
import com.toughra.mlearnplayer.datatx.MLObjectPusher;
import j2meunit.framework.*;
import org.json.me.JSONObject;

/**
 *
 * @author mike
 */
public class UstadMobileTinCanTest extends TestCase {
    
    public UstadMobileTinCanTest() {
        
    }
    
    public UstadMobileTinCanTest(String name, TestMethod method) {
        super(name, method);
    }
    
    
    public Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new UstadMobileTinCanTest("testTinCanAuth", new TestMethod() {
            public void run(TestCase tc) {
                ((UstadMobileTinCanTest)tc).testTinCanAuth();
            }
        }));
        
        suite.addTest(new UstadMobileTinCanTest("testTinCanStmt", new TestMethod() {
            public void run(TestCase tc) {
                ((UstadMobileTinCanTest)tc).testTinCanStmt();
            }
        }));
        return suite;
    }
    
    public void testTinCanAuth() {
        MLCloudConnector cloudCon = MLCloudConnector.getInstance();
        //String username = UstadMobileUnitTest.getTestSetting("validuser");
        //String password = UstadMobileUnitTest.getTestSetting("validpassword");
        String username = "0798258092";
        String password = "12345";
        int result = cloudCon.checkLogin(username, password);
        assertEquals("Valid username and password auth OK", 200, result);
    }
    
    public void testTinCanStmt() {
        EXEStrMgr mgr = EXEStrMgr.getInstance(null);
        EXEStrMgr.lg(10, "Log file opened by Ustad Mobile Unit Tests");
        assertTrue("EXEStorage Manager available", mgr != null);
        
        
        //String username = UstadMobileUnitTest.getTestSetting("validuser");
        //String password = UstadMobileUnitTest.getTestSetting("validpassword");
        
        String username = "0798258092";
        String password = "12345";
        
        JSONObject actorObj = EXEStrMgr.getInstance().getTinCanActor(username);
        JSONObject pageStmt = UMTinCan.makePageViewStmt(
                "http://www.ustadmobile.com/xapi/test", 
                "testpage", "Page Testing", "en-US", (1000*60), actorObj);
        
        EXEStrMgr.getInstance().setCloudUser(username);
        EXEStrMgr.getInstance().setCloudPass(password);
        
        EXEStrMgr.getInstance().queueTinCanStmt(pageStmt);
        
        
        MLObjectPusher pusher = new MLObjectPusher();
        
        MLHTTPRep httpRep = new MLHTTPRep();
        long processedBytes = httpRep.sendOwnLogs(pusher);
        assertTrue("Processed and sent log bytes", processedBytes > 0);
        //mgr.queueTinCanStmt();
    }
    
}
