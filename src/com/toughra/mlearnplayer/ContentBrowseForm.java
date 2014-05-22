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
import com.sun.lwuit.io.util.Util;
import com.sun.lwuit.layouts.BoxLayout;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.*;


/**
 *
 * This is the form that is shown to the user to select a content package 
 * at the start of using the app.
 * 
 * It will go through the file system roots using FileSystemRegistry.listRoots()
 * and then scan for directories that have either an execollection.xml file
 * or a exetoc.xml file.
 * 
 * These will then be presented as a form to the user to select the content package
 * 
 * @author mike
 */
public class ContentBrowseForm extends Form implements ActionListener{

    private final static String UP_DIRECTORY = "..";
    private final static String MEGA_ROOT = "/";
    private final static String SEP_STR = "/";
    private final static char   SEP = '/';
    
    //represents normal exetoc.xml direct export
    static final int TYPE_NORM = 0;
    
    //represents execollection
    static final int TYPE_COL = 1;
        
    private MLearnPlayerMidlet hostMidlet;
    
    private Command[] pkgCmds;
    
    private ContentBrowseItem[] browseItems;
    
    private String noCourse_Flag = "TRUE";
    
    /** Download content form */
    Form contentDownloadForm;
    
    /** For having menu button on the Form **/
    private boolean navListenerOnForm = false;
    
    public ContentBrowseForm(MLearnPlayerMidlet hostMidlet) {
        this.hostMidlet = hostMidlet;
        contentDownloadForm = new ContentDownloadForm(hostMidlet.getMenu());
    }
      
    /**
     * 
     * @param dirName The dirname that is passed to FileConnection
     * 
     * @return ContentBrowseItem if a package or collection, null otherwise
     */
    public ContentBrowseItem getContentItemFromDir(String curSubDir, String actDir, Image defaultContentIcon) throws Exception{
        int type = -1;
        Image icon = defaultContentIcon;
        String title = null;
        String itemFullPath = null;
        FileConnection dirCon = null;
        ContentBrowseItem thisItem = null;
        String lastDirFullPath = "";
        Exception eCaused = null;
        
        try {
        
            //Active directory should be ending with a / - J2ME wants dirs to end with /
            if(!actDir.endsWith("/")) {
                actDir += '/';
            }

            String dirFullPath = MLearnUtils.joinPaths(new String[] {
                "file://localhost/", curSubDir, actDir
            });

            String fullPath = dirFullPath + "exetoc.xml";
            String colPath = dirFullPath + "execollection.xml";
            
            lastDirFullPath = dirFullPath;

            dirCon = (FileConnection)Connector.open(dirFullPath);
            boolean isDir = dirCon.isDirectory();


            boolean hasEXEPkg = false;
            boolean hasEXECol = false;
            boolean hasIcon = false;
            //#ifdef CRAZYDEBUG
            //#  EXEStrMgr.lg(70, "Check is Directory: " + dirFullPath + " : "
            //#        + isDir);
            //#endif
            dirCon.close();
            dirCon = null;

            if(isDir) {
                hasEXEPkg = MLearnUtils.fileExists(
                        MLearnUtils.joinPath(dirFullPath, "exetoc.xml"));
                hasEXECol = MLearnUtils.fileExists(
                        MLearnUtils.joinPath(dirFullPath, "execollection.xml"));
                hasIcon = MLearnUtils.fileExists(
                        MLearnUtils.joinPath(dirFullPath, "mplayicon.png"));
            }



            //#ifdef CRAZYDEBUG
            //#                     EXEStrMgr.lg(69, "dir " + dirFullPath + " has pkg: " + hasEXEPkg + " / has col: " + hasEXECol);
            //#endif
            String iconPath = dirFullPath + "mplayicon.png";

            if(hasEXEPkg) {                       
                itemFullPath = dirFullPath;
                type = TYPE_NORM;
                title = actDir.substring(0, actDir.length()-1);
            }else if(hasEXECol) {
                type = TYPE_COL;
                String collectionId = MLearnUtils.getCollectionID(dirFullPath);
                title = MLearnUtils.getCollectionTitle(dirFullPath);
                itemFullPath = dirFullPath;
            }

            if(type != -1) {
                if(hasIcon) {
                    try {
                        FileConnection iconFileCon = (FileConnection)Connector.open(iconPath);
                        icon = Image.createImage(iconFileCon.openInputStream());
                        iconFileCon.close();
                    }catch(IOException e2) {
                        e2.printStackTrace();
                    }
                }

                thisItem = new ContentBrowseItem(dirFullPath, type, title, icon);
                
            }
        }catch(Exception e) {
            EXEStrMgr.lg(72, "Exception examining " + lastDirFullPath, e);
        }finally {
            MLearnUtils.closeConnector(dirCon, 
                    "Exception closing " + lastDirFullPath);
        }
        
        return thisItem;
    }
    
