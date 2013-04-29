/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.paiwastoon.mlearnplayer.idevices;

import com.sun.lwuit.*;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.geom.Dimension;
import com.sun.lwuit.html.HTMLComponent;
import com.sun.lwuit.layouts.BorderLayout;
import com.sun.lwuit.plaf.Border;
import java.util.Vector;
import net.paiwastoon.mlearnplayer.MLearnPlayerMidlet;

/**
 *
 * @author mike
 */
public class MemoryMatchCell extends Container{
    
    MemoryMatchIdevice idevice;
    
    //pair that we are part of
    MemoryMatchPair pair;
    
    public static final int STATE_COVERED = 0;
    
    public static final int STATE_SHOWING = 1;
    
    public static final int STATE_ANSWERED = 2;
    
    public int state;
    
    private Component showing;
    
    //label when covered
    Label coverLabel;
    
    //text comp
    HTMLComponent htmlCmp;
    
    private Dimension prefSize;
    
    Vector actionListeners;
    
    public static final int CMD_ID_STATECHG = 2;
    
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

    protected Dimension calcPreferredSize() {
        if(prefSize == null) {
            prefSize = new Dimension(idevice.cellWidth, idevice.cellHeight);
        }
        return prefSize;
    }

    public void keyReleased(int k) {
        System.out.println("Key " + k);
        if(k == -5) {
            fireActionEvent();
        }
        super.keyReleased(k);
    }
    
    public String getUIID() {
        return "MemoryMatchCell";
    }

    public void addActionListener(ActionListener l) {
        actionListeners.addElement(l);
    }
    
    public void removeActionListener(ActionListener l) {
        actionListeners.removeElement(l);
    }
    
    protected void fireActionEvent() {
        int num = actionListeners.size();
        Command cmd = new Command("statechg", CMD_ID_STATECHG);
        ActionEvent ae = new ActionEvent(cmd, this, 0, 0);
        
        for(int i = 0; i < num; i++) {
            ((ActionListener)actionListeners.elementAt(i)).actionPerformed(ae);
        }
    }
    
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
