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
import com.toughra.mlearnplayer.FeedbackDialog;
import com.toughra.mlearnplayer.Idevice;
import com.toughra.mlearnplayer.MLearnPlayerMidlet;
import com.sun.lwuit.*;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.geom.Dimension;
import com.sun.lwuit.plaf.Style;
import com.sun.lwuit.plaf.UIManager;
import com.sun.lwuit.table.TableLayout;
import com.toughra.mlearnplayer.EXEStrMgr;
import com.toughra.mlearnplayer.MLearnUtils;
import java.io.InputStream;
import java.util.Random;
import java.util.Vector;
import com.toughra.mlearnplayer.xml.XmlNode;




/**
 * Implements a MathDrill idevice for the user in J2ME
 * 
 * @author mike
 */
public class MathDrillIdevice extends Idevice implements ActionListener {

    /** the detected random part of the expression */
    public static final int SECTION_RAND = 0;
    
    /** the detected addon section (minimum number to generate*/
    public static final int SECTION_ADDON = 1;
    
    /** operate in objects mode - show small pictures */
    public static final int MODE_OBJS = 0;
    
    /** The operator to use (+ - * /) */
    String operator;
    
    /** The source of the object image to show */
    String imgSrc;
    
    /** Image object being used */
    Image objImg;
    
    /** Image src to place over the top of the main object to show it's being crossed out
     * for subtraction */
    String crossOutImgSrc;
    
    /** Image object to place over the top of the main object to show it's being crossed out
     * for subtraction */
    Image crossedOutImg;
    
    /** number of questions to show in the math drill */
    int numQuestions;
    
    /** The main LWUIT form for the idevice */
    Form mainFrm;
    
    /**
     * QuestionOperators in the form of
     * int[index] = SECTION_RAND, SECTION_ADDON
     * So the question would generate Random*SECTION_RAND + SECTION_ADDON
     */
    int[][] questionOperators;
    
    /**
     * The results of running that expression
     */
    int[] exprResults;
    
    /**
     * The current correct answer
     */
    int currentAnswer;
    
    /** Object boxes */
    MathDrillObjectBox[] objectBoxes;
    
    /** result box where user puts in the answer*/
    MathDrillObjectBox resultBox;
    
    /** command that user wants answer checked*/
    Command checkCommand;
    
    /** positiveFeedback HTML to show */
    String positiveFeedback;
    
    /** negativeFeedback HTML to show */
    String negativeFeedback;
    
    /** feedback to show for exercise complete*/
    String completeFeedback;
    
    /** number of questions currently completed by the user*/
    int numQuestionsDone = 0;
    
    /** number of attempts so far by the user*/
    int currentNumAttempts = 0;
    
    /** if the exercise is complete or not*/
    boolean exerciseComplete = false;
            
    /**
     * Constructor
     * 
     * @param host the host midlet
     * @param data the xml data we need
     */
    public MathDrillIdevice(MLearnPlayerMidlet host, XmlNode data) {
        super(host);
        
        XmlNode drillNode = data.findFirstChildByTagName("mathdrill", true);
        operator = drillNode.getAttribute("operator");
        imgSrc = drillNode.getAttribute("imgsrc");
        crossOutImgSrc = drillNode.getAttribute("crossimg");
        numQuestions = Integer.parseInt(drillNode.getAttribute("numquestions"));
        
        try {
            InputStream in1 = host.getInputStreamReaderByURL(imgSrc);
            
            objImg = Image.createImage(in1);
            in1.close();
            in1 = null;
          
            crossedOutImg = Image.createImage(objImg.getWidth(), objImg.getHeight());
            
            InputStream in2 = host.getInputStreamReaderByURL(crossOutImgSrc);
            
            Image crossedOutImgX = Image.createImage(in2);
            in2.close();
            in2 = null;
            
            Graphics g = crossedOutImg.getGraphics();
            g.drawImage(objImg, 0, 0);
            g.drawImage(crossedOutImgX, 0,0);
        }catch(Exception e) {
            System.err.println("math drill error loading images");
            e.printStackTrace();
        }
        Vector exprVector = data.findChildrenByTagName("expr", true);
        questionOperators = new int[exprVector.size()][2];
        for(int i = 0; i < questionOperators.length; i++) {
            XmlNode currentExpr = (XmlNode)exprVector.elementAt(i);
            questionOperators[i][SECTION_RAND] = Integer.parseInt(currentExpr.getAttribute("rand"));
            questionOperators[i][SECTION_ADDON] = Integer.parseInt(currentExpr.getAttribute("add"));
        }
        
        positiveFeedback = data.findFirstChildByTagName("positivefeedback", true).getTextChildContent(0);
        negativeFeedback = data.findFirstChildByTagName("negativefeedback", true).getTextChildContent(0);
        completeFeedback = data.findFirstChildByTagName("completefeedback", true).getTextChildContent(0);        
    }

    public String getDeviceTypeName() {
        return "mathdrill";
    }
    
    

