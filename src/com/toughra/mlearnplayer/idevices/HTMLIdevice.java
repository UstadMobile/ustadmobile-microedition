/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.toughra.mlearnplayer.idevices;
import com.toughra.mlearnplayer.xml.XmlNode;
import com.toughra.mlearnplayer.Idevice;
import com.toughra.mlearnplayer.MLearnPlayerMidlet;
import com.sun.lwuit.*;
import com.sun.lwuit.html.HTMLComponent;
import com.sun.lwuit.layouts.BorderLayout;

        
/**
 *
 * @author mike
 */
public class HTMLIdevice extends Idevice {

    public String htmlStr = null;
    
    //if a slideshow should also be stopped
    public SlideShowIdevice slideShow;
    
    public HTMLIdevice(MLearnPlayerMidlet hostMidlet, XmlNode xmlData) {
        super(hostMidlet);
        
        if(xmlData != null && xmlData.children.size() > 0) {
            htmlStr = ((XmlNode)xmlData.children.elementAt(0)).nodeValue.toString();
        }else {
            htmlStr = "";
        }
    }
    
    public int getMode() {
        return Idevice.MODE_LWUIT_FORM;
    }

    public Form getForm() {
        Form myForm = new Form();
        HTMLComponent htmlComp = hostMidlet.makeHTMLComponent(htmlStr);
        BorderLayout borderLayout = new BorderLayout();
        borderLayout.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_SCALE);
        myForm.setLayout(borderLayout);
        myForm.addComponent(BorderLayout.CENTER, htmlComp);
        return myForm;
    }
    
    public int getMediaTimeMs() {
        
        
        return -1;
    }

    public void stop() {
        super.stop();
        
        if(slideShow != null) {
            slideShow.stop();
        }
    }
    
}
