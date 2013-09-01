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

import javax.bluetooth.*;
import java.util.Vector;
import com.toughra.mlearnplayer.EXEStrMgr;

/**
 * Handles the discovery of services to find out which devices are in fact
 * Ustad Mobile Servers
 * 
 * @author mike
 */
public class MLServiceDiscoverer implements DiscoveryListener {
    
    /**
     * List of remote devices that have been found to search on for Ustad Mobile 
     * service
     */
    Vector remoteDevices;
    
    /**
     * the MLClientManager that we report back to
     */
    MLClientManager manager;
    
    
    String connectionURL;
    
    /** The friendly name of the device currently being examined */
    String currentFriendlyName;
    
    /** UUID of our Server (almost like a port number) - 8851 */
    UUID[] uuidSet = {new UUID(MLServerThread.UUIDSTR, true)};
    
    /**
     * Attributes used in the service discovery process - what things we are going to 
     * look for after (e.g. 0x0100 = service name) 
     */
    int[] attrSet = {0x0100, 0x0003, 0x0004};
    
    /** if this is the last device to search */
    boolean lastSearch = false;
    
    /**
     * Constructor method
     * 
     * @param remoteDevices Vector of RemoteDevice objects to examine for our services
     * @param manager MLClientManager we report back to
     */
    public MLServiceDiscoverer(Vector remoteDevices, MLClientManager manager) {
        this.remoteDevices = remoteDevices;
        this.manager = manager;
    }
    
    /**
     * The meat of the class - go through all remoteDevices and look for the 
     * Ustad Mobil server
     * 
     */
    public void findServices() {
        try {
            LocalDevice localDev = LocalDevice.getLocalDevice();
            DiscoveryAgent discAgent = localDev.getDiscoveryAgent();
            int numDevices = remoteDevices.size();
            for(int i = 0; i < numDevices; i++) {
                if(i == numDevices-1) {
                    lastSearch = true;//flag this so the search completed will notify
                }
                try {
                    RemoteDevice remoteDev = (RemoteDevice)remoteDevices.elementAt(i);

                    currentFriendlyName = remoteDev.getFriendlyName(true);
                    EXEStrMgr.lg(40, "Searching services on " + remoteDev.getBluetoothAddress() 
                            + " / "  + currentFriendlyName);



                    discAgent.searchServices(attrSet, uuidSet, remoteDev, this);
                    EXEStrMgr.lg(40, "Done searching services on " + remoteDev.getBluetoothAddress() 
                            + " / "  +currentFriendlyName);
                }catch(Exception e) {
                    EXEStrMgr.lg(132, "Error with service discovery run " + i, e);
                    if(lastSearch) {
                        //we have had an error on our last run - manually call done
                        EXEStrMgr.lg(132, "Error with last service run");
                        searchDone();
                    }
                }
            }
            //sleep so that we dont have strange errors - 2seconds per device should be OK...
            try { Thread.sleep(2000); }
            catch(InterruptedException e2) {}
        }catch(Exception e) {
            EXEStrMgr.lg(133, "Exception with service discovery part 1 ",e);
        }
    }
    
    
    /**
     * when lastSearch = true this gets called when the service discovery is done
     * so that MLClientManager will fire the actionEvent and the user can see a list
     * of servers that can be used.
     * 
     */
    private final void searchDone() {
        manager.lastStatus = MLClientManager.DONE_OK;
        manager.fireActionEvent();
    }

    /**
     * Not used because this is handling service discovery not device discovery
     * @param btDevice
     * @param cod 
     */
    public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
        //not being used here
    }

    /**
     * Not used because this is handling service discovery not device discovery
     * 
     * 
     * @param discType 
     */
    public void inquiryCompleted(int discType) {
        //not being used here
    }

    /**
     * DiscoveryListener standard method - this is called when the service search
     * for a given device is completed.  If lastSearch = true then call searchDone
     * and let MLClientManager fire the event.
     * 
     * @param transID
     * @param respCode 
     */
    public void serviceSearchCompleted(int transID, int respCode) {
        EXEStrMgr.lg(40, "service search completed");
            
        if(lastSearch == true) {
            searchDone();
        }
    }

    /**
     * Standard DiscoveryListener method that gets called when a set of services
     * is discovered on a device.
     * 
     * Look at each service and see if the service name matches what we are looking
     * for as an Ustad Mobil server.
     * 
     * @param transID transaction id (unused)
     * @param servRecord ServiceRecord array of services found
     */
    public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
        for(int i = 0; i < servRecord.length; i++) {
            DataElement serviceElement = servRecord[i].getAttributeValue(0x0100);
            String serviceName = (String)serviceElement.getValue();
            EXEStrMgr.lg(40, "Found Service on dev name " + serviceName);
            
            //see if this is really our service
            if(serviceName.equals(MLServerThread.SERVNAME)) {
                EXEStrMgr.lg(40, "Found our service on " + i);
                try {
                    String conURL = servRecord[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
                    manager.candidates.put(currentFriendlyName, conURL);
                    EXEStrMgr.lg(40, "Put Connection URL " + conURL);
                }catch(Exception e) {
                    EXEStrMgr.lg(134, "Error getting connection URL ", e);
                }
            }
        }
    }
    
    
    
}
