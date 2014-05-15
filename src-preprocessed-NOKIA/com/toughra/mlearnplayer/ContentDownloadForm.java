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
import com.sun.lwuit.layouts.BoxLayout;
import com.sun.lwuit.layouts.GridLayout;
import com.toughra.mlearnplayer.ContentDownloadByIdForm;

/**
 * This form is a container for three buttons for course downloading...
 * 
 * @author Michael Dawson mike@ustadmobile.com
 */
public class ContentDownloadForm extends Form implements  ActionListener{
    
    /** Labels for this form */
    public static final String[] buttonLabels = new String[] {"By Course ID",
        "From Library", "Back"};
    
    /** Constant for command to download by ID*/
    public static final int DLFORM_BYID = 0;
    
    /** Constant to download from public repo */
    public static final int DLFORM_REPO = 1;
    
    /** Constant to go back to Menu (e.g. quit) */
    public static final int GOBACK = 2;
    
    public MLearnMenu hostMenu;
    
    /** Instance of ContentDownloadByIdForm to show it.**/
    private ContentDownloadByIdForm downloadByIdForm;
    
    /** Instance of ContentDownloadByRepoForm to show it.**/
    private ContentDownloadByRepoForm downloadByRepoForm;
    
    /**
     * 
     * @param hostMenu 
     */
    public ContentDownloadForm(MLearnMenu hostMenu) {
        setTitle("Download Course");
        this.hostMenu = hostMenu;
        
        BoxLayout bLayout = new BoxLayout(BoxLayout.Y_AXIS);
        setLayout(bLayout);
        MLearnUtils.addButtonsToForm(this, buttonLabels, null, "/icons/download_select/icon-");
        
        downloadByIdForm = new ContentDownloadByIdForm(this);
        downloadByRepoForm = new ContentDownloadByRepoForm((this));
    }

    /** Handle events from button
     * 
     * @param evt ActionEvent from button
     */
    public void actionPerformed(ActionEvent evt) {
        Command cmd = evt.getCommand();
        if(cmd != null) {
            switch(cmd.getId()) {
                case DLFORM_BYID:
                    downloadByIdForm.show();
                    //show download by id form
                    break;
                case DLFORM_REPO:
                    downloadByRepoForm.show();
                    //show download from library form
                    break;
                case GOBACK:
                    //show the main menu again
                    hostMenu.show();
            }
                    
        }
    }
    
    
}
