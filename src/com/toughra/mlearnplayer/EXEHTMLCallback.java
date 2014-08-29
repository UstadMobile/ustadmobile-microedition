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
