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
import com.sun.lwuit.html.HTMLComponent;
import com.sun.lwuit.plaf.Border;
import com.sun.lwuit.table.TableLayout;
import java.util.*;
import com.toughra.mlearnplayer.FeedbackDialog;
import com.toughra.mlearnplayer.MLearnPlayerMidlet;
import com.toughra.mlearnplayer.MLearnUtils;

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
            questions.addElement(questionTextHTML);

            Vector questionAnswers = currentNode.findChildrenByTagName("answer", true);
            Vector answerObjVector = new Vector();
            for(int j = 0; j < questionAnswers.size(); j++) {
                XmlNode currentAnswer = (XmlNode)questionAnswers.elementAt(j);
                String answerText = currentAnswer.getTextChildContent(0);
                XmlNode feedbackNode = (XmlNode)currentAnswer.findChildrenByTagName("feedback", 
                        true).elementAt(0);
                String feedback = feedbackNode.getTextChildContent(0);
                Answer answer = new Answer(answerText,
                        currentAnswer.getAttribute("iscorrect").equals("true"), 
                        new Integer(i), feedback);
                
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
        
        TableLayout tLayout = new TableLayout((totalQuestions*2) + totalAnswers,
                2);
        TableLayout.setMinimumSizePerColumn(20);
        form.setLayout(tLayout);
        
        selectedAnswers = new int[totalQuestions];

        for(int i = 0; i < questions.size(); i++) {
            TableLayout.Constraint qConstrtaint = tLayout.createConstraint();
            qConstrtaint.setHorizontalSpan(2);
            
            HTMLComponent qHTML = hostMidlet.makeHTMLComponent(questions.elementAt(i).toString());
            qHTML.setFocusable(false);
            form.addComponent(qConstrtaint, qHTML);
            
            Vector questionAnswers = (Vector)answers.elementAt(i);
            String qId = String.valueOf(i);
            RadioButton[] buttonArr = new RadioButton[questionAnswers.size()];
            
            for(int j = 0; j < questionAnswers.size();j++) {
                Answer currentAnswer = (Answer)questionAnswers.elementAt(j);
                RadioButton rButton = new RadioButton();
                buttonArr[j] = rButton;
                rButton.setCommand(new Command(String.valueOf(j), j));
                rButton.setText("");
                rButton.setGroup(qId);
                rButton.addActionListener(this);
                TableLayout.Constraint rButtonCon = tLayout.createConstraint();
                rButtonCon.setWidthPercentage(20);
                rButton.setAlignment(Component.CENTER);
                form.addComponent(rButtonCon, rButton);
                
                TableLayout.Constraint answerCon = tLayout.createConstraint();
                answerCon.setWidthPercentage(80);
                HTMLComponent answerComp = hostMidlet.makeHTMLComponent(currentAnswer.answerText);
                //answerComp.setFocusable(false);
                form.addComponent(answerCon, answerComp);
            }
            
            for(int j = 0; j < questionAnswers.size();j++) {
                if(j > 0) {
                    buttonArr[j].setNextFocusUp(buttonArr[j-1]);
                }
                if(j < questionAnswers.size() -1) {
                    buttonArr[j].setNextFocusDown(buttonArr[j+1]);
                }
                
                if (j == 0) {
                    buttonArr[j].requestFocus();
                }
            }
            
            
        }
        TableLayout.Constraint bConstrain2 =  tLayout.createConstraint();
        Label labelSpan = new Label(" ");
        labelSpan.getStyle().setBorder(Border.createBevelRaised());
        bConstrain2.setHorizontalSpan(2);
        form.addComponent(bConstrain2, labelSpan);
                
        
        return form;
    }

    /**
     * Recognizes when the user has select an answer - shows the appropriate
     * feedback
     * 
     * @param ae  ActionEvent
     */
    public void actionPerformed(ActionEvent ae) {
        if(ae.getComponent() instanceof RadioButton) {
            ae.consume();
            RadioButton srcButton = (RadioButton)ae.getComponent();
            int qId = Integer.parseInt(srcButton.getGroup());
            
            
            selectedAnswers[qId] = srcButton.getCommand().getId();
        
            int selectedAnswerNum = selectedAnswers[qId];
            FeedbackDialog fbDialog = new FeedbackDialog(hostMidlet);
            Answer selectedAnswer = (Answer)
                    ((Vector)answers.elementAt(qId)).elementAt(selectedAnswerNum);
            fbDialog.showFeedback(srcButton, selectedAnswer.feedback, selectedAnswer.isCorrect);
            
            if(!qAttempted[qId]) {
                qAttempted[qId] = true;
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
