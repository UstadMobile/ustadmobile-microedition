/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.toughra.mlearnplayer;


import java.util.Date;

/**
 *
 * @author mike
 */
public class PerfLog {
    
    public static final boolean PERFLOGON = true;
    
    static long timer;
   
    public static void logit(String message) {
        System.err.println(message);
    }
    
    public static void startTimer() {
        timer = new Date().getTime();
    }
    
    public static void stopTimer(String message) {
        long opTimeSecs = ((new Date().getTime()) - timer)/1000;
        System.out.println(message + " : " + opTimeSecs + "s");
    }
    
}
