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
package com.toughra.mlearnplayer.idevices;

import com.sun.lwuit.events.ActionEvent;
import com.toughra.mlearnplayer.Idevice;
import com.toughra.mlearnplayer.MLearnPlayerMidlet;
import com.toughra.mlearnplayer.xml.XmlNode;
import com.sun.lwuit.*;
import com.sun.lwuit.events.ActionListener;
import com.toughra.mlearnplayer.EXEStrMgr;
import com.toughra.mlearnplayer.MLearnUtils;
import java.util.Vector;

/**
 * This runs a slideshow that automatically advances to the next item when 
 * the audio or video being played is finished
 * 
 * @author mike
 */
public class SlideShowIdevice extends Idevice implements Runnable, ActionListener{
    
    /** the xml data we are using */
    private XmlNode dataNode;

    /** URI of audio file being played*/
    public String audioURI = null;
    
    /** URI of video file being played*/
    public String videoURI = null;
    
    /** VideoComponent used to play video */
    private VideoComponent videoComp = null;
    
    /** the current slide */
    public int currentSlide = -1;
    
    /** Array of XMLNodes representing each &lt;slide&gt; */
    XmlNode[] slideNodes;
    
    /** The Idevice object representing the slide currently showing (unused) */
    private Idevice currentDeviceShowing = null;
    
    /** time to wait for before plauying media */
    int mediaSleepTime = -1;
    
    /** id of this device */
    String id = null;
    
    /** The idevice object representing the slide currently showing */
    Idevice cIdevice;
    
    /** true if it's running, false if finished */
    boolean running = true;
    
    /** whether or not we automatically advance the slides */
    boolean autoMoveFlag = false;
    
    /** Thread that advances the slides */
    Thread advanceSlideThread;

    /**
     * Constructor
     * 
     * @param host our midlet host
     * @param dataNode XML node data
     */
    public SlideShowIdevice(MLearnPlayerMidlet host, XmlNode dataNode) {
        super(host);
        this.dataNode = dataNode;
        this.id = dataNode.getAttribute("id");
        
        Vector slides = dataNode.findChildrenByTagName("slide", true);
        slideNodes = new XmlNode[slides.size()];
        for(int i = 0; i < slides.size(); i++) {
            slideNodes[i] = (XmlNode)slides.elementAt(i);
        }
    }

    public String getDeviceTypeName() {
        return "slideshow";
    }
    
    

    public void actionPerformed(ActionEvent ae) {
        
    }
    
    
    /**
     * This is an LWUIT mode idevice
     * 
     * @return Idevice.MODE_LWUIT_FORM
     */
    public int getMode() {
        return Idevice.MODE_LWUIT_FORM;
    }

    /**
     * Gets the main LWUIT form - in our case - the next slide actually...
     * 
     * @return main LWUIT form 
     */
    public Form getForm() {
        return showNextForm();
    }
    
    /**
     * Shows the next part of the slide show
     * 
     * @return LWUIT Form that is the next part of the slide show
     */
    public Form showNextForm() {
        if(slideNodes != null && currentSlide < (slideNodes.length -1)) {
            if(cIdevice != null) {
                cIdevice.stop();
                cIdevice.dispose();
            }
            
            int nextNode = ++currentSlide;
            String nextNodeType = slideNodes[nextNode].getAttribute("type");
            Idevice nextDevice = null;
            if(nextNodeType.equals("mediaslide")) {
                nextDevice = new MediaSlide(hostMidlet, slideNodes[nextNode]);
                ((MediaSlide)nextDevice).slideShow = this;
                ((MediaSlide)nextDevice).transWait = false;
            }else if(nextNodeType.equals("html")) {
                nextDevice = new HTMLIdevice(hostMidlet, slideNodes[nextNode]);
                ((HTMLIdevice)nextDevice).slideShow = this;
            }
            this.currentSlide = nextNode;
            cIdevice = nextDevice;
            hostMidlet.showIdevice(nextDevice, id);
            
            //set the amount of time to wait for the next slide
            mediaSleepTime = (int)(hostMidlet.currentMediaLength / 1000);
            advanceSlideThread = new Thread(this);
            advanceSlideThread.start();
            return nextDevice.getForm();
        }
        
        
        return null;
    }
    
    /**
     * Starts the slideshow
     */
    public void start() {
        super.start();
        running = true;
    }
    
    /**
     * Stops the slide show
     */
    public void stop() {
        super.stop();
        if(autoMoveFlag == false) {
            running = false;
        }
        
        if(currentSlide == (slideNodes.length -1)) {
            //saw verb
            EXEStrMgr.lg(this, //idevice
                    0, //question id
                    getTimeOnDevice(), //time on device in ms
                    0, //num correctly answered
                    0, //num answered correct first attempt
                    0, //num questions attempted
                    EXEStrMgr.VERB_SAW, //verb
                    0, //score
                    0, //maxScorePossible
                    Idevice.BLANK,//answer given 
                    Idevice.BLANK);//remarks

            EXEStrMgr.lg(this, //idevice
                    0, //question id
                    getTimeOnDevice(), //time on device in ms
                    0, //num correctly answered
                    0, //num answered correct first attempt
                    0, //num questions attempted
                    Idevice.LOGDEVCOMPLETE, //verb
                    0, //score
                    0, //maxScorePossible
                    Idevice.BLANK,//answer given 
                    Idevice.BLANK);//remarks
        }
    }
    
    public void dispose() {
        slideNodes = null;
        System.gc();
    }
    
    /**
     * Main run method that will advance the slides
     */
    public void run() {
        //first wait for it to find out the media time
        
        try { Thread.sleep(mediaSleepTime); }
        catch(InterruptedException e) {}

        
        if(running == true) {
            autoMoveFlag = true;
            Display.getInstance().callSeriallyAndWait(new ShowSlideClass(this));
            autoMoveFlag = false;
        }
    }
    
    /**
     * Used to avoid LWUIT repaint troubles...  put within callSeriallyAndWait
     */
    class ShowSlideClass implements Runnable {
        
        /** our host */
        SlideShowIdevice host;
        
        /**
         * Constructor 
         * @param host  our host device
         */
        ShowSlideClass(SlideShowIdevice host) {
            this.host = host;
        }
        
        /**
         * Will call our host's showNextForm method - used within callSeriallyAndWait
         * so that it will avoid repaint trouble
         */
        public void run() {
            try {
                Form nextForm = host.showNextForm();
            }catch(Exception e) {
                e.printStackTrace();
                Dialog dlg = new Dialog("Error adv slide");
                
                dlg.addComponent(new Label(e.toString()));
                dlg.show();
            }
            
        }
        
        
    }
    
}
