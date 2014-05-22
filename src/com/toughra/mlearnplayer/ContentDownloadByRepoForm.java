/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.toughra.mlearnplayer;

import com.sun.lwuit.Command;
import com.sun.lwuit.Form;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.layouts.BoxLayout;

/**
 *
 * @author varuna
 */
public class ContentDownloadByRepoForm extends Form implements ActionListener {
    
    /** Button Labels for this form */
    public static final String[] buttonLabels = new String[] 
    {"Download", "Go Back"};
    
    
    /** Constant to download a course by specified Course ID **/
    public static final int DL = 0;
    
    /** Constant to go back to Menu **/
    public static final int GOBACK = 1;
    
    /** Such that we can go back to the content form**/
    private Form contentDownForm;
    
    /** Constructor **/
    public ContentDownloadByRepoForm(Form contentDownForm){
        setTitle("Download from Library");
        this.contentDownForm = contentDownForm;
        
        /** Initialising the form as a box layout**/
        BoxLayout bLayout = new BoxLayout(BoxLayout.Y_AXIS);
        setLayout(bLayout);
        
        MLearnUtils.addButtonsToForm(this, buttonLabels, null, "/icons/download_repo/icon-");
        
    }
    
    /** Handle events from button
     * 
     * @param evt ActionEvent from button
     */
    public void actionPerformed(ActionEvent evt){
        Command cmd = evt.getCommand();
        if (cmd !=null){
            switch(cmd.getId()){
                case DL:
                    //Check if Course id is filled and initiate download.
                    break;
                case GOBACK:
                    //show the course list
                    contentDownForm.show();
            }
        }
    }
}
