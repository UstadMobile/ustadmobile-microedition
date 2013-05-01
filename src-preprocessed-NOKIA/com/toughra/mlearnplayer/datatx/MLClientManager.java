/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.toughra.mlearnplayer.datatx;

import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import java.util.Hashtable;
import java.util.Vector;
import com.toughra.mlearnplayer.EXEStrMgr;

/**
 *
 * @author mike
 */
public class MLClientManager {

    MLDeviceDiscovery deviceDiscoverer;
    
    static MLClientManager instance;
    
    Thread discoveryThread;
    
    //This is a list in the form FriendlyName -> connection URL
    Hashtable candidates;
    
    //Those listening for events - we will fire when the search is done
    Vector actionListeners;
    
    public static final int DONE_OK = 0;
    
    public static final int ERROR = 1;
    
    public int lastStatus = -1;
    
    public static MLClientManager getInstance() {
        if(instance == null) {
            instance = new MLClientManager();
        }
        return instance;
    }
    
    public void addActionListener(ActionListener al) {
        if(actionListeners == null) {
            actionListeners = new Vector();
        }
        
        actionListeners.addElement(al);
    }
    
    public void removeActionListener(ActionListener al) {
        if(actionListeners != null) {
            actionListeners.removeElement(al);
        }
    }
    
    protected void fireActionEvent() {
        ActionEvent evt = new ActionEvent(this);
        discoveryThread = null;

        int numAl = actionListeners.size();
        for(int i = 0; i < numAl; i++) {
            ((ActionListener)actionListeners.elementAt(i)).actionPerformed(evt);
        }
        
    }
    
    public Hashtable getCandidates() {
        return candidates;
    }
    
    public int doSearch() {
        if(discoveryThread == null) {
            candidates = new Hashtable();
            if(deviceDiscoverer == null) {
                deviceDiscoverer = new MLDeviceDiscovery(this);
            }
            discoveryThread = new Thread(deviceDiscoverer);
            discoveryThread.start();
            return 1;
        }else {
            EXEStrMgr.po("Already started a discovery thread - cant do another one",
                    EXEStrMgr.WARN);
            return -1;
        }
    }
    
    public int getLastStatus() {
        return lastStatus;
    }
    
}
