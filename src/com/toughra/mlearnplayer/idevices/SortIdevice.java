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

import com.toughra.mlearnplayer.FeedbackDialog;
import com.toughra.mlearnplayer.Idevice;
import com.toughra.mlearnplayer.MLearnPlayerMidlet;
import com.toughra.mlearnplayer.MLearnUtils;
import com.sun.lwuit.*;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.events.FocusListener;
import com.sun.lwuit.html.HTMLComponent;
import com.sun.lwuit.layouts.BoxLayout;
import com.sun.lwuit.layouts.FlowLayout;
import com.sun.lwuit.layouts.GridLayout;
import com.sun.lwuit.plaf.Border;
import com.toughra.mlearnplayer.EXEStrMgr;
import java.util.Vector;
import com.toughra.mlearnplayer.xml.XmlNode;



/**
 * Implements to the sort idevice interaction in J2ME.
 * 
 * @author mike
 */
public class SortIdevice extends Idevice implements ActionListener {

    /**the items that the user will sort*/
    SortableItem[] items; 
    
    /** the items in the correct order*/
    SortableItem[] correctOrder;
    
    /** Sort items left to right flag */
    public static final int DIR_LTR = 1;
    
    /** sort items right to left flag */
    public static final int DIR_RTL = 2;
    
    /** sort items top to bottom flag */
    public static final int DIR_TTB = 3;
    
    /** direction we are operating in */
    int direction = DIR_LTR;
    
    /** width of border when an item is hilighted to be moved */
    static int borderWidthHilight = 3;
    
    /** normal width of border of item*/
    static int borderWidthNormal = 1;
    
    /** padding between the button and it's html contents */
    static int padding = 5;
    
    /** color of border normally */
    static int normalColor = 0x000000;
    
    /** color of border when hilighted to be moved */
    static int activeColor = 0xff0000;
    
    //TODO: there must be a better way
    static final int[] allDirs = {Component.TOP, Component.LEFT, Component.BOTTOM, Component.RIGHT};
    
    /** currently active index or -1 if no item currently active */
    int currentActiveIndex = -1;
    
    /**the form where components live*/
    Form form;
    
    Form containerForm;
    
    /**
     * Increment map is used to determine when a user presses a direction button
     * what happens to the position of the item in the main list.
     * 
     * in the form of keyCode -&gt; increment - e.g. 3(left) to -1 (move down one)
     */
    int[] incMap;
    
    /** 
     * Instruction HTML to show at the beginning
     */
    String instructions;
    
    /** HTML feedback to show when correctly sorted */
    String positiveFeedback;
    
    /** HTML feedback to show when incorrectly sorted */
    String negativeFeedback;
    
    /** The currently focused item */
    SortableItem focusedItem;
    
    /** Text to show on the button for user to select a check */
    String checkText;
    
    /** The Button for the user to request their answer to be checked */
    Button checkButton;
    
    /** number of attempts that have been made so far*/
    int numAttempts = 0;
    
    /** Storing the last known given order*/
    StringBuffer givenOrder;
    
    boolean correctAnswerGiven = false;
    
