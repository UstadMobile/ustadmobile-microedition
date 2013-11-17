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
    
    public ContentBrowseForm(MLearnPlayerMidlet hostMidlet) {
        this.hostMidlet = hostMidlet;
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
        
        Vector itemVector = new Vector();
        
        try {
            while(e.hasMoreElements()) {
                //find folders
                String curSubDir = e.nextElement().toString();
                
                //TODO: Fix ugly hack to avoid nokia bug?
                if(curSubDir.toLowerCase().indexOf("c:") != -1) {
                    continue;
                }
                FileConnection subCon = (FileConnection)Connector.open("file://localhost" 
                        + SEP_STR + curSubDir);
                Enumeration dirContents = subCon.list();
                
                Vector subDirVector = new Vector();
                //copy the enumeration + close the actual connection
                while(dirContents.hasMoreElements()) {
                    subDirVector.addElement(dirContents.nextElement());
                }
                subCon.close();
                subCon = null;
                
                int type = 0;
                Image icon = defaultContentIcon;
                String title = null;
                String itemFullPath = null;
                
                dirContents = subDirVector.elements();
                
                while(dirContents.hasMoreElements()) {
                    type = -1;
                    String actDir = dirContents.nextElement().toString();
                    if(!actDir.endsWith("/")) {
                        actDir += '/';
                    }
                    String dirFullPath = "file://localhost" + SEP_STR + curSubDir + actDir;
                    String fullPath = dirFullPath + "exetoc.xml";
                    String colPath = dirFullPath + "execollection.xml";
                    
                    FileConnection dirCon = (FileConnection)Connector.open(dirFullPath);
                    boolean hasEXEPkg = false;
                    boolean hasEXECol = false;
                    boolean hasIcon = false;
                    if(dirCon.isDirectory()) {
                        hasEXEPkg = dirCon.list("exetoc.xml", false).hasMoreElements();
                        hasEXECol = dirCon.list("execollection.xml", false).hasMoreElements();
                        hasIcon = dirCon.list("mplayicon.png", false).hasMoreElements();
                    }
                    dirCon.close();
                    
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
                        
                        ContentBrowseItem thisItem = new ContentBrowseItem(dirFullPath, type, title, icon);
                        itemVector.addElement(thisItem);
                    }
                    
                    icon = defaultContentIcon;
                }
                
            }
        }catch(Exception ex) {
            ex.printStackTrace();
        }
        
        BoxLayout layout = new BoxLayout(BoxLayout.Y_AXIS);
        setLayout(layout);
        
        int numPkgs = itemVector.size();
        pkgCmds = new Command[numPkgs];
        browseItems = new ContentBrowseItem[numPkgs];
        
        for(int i = 0; i < numPkgs; i++) {
            browseItems[i] = (ContentBrowseItem)itemVector.elementAt(i);
            pkgCmds[i] = new Command(browseItems[i].title, 
                    browseItems[i].icon, i);
            Button pkgButton = new Button(pkgCmds[i]);
            pkgButton.addActionListener(this);
            addComponent(pkgButton);
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
        if(browseItems[cmdId].type == TYPE_NORM) {
            hostMidlet.myTOC.clearCollection();
            hostMidlet.openPackageDir(browseItems[cmdId].fullpath);
        }else if(browseItems[cmdId].type == TYPE_COL){
            hostMidlet.openCollectionDir(browseItems[cmdId].fullpath);
        }
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