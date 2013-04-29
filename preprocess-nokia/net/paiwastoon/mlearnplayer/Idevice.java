package net.paiwastoon.mlearnplayer;

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

    }

    /**
     * This method is called when an idevice shoudl stop it's media / activities
     * etc
     */
    public void stop() {

    }

    /**
     * 100% order to dispose of all references and resources
     */
    public void dispose() {
        
    }



}
