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
package com.toughra.mlearnplayer.idevices;

import com.toughra.mlearnplayer.xml.XmlNode;
import com.toughra.mlearnplayer.Idevice;
import com.toughra.mlearnplayer.MLearnPlayerMidlet;
import com.sun.lwuit.*;
import com.sun.lwuit.geom.Dimension;
import com.sun.lwuit.layouts.BorderLayout;
import com.sun.lwuit.layouts.CoordinateLayout;
import com.toughra.mlearnplayer.EXEStrMgr;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import javax.microedition.media.Player;
import com.toughra.mlearnplayer.MLearnUtils;
import javax.microedition.media.MediaException;
import javax.microedition.media.control.VideoControl;


/**
 * MediaSlide shows an image (optionally with a sound file to play) or a video
 * It can be part of a SlideShow object but can also be an independent idevice on its
 * own
 * 
 * @author mike
 */
public class MediaSlide extends Idevice implements Runnable{

    /** The XMLNode that contains our XML data*/
    private XmlNode dataNode;

    /** URI of audio to play in this slide*/
    public String audioURI = null;
    
    /** URI of video to play in this slide */
    public String videoURI = null;
    
    /** VideoComponent used to play video object*/
    private VideoComponent videoComp = null;
    
    /**set this if it also needs to be stopped - e.g. this MediaSlide is part of
     * a slideshow - so stop the slideshow object when stop is called
     */
    public SlideShowIdevice slideShow = null;
    
    /**add the hostMidlet defined transition wait before starting media*/
    public boolean transWait = true;
    
    Form videoForm = null;

    int[] videoSize = null;
    
    /**
     * Constructor
     * @param host host midlet
     * @param dataNode  our xml data
     */
    public MediaSlide(MLearnPlayerMidlet host, XmlNode dataNode) {
        super(host);
        this.dataNode = dataNode;
    }

    public String getDeviceTypeName() {
        return "mediaslide";
    }

    
    
    /**
     * This is an LWUIT form mode Idevice
     * @return Idevice.MODE_LWUIT_FORM
     */
    public int getMode() {
        return Idevice.MODE_LWUIT_FORM;
    }
    
    /**
     * Provide the time that this should run for
     * @return Provide the time that this should run for (not implemented)
     */
    public int getTime() {
        return -1;//TODO return the time the media will run for
    }

    /**
     * Gets the main LWUIT form.  Looks in the XML for audio or video tags
     * 
     * If a video tag is there - it will show the video in a player and nothing else
     * Otherwise will look for an img tag for an image to show and optionally
     * an audio tag for a sound to play
     * 
     * @return 
     */
    public Form getForm() {
        Form myForm = new Form();
        videoForm = myForm;
        
        Vector audioList = dataNode.findChildrenByTagName("audio", true);
        Vector videoList = dataNode.findChildrenByTagName("video", true);
        
        
        //BorderLayout bLayout = new BorderLayout();
        //myForm.setLayout(bLayout);
        CoordinateLayout cLayout = new CoordinateLayout(
                Display.getInstance().getDisplayWidth(), Display.getInstance().getDisplayHeight());
        myForm.setLayout(cLayout);
        
                    
        //if there is a video - so just make a player for that and return it
        //otherwise look for audio and a picture to show with it
        if(videoList.size() > 0) {
            videoURI = ((XmlNode)videoList.elementAt(0)).getAttribute("src");
            videoComp = null;
            if(hostMidlet.mediaEnabled) {
                try {
                    
                    /*videoComp = VideoComponent.createVideoPeer(
                            hostMidlet.getInputStreamReaderByURL(hostMidlet.currentPackageURI
                            + "/" + videoURI), hostMidlet.getContentType(videoURI));
                    */
                    String newVideoURI = MLearnUtils.reworkMediaURI(hostMidlet.myTOC.videoFormatToUse, 
                            MLearnUtils.VIDEOFORMAT_NAMES, videoURI);
                    
                    if(newVideoURI != null) {
                        videoURI = newVideoURI;
                    }
                    
                    String videoLoc = MLearnUtils.connectorToFile(hostMidlet.currentPackageURI
                            + "/" + videoURI);
                    
                    videoComp = VideoComponent.createVideoPeer(videoLoc);
                    videoSize = adjustVideoWidthHeight(videoComp, videoForm);
                    
                    int x = (Display.getInstance().getDisplayWidth() - videoComp.getWidth())/2;
                    int y = (MLearnUtils.getUsableScreenHeight() - videoComp.getHeight())/2;
                    videoComp.setX(x);
                    videoComp.setY(y);
                   
                    myForm.addComponent(videoComp);
                }catch(IOException e) {
                    throw new RuntimeException("Could not load video");
                }
            }
        }else {
            if(audioList.size() > 0) {
                audioURI = ((XmlNode)audioList.elementAt(0)).getAttribute("src");
            }


            //note - remove the p / p business
            Vector imageList = dataNode.findChildrenByTagName("img", true);
            if(imageList.size() < 1) {
                throw new RuntimeException("No image in image and audio slide");
            }

            XmlNode imageNode = (XmlNode)imageList.elementAt(0);
            String imgPath = imageNode.getAttribute("src");
            if(hostMidlet.myTOC.screenResToUse != -1) {
                imgPath = MLearnUtils.getImageNameForSize(hostMidlet.myTOC.screenResToUse, imgPath);
            }
            
            String imagePath = hostMidlet.currentPackageURI + "/" + imgPath;
            
            Label imageLabel = null;
            try {
                InputStream inStream = hostMidlet.getInputStreamReaderByURL(imagePath);
                Image imgOrig = Image.createImage(inStream);
                //imageLabel = new Label(MLearnUtils.resizeImage(imgOrig, 120, 110, MLearnUtils.RESIZE_FILL));
                imageLabel = new Label(imgOrig);
                imgOrig = null;
                //see what this does
                inStream.close();
                inStream = null;
            }catch(IOException e) {
                Dialog dlg = new Dialog("Error Loading Image in Media Slide");
                String errorMessage = "Error Loading Image in Media Slide / "
                        + "Looking for " + imagePath + " exception: " 
                        + e.toString() + ": " + e.getMessage();
                dlg.addComponent(new TextArea(errorMessage));
                dlg.show();
            }
            //constraint.setHeightPercentage(100);
            //constraint.setWidthPercentage(100);
            imageLabel.setX(0);
            imageLabel.setY(0);
            imageLabel.setWidth(Display.getInstance().getDisplayWidth());
            imageLabel.setHeight(Display.getInstance().getDisplayHeight());
            myForm.addComponent(imageLabel);
        }
        
        return myForm;
    }
    
    
    public static void setVideoControlDisplaySize(VideoComponent comp) {
        
    }
    
