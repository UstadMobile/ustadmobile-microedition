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
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import javax.microedition.media.Player;
import javax.microedition.media.control.VideoControl;
import com.toughra.mlearnplayer.MLearnUtils;


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
    public SlideShowIdevice slideShow;
    
    /**add the hostMidlet defined transition wait before starting media*/
    public boolean transWait = true;

    /**
     * Constructor
     * @param host host midlet
     * @param dataNode  our xml data
     */
    public MediaSlide(MLearnPlayerMidlet host, XmlNode dataNode) {
        super(host);
        this.dataNode = dataNode;
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
        Vector audioList = dataNode.findChildrenByTagName("audio", true);
        Vector videoList = dataNode.findChildrenByTagName("video", true);
        
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
                    String videoLoc = MLearnUtils.connectorToFile(hostMidlet.currentPackageURI
                            + "/" + videoURI);
                    
                    videoComp = VideoComponent.createVideoPeer(videoLoc);
                    
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
            String imagePath = hostMidlet.currentPackageURI + "/" + imageNode.getAttribute("src");
            
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
            myForm.addComponent(imageLabel);
        }
        
        return myForm;
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
    }
    
    /**
     * Stop everything
     */
    public void stop() {
        super.stop();
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
