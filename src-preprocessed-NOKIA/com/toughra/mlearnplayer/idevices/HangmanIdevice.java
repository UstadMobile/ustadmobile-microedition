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

import com.sun.lwuit.Button;
import com.sun.lwuit.Command;
import com.sun.lwuit.Component;
import com.sun.lwuit.Display;
import com.sun.lwuit.Form;
import com.sun.lwuit.Image;
import com.sun.lwuit.Label;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.table.TableLayout;
import com.toughra.mlearnplayer.EXEStrMgr;
import com.toughra.mlearnplayer.FeedbackDialog;
import java.io.IOException;
import java.util.Vector;
import com.toughra.mlearnplayer.Idevice;
import com.toughra.mlearnplayer.MLearnPlayerMidlet;
import com.toughra.mlearnplayer.MLearnUtils;
import com.toughra.mlearnplayer.xml.XmlNode;

/**
 * Implements the Hangman game.  
 * 
 * @author mike
 */
public class HangmanIdevice extends Idevice implements ActionListener{

    /*
     * Static constant colors for the buttons for picking letters
     */
    /** Letter button background for a wrong selection*/
    static final int LB_WRONGCOL = 0xff0000;
    /** Letter button background for a correct selection*/
    static final int LB_CORGUESS = 0x00ff00;
    /** Letter button background for a letter not yet picked*/
    static final int LB_DEFAULT = 0x0000ff;
    /** foreground color for letter button selection*/
    static final int LB_FGCOLOR = 0xffffff;
    
    /**Button objects used to select letters*/
    static Button[] letterButtons;
            
    /*The form for showing image of player status, hint and current answer in progress*/
    Form imageDisplayForm;
    
    /**The label object which is used to show the current life image (e.g. picture of hangman at the current stage) */
    Label imageDisplayLabel;
    
    /** The label that shows the hint for the current word */
    Label hintLabel;
    
    /** the label that shows the word as the player has currently guessed it*/
    Label wordLabel;
    
    /** text to use for the pick next letter button*/
    String pickText;
    
    /**The form for allowing another letter to be picked*/
    Form letterPickerForm;
    
    /**Alphabet to select from e.g. abcdefg etc. can be any character set*/
    String alphabet;
    
    /** Message to show for a wrong guess*/
    String wrongGuessMsg;
    
    /** message to show when all levels are lost*/
    String lostLevelMsg;
    
    /** message to show when a level has been passed*/
    String levelPassMsg;
    
    /** message to show when a game has been won (all levels passed)*/
    String gameWonMsg;
    
    /**index of currently showing image*/
    int currentImageIndex;
    
    /**sources of images */
    String[] imgSrcs;
    
    /**Array of images to use for different lives*/
    Image[] imgs;
    
    /**label object used to show images*/
    Label imgLabel;
    
    /**words to be guessed*/
    String[] words;
    
    /**String of the letters found so far*/
    String lettersFound;
    
    /**hints for those words*/
    String[] hints;
    
    /** the index of word the player is currently on (as per array words)*/
    int currentWordIndex;
    
    /**number of attempts on this level*/
    int levelAttempts = 0;
    
    /** if the game is complete */
    boolean gameComplete = false;
    
    /** For recording logs */
    StringBuffer pickList;
    
    /**
     * Constructor
     * @param hostMidlet the host midlet 
     * @param xmlData our xml data to load game parameters
     */
    public HangmanIdevice(MLearnPlayerMidlet hostMidlet, XmlNode xmlData) {
        super(hostMidlet);  
        
        //TODO: Compact me
        alphabet = MLearnUtils.getTextProperty(xmlData, "alphabet");
        wrongGuessMsg = MLearnUtils.getTextProperty(xmlData, "wrongguessmessage");
        lostLevelMsg = MLearnUtils.getTextProperty(xmlData, "lostlevelmessage");
        levelPassMsg = MLearnUtils.getTextProperty(xmlData, "levelpassedmessage");
        gameWonMsg = MLearnUtils.getTextProperty(xmlData, "gamewonmessage");
        
        Vector imageSrcs = xmlData.findChildrenByTagName("img", true);
        
        int numImages = imageSrcs.size();
        imgSrcs = new String[numImages];
        imgs = new Image[numImages];
        
        //Check the width + height of display
        Display disp = Display.getInstance();
        int dispW = disp.getDisplayWidth();
        int dispH = disp.getDisplayHeight();
        int imgH = dispH / 2;
        
        for(int i = 0; i < numImages; i++) {
            imgSrcs[i] = ((XmlNode)imageSrcs.elementAt(i)).getAttribute("src");
            try { 
                Image tmpImg = Image.createImage(hostMidlet.getInputStreamReaderByURL(imgSrcs[i]));
                imgs[i] = MLearnUtils.resizeImage(tmpImg, dispW, imgH, MLearnUtils.RESIZE_FILL);
                
            }catch(IOException e) {
                System.out.println("Error loading image: " + imgSrcs[i]);
                e.printStackTrace();
            }
        }
        
        
        Vector wordElements = xmlData.findChildrenByTagName("word", true);
        int numWords = wordElements.size();
        hadCorrect = MLearnUtils.makeBooleanArray(false, numWords);
        
        words = new String[numWords];
        hints = new String[numWords];
        lettersFound = "";
        for (int i = 0; i < numWords; i++) {
            XmlNode currentNode = (XmlNode)wordElements.elementAt(i);
            words[i] = 
                    ((XmlNode)currentNode.findChildrenByTagName("answer", true).elementAt(0)).getTextChildContent(0);
            hints[i] = 
                    ((XmlNode)currentNode.findChildrenByTagName("hint", true).elementAt(0)).getTextChildContent(0);            
        }
        
        pickText = "Pick";
        
    }

