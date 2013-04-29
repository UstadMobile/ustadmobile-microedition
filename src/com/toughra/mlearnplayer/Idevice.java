package com.toughra.mlearnplayer;

import java.util.Date;

/**
 * Abstract class representing an idevice that can be shown to the user
 *
 * Idevice can have one of two modes: either providing an LCDUI displayable
 * object or a LWUIT form
 *
 * @author mike
 */
public abstract class Idevice {

    public final static int MODE_LWUIT_FORM = 0;

    public final static int MODE_DISPLAYABLE = 1;

    public abstract int getMode();

    protected MLearnPlayerMidlet hostMidlet;
    
    public String ideviceId;
    
    //tracker of when device use started -1 means not attempted
    long startTime = -1;
    
    //use to check how many answers user got right first time
    protected int correctFirst = 0;
     
    //use to follow how many were eventually gotten correct
    protected boolean[] hadCorrect;
    
    public static int LOG_INFO = 0;
    
    public static int LOG_QUIZ = 1;
    
    String deviceType;
    
    static final String[] logTypes = new String[] {"info", "quiz"};

    public Idevice(MLearnPlayerMidlet host) {
        hostMidlet = host;
    }
    
    public com.sun.lwuit.Form getForm() {
        return null;
    }

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
    
    //returns "quiz" or "info"
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
    
    public int[] getScores() {
        return new int[] {0, 0, 0};
    }
    



}
