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
import com.toughra.mlearnplayer.FeedbackDialog;
import com.toughra.mlearnplayer.Idevice;
import com.toughra.mlearnplayer.MLearnPlayerMidlet;
import com.toughra.mlearnplayer.MLearnUtils;
import com.sun.lwuit.*;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.layouts.BoxLayout;
import com.sun.lwuit.table.TableLayout;
import com.toughra.mlearnplayer.EXEStrMgr;
import java.io.InputStream;
import java.util.Vector;
import com.toughra.mlearnplayer.xml.XmlNode;



/**
 * Runs the MissingLetter idevice interaction in J2ME
 * 
 * @author mike
 */
public class MissingLetterIdevice extends Idevice implements ActionListener {

    /** HTML of positive feedback*/
    String positiveFb;
    
    /**HTML of negative feedback*/
    String negativeFb;
    
    /**HTML of exercise complete feedback*/
    String completeFb;
    
    /** The main form used*/
    Form mainFrm;
    
    /** The button container for the choices for each picture*/
    Container buttonContainer;
    
    /** Label containing an image of what is shown to the user */
    Label imageLabel;
    
    /**the current level*/
    public int currentIndex;
    
    /** Levels that contain the choices to show, the image to show, etc*/
    MissingLetterLevel[] levels;
    
    /** monitoring of free memory */
    long freeMem;
    
    /** number of attempts on this question*/
    int attemptCount = 0;
    
    /** total number of correct answers given */
    int numCorrectCount = 0;
    
    /**
     * Constructor
     * 
     * @param host our host midlet
     * @param data the xml data
     */
    public MissingLetterIdevice(MLearnPlayerMidlet host, XmlNode data ) {
        super(host);
        
        Vector levelVector = data.findChildrenByTagName("missingletterelmt", true);
        
        int numLevels = levelVector.size();
        levels = new MissingLetterLevel[numLevels];
        
        for(int i = 0; i < numLevels; i++) {
            XmlNode currentLevel = (XmlNode)levelVector.elementAt(i);
            levels[i] = new MissingLetterLevel(currentLevel.getAttribute("imgsrc"),
                    currentLevel.findFirstChildByTagName("choices", true).getTextChildContent(0));
        }
        
        positiveFb = data.findFirstChildByTagName("correctfeedback", true).getTextChildContent(0);
        negativeFb = data.findFirstChildByTagName("negativefeedback", true).getTextChildContent(0);
        completeFb = data.findFirstChildByTagName("completeoverlay", true).getTextChildContent(0);
    }

    public String getDeviceTypeName() {
        return "missingletter";
    }
    
    
    
    
    /**
     * Show the level number given by the argument.  Cleans up anything that was
     * there before.
     * 
     * @param levelNum level number to show
     */
    public void showLevel(int levelNum) {
        freeMem = MLearnUtils.checkFreeMem();
        buttonContainer.removeAll();
        attemptCount = 0;
        
        for(int i = 0; i < levels[levelNum].choices.length; i++) {
            Button b = new Button(new Command(levels[levelNum].choices[i]));
            b.addActionListener(this);
            buttonContainer.addComponent(b);
        }
        
        try {
            InputStream reader = hostMidlet.getInputStreamReaderByURL(
                    levels[levelNum].imgURL);
            imageLabel.setIcon(Image.createImage(reader));
            reader.close();
        }catch(Exception e) {
            System.err.println("Error showing message");
            e.printStackTrace();
        }
        
        currentIndex = levelNum;
        
    }

