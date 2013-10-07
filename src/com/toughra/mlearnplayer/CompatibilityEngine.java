/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.toughra.mlearnplayer;

/**
 *
 * @author mike
 */
public class CompatibilityEngine {
    
    static boolean nokiaUI = false;
    
    /**
     * Does this phone support the Nokia UI package?
     * @return true if the phone supports NokiaUI, otherwise false
     */
    public static boolean isNokiaUI() {
        return nokiaUI;
    }
    
    /**
     * This is the startup routine to see what we know about this device
     */
    public static void doDetection() {
        try {
            Class.forName("com.nokia.mid.ui.DeviceControl");
            nokiaUI = true;
        }catch(ClassNotFoundException e) {}
    }
    
}
