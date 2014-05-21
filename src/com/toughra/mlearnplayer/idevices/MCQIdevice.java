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
import com.sun.lwuit.events.ActionEvent;
import com.toughra.mlearnplayer.Idevice;
import com.sun.lwuit.*;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.events.FocusListener;
import com.sun.lwuit.geom.Dimension;
import com.sun.lwuit.html.HTMLComponent;
import com.sun.lwuit.layouts.GridLayout;
import com.sun.lwuit.plaf.Border;
import com.sun.lwuit.table.TableLayout;
import com.toughra.mlearnplayer.EXEStrMgr;
import java.util.*;
import com.toughra.mlearnplayer.FeedbackDialog;
import com.toughra.mlearnplayer.MLearnPlayerMidlet;
import com.toughra.mlearnplayer.MLearnUtils;
import org.json.me.JSONException;
import org.json.me.JSONObject;




/**
 * The MCQIdevice will render a set of Multiple Choice Questions
 * 
 * @author mike
 */
public class MCQIdevice extends Idevice implements ActionListener {


    /** Vector of html text of questions to be shown */
    private Vector questions;
    
    /** 
     * Vector containing child Vector objects, the children contain Answer objects
     * for the questionHTML at the same index in questions
     */
    private Vector answers;
    
    /** title of the question*/
    private String title;
    
    /**used to help determine layout*/
    private int totalQuestions = 0;
    
    /**used to help determine layout*/
    private int totalAnswers = 0;
    
    /**HTML to show before questions */
    private String htmlIntro;
    
    /**used to track which answer the user has currently selected*/
    private int[] selectedAnswers;
    
    /**the HTMLComponent that will be used to show feedback - one for each question*/
    private HTMLComponent[] fbackAreas;
    
    /**if there is an audio file to play when first loading*/
    private String audioURL;
    
    /** The main LWUIT form */
    Form form;
    
    //false if question not yet answered/attempted by student
    boolean[] qAttempted;
    
    /** width of border when an item is hilighted to be moved */
    static int borderWidthHilight = 3;
    
    /** normal width of border of item*/
    static int borderWidthNormal = 1;
    
    /** padding between the button and it's html contents */
    static int padding = 5;
    
    /** color of border normally */
    static int normalColor = 0x000000;
    
    /** color of border when hilighted to be moved */
    static int activeColor = 0xff0000;


    /** 
     * Constructor - setup the for, dig through the questions and answers
     * that are there.
     * 
     * @param host the midlet host
     * @param data the XML data to construct with
     */
    public MCQIdevice(MLearnPlayerMidlet host, XmlNode data) {
        super(host);
        
        
        questions = new Vector();
        answers = new Vector();
        
        Vector dataQuestions = data.findChildrenByTagName("question", true);
        hadCorrect = MLearnUtils.makeBooleanArray(false, dataQuestions.size());
        qAttempted = MLearnUtils.makeBooleanArray(false, dataQuestions.size());
        
        fbackAreas = new HTMLComponent[dataQuestions.size()];
        
        for(int i = 0; i < dataQuestions.size(); i++) {
            XmlNode currentNode = (XmlNode)dataQuestions.elementAt(i);
            if(i == 0 && currentNode.hasAttribute("audio")) {
                audioURL = currentNode.getAttribute("audio");
            }
            
            //CDATA text section will always be the first child after question
            String questionTextHTML = currentNode.getTextChildContent(0);
            XmlNode tinCanNode = currentNode.findFirstChildByTagName("tincan", true);
            String tinCanId = null;
            String tinCanDef = null;
            if(tinCanNode != null) {
                tinCanDef = currentNode.findFirstChildByTagName("activitydef", 
                        true).getTextChildContent(0);
                tinCanId = tinCanNode.getAttribute("id");
            }
            QuestionItem thisItem = new QuestionItem(questionTextHTML, tinCanId, 
                    tinCanDef);
            questions.addElement(thisItem);

            Vector questionAnswers = currentNode.findChildrenByTagName("answer", true);
            Vector answerObjVector = new Vector();
            for(int j = 0; j < questionAnswers.size(); j++) {
                XmlNode currentAnswer = (XmlNode)questionAnswers.elementAt(j);
                String answerText = currentAnswer.getTextChildContent(0);
                XmlNode feedbackNode = (XmlNode)currentAnswer.findChildrenByTagName("feedback", 
                        true).elementAt(0);
                String feedback = feedbackNode.getTextChildContent(0);
                String responseId = currentAnswer.getAttribute("id");
                Answer answer = new Answer(answerText,
                        currentAnswer.getAttribute("iscorrect").equals("true"), 
                        new Integer(i), feedback, j, responseId);
                answer.questionId = i;
                
                if(feedbackNode.hasAttribute("audio")) {
                    answer.fbAudio = feedbackNode.getAttribute("audio");
                }
                
                
                answerObjVector.addElement(answer);
                totalAnswers++;
            }
            answers.addElement(answerObjVector);
            questionAnswers = null;
            totalQuestions++;
        }
        int done = 0;
    }