    /**
     * Constructor 
     * 
     * @param hostMidlet our host midlet
     * @param srcData the XML data for this idevice
     */
    public SortIdevice(MLearnPlayerMidlet hostMidlet, XmlNode srcData) {
        super(hostMidlet);
        givenOrder = new StringBuffer();
        Vector sortItems = srcData.findChildrenByTagName("item", true);
        
        XmlNode optionNode = (XmlNode)srcData.findChildrenByTagName("sortoptions", true).elementAt(0);
        incMap = new int[8];
        if(optionNode.hasAttribute("dir")) {
            if(MLearnUtils.equalsIgnoreCase(optionNode.getAttribute("dir"), "rtl")) {
                direction = DIR_RTL;
            }else if(MLearnUtils.equalsIgnoreCase(optionNode.getAttribute("dir"), "ttb")) {
                direction = DIR_TTB;
            }
        }
        
        checkText = optionNode.getAttribute("checkbuttontext");
        checkButton = new Button(checkText);
        
        
        ///sort out increment direction maps - see the documentation for incMap
        if(direction == DIR_LTR) {
            incMap[3] = -1;
            incMap[4] = 1;
        }else if(direction == DIR_RTL){
            incMap[3] = 1;
            incMap[4] = -1;
        }else if(direction == DIR_TTB) {
            incMap[1] = -1;
            incMap[2] = 1;
        }
        
        
        int numItems = sortItems.size();
        items = new SortableItem[numItems];
        
        
        for(int i = 0; i < numItems; i++) {
            XmlNode currentNode = (XmlNode)sortItems.elementAt(i);
            items[i] = new SortableItem(this, i);
            
            FlowLayout layout;
            
            HTMLComponent htmlComp = hostMidlet.makeHTMLComponent(currentNode.getTextChildContent(0));
            htmlComp.setFocusable(false);
            htmlComp.setScrollable(false);
            items[i].addComponent(htmlComp);
            
                    
            items[i].setFocusable(true);
            
            items[i].addActionListener(this);
            items[i].updateStyle();
        }
        
        correctOrder = new SortableItem[items.length];
        System.arraycopy(items, 0, correctOrder, 0, items.length);
        
        //shuffle
        MLearnUtils.shuffleArray(items);
        
        
        
        
        instructions = srcData.findFirstChildByTagName("instructions", true).getTextChildContent(0);
        positiveFeedback = srcData.findFirstChildByTagName("positivefeedback", true).getTextChildContent(0);
        negativeFeedback = srcData.findFirstChildByTagName("negativefeedback", true).getTextChildContent(0);
        
        updateNextFocus();
        
        
    }

    public String getDeviceTypeName() {
        return "sort";
    }
    
    
    
    /** 
     * Check the answer of the user
     * 
     * @return true if sorted correctly false otherwise
     */
    boolean checkAnswer() {
        int numItems = items.length;
        boolean result = true;
        givenOrder = new StringBuffer();
        
        for(int i = 0; i < numItems; i++) {
            if(!items[i].equals(correctOrder[i])) {
                result = false;
            }
            givenOrder.append("Item ").append(items[i].correctPlace).append(" in pos ")
                    .append(i);
            if(i < numItems -1) {
                givenOrder.append(',');
            }
        }
        
        return result;
    }

    /**
     * Event Listener for when the user requests their item checked, and if the
     * user is making an item active to be moved for an item or moving an already 
     * active item
     * 
     * @param ae ActionEvent
     */
    public void actionPerformed(ActionEvent ae) {
        //see if this is a request for a check
        if(ae.getCommand() != null && ae.getCommand().getCommandName() != null) {
            if(correctAnswerGiven == true) {
                return;//already done
            }
            if(ae.getCommand().getCommandName().equals(checkText)) {
                numAttempts++;
                FeedbackDialog fbDialog = new FeedbackDialog(hostMidlet);
                boolean isCorrect = checkAnswer();
                correctAnswerGiven = isCorrect;
                
                if(numAttempts == 0) {
                    correctFirst = 1;
                }
                EXEStrMgr.lg(this, //idevice
                    0, //question id
                    0, //time on device in ms
                    0, //num correctly answered
                    0, //num answered correct first attempt
                    0, //num questions attempted
                    EXEStrMgr.VERB_ANSWERED, //verb
                    isCorrect ? 1 : 0, //score
                    1, //maxScorePossible
                    givenOrder.toString(),//answer given 
                    Idevice.BLANK);//remarks
                
                fbDialog.showFeedback(form, isCorrect ? positiveFeedback : negativeFeedback);
            }
        }
        
        SortableItem src = null;
        if(ae.getSource() instanceof SortableItem) {
            src = (SortableItem)ae.getSource();
        }else if(currentActiveIndex != -1){
            src = items[currentActiveIndex];
        }else if(focusedItem != null) {
            src = focusedItem;
        }
        else if(form.getFocused() instanceof SortableItem) {
            src = (SortableItem)form.getFocused();
        }
        
        Component srcCompRef = ae.getComponent();
        //if this is not a request for a check 
        if(src != null && !checkButton.hasFocus()) {
        
            int key = ae.getKeyEvent();
            
            boolean needUpdate = false;
            if(!src.active && key == -5) {
                //this is a request to make an item active for moving
                src.setActive(true);
                currentActiveIndex = MLearnUtils.indexInArray(items, src);
                src.requestFocus();
                needUpdate = true;
                //containerForm.setFocused(form);
                //form.setFocused(src);
            }else if(src.active && key == -5) {
                //item is active - now being deactivated
                src.setActive(false);
                currentActiveIndex = -1;
                src.requestFocus();
                needUpdate = true;
                //containerForm.setFocused(form);
                //form.setFocused(src);
            }else if(currentActiveIndex != -1 && src.active && (key == -3 || key == -4 || key == -1 || key == -2)) {
                //active item needs to move
                int moveAmount = incMap[Math.abs(key)];
                moveActiveItem(moveAmount);
                remakeForm();
                needUpdate = true;
            }
            
            if(needUpdate) {
                containerForm.revalidate();
                containerForm.repaint();
            }
        }else {
            System.out.println("other key event: " + ae.getKeyEvent());
        }
    }
    
