/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.paiwastoon.mlearnplayer.datatx;

import java.io.IOException;
import java.util.Vector;
import javax.bluetooth.*;
import net.paiwastoon.mlearnplayer.EXEStrMgr;


/**
 *
 * @author mike
 */
public class MLDeviceDiscovery implements DiscoveryListener, Runnable{

    MLClientManager mgr;
    
    DiscoveryAgent discoveryAgent;
    
    Vector remoteDevices;
    
    public MLDeviceDiscovery(MLClientManager mgr) {
        this.mgr = mgr;
    }
    
    public void run() {
        try {
            LocalDevice localDevice = LocalDevice.getLocalDevice();
            discoveryAgent = localDevice.getDiscoveryAgent();
            EXEStrMgr.po("Got discovery agent", EXEStrMgr.DEBUG);
            remoteDevices = new Vector();
            discoveryAgent.startInquiry(DiscoveryAgent.GIAC, this);
        }catch(Exception e) {
            EXEStrMgr.po("Error with discovery agent " + e.toString(), EXEStrMgr.WARN);
        }
    }

    public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
        try {
            String msg = "Found: " + btDevice.getBluetoothAddress() + " : "
                + btDevice.getFriendlyName(true);
            EXEStrMgr.po(msg, EXEStrMgr.DEBUG);
        }catch(IOException e) {
            EXEStrMgr.po("Error getting friendly naem" + e.toString(), EXEStrMgr.WARN);
            e.printStackTrace();
        }
        
        remoteDevices.addElement(btDevice);
    }

    public void inquiryCompleted(int discType) {
        String stat = null;
        if(discType == DiscoveryListener.INQUIRY_COMPLETED) {
            stat = "Inquiry completed";
        }else if(discType == DiscoveryListener.INQUIRY_TERMINATED) {
            stat = "Inquiry terminated";
        }else if(discType == DiscoveryListener.INQUIRY_ERROR) {
            stat = "Inquiry Error";
        }
        
        EXEStrMgr.po("Discovery: " + stat, EXEStrMgr.DEBUG);
        
        if(discType == DiscoveryListener.INQUIRY_COMPLETED) {
            MLServiceDiscoverer servDisc = new MLServiceDiscoverer(remoteDevices, mgr);
            servDisc.findServices();
        }else {
            mgr.lastStatus = MLClientManager.ERROR;
            mgr.fireActionEvent();
        }
        
    }

    public void serviceSearchCompleted(int transID, int respCode) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }
    
    
    
}