    public String getDeviceTypeName() {
        return "hangman";
    }
    
    

    /**
     * Event handler - show the letter picker form or pick a letter...
     * 
     * @param ae ActionEvent
     */
    public void actionPerformed(ActionEvent ae) {
        Command cmd = ae.getCommand();
        if(cmd.getCommandName().equals(pickText) && gameComplete == false) {
            letterPickerForm.show();
        }else if(cmd.getCommandName().length() == 1) {
            chooseLetter(cmd.getCommandName(), ae.getComponent());
        }
    }
    
    
    /**
     * Starts a level (e.g. word with hint)
     * 
     * @param wordNum  index of the word to use as per the array words
     */
    public void startLevel(int wordNum) {
        currentImageIndex = 0;
        lettersFound = "";
        hintLabel.setText(hints[wordNum]);
        wordLabel.setText(genWordDisplay());
        updateImg();
        imageDisplayForm.show();
        pickList = new StringBuffer();
    }
    
    /**
     * Updates the imageDisplayLabel to make sure it shows the correct image for the
     * current life the player is on
     */
    void updateImg() {
        imageDisplayLabel.setIcon(imgs[currentImageIndex]);
    }
    
    
    /**
     * Called when the player has no remaining chances for this level and must reset
     */
    void loseLevel() {
        EXEStrMgr.lg(this, //idevice
                currentWordIndex, //question id
                0, //time on device in ms
                0, //num correctly answered
                0, //num answered correct first attempt
                0, //num questions attempted
                EXEStrMgr.VERB_FAILED, //verb
                0, //score
                imgSrcs.length, //maxScorePossible
                pickList.toString(),//answer given 
                "Wanted " + words[currentWordIndex] + " Hint was " + hints[currentWordIndex]);//remarks

        showFeedback(lostLevelMsg, false);
        levelAttempts++;
        startLevel(currentWordIndex);
    }
    
    /**
     * Called when the player has successfully guessed the word
     */
    void advanceLevel() {
        showFeedback(levelPassMsg, true);
        
        //reset the buttons
        for(int i = 0; i < letterButtons.length; i++) {
            letterButtons[i].getStyle().setBgColor(LB_DEFAULT);
            letterButtons[i].getUnselectedStyle().setBgColor(LB_DEFAULT);
        }
        if(levelAttempts == 0) {
            //means the user has not used all lives on this level - so say correct firs time
            correctFirst++;
        }
        
        hadCorrect[currentWordIndex] = true;
        
        levelAttempts = 0;
        startLevel(++currentWordIndex);
    }
    
    /**
     * show feedback to the player
     * @param feedback HTML to be shown
     * @param isCorrect true if it's positive, false otherwise
     */
    void showFeedback(String feedback, boolean isCorrect) {
        FeedbackDialog fbDialog = new FeedbackDialog(hostMidlet);
        fbDialog.showFeedback(fbDialog, feedback, isCorrect);
    }
    
    /**
     * Generates the word as it should be displayed to the player.  Puts any
     * letters the player has guessed in and replaces the others with -
     * 
     * @return word display formatted as mentioned in method description
     */
    String genWordDisplay() {
        String wordDisplay = "";
        for(int i = 0; i < words[currentWordIndex].length(); i++) {
            char currentChar = words[currentWordIndex].charAt(i);
            if(lettersFound.indexOf(currentChar) != -1) {
                wordDisplay += currentChar;
            }else if(currentChar == ' ') {
                wordDisplay += ' ';
            }else {
                wordDisplay += '-';
            }
        }
        
        return wordDisplay;
    }
    
