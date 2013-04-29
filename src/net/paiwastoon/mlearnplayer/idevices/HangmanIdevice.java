/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.paiwastoon.mlearnplayer.idevices;

import com.sun.lwuit.Button;
import com.sun.lwuit.Command;
import com.sun.lwuit.Dialog;
import com.sun.lwuit.Display;
import com.sun.lwuit.Form;
import com.sun.lwuit.Image;
import com.sun.lwuit.Label;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.table.TableLayout;
import java.io.IOException;
import java.util.Vector;
import net.paiwastoon.mlearnplayer.Idevice;
import net.paiwastoon.mlearnplayer.MLearnPlayerMidlet;
import net.paiwastoon.mlearnplayer.MLearnUtils;
import net.paiwastoon.mlearnplayer.xml.XmlNode;

/**
 *
 * @author mike
 */
public class HangmanIdevice extends Idevice implements ActionListener{

    //The form for showing image of player status, hint and current answer in progress
    Form imageDisplayForm;
    
    Label imageDisplayLabel;
    
    Label hintLabel;
    
    Label wordLabel;
    
    String pickText;
    
    //The form for allowing another letter to be picked
    Form letterPickerForm;
    
    //Alphabet to select from
    String alphabet;
    
    String wrongGuessMsg;
    
    String lostLevelMsg;
    
    String levelPassMsg;
    
    String gameWonMsg;
    
    //index of currently showing image
    int currentImageIndex;
    
    //sources of images 
    String[] imgSrcs;
    
    //Array of images to use
    Image[] imgs;
    
    //label object used to show images
    Label imgLabel;
    
    //words to be guessed
    String[] words;
    
    //String of the letters found so far
    String lettersFound;
    
    //hints for those words
    String[] hints;
    
    int currentWordIndex;
    
    int levelAttempts = 0;
    
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

    public void actionPerformed(ActionEvent ae) {
        Command cmd = ae.getCommand();
        if(cmd.getCommandName().equals(pickText)) {
            letterPickerForm.show();
        }else if(cmd.getCommandName().length() == 1) {
            chooseLetter(cmd.getCommandName());
        }
    }
    
    
    
    public void startLevel(int wordNum) {
        currentImageIndex = 0;
        lettersFound = "";
        hintLabel.setText(hints[wordNum]);
        wordLabel.setText(genWordDisplay());
        updateImg();
        imageDisplayForm.show();
    }
    
    void updateImg() {
        imageDisplayLabel.setIcon(imgs[currentImageIndex]);
    }
    
    void loseLevel() {
        showFeedback(lostLevelMsg);
        levelAttempts++;
        startLevel(currentWordIndex);
    }
    
    void advanceLevel() {
        showFeedback(levelPassMsg);
        if(levelAttempts == 0) {
            //means the user has not used all lives on this level - so say correct firs time
            correctFirst++;
        }
        
        hadCorrect[currentWordIndex] = true;
        
        levelAttempts = 0;
        startLevel(++currentWordIndex);
    }
    
    void showFeedback(String feedback) {
        Dialog fbDialog = new Dialog();
        fbDialog.setTimeout(hostMidlet.fbTime);
        fbDialog.addComponent(new Label(feedback));
        fbDialog.showDialog();
    }
    
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
    
    public void chooseLetter(String letter) {
        System.out.println("Choose: " + letter);
        if(words[currentWordIndex].indexOf(letter) != -1) {
            lettersFound += letter;
        }else {
            //wrong guess
            if(currentImageIndex < imgSrcs.length - 1) {
                currentImageIndex++;
                updateImg();
            }else {
                loseLevel();
            }
        }
        
        String wordDisplay = genWordDisplay();
        
        
        if(wordDisplay.equals(words[currentWordIndex])) {
            //level passed
            if(currentWordIndex < words.length - 1) {
                advanceLevel();
            }else {
                showFeedback(gameWonMsg);
            }
        }else {
            System.out.println(wordDisplay);
            wordLabel.setText(wordDisplay);

            imageDisplayForm.show();
        }
    }
    
    
    public int getMode() {
        return Idevice.MODE_LWUIT_FORM;
    }
    
    private void buildForms() {
        if(letterPickerForm == null) {
            letterPickerForm = new Form();
            int numLetters = alphabet.length();
            for (int i = 0; i < numLetters; i++) {
                Button button = new Button(new Command(
                        alphabet.substring(i, i+1)));
                button.addActionListener(this);
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
    

    public Form getForm() {
        buildForms();
        return imageDisplayForm;
    }

    public void start() {
        super.start();
        startLevel(0);
    }

    public void stop() {
        super.stop();
    }

    public int getLogType() {
        return LOG_QUIZ;
    }

    public int[] getScores() {
        return new int[] { correctFirst, MLearnUtils.countBoolValues(hadCorrect, true),
            words.length };
    }
    
    
    
}
