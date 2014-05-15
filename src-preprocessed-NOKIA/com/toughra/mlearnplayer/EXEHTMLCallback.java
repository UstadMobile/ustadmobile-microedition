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
 * Implements the HTMLCallback interface of LWUIT - simply repaints and 
 * revalidates when the page is loaded
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
            htmlc.revalidate();
            htmlc.repaint();
        }
    }

    public void selectionChanged(int i, int i1, HTMLComponent htmlc, List list, HTMLElement htmle) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    public void titleUpdated(HTMLComponent htmlc, String string) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean parsingError(int i, String string, String string1, String string2, String string3) {
        //throw new UnsupportedOperationException("Not supported yet.");
        return false;
    }
    
}
