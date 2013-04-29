/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.toughra.mlearnplayer;

import com.sun.lwuit.Component;
import com.sun.lwuit.List;
import com.sun.lwuit.TextArea;
import com.sun.lwuit.TextField;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.html.HTMLCallback;
import com.sun.lwuit.html.HTMLComponent;
import com.sun.lwuit.html.HTMLElement;

/**
 *
 * @author mike
 */
public class EXEHTMLCallback implements HTMLCallback{

    public void actionPerformed(ActionEvent ae, HTMLComponent htmlc, HTMLElement htmle) {
        
    }

    public void dataChanged(int i, int i1, HTMLComponent htmlc, TextField tf, HTMLElement htmle) {
        
    }

    public String fieldSubmitted(HTMLComponent htmlc, TextArea ta, String string, String string1, String string2, int i, String string3) {
        return string1;
    }

    public void focusGained(Component cmpnt, HTMLComponent htmlc, HTMLElement htmle) {
        
    }

    public void focusLost(Component cmpnt, HTMLComponent htmlc, HTMLElement htmle) {
        
    }

    public String getAutoComplete(HTMLComponent htmlc, String string, String string1) {
        return string;
    }

    public int getLinkProperties(HTMLComponent htmlc, String string) {
        return HTMLCallback.LINK_REGULAR;
    }

    public boolean linkClicked(HTMLComponent htmlc, String string) {
        return true;
    }

    public void pageStatusChanged(HTMLComponent htmlc, int status, String string) {
        if(status == HTMLCallback.STATUS_COMPLETED) {
            System.out.println("page completed");
            htmlc.revalidate();
            htmlc.repaint();
        }
    }

    public void selectionChanged(int i, int i1, HTMLComponent htmlc, List list, HTMLElement htmle) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void titleUpdated(HTMLComponent htmlc, String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean parsingError(int i, String string, String string1, String string2, String string3) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
