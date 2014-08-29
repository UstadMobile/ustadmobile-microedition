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

/**
 * Simple utility object that represents an Answer in a multiple choice question etc
 * 
 * @author mike
 */
public class Answer {
    
    /** the text (in HTML) of this answer*/
    public String answerText = null;
    
    /** the feedback to show when this answer is selected*/
    public String feedback = null;

    /** true if this is a correct answer, false otherwise*/
    public boolean isCorrect = false;
    
    /** Reference to the question for which this is a potential answer*/
    public Object question;
    
    /**URI to audio to playback for feedback*/
    public String fbAudio;

    /** The Question ID that this answer is tied to (optional) */
    public int questionId;
    
    /** The answer ID (used for logging etc)*/
    public int answerId;
    
    /** ResponseID that is used in connection with TinCan */
    public String responseId;
    
    /**
     * Constructor
     * 
     * @param text text of this answer
     * @param isCorrect true/false is this a correct answer
     */
    public Answer(String text, boolean isCorrect, int answerId, String responseId) {
        this.answerText = text;
        this.isCorrect = isCorrect;
        this.answerId = answerId;
        this.responseId = responseId;
    }
    
    public Answer(String text, boolean isCorrect, int answerId) {
        this(text, isCorrect, answerId, null);
    }
    
    /**
     * Constructor
     * 
     * @param text text of this answer
     * @param isCorrect true/false is this a correct answer
     * @param question the question that this is an answer for
     * @param feedback feedback to show when this answer is selected
     * @param responseId responseId used in TinCan reporting
     */
    public Answer(String text, boolean isCorrect, Object question, String feedback, int answerId, String responseId) {
        this(text, isCorrect, answerId, responseId);
        this.question = question;
        this.feedback = feedback;
    }
    
    /**
     * Constructor
     * 
     * @param text text of this answer
     * @param isCorrect true/false is this a correct answer
     * @param question the question that this is an answer for
     * @param feedback feedback to show when this answer is selected
     */
    public Answer(String text, boolean isCorrect, Object question, String feedback, int answerId) {
        this(text, isCorrect, answerId);
        this.question = question;
        this.feedback = feedback;
    }

}
