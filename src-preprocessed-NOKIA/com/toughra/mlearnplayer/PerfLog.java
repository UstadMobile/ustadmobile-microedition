/*
 * Ustad Mobil.  
 * Copyright 2011-2013 Toughra Technologies FZ LLC.
 * www.toughra.com
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.toughra.mlearnplayer;


import java.util.Date;

/**
 * Performance logging utlities
 * 
 * @author mike
 */
public class PerfLog {
    
    public static final boolean PERFLOGON = true;
    
    static long timer;
   
    /**
     * Simply puts a message out on System.err
     * 
     * @param message message to print
     */
    public static void logit(String message) {
        System.err.println(message);
    }
    
    /** Starts a timer
     * 
     */
    public static void startTimer() {
        timer = new Date().getTime();
    }
    
    /**
     * Ends the timer
     * @param message  message to show (e.g. what was done)
     */
    public static void stopTimer(String message) {
        long opTimeSecs = ((new Date().getTime()) - timer)/1000;
        System.out.println(message + " : " + opTimeSecs + "s");
    }
    
}
