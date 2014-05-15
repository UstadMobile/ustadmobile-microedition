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
import com.sun.lwuit.*;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.layouts.BorderLayout;
import com.sun.lwuit.layouts.BoxLayout;
import com.toughra.mlearnplayer.datatx.MLCloudConnector;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This Form shows the login for the server and checks it out.  Will proceed
 * once the student has a valid login
 * 
 * run method is called when the result comes back from the server via callSerially
 * 
 * @author mike
 */
public class ServerLoginForm extends Form implements ActionListener, Runnable{

    MLearnPlayerMidlet hostMidlet;
    
    TextField userIDField;
    TextField passcodeField;
    Button loginButton;
    Command loginCommand;
    public static final int CMDID_LOGIN=0;
    
    Button skipLoginButton;
    Command skipLoginCommand;
    public static final int CMDID_SKIP=1;
    
    LoginCheckThread checkThread = null;
    int lastResult = -1;
    Dialog loadingDialog = null;
    int errorShowTime = 5000;
    
    Vector actionListeners;
    
    public ServerLoginForm(MLearnPlayerMidlet hostMidlet) {
        this.hostMidlet = hostMidlet;
        
        String[] inputModeOrder = {"123"};
        
        setTitle(MLearnUtils._("Login"));
        
        BoxLayout boxLayout = new BoxLayout(BoxLayout.Y_AXIS);
        Label userIDLabel = new Label(MLearnUtils._("id"));
        addComponent(userIDLabel);
        
        
        userIDField = new MLTextField();
        //userIDField.setInputModeOrder(inputModeOrder); //Commented such that the Login field does not include numbers only.
        
        //TODO:RE-ENABLE ME IN NOKIA
        //userIDField.setT9Enabled(false);
        
        //userIDField.setUseSoftkeys(true);
        addComponent(userIDField);
        
        Label passwordLabel = new Label(MLearnUtils._("passcode"));
        addComponent(passwordLabel);
        
        passcodeField = new MLTextField();
        //passcodeField.setInputModeOrder(inputModeOrder); //Commented such that the Login field does not include numbers only
        
        //TODO:RE-ENABLE ME IN NOKIA
        //passcodeField.setT9Enabled(false);
        //passcodeField.setInputMode("123"); //Commented such that the Login field does not only accept numbers.
        addComponent(passcodeField);
        
        loginCommand = new Command(MLearnUtils._("login"), CMDID_LOGIN);
        loginButton = new Button(loginCommand);
        loginButton.addActionListener(this);
        addComponent(loginButton);
        
        if(MLearnPlayerMidlet.canSkipLogin) {
            skipLoginCommand = new Command(MLearnUtils._("Skip"), CMDID_SKIP);
            skipLoginButton = new Button(skipLoginCommand);
            skipLoginButton.addActionListener(this);
            addComponent(skipLoginButton);
        }
        
        actionListeners = new Vector();
        
        setupTextFields();
    }
    
    /**
     * This will set the correct TextField setup for Ustad Mobile
     * We don't want to use the T9 menu (keep it simple) 
     * We use our own setSkipKey so that the * button does not popup a 
     * symbol dialog
     */
    public static void setTextFieldDefaults() {
        TextField.setUseNativeTextInput(false);
        TextField.setReplaceMenuDefault(false);
        
        //TODO:RE-ENABLE ME IN NOKIA
        //TextField.setSkipKey(42);
        
        //TextField.setReplaceMenuDefault(true);
    }
    
    public void addActionListener(ActionListener l) {
        actionListeners.addElement(l);
    }
    
    public void removeActionListener(ActionListener l) {
        actionListeners.removeElement(l);
    }
    
    /**
     * 
     */
    protected void fireActionEvent() {
        ActionEvent evt = new ActionEvent(this);
        for(int i = 0; i < actionListeners.size(); i++) {
            ((ActionListener)actionListeners.elementAt(i)).actionPerformed(evt);
        }
    }
    
    public void actionPerformed(ActionEvent evt) {
        if(evt.getCommand().equals(loginCommand)) {
            if(checkThread == null) {
                loadingDialog = hostMidlet.makeLoadingDialog();
                checkThread = new LoginCheckThread(this, userIDField.getText(),
                        passcodeField.getText());
                checkThread.start();
                loadingDialog.showPacked(BorderLayout.CENTER, true);
            }
        }else if(skipLoginCommand != null && evt.getCommand().equals(skipLoginCommand)) {
            fireActionEvent();
        }
    }
    
    public void processResult(int result) {
        checkThread = null;
        loadingDialog.setVisible(false);
        loadingDialog.dispose();
        loadingDialog = null;
        
        if(result == 200) {
            fireActionEvent();
        }else {
            final Dialog errorWin = new Dialog(MLearnUtils._("Error"));
            TextArea errorText = new TextArea();
            if(result == 401) {
                errorText.setText(MLearnUtils._("Invalid userid/passcode"));
            }else {
                errorText.setText(MLearnUtils._("Communication error"));
            }
            errorWin.addComponent(errorText);
            errorWin.setTimeout(errorShowTime);
            errorWin.showPacked(BorderLayout.CENTER, true);
        }
    }
    
    private void setupTextFields() {
        Hashtable numbers = new Hashtable();
        for(int iter = 0 ; iter < 10 ; iter++) {
            numbers.put(new Integer('0' + iter), String.valueOf(iter));
        }
        
        numbers.put(new Integer(MLearnPlayerMidlet.KEY_PREV), "-");
        
        TextField.addInputMode("123", numbers, false);
    }
    
    public void run() {
        //if login is OK move to available books
        processResult(lastResult);
    }
    
}

class LoginCheckThread extends Thread {
    
    ServerLoginForm loginForm;
    String userID;
    String passcode;
    
    public LoginCheckThread(ServerLoginForm loginForm, String userID, String passcode) {
        this.loginForm = loginForm;
        this.userID = userID;
        this.passcode = passcode;
    }
    
    public void run() {
        MLCloudConnector cloudCon = MLCloudConnector.getInstance();
        int result = cloudCon.checkLogin(userID, passcode);
        if(result == 200) {
            cloudCon.getPreferences();//load preferences from server.
        }
        loginForm.lastResult = result;
        Display.getInstance().callSerially(loginForm);
    }
    
}


class MLTextField extends TextField {

    public MLTextField() {
        super();
    }

    
    
}
