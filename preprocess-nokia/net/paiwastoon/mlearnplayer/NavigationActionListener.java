/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.paiwastoon.mlearnplayer;
import com.sun.lwuit.Command;
import com.sun.lwuit.Form;
import com.sun.lwuit.MenuBar;
import com.sun.lwuit.events.*;

/**
 *
 * @author mike
 */
public class NavigationActionListener implements ActionListener{

    private MLearnPlayerMidlet hostMidlet;
    
    static String menuLabel = "Menu";

    public NavigationActionListener(MLearnPlayerMidlet hostMidlet) {
        this.hostMidlet = hostMidlet;
    }

    public void actionPerformed(ActionEvent ae) {
        String commandName = ae.getCommand().getCommandName();
        int incrementIdevice = 0;
        if(commandName.equals(menuLabel)) {
            hostMidlet.showMenu();
        }
    }

    public void addMenuCommandsToForm(Form form) {
        
        Command menuCommand = new Command("Menu");
        form.addCommand(menuCommand);
        menuCommand.setEnabled(true);

        form.addCommandListener(this);
    }

}
