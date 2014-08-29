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

import com.sun.lwuit.*;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.geom.Dimension;
import com.sun.lwuit.html.HTMLComponent;
import com.sun.lwuit.layouts.BorderLayout;
import com.sun.lwuit.plaf.Border;
import java.util.Vector;
import com.toughra.mlearnplayer.MLearnPlayerMidlet;

/**
 * Represents a cell in the Memory match Idevice
 * 
 * @author mike
 */
public class MemoryMatchCell extends Container{
    
    /** Our host idevice*/
    MemoryMatchIdevice idevice;
    
    /**pair that we are part of*/
    MemoryMatchPair pair;
    
    /** Cell is currently covered*/
    public static final int STATE_COVERED = 0;
    
    /** Cell is currently showing waiting for another selection*/
    public static final int STATE_SHOWING = 1;
    
    /** Cell is answered / done */
    public static final int STATE_ANSWERED = 2;
    
    /** State of the cell as per STATE_ static variables */
    public int state;
    
    /** The component that is showing for this cell right now*/
    private Component showing;
    
    /**label when covered*/
    Label coverLabel;
    
    /** the html component that is being matched */
    HTMLComponent htmlCmp;
    
    /** preferred size */
    private Dimension prefSize;
    
    /** Action Listener for when this is clicked */
    Vector actionListeners;
    
    /** ID for state change event */
    public static final int CMD_ID_STATECHG = 2;
    
    
    /**
     * Constructor
     * 
     * @param idevice our host idevice
     * @param hostMidlet our host midlet
     * @param coverImg the cover Image for this cell to show when covered
     * @param displayStr HTML of what to show inside this cell
     * @param pair Pair object so we can figure out which other cell we match with
     */
    public MemoryMatchCell(MemoryMatchIdevice idevice, MLearnPlayerMidlet hostMidlet, Image coverImg, String displayStr, MemoryMatchPair pair) {
        this.idevice = idevice;
        this.pair = pair;
        setLayout(new BorderLayout());
        this.coverLabel = new Label(coverImg);
        this.coverLabel.setFocusable(false);
        this.htmlCmp = hostMidlet.makeHTMLComponent(displayStr);
        this.htmlCmp.setFocusable(false);
        
        setWidth(idevice.cellHeight);
        setHeight(idevice.cellHeight);
        addComponent(BorderLayout.CENTER, coverLabel);
        showing = coverLabel;
        
        setFocusable(true);
        getSelectedStyle().setBorder(Border.createLineBorder(2, 0xee0000));
        
        actionListeners = new Vector();
    }

    /**
     * Provides the preferred dimension - refers back to idevice.cellWidth and cellHeight
     * 
     * @return Dimension representing preferred size
     */
    protected Dimension calcPreferredSize() {
        if(prefSize == null) {
            prefSize = new Dimension(idevice.cellWidth, idevice.cellHeight);
        }
        return prefSize;
    }

    /**
     * Watch for when the main event happens.
     * 
     * @param k 
     */
    public void keyReleased(int k) {
        System.out.println("Key " + k);
        if(k == -5) {
            fireActionEvent();
        }
        super.keyReleased(k);
    }
    
    
    /**
     * LWUIT UIID for this component
     * 
     * @return "MemoryMatchCell"
     */
    public String getUIID() {
        return "MemoryMatchCell";
    }

    /**
     * Add action listener
     * @param l Listener to add
     */
    public void addActionListener(ActionListener l) {
        actionListeners.addElement(l);
    }
    
    /**
     * Remove action listener
     * 
     * @param l Listener to remove
     */
    public void removeActionListener(ActionListener l) {
        actionListeners.removeElement(l);
    }
    
    /**
     * Fires the event when the main button is pushed to change the state
     * of this cell
     */
    protected void fireActionEvent() {
        int num = actionListeners.size();
        Command cmd = new Command("statechg", CMD_ID_STATECHG);
        ActionEvent ae = new ActionEvent(cmd, this, 0, 0);
        
        for(int i = 0; i < num; i++) {
            ((ActionListener)actionListeners.elementAt(i)).actionPerformed(ae);
        }
    }
    
    /**
     * Handles a state change to the desired newState and paints the item
     * appropriately 
     * @param newState STATE_SHOWING, STATE_COVERED 
     */
    public void changeState(int newState) {
        if(newState != this.state) {
            removeComponent(showing);
            Component nCmp = null;
            if(newState == STATE_SHOWING) {
                nCmp = htmlCmp;
            }else if(newState == STATE_COVERED) {
                nCmp = coverLabel;
            }
            addComponent(BorderLayout.CENTER, nCmp);
            
            state = newState;
            repaint();
        }
    }
    
    /**
     * See if this cell matches with another cell
     * @param otherCell the other cell to check against
     * @return true if this is a correct match, false otherwise
     */
    public boolean isMatch(MemoryMatchCell otherCell) {
        MemoryMatchCell myOther;
        if(pair.aCell == this) {
            myOther = pair.qCell;
        }else {
            myOther = pair.aCell;
        }
        
        return (otherCell == myOther);
    }
    
}
