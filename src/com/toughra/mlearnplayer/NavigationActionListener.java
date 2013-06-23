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
import com.sun.lwuit.Command;
import com.sun.lwuit.Form;
import com.sun.lwuit.Image;
import com.sun.lwuit.events.*;

/**
 * Utility class that handles the LWUIT Form object and puts the menu command
 * softbutton on it.
 * 
 * @author mike
 */
public class NavigationActionListener implements ActionListener{

    /** Host midlet*/
    private MLearnPlayerMidlet hostMidlet;
       
    /** Default menu icon*/
    private Image menuIcon;
    
    private int MENUCMD = 1;

    /**
     * Constructor
     * 
     * @param hostMidlet  the host midlet
     */
    public NavigationActionListener(MLearnPlayerMidlet hostMidlet) {
        this.hostMidlet = hostMidlet;
        try {
            this.menuIcon = Image.createImage(
                hostMidlet.getInputStreamReaderByURL("/icons/next.png"));
        }catch(Exception e) {}//should not happen
    }

    /**
     * If the user has selected this - show the menu
     * 
     * @param ae ActionEvent
     */
    public void actionPerformed(ActionEvent ae) {
        int incrementIdevice = 0;
        if(ae.getCommand().getId() == MENUCMD) {
            hostMidlet.showMenu();
        }
    }

    /**
     * Adds the menu command to the given (e.g. idevice) LWUIT Form
     * 
     * @param form 
     */
    public void addMenuCommandsToForm(Form form) {
        Command menuCommand = new Command(null, menuIcon, MENUCMD);
        form.addCommand(menuCommand);
        menuCommand.setEnabled(true);
        form.addCommandListener(this);
    }

}
