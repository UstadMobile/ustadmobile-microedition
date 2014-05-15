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
import com.toughra.mlearnplayer.EXEStrMgr;
import com.toughra.mlearnplayer.MLearnUtils;

        
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
            htmlStr = reworkHTMLStrImgSizes(htmlStr, hostMidlet.myTOC.screenResToUse);
        }else {
            htmlStr = "";
        }
    }

    public String getDeviceTypeName() {
        return "html";
    }
    
    
    
    /**
     * Constructor
     * 
     * @param hostMidlet The host midlet
     * @param html The HTML String that is going to be displayed
     */
    public HTMLIdevice(MLearnPlayerMidlet hostMidlet, String html) {
        super(hostMidlet);
        this.htmlStr = reworkHTMLStrImgSizes(html, hostMidlet.myTOC.screenResToUse);
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
     * This is used to filter and change width and height attributes so they display
     * as intended on this device
     * 
     * @param html
     * @return 
     */
    public static String reworkHTMLStrImgSizes(String html, int resolutionProfile) {
        String htmlLower = html.toLowerCase();
        int[] lastFound = null;
        boolean doneFinding = false;
        int startPos = 0;
        do {
            lastFound = MLearnUtils.getSingleTagStartAndEndPos(htmlLower, "img", startPos, true);
            if(lastFound[0] == -1) {
                doneFinding = true;
            }else {
                String imgTagStr = html.substring(lastFound[0], lastFound[1]);
                String newImgTagStr = reworkImgTag(imgTagStr, resolutionProfile);
                html = html.substring(0, lastFound[0]) + newImgTagStr + html.substring(lastFound[1]+1);
                startPos = lastFound[1]+1;
            }
        }while(!doneFinding);
        
        return html;
    }
    
    /**
     * This is used to rework an HTML tag - given the string from <img to />
     */
    public static String reworkImgTag(String htmlStr, int resolutionProfile) {
        String ustadSizesStr = "data-ustadsizes=";
        int ustadSizesStartPos = htmlStr.indexOf(ustadSizesStr);
        if(ustadSizesStartPos == -1) {
            //this tag is not here
            return htmlStr;
        }
        
        int ustadSizesValuePos = ustadSizesStartPos + ustadSizesStr.length() + 1;
        char sizeQuoteChar = htmlStr.charAt(ustadSizesValuePos-1);
        int ustadSizesStopPos = htmlStr.indexOf(sizeQuoteChar, ustadSizesValuePos+1);
        String ustadSizeStr = htmlStr.substring(ustadSizesValuePos, ustadSizesStopPos);
        int[] newDimensions = getWidthHeightFromResString(ustadSizeStr, resolutionProfile);
        
        //find and replace the width and height tag
        
        String[] tagNames = new String[]{"width", "height"};
        for(int i = 0; i < tagNames.length; i++) {
            int[] dimStartEnd = MLearnUtils.getAttribStartEndPos(htmlStr, tagNames[i]);
            htmlStr = htmlStr.substring(0, dimStartEnd[0]) + " " + tagNames[i] + "=\"" + newDimensions[i] + "\" " 
                + htmlStr.substring(dimStartEnd[1]+1);
        }
        
        return htmlStr;
    }
    
    /**
     * Figure out the width and height to use according to the profile
     * 
     * @param resString - e.g. 240x320:182,99;320x240:182,99; as generated by exelearning
     * @param profile as per MLearnUtils.SCREENSIZE_NAMES
     * @return width and height of image for this profile
     */
    public static int[] getWidthHeightFromResString(String resString, int profile) {
        String profName = MLearnUtils.SCREENSIZE_NAMES[profile];
        String profSearchStr = profName + ":";
        int profStartPos = resString.indexOf(profSearchStr);
        int profSepPos = resString.indexOf(",", profStartPos+1);
        int profEndPos = resString.indexOf(";", profSepPos);
        String widthStr = resString.substring(profStartPos+profSearchStr.length(), profSepPos);
        String heightStr = resString.substring(profSepPos+1, profEndPos);
        int width = Integer.parseInt(widthStr);
        int height = Integer.parseInt(heightStr);
        return new int[] {width, height};
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
        
        if(slideShow == null) {
            EXEStrMgr.lg(this, -1, getTimeOnDevice(), 0, 0, 0, LOGDEVCOMPLETE, 0, 0, "", "");
        }else {
            slideShow.stop();
        }
    }
    
}
