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
 * Abstract class representing an idevice that can be shown to the user.  Each
 * idevice is represented by an &lt;idevice&gt; tag in the page.xml file and then
 * has parameters that are specified to that idevice.
 *
 * Idevice can have one of two modes: either providing an LCDUI displayable
 * object or a LWUIT form
 *
 * @author mike
 */
public abstract class Idevice {

    /** Indicates that we are returning an LWUIT Form - the default*/
    public final static int MODE_LWUIT_FORM = 0;

    /** Indicates that we are providing a DISPLAYABLE object - (unimplemented) */
    public final static int MODE_DISPLAYABLE = 1;

    /**
     * Tells us what kind of displayable object we provide
     * @return MODE_LWUIT_FORM or MODE_DISPLAYABLE
     */
    public abstract int getMode();

    /**
     * The host midlet
     */
    protected MLearnPlayerMidlet hostMidlet;
    
    /**
     * The ideviceId as per the xml file
     */
    public String ideviceId;
    
    //tracker of when device use started -1 means not attempted
    long startTime = -1;
    
    //use to check how many answers user got right first time
    protected int correctFirst = 0;
     
    //use to follow how many were eventually gotten correct
    protected boolean[] hadCorrect;
    
    /**
     * Indicates this is an info only idevice
     */
    public static int LOG_INFO = 0;
    
    /**
     * Indicates this is a quiz idevice
     */
    public static int LOG_QUIZ = 1;
    
    /**
     * deviceType: 'info' or 'quiz'
     */
    String deviceType;
    
    /**
     * Log types that are implemented
     */
    static final String[] logTypes = new String[] {"info", "quiz"};

    /**
     * Constructor
     * 
     * @param host the host midlet
     */
    public Idevice(MLearnPlayerMidlet host) {
        hostMidlet = host;
    }
    
    /**
     * If MODE_LWUIT_FORM this should return an LWUIT form that runs the idevice
     * 
     * @return Form that runs the idevice
     */
    public com.sun.lwuit.Form getForm() {
        return null;
    }

    /**
     * If MODE_DISPLAYABLE should return a displayable object that runs the idevice
     * currently not implemented
     * 
     * @return Displayable
     */
    public javax.microedition.lcdui.Displayable getDisplayable() {
        return null;
    }

    /**
     * This method is called when an idevice should start it's media / activities
     * etc
     */
    public void start() {
        startTime = new Date().getTime();
    }

    /**
     * This method is called when an idevice should stop it's media / activities
     * etc
     */
    public void stop() {

    }

    /**
     * 100% order to dispose of all references and resources
     */
    public void dispose() {
        
    }
    
    /**
     * @return "quiz" or "info"
     */
    public int getLogType() {
        return LOG_INFO;
    }
    
    //TODO: Implement this timer here between start and stop
    public int getTimeonDevice() {
        return (int)(new Date().getTime() - startTime);
    }
    
    
    public String getLogLine() {
        String retVal = ideviceId + " " + deviceType + " " + logTypes[getLogType()]
                + " " + getTimeonDevice();
        int[] scoreData = getScores();
        retVal += " " + scoreData[0] + "/" + scoreData[1] + "/" + scoreData[2];
        return retVal;
    }
    
    /**
     * Provide an array of quiz score answers as follows for those idevices
     * that are of type Idevice.LOG_QUIZ
     * @return int array {Num Correct first attempt, num correct eventually, num questions attempted}
     */
    public int[] getScores() {
        return new int[] {0, 0, 0};
    }
    



}
