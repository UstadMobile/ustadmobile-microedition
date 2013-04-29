/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.toughra.mlearnplayer;
import com.sun.lwuit.Command;
import com.sun.lwuit.Form;
import com.sun.lwuit.Image;
import com.sun.lwuit.events.*;

/**
 *
 * @author mike
 */
public class NavigationActionListener implements ActionListener{

    private MLearnPlayerMidlet hostMidlet;
       
    private Image menuIcon;
    
    private int MENUCMD = 1;

    public NavigationActionListener(MLearnPlayerMidlet hostMidlet) {
        this.hostMidlet = hostMidlet;
        try {
            this.menuIcon = Image.createImage(
                hostMidlet.getInputStreamReaderByURL("/icons/next.png"));
        }catch(Exception e) {}//should not happen
    }

    public void actionPerformed(ActionEvent ae) {
        int incrementIdevice = 0;
        if(ae.getCommand().getId() == MENUCMD) {
            hostMidlet.showMenu();
        }
    }

    public void addMenuCommandsToForm(Form form) {
        Command menuCommand = new Command(null, menuIcon, MENUCMD);
        form.addCommand(menuCommand);
        menuCommand.setEnabled(true);
        form.addCommandListener(this);
    }

}
