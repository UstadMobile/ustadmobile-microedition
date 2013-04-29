/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 * 
 * NOTE: For some reason this is not setting the selected device properly
 * 
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
import com.sun.lwuit.plaf.Border;
import java.util.Vector;
import com.toughra.mlearnplayer.xml.XmlNode;


/**
 *
 * @author mike
 */
public class SortIdevice extends Idevice implements ActionListener {

    //the items that the user will sort
    SortableItem[] items; 
    
    //The correct order
    SortableItem[] correctOrder;
    
    public static final int DIR_LTR = 1;
    
    public static final int DIR_RTL = 2;
    
    int direction = DIR_LTR;
    
    static int borderWidthHilight = 3;
    static int borderWidthNormal = 1;
    static int padding = 5;
    
    static int normalColor = 0x000000;
    static int activeColor = 0xff0000;
    
    //TODO: there must be a better way
    static final int[] allDirs = {Component.TOP, Component.LEFT, Component.BOTTOM, Component.RIGHT};
    
    //the item currently active or -1 if none
    int currentActiveIndex = -1;
    
    //the form where components live
    Form form;
    
    Form containerForm;
    
    //increment map - key code (absolute) to how much to move the idevice
    int[] incMap;
    
    String instructions;
    
    String positiveFeedback;
    
    String negativeFeedback;
    
    SortableItem focusedItem;
    
    String checkText;
    
    Button checkButton;
    
    int numAttempts = 0;
    
    public SortIdevice(MLearnPlayerMidlet hostMidlet, XmlNode srcData) {
        super(hostMidlet);
        Vector sortItems = srcData.findChildrenByTagName("item", true);
        
        XmlNode optionNode = (XmlNode)srcData.findChildrenByTagName("sortoptions", true).elementAt(0);
        incMap = new int[8];
        if(optionNode.hasAttribute("dir")) {
            if(optionNode.getAttribute("dir").equalsIgnoreCase("rtl")) {
                direction = DIR_RTL;
            }
        }
        
        checkText = optionNode.getAttribute("checkbuttontext");
        checkButton = new Button(checkText);
        
        
        if(direction == DIR_LTR) {
            incMap[3] = -1;
            incMap[4] = 1;
        }else {
            incMap[3] = 1;
            incMap[4] = -1;
        }
        
        
        int numItems = sortItems.size();
        items = new SortableItem[numItems];
        
        
        
        
        
        for(int i = 0; i < numItems; i++) {
            XmlNode currentNode = (XmlNode)sortItems.elementAt(i);
            items[i] = new SortableItem(this);
            
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
    
    boolean checkAnswer() {
        int numItems = items.length;
        for(int i = 0; i < numItems; i++) {
            if(!items[i].equals(correctOrder[i])) {
                return false;
            }
        }
        
        return true;
    }

    public void actionPerformed(ActionEvent ae) {
        if(ae.getCommand() != null && ae.getCommand().getCommandName() != null) {
            if(ae.getCommand().getCommandName().equals("checkit")) {
                FeedbackDialog fbDialog = new FeedbackDialog(hostMidlet);
                boolean isCorrect = checkAnswer();
                if(numAttempts == 0) {
                    correctFirst = 1;
                }
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
        if(src != null && !checkButton.hasFocus()) {
        
            int key = ae.getKeyEvent();
            
            boolean needUpdate = false;
            if(!src.active && key == -5) {
                src.setActive(true);
                currentActiveIndex = MLearnUtils.indexInArray(items, src);
                src.requestFocus();
                needUpdate = true;
                //containerForm.setFocused(form);
                //form.setFocused(src);
            }else if(src.active && key == -5) {
                src.setActive(false);
                currentActiveIndex = -1;
                src.requestFocus();
                needUpdate = true;
                //containerForm.setFocused(form);
                //form.setFocused(src);
            }else if(currentActiveIndex != -1 && src.active && (key == -3 || key == -4)) {
                moveActiveItem(incMap[Math.abs(key)]);
                remakeForm();
                needUpdate = true;
            }
            
            if(needUpdate) {
                //form.revalidate();
                //form.repaint();
                containerForm.revalidate();
                containerForm.repaint();
            }
        }else {
            System.out.println("other key event: " + ae.getKeyEvent());
        }
    }
    
    private void updateNextFocus() {
        //set the correct focus movements
        int numItems = items.length;
        
        checkButton.setNextFocusUp(items[items.length - 1]);
        for(int i = 0; i < numItems; i++) {
            items[i].setNextFocusDown(checkButton);
            
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
            }else {
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
     * Allow the user to reorder things
     * 
     * @param increment 
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

    
    
    public int getMode() {
        return Idevice.MODE_LWUIT_FORM;
    }

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
        
        form.setLayout(new FlowLayout());
        for (int i = 0; i < items.length; i++) {
            form.addComponent(items[i]);
        }
        
        if(items.length > 0) {
            form.setFocused(items[0]);
        }
        
        containerForm.addKeyListener(-3, this);
        containerForm.addKeyListener(-4, this);
        containerForm.addKeyListener(-5, this);
        
        containerForm.addComponent(form);
        
        containerForm.addComponent(checkButton);
        checkButton.setCommand(new Command("checkit"));
        checkButton.setNextFocusUp(items[items.length-1]);
        checkButton.addActionListener(this);
        return containerForm;
    }

    public void start() {
        super.start();
        items[0].requestFocus();
    }

    public void stop() {
        super.stop();
    }

    public void dispose() {
        form = null;
    }
    
    
    
}
class SortableItem extends Container implements FocusListener{
    
    private SortIdevice idevice;
    
    SortableItem(SortIdevice idevice) {
        this.idevice = idevice;
        addFocusListener(this);
    }
    
    public boolean equals(Object other) {
        if(other == this) {
            return true;
        }
        
        return false;
    }

    public void focusGained(Component cmpnt) {
        int x = 0;
        idevice.focusedItem = this;
    }

    public void focusLost(Component cmpnt) {
        int x = 0;
    }
    
    
    //item is held down / ready to move
    boolean active = false;
    
    Vector actionListeners;
    
    public void addActionListener(ActionListener l) {
        if(actionListeners == null) {
            actionListeners = new Vector();
        }
        actionListeners.addElement(l);
    }
    
    public void removeActionListener(ActionListener l) {
        actionListeners.removeElement(l);
    }
    
    void fireActionEvent(int k) {
        ActionEvent evt = new ActionEvent(this, k);
        MLearnUtils.fireActionEventToAll(actionListeners, evt);
    }
    
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
