/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.toughra.mlearnplayer;

import com.toughra.mlearnplayer.datatx.MLClientManager;
import com.toughra.mlearnplayer.datatx.MLReportRequest;
import com.toughra.mlearnplayer.datatx.MLServerThread;
import com.toughra.mlearnplayer.datatx.MLObjectPusher;
import com.sun.lwuit.*;
import com.sun.lwuit.events.*;
import com.sun.lwuit.layouts.BoxLayout;
import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

/**
 *
 * @author mike
 */
public class MLearnMenu extends Form implements ActionListener, DataChangedListener{

    private MLearnPlayerMidlet host;
    
    String[] labels = {"continue", "repeat", "back", "contents",  "collection", "opencourse", "settings", "teacherreport", "quit"};
    
    private static final int CONTINUE = 0;
    
    private static final int REPEAT = 1;
    
    private static final int BACK = 2;
    
    private static final int CONTENTS = 3;
    
    private static final int COLLECTION = 4;
    
    private static final int OPENCOURSE = 5;
    
    private static final int SETTINGS = 6;
    
    private static final int ABOUTFORM = 7;
    
    private static final int QUIT = 8;
    
    private static final int SEARCHBT = 43;
    
    //start of stuff for settings form
    Form settingsForm;
    Slider volSlider;
    Command setOKCmd;
    
    //end of settings form stuff
    
    Button[] buttons;
    
    Command[] cmds;
    
    BoxLayout bLayout;
    
    //name of the learner (for teacher record transmission etc)
    TextField nameField;
    
    //The combo box for setting the language
    ComboBox langCombo;
    
    //Available lang codes
    String[] langCodeStr;
    
    //Learner ID field
    TextField lIdField;
    
    CheckBox svrOnCheckBox;
    
    //command for doing the bluetooth search
    Command doSearchCmd;
    
    //button for that
    Button searchButton;
    
    //Bluetooth stuff here
    Form btDevicesForm;
    
    MLClientManager clientMgr;
    
    BTActionListener btActionListener;
    Vector btClientURLs;
    BTSelectActionListener btSelectActionListener;
    
    Command sendTestCmd;
    Button sendTestBtn;
    private static final int SENDTEST = 45;
    
    Dialog loadingDialog;
    
    //end bluetooth stuff
    
    
    //===http related stuff===
    
    Command cmdShowHTTP;
    Form frmHTTP;
    
    //the about form
    Form aboutForm;
    
    //Vector index -> Filename
    Vector httpSettingsList;
    
    //For the radio buttons
    ButtonGroup httpSettingGrp;
    
    TextField httpUserTF;
    
    TextField httpPassTF;
    
    Command cmdHTTPOK;
    
    Command cmdHTTPSend;
    
    //===end http stuff===
    
    
    public MLearnMenu(MLearnPlayerMidlet host) {
        this.host = host;
        buttons = new Button[labels.length];
        cmds = new Command[labels.length];
        
        bLayout = new BoxLayout(BoxLayout.Y_AXIS) ;
        setLayout(bLayout);
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
        
        setOKCmd = new Command("OK", 42);

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
        
        
        Label tchrLabel = new Label(MLearnUtils._("Teachers Phone"));
        settingsForm.addComponent(tchrLabel);
        
        String btServerName = EXEStrMgr.getInstance().getPref("server.bt.name");
        String btBtnStr = "[Find]";
        if(btServerName != null) {
            btBtnStr = btServerName;
        }
        
        doSearchCmd = new Command(btBtnStr, SEARCHBT);
        searchButton = new Button(doSearchCmd);
        searchButton.addActionListener(this);
        settingsForm.addComponent(searchButton);
        
        sendTestCmd = new Command("Send Test Data", SENDTEST);
        sendTestBtn = new Button(sendTestCmd);
        sendTestBtn.addActionListener(this);
        settingsForm.addComponent(sendTestBtn);
        
        svrOnCheckBox = new CheckBox(MLearnUtils._("serveron"));
        String svrEnabled = EXEStrMgr.getInstance().getPref("server.enabled");
        if(svrEnabled != null && svrEnabled.equals("true")) {
            svrOnCheckBox.setSelected(true);
        }
        settingsForm.addComponent(svrOnCheckBox);
        

        
        Button settingOKButton = new Button(setOKCmd);
        settingOKButton.addActionListener(this);
        settingsForm.addComponent(settingOKButton);
        
        //http replications settings
        cmdShowHTTP = new Command("Log Sending", 200);
        Button httpRepButton = new Button(cmdShowHTTP);
        httpRepButton.addActionListener(this);
        settingsForm.addComponent(httpRepButton);
        
        makeHTTPFrm();
        
        
    }
    
