/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.paiwastoon.mlearnplayer.idevices;

import com.sun.lwuit.events.ActionEvent;
import net.paiwastoon.mlearnplayer.Idevice;
import net.paiwastoon.mlearnplayer.MLearnPlayerMidlet;
import net.paiwastoon.mlearnplayer.xml.XmlNode;
import com.sun.lwuit.*;
import com.sun.lwuit.events.ActionListener;
import java.util.Vector;

/**
 *
 * @author mike
 */
public class SlideShowIdevice extends Idevice implements Runnable, ActionListener{
    
    private XmlNode dataNode;

    public String audioURI = null;
    public String videoURI = null;
    private VideoComponent videoComp = null;
    
    public int currentSlide = -1;
    XmlNode[] slideNodes;
    
    private Idevice currentDeviceShowing = null;
    
    int mediaSleepTime = -1;
    
    String id = null;
    
    Idevice cIdevice;
    
    boolean running = true;
    
    boolean autoMoveFlag = false;
    
    Thread advanceSlideThread;

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

    public void actionPerformed(ActionEvent ae) {
        
    }
    
    
    
    public int getMode() {
        return Idevice.MODE_LWUIT_FORM;
    }

    public Form getForm() {
        return showNextForm();
    }
    
    public Form showNextForm() {
        if(currentSlide < (slideNodes.length -1)) {
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
    
    public void start() {
        super.start();
        running = true;
    }
    
    public void stop() {
        super.stop();
        if(autoMoveFlag == false) {
            running = false;
        }
    }
    
    public void dispose() {
        slideNodes = null;
        System.gc();
    }
    
    
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
    
    class ShowSlideClass implements Runnable {
        
        SlideShowIdevice host;
        
        ShowSlideClass(SlideShowIdevice host) {
            this.host = host;
        }
        
        public void run() {
            try {
                Form nextForm = host.showNextForm();
            }catch(Exception e) {
                Dialog dlg = new Dialog("Error adv slide");
                
                dlg.addComponent(new Label(e.toString()));
                dlg.show();
            }
            
        }
        
        
    }
    
}
