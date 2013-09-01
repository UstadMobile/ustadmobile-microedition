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

package com.toughra.mlearnplayer.datatx;

import java.io.IOException;
import java.util.Vector;
import javax.bluetooth.*;
import com.toughra.mlearnplayer.EXEStrMgr;


/**
 * This is used by the client (e.g. student) phone to find servers.  It runs
 * the first part of the bluetooth discovery process - the device discovery
 * 
 * It will then pass over to MLServiceDiscoverer to look for the devices which
 * are actually ustad mobile servers
 * 
 * @author mike
 */
public class MLDeviceDiscovery implements DiscoveryListener, Runnable{

    /** The MLClientManager that we are reporting back to*/
    MLClientManager mgr;
    
    /** Bluetooth Object*/
    DiscoveryAgent discoveryAgent;
    
    /** Vector of RemoteDevices found*/
    Vector remoteDevices;
    
    /**
     * Constructor
     * 
     * @param mgr our MLClientManager to report back to
     */
    public MLDeviceDiscovery(MLClientManager mgr) {
        this.mgr = mgr;
    }
    
    /**
     * Runs the discovery process using bluetooth DiscoveryAgent.
     * Uses the startInquiry method
     * 
     */
    public void run() {
        try {
            LocalDevice localDevice = LocalDevice.getLocalDevice();
            discoveryAgent = localDevice.getDiscoveryAgent();
            EXEStrMgr.lg(16, "Got discovery agent");
            remoteDevices = new Vector();
            discoveryAgent.startInquiry(DiscoveryAgent.GIAC, this);
        }catch(Exception e) {
            EXEStrMgr.lg(130, "Error with discovery agent ", e);
        }
    }

    /**
     * Standard DiscoveryListener method - will add the device that we found
     * to the remoteDevices vector
     * 
     * @param btDevice device found
     * @param cod DeviceClass (not used)
     */
    public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
        try {
            String msg = "Found: " + btDevice.getBluetoothAddress() + " : "
                + btDevice.getFriendlyName(true);
            EXEStrMgr.lg(27, msg);
        }catch(IOException e) {
            EXEStrMgr.lg(131, "Error getting friendly name", e);
        }
        
        remoteDevices.addElement(btDevice);
    }

    /**
     * Standard DiscoveryListener method.  If the search is done OK - e.g.
     * discType = DiscoveryListener.INQUIRY_COMPLETED then starts the 
     * MLServiceDiscoverer to figure out which devices are Ustad Mobil servers
     * 
     * @param discType DiscoveryListener discovery type
     */
    public void inquiryCompleted(int discType) {
        String stat = null;
        if(discType == DiscoveryListener.INQUIRY_COMPLETED) {
            stat = "Inquiry completed";
        }else if(discType == DiscoveryListener.INQUIRY_TERMINATED) {
            stat = "Inquiry terminated";
        }else if(discType == DiscoveryListener.INQUIRY_ERROR) {
            stat = "Inquiry Error";
        }
        
        EXEStrMgr.lg(28, "Discovery: " + stat);
        
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
