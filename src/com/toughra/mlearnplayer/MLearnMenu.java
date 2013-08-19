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
package com.toughra.mlearnplayer;

import com.toughra.mlearnplayer.datatx.MLClientManager;
import com.toughra.mlearnplayer.datatx.MLServerThread;
import com.toughra.mlearnplayer.datatx.MLObjectPusher;
import com.sun.lwuit.*;
import com.sun.lwuit.events.*;
import com.sun.lwuit.layouts.BoxLayout;
import com.toughra.mlearnplayer.datatx.MLCloudConnector;
import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

/**
 * This class is used to run the application menu where the user can choose
 * to return to the table of contents, change settings etc.
 * 
 * @author mike
 */
public class MLearnMenu extends Form implements ActionListener, DataChangedListener{

    /** The host midlet*/
    private MLearnPlayerMidlet host;
    
    /** The keys that are used in the menu (looked up in the localization res) */
    String[] labels = {"continue", "repeat", "back", "contents",  "collection", "opencourse", "settings", "about", "quit", "logout"};
    
    /** Continue command ID */
    private static final int CONTINUE = 0;
    
    /** Repeat command ID */
    private static final int REPEAT = 1;
    
    /** Back command ID */
    private static final int BACK = 2;
    
    /** Go to contents command ID */
    private static final int CONTENTS = 3;
    
    /** Go to collection contents ID */
    private static final int COLLECTION = 4;
    
    /** Go to open course command ID */
    private static final int OPENCOURSE = 5;
    
    /** Go to settings command ID*/
    private static final int SETTINGS = 6;
    
    /** Go to the about form command ID*/
    private static final int ABOUTFORM = 7;
    
    /** Quit command ID*/
    private static final int QUIT = 8;
    
    /** Logout command ID*/
    private static final int LOGOUT = 9;
    
    private static final int SEARCHBT = 43;
    
    private static final int LOGOUTYES = 102;
    
    private static final int LOGOUTNO = 100;
    
    /** The main form object for the settings dialog*/
    Form settingsForm;
    
    /**The slider object used to control the playback volume */
    Slider volSlider;
    
    /** Command object for settings OK */
    Command setOKCmd;
    
    //end of settings form stuff
    
    /** Array of buttons for the main menu*/
    Button[] buttons;
    
    /** Array of commands that will be formed for those buttons*/
    Command[] cmds;
    
    /** Layout used for the main form*/
    BoxLayout bLayout;
    
    /** Name of the learner (for teacher record transmission etc)*/
    TextField nameField;
    
    //The combo box for setting the language
    ComboBox langCombo;
    
    /** The language codes that are available to select from*/
    String[] langCodeStr;
    
    /**Learner ID field*/
    TextField lIdField;
    
    /**Checkbox for turning on/off the bluetooth log receive server*/
    CheckBox svrOnCheckBox;
    
    /**command for doing the bluetooth search*/
    Command doSearchCmd;
    
    /** Button to do the bluetooth search */
    Button searchButton;
    
    /** Form that contains bluetooth servers that have been found*/
    Form btDevicesForm;
    
    /**Client Manager object that handles doing the bluetooth search? */
    MLClientManager clientMgr;
    
    /** BlueToothAction Listener*/
    BTActionListener btActionListener;
    
    /** Vector used to save discovered Bluetooth servers*/
    Vector btClientURLs;
    
    /**Action Listener used to wait whilst the discovery is running*/
    BTSelectActionListener btSelectActionListener;
    
    /** Command to send all logs (puts the threads countdown to 0)*/
    Command sendTestCmd;
    
    /** Button for sending logs right now*/
    Button sendTestBtn;
    
    /** Command ID for the send run button*/
    private static final int SENDTEST = 45;
    
    /** Dialog used to indicate a loading status (e.g. waiting for bluetooth disc)*/
    Dialog loadingDialog;
    
    //end bluetooth stuff
    
    
    //===http related stuff===
   
    
    /** Form that contains the about dialog*/
    Form aboutForm;
    