    public String getDeviceTypeName() {
        return "mcq";
    }
    
    

    /**
     * This is an LWUIT mode idevice 
     * @return Idevice.MODE_LWUIT_FORM
     */
    public int getMode() {
        return Idevice.MODE_LWUIT_FORM;
    }

    /**
     * Will make a table layout including questions, answers and feedback
     * 
     * @return 
     */
    public Form getForm() {
        form = new Form();

        
        //GridLayout gLayout = new GridLayout(totalQuestions + totalAnswers, 1);
        TableLayout tLayout = new TableLayout(totalQuestions + totalAnswers, 1);
        /*TableLayout tLayout = new TableLayout((totalQuestions*2) + totalAnswers,
                2);
        TableLayout.setMinimumSizePerColumn(20);
        form.setLayout(tLayout);
        */
        form.setLayout(tLayout);
        
        selectedAnswers = new int[totalQuestions];

        Component toFocus = null;
        
        for(int i = 0; i < questions.size(); i++) {
            
            HTMLComponent qHTML = hostMidlet.makeHTMLComponent(questions.elementAt(i).toString());
            qHTML.setFocusable(false);
            form.addComponent(qHTML);
            
            Vector questionAnswers = (Vector)answers.elementAt(i);
            String qId = String.valueOf(i);
            
            MCQAnswerItem[] itemArr = new MCQAnswerItem[questionAnswers.size()];
            
            for(int j = 0; j < questionAnswers.size();j++) {
                Answer currentAnswer = (Answer)questionAnswers.elementAt(j);
                HTMLComponent answerComp = hostMidlet.makeHTMLComponent(currentAnswer.answerText);
                MCQAnswerItem thisItem = new MCQAnswerItem(this, answerComp, currentAnswer);
                
                thisItem.updateStyle();
                itemArr[j] = thisItem;
                TableLayout.Constraint tConst = tLayout.createConstraint();
                tConst.setWidthPercentage(100);
                form.addComponent(tConst, thisItem);
                
                if(i == 0 && j == 0) {
                    form.setFocused(thisItem);
                    thisItem.setFocus(true);
                    thisItem.updateStyle();
                }
                thisItem.addActionListener(this);
            }
            
            for(int j = 0; j < questionAnswers.size();j++) {
                if(j > 0) {
                    itemArr[j].setNextFocusUp(itemArr[j-1]);
                }
                if(j < questionAnswers.size() -1) {
                    itemArr[j].setNextFocusDown(itemArr[j+1]);
                }
            }
            
            
        }
        
        return form;
    }

