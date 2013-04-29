/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
import java.io.InputStream;
import java.util.Random;
import java.util.Vector;
import com.toughra.mlearnplayer.xml.XmlNode;



/**
 *
 * @author mike
 */
public class MathDrillIdevice extends Idevice implements ActionListener {
    
    public static final int SECTION_RAND = 0;
    public static final int SECTION_ADDON = 1;
    
    public static final int MODE_OBJS = 0;
    
    
    String operator;
    
    String imgSrc;
    Image objImg;
    
    String crossOutImgSrc;
    Image crossedOutImg;
    
    int numQuestions;
    
    Form mainFrm;
    
    int[][] questionOperators;
    
    int[] exprResults;
    
    int currentAnswer;
    
    MathDrillObjectBox[] objectBoxes;
    
    MathDrillObjectBox resultBox;
    
    Command checkCommand;
    
    String positiveFeedback;
    
    String negativeFeedback;
    
    String completeFeedback;
    
    int numQuestionsDone = 0;
    
    int currentNumAttempts = 0;
            
    
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

    public int getMode() {
        return Idevice.MODE_LWUIT_FORM;
    }

    public void dispose() {
        
    }

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
    
    public void showQuestion() {
        this.exprResults = new int[this.questionOperators.length];
        for(int i = 0; i < this.questionOperators.length;i++) {
            Random r = new Random();
            exprResults[i] = r.nextInt(questionOperators[i][SECTION_RAND])
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

    public void actionPerformed(ActionEvent ae) {
        if(ae.getCommand().equals(checkCommand)) {
            ae.consume();
            
            FeedbackDialog fbDialog = new FeedbackDialog(hostMidlet);
            if(resultBox.getNumObjects() == currentAnswer) {
                if(currentNumAttempts == 0) {
                    correctFirst++;
                }
                
                fbDialog.showFeedback(mainFrm, positiveFeedback, true);
                numQuestionsDone++;
                
                if(numQuestionsDone <= numQuestions) {
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

    public int getLogType() {
        return Idevice.LOG_QUIZ;
    }

    public int[] getScores() {
        return new int[] { correctFirst, numQuestionsDone, numQuestions };
    }
    
    public void start() {
        super.start();
        showQuestion();
    }

    public void stop() {
        super.stop();
    }
    
    
}
class MathDrillObjectBox extends Component {
    
    MathDrillIdevice device;
    
    int numObjects;
    
    static final int MODE_DISPLAY = 0;
    
    static final int MODE_ADD = 1;
    
    static final int MODE_SUBTRACT = 2;
    
    int subtractFrom = 0;
    
    int mode;
    
    int numItemsInCol = 10;
    
    public MathDrillObjectBox(MathDrillIdevice device, int numObjects) {
        this.device = device;
        this.numObjects = numObjects;
        setFocusable(false);
        
        mode = MODE_DISPLAY;
    }
    
    
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
    
    
    public void keyReleased(int keyCode) {
        if(keyCode == -5 && mode == MODE_ADD) {
            setNumObjects(numObjects + 1);
        }else if(keyCode == -5 && mode == MODE_SUBTRACT) {
            setNumObjects(numObjects -1);
        }else {
            super.keyReleased(keyCode);
        }
    }
    
    public String getUIID() {
        return "MathDrillObjectBox";
    }
    
    public Dimension calcPreferredSize() {
        return new Dimension(30, 100);
    }
    
    public void setNumObjects(int num) {
        this.numObjects = num;
        repaint();
    }
    
    public int getNumObjects() {
        return numObjects;
    }
}