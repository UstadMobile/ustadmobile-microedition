/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.toughra.mlearnplayer.test;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

/**
 *
 * @author mike
 */
public class UstadMobileBaseTest extends TestCase {
    
    public UstadMobileBaseTest() {
        
    }
    
    public UstadMobileBaseTest(String name, TestMethod method) {
        super(name, method);
    }
    
    public Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new UstadMobileBaseTest("testTrue", new TestMethod() {
            public void run(TestCase tc) {
                ((UstadMobileBaseTest)tc).testTrue();
            }
        }));
        
        return suite;
    }
    
    public void testTrue() {
        assertTrue("Truth is true", true);
    }
    

}