    /**
     * Recognizes when the user has select an answer - shows the appropriate
     * feedback
     * 
     * @param ae  ActionEvent
     */
    public void actionPerformed(ActionEvent ae) {
        if(ae.getComponent() instanceof MCQAnswerItem) {
            ae.consume();
            MCQAnswerItem ansItem = (MCQAnswerItem)ae.getComponent();
            
            
            FeedbackDialog fbDialog = new FeedbackDialog(hostMidlet);
            Answer selectedAnswer = ansItem.answer;
            
            JSONObject stmtObject= new JSONObject();
            JSONObject actorObj = EXEStrMgr.getInstance().getTinCanActor();
            QuestionItem question = (QuestionItem)questions.elementAt(
                    selectedAnswer.questionId);
            
            
            try {
                stmtObject.put("actor", actorObj);
                JSONObject activityDef = new JSONObject(question.tinCanDefinition);
                JSONObject objectDef = new JSONObject();
                objectDef.put("id", question.activityId);
                objectDef.put("definition", activityDef);
                objectDef.put("objectType", "activity");
                stmtObject.put("object", objectDef);
                
                JSONObject verbDef = new JSONObject();
                verbDef.put("id", "http://adlnet.gov/expapi/verbs/answered");
                JSONObject verbDisplay = new JSONObject();
                verbDisplay.put("en-US", "answered");
                verbDef.put("display", verbDisplay);
                stmtObject.put("verb", verbDef);
                
                JSONObject resultDef = new JSONObject();
                resultDef.put("success", selectedAnswer.isCorrect);
                resultDef.put("response", selectedAnswer.responseId);
                stmtObject.put("result", resultDef);
            }catch(JSONException je) {
                EXEStrMgr.lg(180, "Exception creating json tincan statement", 
                        je);
            }
            
            
            String totalStmtStr = stmtObject.toString();
            
            EXEStrMgr.lg(this, //idevice
                selectedAnswer.questionId, //question id
                0, //time on device in ms
                0, //num correctly answered
                0, //num answered correct first attempt
                0, //num questions attempted
                EXEStrMgr.VERB_ANSWERED, //verb
                selectedAnswer.isCorrect ? 1 : 0, //score
                1, //maxScorePossible
                String.valueOf(selectedAnswer.answerId),//answer given 
                Idevice.BLANK);//remarks
        
            
            fbDialog.showFeedback(ansItem, selectedAnswer.feedback, selectedAnswer.isCorrect);
            int qId = selectedAnswer.questionId;
            
            if(!qAttempted[qId]) {
                qAttempted[qId] = true;
                if(correctFirst == -1) {
                    correctFirst = 0;
                }
                
                if(selectedAnswer.isCorrect) {
                    correctFirst++;
                }
            }
            
            if(selectedAnswer.isCorrect) {
                hadCorrect[qId] = true;
            }
            
            if(selectedAnswer.fbAudio != null) {
                hostMidlet.playMedia(selectedAnswer.fbAudio);
            }
        }
                
    }

    /**
     * Start the idevice and play the starting sound if there is one
     */
    public void start() {
        super.start();
        if(audioURL != null) {
            hostMidlet.playMedia(audioURL);
        }
    }

    /**
     * Stop the idevice - stop all media
     */
    public void stop() {
        
        super.stop();
        
        EXEStrMgr.lg(this, //idevice
                0, //question id
                getTimeOnDevice(), //time on device in ms
                MLearnUtils.countBoolValues(hadCorrect, true), //num correctly answered
                Math.max(0, correctFirst), //num answered correct first attempt
                MLearnUtils.countBoolValues(qAttempted, true), //num questions attempted
                Idevice.LOGDEVCOMPLETE, //verb
                0, //score
                0, //maxScorePossible
                Idevice.BLANK,//answer given 
                Idevice.BLANK);//remarks
    }

    /**
     * Dispose of components
     */
    public void dispose() {
        form.removeAll();
        super.dispose();
    }

    /**
     * This is a quiz mode idevice
     * @return Idevice.LOG_QUIZ
     */
    public int getLogType() {
        return Idevice.LOG_QUIZ;
    }

    /**
     * Provide the quiz scores for logging purposes
     * 
     * @return array of quiz scores 
     */
    public int[] getScores() {
        return new int[] {correctFirst, MLearnUtils.countBoolValues(hadCorrect, true),
            questions.size()
        };
    }
    
}

/**
 * Utility container class representing a question and its tincanid and
 * tincan activity definition
 */
class QuestionItem {
    
    /** Activity TinCan Id - might be null*/
    String activityId;
    
    /** Activity TinCan Definition (JSON string)  - might be null*/
    String tinCanDefinition;
    
    /** HTML text of question */
    String questionHTML;
    
    QuestionItem(String questionHTML, String activityId, String tinCanDefinition) {
        this.questionHTML = questionHTML;
        this.activityId = activityId;
        this.tinCanDefinition = tinCanDefinition;
    }
    