    /** Yes/No confirmation form */
    Form logoutForm;
    
    
    /**
     * Constructor
     * 
     * @param host host midlet
     */
    public MLearnMenu(MLearnPlayerMidlet host) {
        this.host = host;
        buttons = new Button[labels.length];
        cmds = new Command[labels.length];
        
        bLayout = new BoxLayout(BoxLayout.Y_AXIS) ;
        setLayout(bLayout);
        /*
         * Go through all the buttons we have in the main menu.  Expect an icon
         * in the resources /icon/menu-index.png
         */
        for(int i = 0; i < labels.length; i++) {
            InputStream imgIn = getClass().getResourceAsStream("/icons/menu-" + i + ".png");
            try {
                String cmdL10n = MLearnUtils._(labels[i]);
                if(cmdL10n == null) {
                    cmdL10n = labels[i];
                }
                cmds[i] = new Command(cmdL10n, 
                        Image.createImage(imgIn), i);
                imgIn.close();
                
                buttons[i] = new Button(cmds[i]);
                buttons[i].setTextPosition(RIGHT);
                buttons[i].addActionListener(this);
            }catch(IOException e) {
                e.printStackTrace();
                buttons[i] = new Button(labels[i]);
            }
            addComponent(buttons[i]);
            
            
        }
        
        //the meaning of life, the universe and everything
        setOKCmd = new Command("OK", 42);

        //The about form
        aboutForm = new Form("About");
        Label versionLabel = new Label(MLearnPlayerMidlet.versionInfo);
        TextArea aboutText = new TextArea("Ustad Mobile.  Copyright 2012-2013 Toughra Technologies FZ LLC.\n"
                    + "Written by Mike Dawson \n\n"
                    + "This program is free software: you can redistribute it and/or modify " +
"    it under the terms of the GNU General Public License as published by " +
"    the Free Software Foundation, either version 3 of the License, or " +
"    (at your option) any later version..\nThis program is distributed in the hope that it will be useful, " +
"    but WITHOUT ANY WARRANTY; without even the implied warranty of " +
"    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the " +
"    GNU General Public License for more details.  " +
"\n" +
"    You should have received a copy of the GNU General Public License" +
"    along with this program.  If not, see <http://www.gnu.org/licenses/>.");                
        aboutText.setEditable(false);
        aboutForm.addComponent(versionLabel);
        aboutForm.addComponent(aboutText);
        Button aboutGoBack = new Button(setOKCmd);
        aboutGoBack.addActionListener(this);
        aboutForm.addComponent(aboutGoBack);
        
        
        //The settings where you can set volume etc.
        settingsForm = new Form("Settings");
        BoxLayout bLayout = new BoxLayout(BoxLayout.Y_AXIS);
        settingsForm.setLayout(bLayout);
        
        volSlider = new Slider();
        volSlider.setText("Volume");
        volSlider.setMaxValue(100);
        volSlider.setMinValue(0);
        volSlider.setProgress(host.volume);
        volSlider.setIncrements(10);
        volSlider.setTextPosition(TOP);
        volSlider.setEditable(true);
        volSlider.addDataChangedListener(this);
        settingsForm.addComponent(volSlider);
        settingsForm.setFocused(volSlider);
        
        
        
        String nameSet = EXEStrMgr.getInstance().getPref("learnername");
        if(nameSet == null) {
            nameSet = "";
        }
        settingsForm.addComponent(new Label(MLearnUtils._("yourname:")));
        nameField = new TextField(nameSet);
        settingsForm.addComponent(nameField);
        
        
        //The combo box for changing language
        Vector langs = EXEStrMgr.getInstance().getLocaleList(host.localeRes, 
                EXEStrMgr.localeResName);
        int numLangs = langs.size();
        langCodeStr = new String[numLangs];
        langs.copyInto(langCodeStr);
        langs = null;
        
        String[] langNameStr = new String[numLangs];
        int langSelIndex = 0;
        String localeNow = EXEStrMgr.getInstance().getLocale();
        for(int i = 0; i < numLangs; i++) {
            langNameStr[i] = MLearnUtils._(langCodeStr[i]);
            if(localeNow.equals(langNameStr[i])) {
                langSelIndex = i;
            }
        }
        
        langCombo = new ComboBox(langNameStr);
        langCombo.setSelectedIndex(langSelIndex, true);
        Label langLabel = new Label(MLearnUtils._("Language"));
        settingsForm.addComponent(langLabel);
        settingsForm.addComponent(langCombo);
        
        //the label for selecting the bluetooth server to talk with (teacher phone)
        Label tchrLabel = new Label(MLearnUtils._("Teachers Phone"));
        //settingsForm.addComponent(tchrLabel);
        
        String btServerName = EXEStrMgr.getInstance().getPref("server.bt.name");
        String btBtnStr = "[Find]";
        if(btServerName != null) {
            btBtnStr = btServerName;
        }
        
        doSearchCmd = new Command(btBtnStr, SEARCHBT);
        searchButton = new Button(doSearchCmd);
        searchButton.addActionListener(this);
        //bluetooth is not supported for the moment
        //settingsForm.addComponent(searchButton);
        
        sendTestCmd = new Command("Send Data Now", SENDTEST);
        sendTestBtn = new Button(sendTestCmd);
        sendTestBtn.addActionListener(this);
        settingsForm.addComponent(sendTestBtn);
        
        //checkbox for switching server (e.g. teacher) mode on/off
        svrOnCheckBox = new CheckBox(MLearnUtils._("serveron"));
        String svrEnabled = EXEStrMgr.getInstance().getPref("server.enabled");
        if(svrEnabled != null && svrEnabled.equals("true")) {
            svrOnCheckBox.setSelected(true);
        }
        //bluetooth is not supported for the moment
        //settingsForm.addComponent(svrOnCheckBox);
        
        ///settings OK button
        Button settingOKButton = new Button(setOKCmd);
        settingOKButton.addActionListener(this);
        settingsForm.addComponent(settingOKButton);
        
                
        logoutForm = new Form(MLearnUtils._("Are you sure?"));
        logoutForm.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        
        String[] cmdLabels = {"No", "Yes"};
        int[] cmdIds = {LOGOUTNO, LOGOUTYES};
        for(int i = 0; i < cmdLabels.length; i++) {
            Command cmd = new Command(MLearnUtils._(cmdLabels[i]), cmdIds[i]);
            Button btn = new Button(cmd);
            btn.addActionListener(this);
            logoutForm.addComponent(btn);
        }
    }
    
