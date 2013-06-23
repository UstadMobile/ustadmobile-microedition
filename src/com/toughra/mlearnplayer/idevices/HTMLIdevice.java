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
import com.toughra.mlearnplayer.xml.XmlNode;
import com.toughra.mlearnplayer.Idevice;
import com.toughra.mlearnplayer.MLearnPlayerMidlet;
import com.sun.lwuit.*;
import com.sun.lwuit.html.HTMLComponent;
import com.sun.lwuit.layouts.BorderLayout;

        
/**
 * IDevice that simply displays an HTML document
 * 
 * @author mike
 */
public class HTMLIdevice extends Idevice {

    /** the HTML that is to be displayed*/
    public String htmlStr = null;
    
    /**if a part of a slideshow that should also be stopped*/
    public SlideShowIdevice slideShow;
    
    /**
     * Constructor
     * 
     * @param hostMidlet The host midlet
     * @param xmlData XMLNode data containing the HTML we want in a CDATA section
     */
    public HTMLIdevice(MLearnPlayerMidlet hostMidlet, XmlNode xmlData) {
        super(hostMidlet);
        
        if(xmlData != null && xmlData.children.size() > 0) {
            htmlStr = ((XmlNode)xmlData.children.elementAt(0)).nodeValue.toString();
        }else {
            htmlStr = "";
        }
    }
    
    
    /**
     * Constructor
     * 
     * @param hostMidlet The host midlet
     * @param html The HTML String that is going to be displayed
     */
    public HTMLIdevice(MLearnPlayerMidlet hostMidlet, String html) {
        super(hostMidlet);
        this.htmlStr = html;
    }
    
    /**
     * This is a LWUIT form idevice
     * 
     * @return Idevice.MODE_LWUIT_FORM
     */
    public int getMode() {
        return Idevice.MODE_LWUIT_FORM;
    }

    /**
     * Builds the form using MLearnPlayer.makeHTMLComponent and then puts it
     * inside a form
     * 
     * @return LWUIT Form object with an HTMLComponent displaying the specified HTML
     */
    public Form getForm() {
        Form myForm = new Form();
        HTMLComponent htmlComp = hostMidlet.makeHTMLComponent(htmlStr);
        BorderLayout borderLayout = new BorderLayout();
        borderLayout.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_SCALE);
        myForm.setLayout(borderLayout);
        myForm.addComponent(BorderLayout.CENTER, htmlComp);
        return myForm;
    }
    
    /**
     * We dont support media together inside HTML with J2ME
     * @return 
     */
    public int getMediaTimeMs() {
        
        
        return -1;
    }

    /**
     * Stop slideshow if we need to...
     */
    public void stop() {
        super.stop();
        
        if(slideShow != null) {
            slideShow.stop();
        }
    }
    
}