    /**
     * Handles when the player chooses a letter as a guess
     * 
     * @param letter the letter the player has chosen (e.g. a)
     * @param c The button that the player clicked on to choose that letter
     */
    public void chooseLetter(String letter, Component c) {
        pickList.append(letter).append(',');
        if(words[currentWordIndex].indexOf(letter) != -1) {
            c.getStyle().setBgColor(LB_CORGUESS);
            c.getUnselectedStyle().setBgColor(LB_CORGUESS);
            lettersFound += letter;
        }else {
            //wrong guess
            if(currentImageIndex < imgSrcs.length - 1) {
                currentImageIndex++;
                c.getStyle().setBgColor(LB_WRONGCOL);
                c.getUnselectedStyle().setBgColor(LB_WRONGCOL);
                updateImg();
            }else {
                loseLevel();
            }
        }
        
        String wordDisplay = genWordDisplay();
        
        wordLabel.setText(wordDisplay);
        
        if(wordDisplay.equals(words[currentWordIndex])) {
            //level passed
            EXEStrMgr.lg(this, //idevice
                currentWordIndex, //question id
                0, //time on device in ms
                0, //num correctly answered
                0, //num answered correct first attempt
                0, //num questions attempted
                EXEStrMgr.VERB_ANSWERED, //verb
                imgSrcs.length - currentImageIndex, //score
                imgSrcs.length, //maxScorePossible
                pickList.toString(),//answer given 
                "Wanted " + words[currentWordIndex] + " Hint was " + hints[currentWordIndex]);//remarks
        
            
            if(currentWordIndex < words.length - 1) {
                advanceLevel();
            }else {
                imageDisplayForm.show();
                gameComplete = true;
                showFeedback(gameWonMsg, true);
            }
        }else {
            System.out.println(wordDisplay);
            
            imageDisplayForm.show();
        }
    }
    
    /**
     * This is a LWUIT idevice
     * @return Idevice.MODE_LWUIT_FORM
     */
    public int getMode() {
        return Idevice.MODE_LWUIT_FORM;
    }
    
    /**
     * Puts the alphabet selection form together given the letters that can be 
     * selected.
     * 
     * Puts the image display form together to show the player the life they
     * are currently on
     */
    private void buildForms() {
        if(letterPickerForm == null) {
            letterPickerForm = new Form();
            int numLetters = alphabet.length();
            letterButtons = new Button[numLetters];
            for (int i = 0; i < numLetters; i++) {
                Button button = new Button(new Command(
                        alphabet.substring(i, i+1)));
                letterButtons[i] = button;
                button.addActionListener(this);
                button.getStyle().setFgColor(LB_FGCOLOR);
                button.getStyle().setBgColor(LB_DEFAULT);
                letterPickerForm.addComponent(button);
            }
        }
        
        if(imageDisplayForm == null) {
            imageDisplayForm = new Form();
            TableLayout tLayout = new TableLayout(2, 2);
            imageDisplayForm.setLayout(tLayout);
            
            TableLayout.Constraint imgConst = tLayout.createConstraint();
            imgConst.setHorizontalSpan(2);
            imgConst.setHeightPercentage(60);
            imgConst.setWidthPercentage(100);
            imageDisplayLabel = new Label(imgs[0]);
            imageDisplayForm.addComponent(imgConst, imageDisplayLabel);
            
            TableLayout.Constraint hintConst = tLayout.createConstraint();
            hintConst.setWidthPercentage(100);
            hintConst.setHorizontalSpan(2);
            hintLabel = new Label(hints[0]);
            imageDisplayForm.addComponent(hintConst, hintLabel);
            
            TableLayout.Constraint wordConst = tLayout.createConstraint();
            wordConst.setWidthPercentage(100);
            wordConst.setHorizontalSpan(2);
            wordLabel = new Label("----");
            imageDisplayForm.addComponent(wordConst, wordLabel);
            
            //button for picking a new letter
            Button pButton = new Button(new Command(pickText));
            pButton.addActionListener(this);
            TableLayout.Constraint buttonConst = tLayout.createConstraint();
            buttonConst.setWidthPercentage(50);
            imageDisplayForm.addComponent(buttonConst, pButton);
        }
    }
    

    /**
     * The main LWUIT Idevice form
     * @return LWUIT form with the idevice on it
     */
    public Form getForm() {
        buildForms();
        return imageDisplayForm;
    }

    /**
     * idevice start
     */ 
    public void start() {
        super.start();
        startLevel(0);
    }

    /**
     * idevice stop
     */
    public void stop() {
        super.stop();
        EXEStrMgr.lg(this, //idevice
                0, //question id
                getTimeOnDevice(), //time on device in ms
                currentWordIndex, //num correctly answered
                correctFirst, //num answered correct first attempt
                currentWordIndex, //num questions attempted
                Idevice.LOGDEVCOMPLETE, //verb
                0, //score
                0, //maxScorePossible
                Idevice.BLANK,//answer given 
                Idevice.BLANK);//remarks
        
    }

    /**
     * Is a quiz style idevice
     * @return Idevice.LOG_QUIZ
     */
    public int getLogType() {
        return LOG_QUIZ;
    }

    /**
     * Returns scores for the log
     * @return Scores array - see Idevice.getScores
     */
    public int[] getScores() {
        return new int[] { correctFirst, MLearnUtils.countBoolValues(hadCorrect, true),
            words.length };
    }
    
    
    
}