    /**
     * this is a LWUIT idevice
     * @return Idevice.MODE_LWUIT_FORM
     */
    public int getMode() {
        return Idevice.MODE_LWUIT_FORM;
    }

    public void dispose() {
        
    }

    /**
     * Returns the main form that is going to be used to render this exercise
     * 
     * @return LWUIT form to be used to render this exercise
     */
    public Form getForm() {
        if(mainFrm == null) {
            mainFrm = new Form();
            TableLayout tLayout = new TableLayout(2, 5);
            mainFrm.setLayout(tLayout);
            
            objectBoxes = new MathDrillObjectBox[questionOperators.length];
            
            int boxWidthPercent = (80/(questionOperators.length+1));
            
            for(int i = 0; i < questionOperators.length; i++) {
                if(i != 0) {
                    TableLayout.Constraint operatorConst = tLayout.createConstraint();
                    operatorConst.setWidthPercentage(10);
                    mainFrm.addComponent(operatorConst, new Label(operator));
                }
                //TODO: Figure out what this box has in it...
                objectBoxes[i] = new MathDrillObjectBox(this, 0);
                TableLayout.Constraint boxConst = tLayout.createConstraint();
                boxConst.setHeightPercentage(90);
                boxConst.setWidthPercentage(boxWidthPercent);
                mainFrm.addComponent(boxConst, objectBoxes[i]);
            }
            
            TableLayout.Constraint equalSignConst = tLayout.createConstraint();
            equalSignConst.setWidthPercentage(10);
            mainFrm.addComponent(equalSignConst, new Label("="));
            
            resultBox = new MathDrillObjectBox(this, 0);
            if(operator.equals("+") || operator.equals("*")) {
                resultBox.mode = MathDrillObjectBox.MODE_ADD;
            }else {
                resultBox.mode = MathDrillObjectBox.MODE_SUBTRACT;
            }
            
            resultBox.setFocusable(true);
            TableLayout.Constraint resultBoxConst = tLayout.createConstraint();
            resultBoxConst.setWidthPercentage(boxWidthPercent);
            
            mainFrm.addComponent(resultBoxConst, resultBox);
            
            checkCommand = new Command("Check", null, 1);
            Button checkButton = new Button(checkCommand);
            checkButton.addActionListener(this);
            
            TableLayout.Constraint buttonConst = tLayout.createConstraint();
            buttonConst.setHorizontalSpan(5);
            buttonConst.setHeightPercentage(10);
            mainFrm.addComponent(buttonConst, checkButton);
        }
        
        return mainFrm;
    }
    
    /**
     * Show the next question - generate the sum that is going to be shown, and 
     * the answer of the sum.  Reset the answer box.
     */
    public void showQuestion() {
        this.exprResults = new int[this.questionOperators.length];
        for(int i = 0; i < this.questionOperators.length;i++) {
            Random r = new Random();
            exprResults[i] = MLearnUtils.nextRandom(r, questionOperators[i][SECTION_RAND])
                    + questionOperators[i][SECTION_ADDON];
        }
        
        currentAnswer = 0;
        currentNumAttempts = 0;
        
        if(operator.equals("+")) {
            currentAnswer = exprResults[0] + exprResults[1];
        }else if(operator.equals("-")) {
            currentAnswer = exprResults[0] - exprResults[1];
        }else if(operator.equals("*")) {
            currentAnswer = exprResults[0] * exprResults[1];
        }else if(operator.equals("/")) {
            currentAnswer = exprResults[0] / exprResults[1];
        }
        
        objectBoxes[0].setNumObjects(exprResults[0]);
        objectBoxes[1].setNumObjects(exprResults[1]);
        
        //set this anyway - will be ignored if we are not doing subtraction
        
        
        if(resultBox.mode == MathDrillObjectBox.MODE_SUBTRACT) {
            resultBox.setNumObjects(exprResults[0]);
            resultBox.subtractFrom = exprResults[0];
        }else {
            resultBox.setNumObjects(0); 
        }
    }

    /**
     * Fired when the user wants their answer checked
     * @param ae ActionEvent from button
     */
    public void actionPerformed(ActionEvent ae) {
        if(ae.getCommand().equals(checkCommand)) {
            ae.consume();
            
            FeedbackDialog fbDialog = new FeedbackDialog(hostMidlet);
            boolean isCorrect = (resultBox.getNumObjects() == currentAnswer);
            StringBuffer remarkStr = new StringBuffer("Q:").append(exprResults[0]).
                    append(operator).append(exprResults[1]);
            EXEStrMgr.lg(this, //idevice
                0, //question id
                0, //time on device in ms
                0, //num correctly answered
                0, //num answered correct first attempt
                0, //num questions attempted
                EXEStrMgr.VERB_ANSWERED, //verb
                isCorrect ? 1 : 0, //score
                1, //maxScorePossible
                String.valueOf(currentAnswer),//answer given 
                remarkStr.toString());//remarks
            
            if(resultBox.getNumObjects() == currentAnswer) {
                if(currentNumAttempts == 0) {
                    if(correctFirst == -1) {
                        correctFirst = 0;
                    }
                    correctFirst++;
                }
                
                if(numQuestionsDone <= numQuestions) {
                    fbDialog.showFeedback(mainFrm, positiveFeedback, true);
                    numQuestionsDone++;
                    showQuestion();
                }else {
                    fbDialog.showFeedback(mainFrm, completeFeedback, true);
                }
                
            }else {
                currentNumAttempts++;
                fbDialog.showFeedback(mainFrm, negativeFeedback, false);
            }
        }
    }

