/*
 * Ustad Mobile (Micro Edition App)
 * 
 * Copyright 2011-2014 UstadMobile Inc. All rights reserved.
 * www.ustadmobile.com
 *
 * Ustad Mobile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version with the following additional terms:
 * 
 * All names, links, and logos of Ustad Mobile and Toughra Technologies FZ
 * LLC must be kept as they are in the original distribution.  If any new
 * screens are added you must include the Ustad Mobile logo as it has been
 * used in the original distribution.  You may not create any new
 * functionality whose purpose is to diminish or remove the Ustad Mobile
 * Logo.  You must leave the Ustad Mobile logo as the logo for the
 * application to be used with any launcher (e.g. the mobile app launcher).
 * 
 * If you want a commercial license to remove the above restriction you must
 * contact us and purchase a license without these restrictions.
 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 
 * Ustad Mobile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
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