    /**
     * For each sortable item make sure that the correct next item 
     */
    private void updateNextFocus() {
        //set the correct focus movements
        int numItems = items.length;
        
        checkButton.setNextFocusUp(items[items.length - 1]);
        for(int i = 0; i < numItems; i++) {
            if(direction == DIR_LTR || direction == DIR_RTL) {
                items[i].setNextFocusDown(checkButton);
            }
            
            if(direction == DIR_LTR) {
                if(i < numItems -1) {
                    items[i].setNextFocusRight(items[i+1]);
                }else {
                    items[i].setNextFocusRight(items[numItems-1]);
                }
                if(i > 0) {
                    items[i].setNextFocusLeft(items[i-1]);
                }else {
                    items[i].setNextFocusLeft(items[0]);
                }
            }else if(direction == DIR_RTL){
                if(i < numItems -1) {
                    items[i].setNextFocusLeft(items[i+1]);
                }else {
                    items[i].setNextFocusLeft(items[numItems-1]);
                }
                if(i > 0) {
                    items[i].setNextFocusRight(items[i-1]);
                }else {
                    items[i].setNextFocusRight(items[0]);
                }
            }
        }
    }
    
    /**
     * Removes all the current sortable items from the form, then adds
     * them.  to be called when the components have been reordered by the user
     */
    public void remakeForm() {
        for(int i = 0; i < items.length; i++) {
            form.removeComponent(items[i]);
        }
        
        for(int i = 0; i < items.length; i++) {
            form.addComponent(items[i]);
        }
        
        items[currentActiveIndex].requestFocus();
        items[currentActiveIndex].updateStyle();
        
        
        updateNextFocus();
    }
    
    /**
     * Allow the user to reorder things - move the active item by the increment
     * 
     * @param increment +1 or -1 - amount by which to move the active SortableItem
     */
    public void moveActiveItem(int increment){
        int newPos = currentActiveIndex + increment;
        if(newPos < 0 || newPos >= items.length) {
            return;//not really possible
        }
        
        SortableItem otherItem = items[newPos];
        SortableItem activeItem = items[currentActiveIndex];
        
        //swap them around now
        items[newPos] = activeItem;
        items[currentActiveIndex] = otherItem;
        
        currentActiveIndex = newPos;
    }

    
    /**
     * This is a LWUIT mode idevice
     * @return Idevice.MODE_LWUIT_FORM
     */
    public int getMode() {
        return Idevice.MODE_LWUIT_FORM;
    }

    /**
     * Return the main form 
     * 
     * @return LWUIT form to be used to run the idevice
     */
    public Form getForm() {
        containerForm = new Form();
        containerForm.setScrollableY(true);
        containerForm.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        HTMLComponent instComp = hostMidlet.makeHTMLComponent(instructions);
        instComp.setFocusable(false);
        containerForm.addComponent(instComp);
        
        form = new Form();
        
        FlowLayout layoutMgr = new FlowLayout(
                direction == DIR_LTR ? Component.LEFT : Component.RIGHT);
        
        if(direction == DIR_RTL) {
            form.setRTL(true);
        }
        
        if(direction != DIR_TTB) {
            form.setLayout(new FlowLayout());
        }else {
            form.setLayout(new GridLayout(items.length+1, 1));
        }
        for (int i = 0; i < items.length; i++) {
            form.addComponent(items[i]);
        }
        
        if(items.length > 0) {
            form.setFocused(items[0]);
        }
        
        containerForm.addKeyListener(-3, this);
        containerForm.addKeyListener(-4, this);
        containerForm.addKeyListener(-5, this);
        containerForm.addKeyListener(-1, this);//UP
        containerForm.addKeyListener(-2, this);//Down
        
        containerForm.addComponent(form);
        
        containerForm.addComponent(checkButton);
        checkButton.setCommand(new Command(checkText));
        checkButton.setNextFocusUp(items[items.length-1]);
        checkButton.addActionListener(this);
        return containerForm;
    }