    public String toString() {
        return this.questionHTML;
    }
    
}


/**
 * Utility container class to represent an MCQ Answer item.  Replaces the 
 * RadioButton based version so that it's easier to select and has fewer
 * focus issues
 * 
 * 
 * @author mike
 */
class MCQAnswerItem extends Container implements FocusListener{
    
    /** our host idevice*/
    private MCQIdevice idevice;
    
    protected Answer answer;
    
    HTMLComponent realComp;
    
    /**
     * Constructor 
     * 
     * @param idevice our host idevice
     */
    MCQAnswerItem(MCQIdevice idevice,HTMLComponent realComp, Answer answer) {
        this.idevice = idevice;
        this.answer = answer;
        addFocusListener(this);
        GridLayout layout = new GridLayout(1,1);
        setLayout(layout);
        this.realComp = realComp;
        
        realComp.setFocusable(false);
        realComp.setScrollable(false);
        addComponent(realComp);
        setFocusable(true);
        Form f;
    }
    
    public Dimension getPreferredSize() {
        Dimension d = realComp.getPreferredSize();
        
        return new Dimension(d.getWidth() + (idevice.borderWidthHilight*2) + (idevice.padding * 2),
                d.getHeight() + (idevice.borderWidthHilight * 2) + (idevice.padding * 2));
    }
    
    /**
     * Comparator method to be used to check for correct ordering by the user
     * 
     * @param other
     * @return true if this is the same object, false otherwise
     */
    public boolean equals(Object other) {
        if(other == this) {
            return true;
        }
        
        return false;
    }

    public void keyReleased(int k) {
        //the big button
        if(k == -5) {
            fireActionEvent(k);
        }
        super.keyReleased(k); //To change body of generated methods, choose Tools | Templates.
    }
    
    

    /**
     * When we get focus - tell the parent idevice about this
     * 
     * @param cmpnt 
     */
    public void focusGained(Component cmpnt) {
        int x = 0;
    }

    public void focusLost(Component cmpnt) {
        int x = 0;
    }
    
    
    /**item is held down / ready to move*/
    boolean active = false;
    
    /** action listeners - used for going active/inactive*/
    Vector actionListeners;
    
    /**
     * Add ActionListener for when this item goes active/inactive
     * @param l ActionListener
     */
    public void addActionListener(ActionListener l) {
        if(actionListeners == null) {
            actionListeners = new Vector();
        }
        actionListeners.addElement(l);
    }
    
    /**
     * Remove an ActionListener for when this item goes active/inactive
     * 
     * @param l ActionListener
     */
    public void removeActionListener(ActionListener l) {
        actionListeners.removeElement(l);
    }
    
    /**
     * Fire event for keycode
     * @param k keycode
     */
    void fireActionEvent(int k) {
        ActionEvent evt = new ActionEvent(this, k);
        MLearnUtils.fireActionEventToAll(actionListeners, evt);
    }
    
    /**
     * Update the style to set the border color and width for this item according
     * to whether it is currently active or inactive
     */
    public void updateStyle() {
        for(int j = 0; j < SortIdevice.allDirs.length; j++) {
            //TODO: remove dependency on SortIdevice
            getStyle().setPadding(SortIdevice.allDirs[j], idevice.padding, true);
            getSelectedStyle().setPadding(SortIdevice.allDirs[j], idevice.padding, true);
        }
        
        int color = (active == true) ? idevice.activeColor : 0x000000;

        getStyle().setBorder(
                Border.createLineBorder(idevice.borderWidthNormal, color));

        getSelectedStyle().setBorder(
                Border.createLineBorder(idevice.borderWidthHilight, color));

    }
    
    /**
     * Set whether this so it can be moved around (or not) 
     * @param active true if it should be 
     */
    public void setActive(boolean active) {
        if(active) {
            getSelectedStyle().setBorder(Border.createLineBorder(
                    MCQIdevice.borderWidthHilight, SortIdevice.activeColor));
        }else {
            getSelectedStyle().setBorder(Border.createLineBorder(
                    MCQIdevice.borderWidthHilight, SortIdevice.normalColor));
        }
        
        this.active = active;
        repaint();
    }
    
    
}