    public void updateFieldsFromPrefs() {
        nameField.setText(EXEStrMgr.getInstance().getPref(EXEStrMgr.KEY_LEARNERNAME));
        int langSelIndex = 0;
        String localeNow = EXEStrMgr.getInstance().getLocale();
        
        for(int i = 0; i < langCodeStr.length; i++) {
            if(localeNow.equals(langCodeStr[i])) {
                langSelIndex = i;
            }
        }
        
        langCombo.setSelectedIndex(langSelIndex);
    }
    
    /**
     * Continue back to the main menu of the form.  Check for certain settings
     * changes and see if we need to run the bluetooth server / log transmit etc
     */
    protected void contMain() {
        if(host.curFrm != null) {
            host.curFrm.show();   
        }
        EXEStrMgr.getInstance().setPref("learnername", nameField.getText());
        EXEStrMgr.getInstance().setPref("server.enabled", 
                String.valueOf(svrOnCheckBox.isSelected()));
        
        String localeNow = EXEStrMgr.getInstance().getLocale();
        String newLocale = langCodeStr[langCombo.getSelectedIndex()];
        if(!newLocale.equals(localeNow)) {
            //you must restart
            boolean changed = Dialog.show(MLearnUtils._("Change Language"), 
                    MLearnUtils._("Are you sure?  Restart needed if yes"), 
                    MLearnUtils._("OK"), MLearnUtils._("Cancel"));
            if(changed) {
                EXEStrMgr.getInstance().setPref("locale", newLocale);
            }
        }
        MLServerThread.getInstance().checkServer();
    }
    
    
    
    /**
     * Main action event handler - see where the users wants to go
     * 
     * @param ae 
     */
    public void actionPerformed(ActionEvent ae) {
        Command cmd = ae.getCommand();
        if(cmd != null) {
            if(cmd.getId() == CONTINUE) {
                contMain();
            }else if(cmd.getId() ==  REPEAT) {
                host.showNextDevice(0);
            }else if(cmd.getId() == BACK) {
                host.showNextDevice(-1);
            }else if(cmd.getId() == CONTENTS) {
                host.showTOC();
            }else if(cmd.getId() == COLLECTION) {
                if(host.myTOC.collection != null) {
                    host.openCollectionDir(host.myTOC.colBaseHREF + "/");
                }
            }else if(cmd.getId() == QUIT) {
                host.destroyApp(true);
                host.notifyDestroyed();
            }else if(cmd.equals(setOKCmd)) {
                contMain();
            }else if(cmd.getId() == SEARCHBT) {
                if(btActionListener == null) {
                    EXEStrMgr.po("Setting up search...", EXEStrMgr.DEBUG);
                    //loadingDialog = MLearnPlayerMidlet.getInstance().makeLoadingDialog();
                    EXEStrMgr.po("calling search...", EXEStrMgr.DEBUG);
                    
                    btActionListener = new BTActionListener(this);
                    MLClientManager.getInstance().addActionListener(btActionListener);
                    MLClientManager.getInstance().doSearch();
                    
                    //loadingDialog.showPacked(BorderLayout.CENTER, true);
                }
            }else if(cmd.getId() == SETTINGS) {
                settingsForm.show();
            }else if(cmd.getId() == OPENCOURSE) {
                host.contentBrowser.show();
            }else if(cmd.getId() ==SENDTEST) {
                MLObjectPusher.countDown = 0;//set this to zero to force the thread to run next tick
            }else if(cmd.getId() == ABOUTFORM) {
                aboutForm.show();
            }else if(cmd.getId() == LOGOUT) {
                logoutForm.show();
            }else if(cmd.getId() == LOGOUTNO) {
                show();
            }else if(cmd.getId() == LOGOUTYES) {
                MLearnPlayerMidlet.getInstance().showLoginForm(true);
            }
        }
    }
    