    /** 
     * this is a quiz style idevice
     * @return Idevice.LOG_QUIZ
     */
    public int getLogType() {
        return Idevice.LOG_QUIZ;
    }

    /**
     * Return quiz scores
     * @return array of int as per Idevice.getScores
     */
    public int[] getScores() {
        return new int[] { correctFirst, numQuestionsDone, numQuestions };
    }
    
    /**
     * start and show first question
     */
    public void start() {
        super.start();
        showQuestion();
    }

    /**
     * stop
     */
    public void stop() {
        int numAttempted = numQuestionsDone + ((currentNumAttempts > 0) ? 1 : 0);
        EXEStrMgr.lg(this, //idevice
                0, //question id
                getTimeOnDevice(), //time on device in ms
                numQuestionsDone, //num correctly answered
                Math.max(0, correctFirst), //num answered correct first attempt
                numAttempted, //num questions attempted
                EXEStrMgr.VERB_ANSWERED, //verb
                0, //score
                0, //maxScorePossible
                Idevice.BLANK,//answer given 
                Idevice.BLANK);
        super.stop();
    }
    
    
}
/**
 * Component that will show a given number of objects from a MathDrillIdevice
 * 
 * @author mike
 */
class MathDrillObjectBox extends Component {
    
    /** our host idevice*/
    MathDrillIdevice device;
    
    /** current number of objects*/
    int numObjects;
    
    /** this is used just to display*/
    static final int MODE_DISPLAY = 0;
    
    /** in add mode - when user clicks add */
    static final int MODE_ADD = 1;
    
    /** subtact mode - when user clicks cross one out*/
    static final int MODE_SUBTRACT = 2;
    
    /** if in subtract mode, the numbe to subtract from */
    int subtractFrom = 0;
    
    /** mode to operate in MODE_DISPLAY, MODE_ADD or MODE_SUBTRACT */
    int mode;
    
    /** number of items to show in one column */
    int numItemsInCol = 10;
    
    /**
     * Constructor
     * 
     * @param device host device
     * @param numObjects the number of objects to show here
     */
    public MathDrillObjectBox(MathDrillIdevice device, int numObjects) {
        this.device = device;
        this.numObjects = numObjects;
        setFocusable(false);
        
        mode = MODE_DISPLAY;
    }
    
    /**
     * Main paint method
     * @param g Graphics object to use
     */
    public void paint(Graphics g) {
        UIManager.getInstance().getLookAndFeel().setFG(g, this);
        Style style = getStyle();
        int numRows = 10;
        
        //we'll set this according to the number of objects to draw on screen
        //e.g. if subtract it's always all of them
        int objsToDraw = 0;
        if(mode == MODE_SUBTRACT) {
            objsToDraw = subtractFrom;
        }else {
            objsToDraw = numObjects;
        }
        
        
        for(int i = 0; i < objsToDraw; i++) {
            int rowOffset = device.objImg.getHeight() * (i/numRows);
            int posX = getX() + style.getPadding(LEFT) + rowOffset;
            int posY = getY() + ((i % numRows) * device.objImg.getHeight()) + style.getPadding(TOP);
            Image toDrawImg = null;
            if(mode == MODE_SUBTRACT) {
                if(i >= numObjects) {
                    toDrawImg = device.crossedOutImg;
                }else {
                    toDrawImg = device.objImg;
                }
            }else {
                toDrawImg = device.objImg;
            }
            
            g.drawImage(toDrawImg, posX, posY);
        }
    }
    
    /**
     * If the user presses the main center button - decide what to do
     * @param keyCode keyCode user pressed
     */
    public void keyReleased(int keyCode) {
        if(keyCode == -5 && mode == MODE_ADD) {
            setNumObjects(numObjects + 1);
        }else if(keyCode == -5 && mode == MODE_SUBTRACT) {
            setNumObjects(numObjects -1);
        }else {
            super.keyReleased(keyCode);
        }
    }
    
    /**
     * LWUIT name of this type of component
     * @return LWUIT name of this type of component
     */
    public String getUIID() {
        return "MathDrillObjectBox";
    }
    
    /**
     * Hard coded preferred size
     * @return Dimension(30,100)
     */
    public Dimension calcPreferredSize() {
        return new Dimension(30, 100);
    }
    
    
    /**
     * set number of objects currently active
     * @param num  number of objects currently active here
     */
    public void setNumObjects(int num) {
        this.numObjects = num;
        repaint();
    }
    
    /**
     * getter for number of objects currently active
     * @return number of objects currently active.
     */
    public int getNumObjects() {
        return numObjects;
    }
}