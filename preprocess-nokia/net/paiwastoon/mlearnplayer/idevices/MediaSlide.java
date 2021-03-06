/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.paiwastoon.mlearnplayer.idevices;

import net.paiwastoon.mlearnplayer.Idevice;
import net.paiwastoon.mlearnplayer.MLearnPlayerMidlet;
import net.paiwastoon.mlearnplayer.xml.*;
import com.sun.lwuit.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import javax.microedition.media.Player;
import net.paiwastoon.mlearnplayer.MLearnUtils;


/**
 *
 * @author mike
 */
public class MediaSlide extends Idevice{

    private XmlNode dataNode;

    public String audioURI = null;
    public String videoURI = null;
    private VideoComponent videoComp = null;
    
    //set this if it also needs to be stopped
    public SlideShowIdevice slideShow;

    public MediaSlide(MLearnPlayerMidlet host, XmlNode dataNode) {
        super(host);
        this.dataNode = dataNode;
    }

    public int getMode() {
        return Idevice.MODE_LWUIT_FORM;
    }
    
    public int getTime() {
        return -1;//TODO return the time the media will run for
    }

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
                    videoComp = VideoComponent.createVideoPeer(
                            hostMidlet.getInputStreamReaderByURL(hostMidlet.currentPackageURI
                            + "/" + videoURI), hostMidlet.getContentType(videoURI));
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
                imageLabel = new Label(MLearnUtils.resizeImage(imgOrig, 120, 110, MLearnUtils.RESIZE_FILL));
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

    public void start() {
        if(audioURI != null) {
            //Make a player
            hostMidlet.playMedia(audioURI);
        }
        
        if(videoURI != null  && hostMidlet.mediaEnabled == true) {
            videoComp.start();
        }
    }
    
    public void stop() {
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
