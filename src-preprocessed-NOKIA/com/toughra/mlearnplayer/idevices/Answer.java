/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.toughra.mlearnplayer.idevices;

/**
 *
 * @author mike
 */
public class Answer {
    
    public String answerText = null;
    
    public String feedback = null;

    public boolean isCorrect = false;
    
    public Object question;
    
    //URI to audio to playback for feedback
    public String fbAudio;

    public Answer(String text, boolean isCorrect) {
        this.answerText = text;
        this.isCorrect = isCorrect;
    }
    
    public Answer(String text, boolean isCorrect, Object question, String feedback) {
        this(text, isCorrect);
        this.question = question;
        this.feedback = feedback;
    }

}