    /**
     * This is will adjust the videoComponent width and height to match the
     * maximum available size within the ratio of the video
     * @param videoComp 
     * @return array of width, height
     */
    public static int[] adjustVideoWidthHeight(VideoComponent videoComp, Form vidForm) {
        int[] retVal = new int[] {-1, -1};
        
        Object playObj = videoComp.getClientProperty("Player");
        if(playObj != null && playObj instanceof Player) {
            Player player = (Player)playObj;
            int state = player.getState();
            boolean realized = state >= Player.REALIZED;
            
            //this will trick MIDP implementation into making the control object and
            //initializing it I think
            videoComp.setVisible(false);
            
            //try and get controls object
            Object vidControlObj = videoComp.getClientProperty("VideoControl");
            videoComp.setVisible(true);
            
            if(vidControlObj != null && vidControlObj instanceof VideoControl) {
                VideoControl vc = (VideoControl)vidControlObj;
                int vWidth = vc.getSourceWidth();
                int vHeight = vc.getSourceHeight();

                if(vHeight > 1 && vWidth > 1) {
                    float vRatio = (float)vWidth / (float)vHeight;

                    int screenWidth = Display.getInstance().getDisplayWidth();
                    int screenHeight = MLearnUtils.getUsableScreenHeight();
                    
                    int formHeight = vidForm.getHeight();
                    int formWidth = vidForm.getWidth();
                    
                    float screenRatio = (float)screenWidth / (float)screenHeight;
                    
                    int displayW = screenWidth;
                    int displayH = screenHeight;
                    if(screenRatio < vRatio) {
                        //width is the constraint
                        displayH = (int)(displayW / vRatio);
                    }else {
                        displayW = (int)(displayH * vRatio);
                    }
                    
                    videoComp.setWidth(displayW);
                    videoComp.setHeight(displayH);
                    
                    retVal = new int[] {displayW, displayH};
                    try {
                        vc.setDisplaySize(displayW, displayH);
                    }catch(MediaException e) {
                        EXEStrMgr.lg(101, "Exceptoin setting display width and height", e);
                    }
                    
                }
                int x = 0;
            }

        }
        
        return retVal;
    }
    
    
    /**
     * This is called to wait for the transition to finish and then play the media
     */
    public void run() {
        try { Thread.sleep(hostMidlet.transitionTime + hostMidlet.transitionWait); }
        catch(InterruptedException e) {}
        hostMidlet.playMedia(audioURI);
    }

    /**
     * Main start method - wait for the transition if requested and then play media
     */
    public void start() {
        super.start();
        if(audioURI != null) {
            //Make a player with a delay so that the animation can run
            if(transWait) {
                new Thread(this).start();
            }else {
                hostMidlet.playMedia(audioURI);
            }
        }
        
        if(videoURI != null  && hostMidlet.mediaEnabled == true) {
            videoComp.start();
        }
        
        if(slideShow == null) {
            EXEStrMgr.lg(this, -1, 0, 0, 0, 0, "saw", 0, 0, "", "");
        }
    }
    
    /**
     * Stop everything
     */
    public void stop() {
        super.stop();
        if(slideShow == null) {
            EXEStrMgr.lg(this, -1, getTimeOnDevice(), 0, 0, 0, LOGDEVCOMPLETE, 0, 0, "", "");
        }
        if(videoComp != null) {
            Object nativePeer = videoComp.getNativePeer();
            videoComp.stop();
            if(nativePeer != null) {
                Player player = (Player)nativePeer;
                player.close();
            }
            videoComp = null;
        }
        
        if(audioURI != null) {
            hostMidlet.stopMedia();
        }
        
        if(slideShow != null) {
            slideShow.stop();
        }
    }



}
