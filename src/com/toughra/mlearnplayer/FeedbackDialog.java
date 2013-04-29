/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.toughra.mlearnplayer;

import com.sun.lwuit.*;
import com.sun.lwuit.html.DefaultHTMLCallback;
import com.sun.lwuit.html.HTMLCallback;
import com.sun.lwuit.html.HTMLComponent;

/**
 *
 * @author mike
 */
public class FeedbackDialog extends Dialog {
    
    private HTMLComponent htmlComp;
    
    private MLearnPlayerMidlet hostMidlet;

    private Component callComp;
    
    //by default the fbtime, however can be changed before calling the show method
    public int timeout;
    
    public boolean callSound = false;
    
    public boolean answerCorrect = false;
    
    public FeedbackDialog(MLearnPlayerMidlet hostMidlet) {
        this.hostMidlet = hostMidlet;
        timeout = hostMidlet.fbTime;
    }
    
    public void showFeedback(Component comp, String htmlStr) {
        // we have to do this as an html call back to wait for everything to load
        HTMLCallback callback = new ChangeListener(this);
        this.callComp = comp;
        htmlComp = hostMidlet.makeHTMLComponent(htmlStr, callback);
        addComponent(htmlComp);
        setTimeout(timeout);
    }
    
    public void showFeedback(Component comp, String htmlStr, boolean isCorrect) {
        callSound = true;
        answerCorrect = isCorrect;
        showFeedback(comp, htmlStr);
    }
    
    
    private class ChangeListener extends DefaultHTMLCallback {
        
        FeedbackDialog parent;
        
        private ChangeListener(FeedbackDialog parent) {
            this.parent = parent;
        }

        public void pageStatusChanged(HTMLComponent htmlC, int status, String url) {
            if(status == STATUS_COMPLETED) {
                if(parent.callSound) {
                    if(parent.answerCorrect) {
                        parent.hostMidlet.playPositiveSound();
                    }else {
                        parent.hostMidlet.playNegativeSound();
                    }
                }
                
                parent.showDialog();
            }
        }
        
        
    }
    
}