    private void makeHTTPFrm() {
        frmHTTP = new Form("Log Settings");
        //go and look for settings
        frmHTTP.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        frmHTTP.addComponent(new Label(MLearnUtils._("Transmit Set")));
        
        try {
            FileConnection con = (FileConnection)Connector.open(
                        EXEStrMgr.getInstance().getPref("basefolder") + "/txsettings");
            if(!con.exists()) {
                con.mkdir();
            }
            
            httpSettingsList = MLearnUtils.enumToVector(con.list("*.ht", true));
            httpSettingGrp = new ButtonGroup();
            for(int i = 0; i < httpSettingsList.size(); i++) {
                RadioButton rb = new RadioButton(httpSettingsList.elementAt(i).toString());
                httpSettingGrp.add(rb);
                frmHTTP.addComponent(rb);
            }
            RadioButton noTXBtn = new RadioButton("None");
            httpSettingGrp.add(noTXBtn);
            frmHTTP.addComponent(noTXBtn);
        }catch(IOException e) {
            EXEStrMgr.po(e, "Exception making http settings form");
        }
        
        frmHTTP.addComponent(new Label(MLearnUtils._("Username")));
        httpUserTF = new TextField();
        frmHTTP.addComponent(httpUserTF);
        
        frmHTTP.addComponent(new Label(MLearnUtils._("Password")));
        httpPassTF = new TextField();
        frmHTTP.addComponent(httpPassTF);
        
        cmdHTTPOK = new Command(MLearnUtils._("OK"), 210);
        Button okBtn = new Button(cmdHTTPOK);
        okBtn.addActionListener(this);
        frmHTTP.addComponent(okBtn);
        
        /*
        cmdHTTPSend = new Command("Send Now", 220);
        Button sendBtn = new Button(cmdHTTPSend);
        sendBtn.addActionListener(this);
        frmHTTP.addComponent(sendBtn);
        * 
        */
    }
    
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
    
    private void updateHTTPSettings() {
        int httpSel = httpSettingGrp.getSelectedIndex();
        
        if(httpSel == httpSettingsList.size()) {
            //they have select no tx
            EXEStrMgr.getInstance().delPref("httptx.url");
            EXEStrMgr.getInstance().delPref("httptx.username");
            EXEStrMgr.getInstance().delPref("httptx.password");    
            EXEStrMgr.getInstance().savePrefs();
        }else {
            String basename = httpSettingsList.elementAt(httpSel).toString();
            Hashtable setHT = MLearnUtils.loadHTFile(EXEStrMgr.getInstance().getPref("basefolder")
                    + "/txsettings/" + basename);
            if(setHT != null) {
                String txURL = setHT.get("server.url").toString();
                EXEStrMgr.getInstance().setPref("httptx.url", 
                        txURL);
                EXEStrMgr.getInstance().setPref("httptx.username",
                        setHT.get("server.username").toString());
                EXEStrMgr.getInstance().setPref("httptx.password",
                        setHT.get("server.password").toString());
                EXEStrMgr.getInstance().savePrefs();
                EXEStrMgr.po("Updated http settings OK to " + txURL, EXEStrMgr.DEBUG);
                MLObjectPusher.countDown = 5;
            }else {
                EXEStrMgr.po("Failure updating http settings", EXEStrMgr.DEBUG);
            }
        }
    }
    
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
                //MLReportRequest req = new MLReportRequest(this);
                //req.setupForm();
                //req.show();
                
                aboutForm.show();
            }else if(cmd.getId() == cmdShowHTTP.getId()) {
                frmHTTP.show();
            }else if(cmd.getId() == cmdHTTPOK.getId()) {
                updateHTTPSettings();
                settingsForm.show();
            }else if(cmd.getId() == cmdHTTPSend.getId()) {
                
            }
        }
    }
    
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

    public void dataChanged(int id, int level) {
       host.setVolume(level);
    }
    
    
    
}



class BTSelectActionListener implements ActionListener {

    MLearnMenu host;
    
    public BTSelectActionListener(MLearnMenu host) {
        this.host = host;
    }

    
    
    public void actionPerformed(ActionEvent ae) {
        Command cmd = ae.getCommand();
        int id = cmd.getId();
        
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