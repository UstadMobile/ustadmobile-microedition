/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.toughra.mlearnplayer.test;

import com.toughra.mlearnplayer.datatx.MLCloudConnector;
import j2meunit.framework.*;

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
        return suite;
    }
    
    public void testTinCanAuth() {
        MLCloudConnector cloudCon = MLCloudConnector.getInstance();
        String username = UstadMobileUnitTest.getTestSetting("validuser");
        String password = UstadMobileUnitTest.getTestSetting("validpassword");
        int result = cloudCon.checkLogin(username, password);
        assertEquals("Valid username and password auth OK", result, 200);
    }
    
}