    /**
     * This is called after going through the bluetooth discovery process
     * to show available servers to the user.
     */
    public void showAvailableBT() {
        //this is coming through the BT thread
        btDevicesForm = new Form("Available Devices");
        btDevicesForm.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        
        Hashtable candidates = MLClientManager.getInstance().getCandidates();
        btSelectActionListener = new BTSelectActionListener(this);
        
        btClientURLs = new Vector();
        Enumeration devNames = candidates.keys();
        int btCount = 0;
        while(devNames.hasMoreElements()) {
            String dName = devNames.nextElement().toString();
            String url = candidates.get(dName).toString();
            btClientURLs.addElement(url);
            Command devCmd = new Command(dName, btCount);
            
            Button devBtn = new Button(devCmd);
            devBtn.addActionListener(btSelectActionListener);
            
            btDevicesForm.addComponent(devBtn);
            btCount++;
        }
        
        if(btCount == 0) {
            Label noDevLabel = new Label(MLearnUtils._("nobtdevices"));
            btDevicesForm.addComponent(noDevLabel);
        }
        
        Command cancelCmd = new Command("Cancel", 1000);
        Button cancelButton = new Button(cancelCmd);
        btDevicesForm.addComponent(cancelButton);
        cancelButton.addActionListener(btSelectActionListener);
        
        btDevicesForm.show();
    }

    /**
     * Event handler for the volume slider
     * 
     * @param id
     * @param level 
     */
    public void dataChanged(int id, int level) {
       host.setVolume(level);
    }
    
    
    
}


/**
 * Utility ActionListener to Listen for when a user has selected one of the
 * available bluetooth servers and save this into the preferences.
 * 
 * @author mike
 */
class BTSelectActionListener implements ActionListener {

    MLearnMenu host;
    
    public BTSelectActionListener(MLearnMenu host) {
        this.host = host;
    }

    
    /**
     * Lookup which server has been selected and save to preferences.
     * 
     * @param ae 
     */
    public void actionPerformed(ActionEvent ae) {
        Command cmd = ae.getCommand();
        int id = cmd.getId();
        
        //check that the user has not selected the cancel (e.g. no server) option
        if(id != 1000) {
            String URL = host.btClientURLs.elementAt(id).toString();
            String friendlyName = cmd.getCommandName();
            EXEStrMgr.getInstance().setPref("server.bt.url", URL);
            EXEStrMgr.getInstance().setPref("server.bt.name", friendlyName);
            EXEStrMgr.getInstance().savePrefs();
            host.doSearchCmd.setCommandName(friendlyName);
            host.searchButton.setText(friendlyName);
        }
        host.settingsForm.show();
    }
    
}

/**
 * Bluetooth Action Listener will remove itself from the action listener list
 * to avoid repeat calls and then call the menu to show the list of available
 * servers
 * 
 * @author mike
 */
class BTActionListener implements ActionListener, Runnable{
    
    MLearnMenu host;
    
    BTActionListener(MLearnMenu host) {
        this.host = host;
    }

    public void actionPerformed(ActionEvent ae) {
        Display.getInstance().callSerially(this);
    }
    
    public void run() {
        MLClientManager.getInstance().removeActionListener(host.btSelectActionListener);
        host.btActionListener = null;
        
        
        if(MLClientManager.getInstance().getLastStatus() == MLClientManager.DONE_OK) {
            host.showAvailableBT();
        }else {
            Label errLabel = new Label("Bluetooth Error");
            Command[] cmds = new Command[] { new Command("OK") };
            Dialog.show("Error", errLabel, cmds, Dialog.TYPE_ERROR, null);
        }
    }
    
    
}
