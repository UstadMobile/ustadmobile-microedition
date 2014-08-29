/*
 * Ustad Mobile (Micro Edition App)
 * 
 * Copyright 2011-2014 UstadMobile Inc. All rights reserved.
 * www.ustadmobile.com
 *
 * Ustad Mobile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version with the following additional terms:
 * 
 * All names, links, and logos of Ustad Mobile and Toughra Technologies FZ
 * LLC must be kept as they are in the original distribution.  If any new
 * screens are added you must include the Ustad Mobile logo as it has been
 * used in the original distribution.  You may not create any new
 * functionality whose purpose is to diminish or remove the Ustad Mobile
 * Logo.  You must leave the Ustad Mobile logo as the logo for the
 * application to be used with any launcher (e.g. the mobile app launcher).
 * 
 * If you want a commercial license to remove the above restriction you must
 * contact us and purchase a license without these restrictions.
 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 
 * Ustad Mobile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
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
    
    
    /** For having menu button on the Form **/
    //private boolean navListenerOnForm = false;
    
    //private MLearnPlayerMidlet hostMidlet;
    
    /** To figure what the previous form is such that when we go back, we go to the relevant form **/
    //private String prevForm = "none";
    
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
                    
                    // Both work. Must find better one.
                    //hostMidlet.contentBrowser.show();
                    hostMenu.show();
                    /**
                    if (prevForm == "hostMidlet"){
                        hostMidlet.contentBrowser.makeForm();
                        
                        // Both work. Must find better one.
                        //hostMidlet.contentBrowser.show();
                        hostMidlet.showMenu();
                    }else if (prevForm == "hostMenu"){
                        hostMenu.show();
                        
                    }
                    * **/
                    
            }
                    
        }
    }
    
    
}