    /**
     * Start the idevice
     */
    public void start() {
        super.start();
        items[0].requestFocus();
    }

    /**
     * Stop the idevice
     */
    public void stop() {
        super.stop();
        EXEStrMgr.lg(this, //idevice
                    0, //question id
                    getTimeOnDevice(), //time on device in ms
                    correctAnswerGiven ? 1  : 0, //num correctly answered
                    correctFirst, //num answered correct first attempt
                    numAttempts, //num questions attempted
                    Idevice.LOGDEVCOMPLETE, //verb
                    0, //score
                    0, //maxScorePossible
                    givenOrder.toString(),//answer given 
                    Idevice.BLANK);//remarks
    }

    /**
     * dispose of all items
     */
    public void dispose() {
        form = null;
    }
    
    
    
}
/**
 * Utility container class to represent a sortable item in the list
 * 
 * When a SortableItem is 'active' this means when the user presses the direction
 * keys it should move.  
 * 
 * @author mike
 */
class SortableItem extends Container implements FocusListener{
    
    /** our host idevice*/
    private SortIdevice idevice;
    
    /** where we belong */
    int correctPlace = 0;
    
    /**
     * Constructor 
     * 
     * @param idevice our host idevice
     */
    SortableItem(SortIdevice idevice, int correctPlace) {
        this.idevice = idevice;
        addFocusListener(this);
        this.correctPlace = correctPlace;
    }
    
    /**
     * Comparator method to be used to check for correct ordering by the user
     * 
     * @param other
     * @return true if this is the same object, false otherwise
     */
    public boolean equals(Object other) {
        if(other == this) {
            return true;
        }
        
        return false;
    }

    /**
     * When we get focus - tell the parent idevice about this
     * 
     * @param cmpnt 
     */
    public void focusGained(Component cmpnt) {
        int x = 0;
        idevice.focusedItem = this;
    }

    public void focusLost(Component cmpnt) {
        int x = 0;
    }
    
    
    /**item is held down / ready to move*/
    boolean active = false;
    
    /** action listeners - used for going active/inactive*/
    Vector actionListeners;
    
    /**
     * Add ActionListener for when this item goes active/inactive
     * @param l ActionListener
     */
    public void addActionListener(ActionListener l) {
        if(actionListeners == null) {
            actionListeners = new Vector();
        }
        actionListeners.addElement(l);
    }
    
    /**
     * Remove an ActionListener for when this item goes active/inactive
     * 
     * @param l ActionListener
     */
    public void removeActionListener(ActionListener l) {
        actionListeners.removeElement(l);
    }
    
    /**
     * Fire event for keycode
     * @param k keycode
     */
    void fireActionEvent(int k) {
        ActionEvent evt = new ActionEvent(this, k);
        MLearnUtils.fireActionEventToAll(actionListeners, evt);
    }
    
    /**
     * Update the style to set the border color and width for this item according
     * to whether it is currently active or inactive
     */
    public void updateStyle() {
        for(int j = 0; j < SortIdevice.allDirs.length; j++) {
            getStyle().setPadding(SortIdevice.allDirs[j], idevice.padding, true);
        }
        
        int color = (active == true) ? idevice.activeColor : 0x000000;

        getStyle().setBorder(
                Border.createLineBorder(idevice.borderWidthNormal, color));

        getSelectedStyle().setBorder(
                Border.createLineBorder(idevice.borderWidthHilight, color));

    }
    
    /**
     * Set whether this so it can be moved around (or not) 
     * @param active true if it should be 
     */
    public void setActive(boolean active) {
        if(active) {
            getSelectedStyle().setBorder(Border.createLineBorder(
                    SortIdevice.borderWidthHilight, SortIdevice.activeColor));
        }else {
            getSelectedStyle().setBorder(Border.createLineBorder(
                    SortIdevice.borderWidthHilight, SortIdevice.normalColor));
        }
        
        this.active = active;
        repaint();
    }
    
    
}
