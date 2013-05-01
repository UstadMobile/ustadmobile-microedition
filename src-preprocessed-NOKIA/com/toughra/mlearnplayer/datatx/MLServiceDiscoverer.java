/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.toughra.mlearnplayer.datatx;

import javax.bluetooth.*;
import java.util.Vector;
import com.toughra.mlearnplayer.EXEStrMgr;

/**
 *
 * @author mike
 */
public class MLServiceDiscoverer implements DiscoveryListener {
    
    Vector remoteDevices;
    MLClientManager manager;
    String connectionURL;
    
    String currentFriendlyName;
    
    UUID[] uuidSet = {new UUID(MLServerThread.UUIDSTR, true)};
    int[] attrSet = {0x0100, 0x0003, 0x0004};
    
    boolean lastSearch = false;
    
    public MLServiceDiscoverer(Vector remoteDevices, MLClientManager manager) {
        this.remoteDevices = remoteDevices;
        this.manager = manager;
    }
    
    
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
                    EXEStrMgr.po("Searching services on " + remoteDev.getBluetoothAddress() 
                            + " / "  + currentFriendlyName, EXEStrMgr.DEBUG);



                    discAgent.searchServices(attrSet, uuidSet, remoteDev, this);
                    EXEStrMgr.po("Done searching services on " + remoteDev.getBluetoothAddress() 
                            + " / "  +currentFriendlyName, EXEStrMgr.DEBUG);
                }catch(Exception e) {
                    EXEStrMgr.po("Error with service discovery run " + i, EXEStrMgr.DEBUG);
                    if(lastSearch) {
                        //we have had an error on our last run - manually call done
                        EXEStrMgr.po("Error with last service run", EXEStrMgr.DEBUG);
                        searchDone();
                    }
                }
            }
            try { Thread.sleep(2000); }
            catch(InterruptedException e2) {}
        }catch(Exception e) {
            EXEStrMgr.po("Exception with service discovery part 1 " +
                    e.toString(), EXEStrMgr.WARN);
        }
    }
    
    private final void searchDone() {
        manager.lastStatus = MLClientManager.DONE_OK;
        manager.fireActionEvent();
    }

    public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
        //not being used here
    }

    public void inquiryCompleted(int discType) {
        //not being used here
    }

    public void serviceSearchCompleted(int transID, int respCode) {
        EXEStrMgr.po("service search completed", EXEStrMgr.DEBUG);
            
        if(lastSearch == true) {
            searchDone();
        }
    }

    public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
        //here we go...
        for(int i = 0; i < servRecord.length; i++) {
            DataElement serviceElement = servRecord[i].getAttributeValue(0x0100);
            String serviceName = (String)serviceElement.getValue();
            EXEStrMgr.po("Found Service on dev name " + serviceName, EXEStrMgr.DEBUG);
            
            if(serviceName.equals(MLServerThread.SERVNAME)) {
                EXEStrMgr.po("Found our service on " + i, EXEStrMgr.DEBUG);
                try {
                    String conURL = servRecord[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
                    manager.candidates.put(currentFriendlyName, conURL);
                    EXEStrMgr.po("Put Connection URL " + conURL, EXEStrMgr.DEBUG);
                }catch(Exception e) {
                    EXEStrMgr.po("Error getting connection URL " + e.toString(), EXEStrMgr.WARN);
                }
            }
        }
    }
    
    
    
}
