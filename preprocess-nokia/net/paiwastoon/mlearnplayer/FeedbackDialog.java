/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.paiwastoon.mlearnplayer;

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
    
    public FeedbackDialog(MLearnPlayerMidlet hostMidlet) {
        this.hostMidlet = hostMidlet;
    }
    
    public void showFeedback(Component comp, String htmlStr) {
        // we have to do this as an html call back to wait for everything to load
        HTMLCallback callback = new ChangeListener(this);
        this.callComp = comp;
        htmlComp = hostMidlet.makeHTMLComponent(htmlStr, callback);
        addComponent(htmlComp);
        setTimeout(hostMidlet.fbTime);
    }
    
    private class ChangeListener extends DefaultHTMLCallback {
        
        FeedbackDialog parent;
        
        private ChangeListener(FeedbackDialog parent) {
            this.parent = parent;
        }

        public void pageStatusChanged(HTMLComponent htmlC, int status, String url) {
            if(status == STATUS_COMPLETED) {
                parent.showDialog();
                
            }
        }
        
        
    }
    
}