    /**
     * The main meat of the class is here - go through the file system roots 
     * as per the class documentation.  
     */
    public void makeForm() {
        Enumeration e = FileSystemRegistry.listRoots();
        String dirName = null;
        Vector menuItems = new Vector();

        Image defaultContentIcon = null;
        try {
            defaultContentIcon = Image.createImage(
                getClass().getResourceAsStream("/icons/default-content.png"));
        }catch(IOException e2) {
            e2.printStackTrace();
            defaultContentIcon = Image.createImage(22, 22, 0xf00);
        }
        
        //holds both the root and ustadMobileContent
        Vector rootsAndSubs = new Vector();
        while(e.hasMoreElements()) {
            String thisRoot = e.nextElement().toString();
            rootsAndSubs.addElement(thisRoot);
            EXEStrMgr.lg(5, "Discovered directory : " + thisRoot);
            String umobileSubDir = MLearnUtils.joinPath(thisRoot, 
                    "ustadmobileContent/");
            EXEStrMgr.lg(5, "Adding for scanning " + umobileSubDir);
            rootsAndSubs.addElement(umobileSubDir);
            
        }
        
        e = rootsAndSubs.elements();
        
        Vector itemVector = new Vector();
        
        try {
            while(e.hasMoreElements()) {
                //find folders
                String curSubDir = e.nextElement().toString();
                
                //TODO: Fix ugly hack to avoid nokia bug?
                if(curSubDir.toLowerCase().indexOf("c:") != -1) {
                    EXEStrMgr.lg(5, "Skipping: has c: " + curSubDir);
                    continue;
                }
                
                String fileConDir = MLearnUtils.joinPath("file://localhost", curSubDir);
      
                FileConnection subCon = (FileConnection)Connector.open(fileConDir); 
                
                boolean dirExists = subCon.isDirectory();
                try {
                    if(curSubDir.endsWith("ustadmobileContent/") && !dirExists) {
                            subCon.mkdir();
                    }
                }catch(Exception err) {
                    //oh shit!
                    EXEStrMgr.lg(666, "attempted to make - but could not " + fileConDir, err);
                }
                
                dirExists = subCon.isDirectory();
                
                if(!dirExists) {
                    subCon.close();
                    subCon = null;
                    continue;//nothing to do here - it does not actually exist
                }
                Enumeration dirContents = subCon.list();
                
                Vector subDirVector = new Vector();
                //copy the enumeration + close the actual connection
                while(dirContents.hasMoreElements()) {
                    subDirVector.addElement(dirContents.nextElement());
                }
                subCon.close();
                subCon = null;
                
                
                
                dirContents = subDirVector.elements();
                
                while(dirContents.hasMoreElements()) {
                    String actDir = dirContents.nextElement().toString();
                    EXEStrMgr.lg(5, "Scanning for content in: " + actDir);
                    ContentBrowseItem thisItem = getContentItemFromDir(curSubDir, actDir, defaultContentIcon);
                    if(thisItem != null) {
                        noCourse_Flag = "FALSE";
                        itemVector.addElement(thisItem);
                        EXEStrMgr.lg(5, "Discovered content item in " + actDir);
                    }
                }
                
            }
        }catch(Exception ex) {
            ex.printStackTrace();
            EXEStrMgr.lg(6, "Exception scanning directories...", ex);
        }
        removeAll(); //Added such that we remove everything before adding all the buttons and course list again.
        
        BoxLayout layout = new BoxLayout(BoxLayout.Y_AXIS);
        setLayout(layout);
        
        int numPkgs = itemVector.size();
        pkgCmds = new Command[numPkgs];
        browseItems = new ContentBrowseItem[numPkgs];
        
        if(noCourse_Flag != "TRUE"){
            Label userIDLabel = new Label(MLearnUtils._("Courses:"));
            addComponent(userIDLabel);
        
            for(int i = 0; i < numPkgs; i++) {
                browseItems[i] = (ContentBrowseItem)itemVector.elementAt(i);
                pkgCmds[i] = new Command(browseItems[i].title, 
                        browseItems[i].icon, i);
                Button pkgButton = new Button(pkgCmds[i]);
                pkgButton.addActionListener(this);
                addComponent(pkgButton);
            }
        }else if(noCourse_Flag == "TRUE"){
            Label noCourseLabel01 = new Label(MLearnUtils._(""
                    + "No courses found."));
            addComponent(noCourseLabel01);
            /**Label  noCourseLabel02 = new Label(MLearnUtils._("Menu > Download"));
            addComponent(noCourseLabel02);
            * */
            
            String[] buttonLabels = new String[] 
                {"Download New"};
            MLearnUtils.addButtonsToForm(this, buttonLabels, 
                    null, "/icons/download_menu/icon-");
        }
        
        if(!navListenerOnForm) {
            boolean[] specialItems = MLearnUtils.makeBooleanArray(true, 
                    MLearnMenu.labels.length);
            specialItems[0] = false;//disable continue
            specialItems[1] = false;
            specialItems[2] = false; //Go back is to go back with respect
                                    // to idevices. Disabling..
            specialItems[3] = false;    //Contents NA in main menu
            specialItems[4] = false;    //Collection NA in main menu
            
            hostMidlet.navListener.addMenuCommandsToForm(this, specialItems);
            navListenerOnForm = true;
        }
    }
    
    
    /**
     * What to do when an item from the list is selected - notifies the 
     * MLearnPlayerMidlet instance of the selection
     * 
     * @param ae 
     */
    public void actionPerformed(ActionEvent ae) {
        int cmdId = ae.getCommand().getId();
        try{
            if(noCourse_Flag == "TRUE"){
                Command cmd = ae.getCommand();
                if (cmd !=null){
                    switch(cmd.getId()){
                        case 0:
                            contentDownloadForm.show();
                    }
                }
            }else if(browseItems[cmdId].type == TYPE_NORM) {
                hostMidlet.myTOC.clearCollection();
                hostMidlet.openPackageDir(browseItems[cmdId].fullpath);
            }else if(browseItems[cmdId].type == TYPE_COL){
                hostMidlet.openCollectionDir(browseItems[cmdId].fullpath);
            }
        }catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * This is used purely to help find the auto open package which is used
     * in EXELearning for preview
     * 
     * @return path for the first item that matches the title, null if none
     */
    public String getPathForAutoOpenItem(String title) {
        for(int i = 0; i < browseItems.length; i++) {
            if(title.equals(browseItems[i].title)) {
                return browseItems[i].fullpath;
            }
        }
        
        return null;
    }
}

/**
 * Simple holder class for items in the list
 * 
 * @author mike
 */
class ContentBrowseItem {
    int type;
    String title;
    Image icon;
    String fullpath;
    
    ContentBrowseItem(String fullpath, int type, String title, Image icon) {
        this.type = type;
        this.title = title;
        this.icon = icon;
        this.fullpath = fullpath;
    }
    
}