    /**
     * Main event handler - check the users answer
     * @param ae ActionEvent
     */
    public void actionPerformed(ActionEvent ae) {
        String cmdStr = ae.getCommand().getCommandName();
        FeedbackDialog fbDialog = new FeedbackDialog(hostMidlet);
        boolean wasCorrect = false;
        
        if(numCorrectCount == levels.length) {
            //sorry all done
            return;
        }
        
        EXEStrMgr.lg(this, //idevice
                currentIndex, //question id
                0, //time on device in ms
                0, //num correctly answered
                0, //num answered correct first attempt
                0, //num questions attempted
                EXEStrMgr.VERB_ANSWERED, //verb
                wasCorrect ? 1 : 0, //score
                1, //maxScorePossible
                cmdStr,//answer given 
                "Correct is " + levels[currentIndex].correctChoice);//remarks
        
        if(cmdStr.equals(levels[currentIndex].correctChoice)) {
            fbDialog.showFeedback(mainFrm, positiveFb, true);
            wasCorrect = true;
            if(attemptCount == 0) {
                correctFirst++;
            }
            numCorrectCount++;
        }else {
            attemptCount++;
            fbDialog.showFeedback(mainFrm, negativeFb, false);
        }
        
        if(wasCorrect) {
            if(currentIndex < levels.length - 1) {
                try { Thread.sleep(1000); }
                catch(InterruptedException e) {}
                showLevel(currentIndex + 1);
            }else {
                //fbDialog.showFeedback(mainFrm, completeFb, true);
            }
        }
    }
    
    
    /**
     * This is a LWUIT mode idevice 
     * @return Idevice.MODE_LWUIT_FORM
     */
    public int getMode() {
        return Idevice.MODE_LWUIT_FORM;
    }

    /**
     * Dispose
     */
    public void dispose() {
        super.dispose();
    }

    /**
     * Return the main LWUIT Form
     * 
     * @return Idevice's main LWUIT Form
     */
    public Form getForm() {
        //make buttons
        if(mainFrm == null) {
            mainFrm = new Form();
            buttonContainer = new Container();
            buttonContainer.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
            TableLayout tLayout = new TableLayout(1,2);
            mainFrm.setLayout(tLayout);
            TableLayout.Constraint buttonConst = tLayout.createConstraint();
            buttonConst.setWidthPercentage(30);
            mainFrm.addComponent(buttonConst, buttonContainer);
            imageLabel = new Label();
            TableLayout.Constraint imgConst = tLayout.createConstraint();
            imgConst.setWidthPercentage(70);
            mainFrm.addComponent(imgConst, imageLabel);
        }
        
        return mainFrm;
    }

    /**
     * Start method - show the first question
     */
    public void start() {
        super.start();
        showLevel(0);
    }

    /**
     * Stop
     */
    public void stop() {
        super.stop();
        int questionsAttempted = numCorrectCount + ((attemptCount > 0) ? 1 : 0);
        EXEStrMgr.lg(this, //idevice
                0, //question id
                getTimeOnDevice(), //time on device in ms
                numCorrectCount, //num correctly answered
                correctFirst, //num answered correct first attempt
                questionsAttempted, //num questions attempted
                Idevice.LOGDEVCOMPLETE, //verb
                0, //score
                0, //maxScorePossible
                Idevice.BLANK,//answer given 
                Idevice.BLANK);//remarks
        
    }
    
    
    
}
/**
 * Utility container representing a level in the exercise contains the image
 * url and the choices of possible answers
 * 
 * @author mike
 */
class MissingLetterLevel {
    
    /** URL to the image to be shown*/
    String imgURL;
    
    /** Choices that the user can select*/
    String[] choices;
    
    /** The index of the correct choice */
    String correctIndex;
    
    /** String used to separate answers */
    static final String sepStr = ",";
    
    /** the current choice of the user */
    String correctChoice;
    
    /**
     * Constructor
     * @param imgURL URL of image to show
     * @param choiceList List of choices for the buttons for the user separated by ,
     */
    MissingLetterLevel(String imgURL, String choiceList) {
        this.imgURL = imgURL;
        Vector choicesVector = new Vector();
        int nextIndex = -1;
        int lastIndex = 0;
        
        //check the last character, trim it off if required
        if(!choiceList.endsWith(sepStr)) {
            choiceList = choiceList + ",";
        }
        
        while((nextIndex = choiceList.indexOf(",", lastIndex)) != -1) {
            String thisToken = choiceList.substring(lastIndex, nextIndex);
            choicesVector.addElement(thisToken);
            lastIndex = nextIndex + 1;
        }
        
        choices = new String[choicesVector.size()];
        
        choicesVector.copyInto(choices);
        correctChoice = choices[0];
        MLearnUtils.shuffleArray(choices);
    }
    
    
}
