package net.paiwastoon.mlearnplayer.idevices;

import com.sun.lwuit.events.ActionEvent;
import net.paiwastoon.mlearnplayer.Idevice;
import net.paiwastoon.mlearnplayer.xml.*;
import com.sun.lwuit.*;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.html.HTMLComponent;
import com.sun.lwuit.plaf.Border;
import com.sun.lwuit.table.TableLayout;
import java.util.*;
import net.paiwastoon.mlearnplayer.FeedbackDialog;
import net.paiwastoon.mlearnplayer.MLearnPlayerMidlet;
import net.paiwastoon.mlearnplayer.MLearnUtils;

/**
 * This needs to be used for both multiple choice and multiple select (checkbox)
 * @author mike
 */
public class MCQIdevice extends Idevice implements ActionListener {

    private Vector questions;
    
    //vector of vectors
    private Vector answers;
    
    

    private String title;
    
    //used to help determine layout
    private int totalQuestions = 0;
    private int totalAnswers = 0;
    
    private String htmlIntro;
    
    //used to track which answer the user has currently selected
    private int[] selectedAnswers;
    
    ///used to track stuff
    private HTMLComponent[] fbackAreas;
    
    //if there is an audio file to play when first loading
    private String audioURL;
    
    Form form;
    
    //false if question not yet answered/attempted by student
    boolean[] qAttempted;

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

    public void start() {
        super.start();
        if(audioURL != null) {
            hostMidlet.playMedia(audioURL);
        }
    }

    public void stop() {
        
        super.stop();
    }

    public void dispose() {
        form.removeAll();
        super.dispose();
    }

    public int getLogType() {
        return Idevice.LOG_QUIZ;
    }

    public int[] getScores() {
        return new int[] {correctFirst, MLearnUtils.countBoolValues(hadCorrect, true),
            questions.size()
        };
    }
    
}
