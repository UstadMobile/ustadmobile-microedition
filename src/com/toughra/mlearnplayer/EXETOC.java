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
import com.toughra.mlearnplayer.xml.XmlNode;
import com.toughra.mlearnplayer.xml.GenericXmlParser;
import com.sun.lwuit.events.ActionEvent;
import java.io.*;
import org.kxml2.io.KXmlParser;
import java.util.*;
import com.sun.lwuit.*;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.list.DefaultListCellRenderer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 *
 * This class implements the Table of Contents to read an EXE Generated XML
 * table of contents file and then provide the user with a list that they can
 * use to select the item that they wish to go to.
 * 
 * It caches the content of the package as a set of objects so that it need
 * not be re-read as the learner navigates.
 *
 * @author mike
 */
public class EXETOC implements ActionListener{


    //set of static ints for use with the getParsers method
    private static final int RET_GPARSER = 0;

    private static final int RET_KXMLPARSER = 1;
    
    private static final int RET_INPUTREADER = 2;
    
    
    //set of static ints for use with collections
    public static final int PAGELIST_IDEVICEIDS = 0;

    public static final int PAGELIST_IDEVICETITLES = 1;
    
    public TOCCache cache;
    
    /**
     * 2 dimensional string with title/dir for each item in collection
     */
    public String[][] collection;
    
    /**
     * Title of the collection
     */
    public String colTitle;
    
    /**
     * Collection base dir
     */
    public String colBaseDir;
    
    /**
     * The resolutions that are supported in this package
     */
    public boolean[] supportedResolutions;
    
    /**
     * The screen resolution that should be used for this package
     */
    public static int screenResToUse = -1;
    
    /**
     * The audio format to use - as indexed by MLearnUtils.AUDIOFORMAT_NAMES
     */
    public static int audioFormatToUse = -1;
    
    /**
     * The video format to use - as indexed by MLearnUtils.VIDEOFORMAT_NAMES
     */
    public static int videoFormatToUse = -1;
    
    /**
     * The video format to use
     */
    
    
    /** The collection title*/
    public static final int COL_TITLE = 0;
    
    /** The collection directory*/
    public static final int COL_DIR = 1;
    
    /** Base folder for the collection */
    public String colBaseHREF;
    
    /**
     * Table of contents for the collection
     */
    public Form colForm;
    
    /**
     * TOC Form
     */
    public Form tocForm;
    
    /**
     * LWUIT List for the collection
     */
    List colList;
    
    /**The URL of the collection */
    String currentURL;
    
    /** If the current package loaded is right to left
     * This will get detected based on the language specified in the package
     */
    public boolean currentIsRTL;
    
    /**Language codes that are RTL*/
    static final String[] RTLLANGS = {"ar", "fa", "ps"};

    /* List for GUI */
    List tocList;

    /** The host midlet*/
    private MLearnPlayerMidlet hostMidlet;

    /**
     * Constructor method takes just only the host midlet
     * 
     * @param hostMidlet 
     */
    public EXETOC(MLearnPlayerMidlet hostMidlet) {
        this.hostMidlet = hostMidlet;
    }

    /**
     * Get the number of pages
     * 
     * @return number of pages in current package.
     */
    public int getNumPages() {
        return cache.pages.length;
    }
    
    /**
     * Get the href for the given page index.
     * 
     * @param index
     * @return String href of the given index
     */
    public String getPageHref(int index) {
        return cache.pages[index].href;
    }
    
    /**
     * Go through a collection (execollection.xml file that details multiple
     * packages that go together).  Cache the titles and hrefs of each package
     * in the collection so the user can make a selection
     * 
     * @param href 
     */
    public void readCollection(String href) {
        try {
            Object[] parsers = getParser(href);
            GenericXmlParser gParser = (GenericXmlParser)parsers[RET_GPARSER];
            XmlNode rootNode = gParser.parseXML((KXmlParser)parsers[RET_KXMLPARSER],
                    true);
            InputStreamReader reader = (InputStreamReader)parsers[RET_INPUTREADER];
            reader.close();
            
            colTitle = rootNode.getAttribute("title");
            Vector itemChildren = rootNode.findChildrenByTagName("item", true);
            int numChildren = itemChildren.size();
            collection = new String[numChildren][2];
            for(int i = 0; i < numChildren; i++) {
                XmlNode child = (XmlNode)itemChildren.elementAt(i);
                collection[i][COL_DIR] = child.getTextChildContent(0);
                collection[i][COL_TITLE] = child.getAttribute("title");
            }
            //reset this
            colForm = null;
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Clear out details of the current collection
     */
    public void clearCollection() {
        collection = null;
        colBaseDir = null;
        colTitle = null;
    }
    
    /**
     * Makes a form for the user to select a package from the current collection
     * You need to call readCollection first.
     * 
     * @return 
     */
    public Form getCollectionForm() {
        if(colForm == null) {
            colForm = new Form(colTitle);
            colList = new List();
            colList.setRenderer(new DefaultListCellRenderer(false));
            int numItems = collection.length;
            for(int i = 0; i < numItems; i++) {
                colList.addItem(collection[i][COL_TITLE]);
            }
            colList.addActionListener(this);
            colForm.addComponent(colList);
            hostMidlet.navListener.addMenuCommandsToForm(colForm);
        }
        
        return colForm;
    }
    
    /**
     * Where fullpath ends without a trailing / and does not have a filename in
     * it...
     * 
     * @param fullPath 
     */
    public int getCollectionIndex(String fullPath) {
        //get the last part of the directory string
        //in case the directory name ends with a slash, then chop that off
        if(fullPath.endsWith("/")) {
            fullPath = fullPath.substring(0, fullPath.length()-1);
        }
        
        int lastSlash = fullPath.lastIndexOf('/');
        String dirName = fullPath.substring(lastSlash+1);
        for(int i = 0; i < collection.length; i++) {
            if(dirName.equals(collection[i][COL_DIR])) {
                return i;
            }
        }
        
        return -1;
    }
    
    /**
     * Work out the starting path for an entry in the currently loaded collection
     * by index
     */ 
    public String getColBaseHref(int index) {
        return colBaseHREF + "/" + collection[index][COL_DIR] + "/";
    }
    
    /**
     * Get the package collection dir by index
     */
    public String getColPkgId(int index) {
        return collection[index][COL_DIR];
    }

    /**
     * Notifies the hostMidlet when the user has selected a package from the 
     * current collection
     * 
     * @param ae 
     */
    public void actionPerformed(ActionEvent ae) {
        if(ae.getSource() == colList) {
            //open 
            int index = colList.getSelectedIndex();
            System.out.println("Open Collection item" + collection[index][COL_DIR]);
            hostMidlet.openPackageDir(getColBaseHref(index));
        }
    }
    
    
    

    
    /**
     * This will get the list of idevice ids and titles for a given page
     * 
     * @param URL
     * @param pageHref
     * @return
     */
    public Object[] getPageIdeviceList(String URL, String pageHref) {
        for(int i = 0; i < cache.pages.length; i++) {
            if(cache.pages[i].href.equals(pageHref)) {
                int numIdevices = cache.pages[i].idevices.length;
                String[] ideviceIds = new String[numIdevices];
                String[] ideviceTitles = new String[numIdevices];
                for(int j = 0; j < numIdevices; j++) {
                    ideviceIds[j] = cache.pages[i].idevices[j][TOCCachePage.DEV_ID];
                    ideviceTitles[j] = cache.pages[i].idevices[j][TOCCachePage.DEV_TITLE];
                }
                
                return new Object[]{ideviceIds, ideviceTitles};
            }   
        }
        
        return null;
    }
    
    
    
    
    /**
     * Utility method to get a generic xml parser and the kxmlparser for a given
     * URL
     *  
     * @param URL
     * @return
     * @throws IOException
     * @throws org.xmlpull.v1.XmlPullParserException
     */
    public Object[] getParser(String URL) throws IOException, org.xmlpull.v1.XmlPullParserException {
        InputStreamReader reader = hostMidlet.getUTF8StreamForURL(URL);
        KXmlParser parser = new KXmlParser();
        parser.setInput(reader);
        GenericXmlParser gParser = new GenericXmlParser();
        return new Object[] {gParser, parser, reader};
    }
    
    
    /**
     * Looks through the list of screen sizes that are included in this package
     * and returns a boolean array representing which ones are included and 
     * which ones are not
     * 
     * @param screenSizeStr
     * @return boolean array with true for those that are supported
     */
    public static boolean[] getSupportedScreenSizes(String screenSizeStr) {
        boolean[] retVal = new boolean[MLearnUtils.SCREENSIZE_NAMES.length];
        for(int i = 0; i < retVal.length; i++) {
            if(screenSizeStr.indexOf(MLearnUtils.SCREENSIZE_NAMES[i]) != -1) {
                retVal[i] = true;
            }
        }
        
        return retVal;
    }

    /**
     * This will read exetoc.xml and cache the list of pages and idevices
     * that are in the package.  
     * 
     * @param URL the path to the exetoc.xml file
     */
    public void readList(String URL) {
        if(currentURL != null && currentURL.equals(URL)) {
            //we have cached that - just return
            return;
        }
        
        currentURL = null;
        tocForm = null;
        tocList = null;
        
        try {
            Object[] parsers = getParser(URL);
            GenericXmlParser gParser = (GenericXmlParser)parsers[RET_GPARSER];
            cache = new TOCCache();
            XmlNode rootNode = gParser.parseXML((KXmlParser)parsers[RET_KXMLPARSER],
                    true);
            String lang = null;
            if(MLearnPlayerMidlet.rtlMode == MLearnPlayerMidlet.RTLMODE_SETTINGS) {
                lang = EXEStrMgr.getInstance().getPref("locale");
            }else {
                lang = rootNode.getAttribute("xml:lang");
            }
            
            String resolutions = rootNode.getAttribute("resolutions");
            if(resolutions != null) {
                supportedResolutions = getSupportedScreenSizes(resolutions);
            }else {
                //this is an older export... we support only the original image here
                supportedResolutions = new boolean[MLearnUtils.SCREENSIZES.length];
            }
            
            String[] deviceSupportedMediaTypes = MLearnUtils.getSupportedMedia();
            
            String audioFormatsInPkg = rootNode.getAttribute("audioformats");
            if(audioFormatsInPkg != null) {
                boolean[] formatsInPkg = MLearnUtils.formatListToBooleans(
                        MLearnUtils.AUDIOFORMAT_NAMES, audioFormatsInPkg);
                boolean[] formatsToUse = MLearnUtils.cutUnsupportedFormats(
                        deviceSupportedMediaTypes, MLearnUtils.AUDIOFORMAT_NAMES, formatsInPkg);
                audioFormatToUse = MLearnUtils.getPreferredFormat(formatsToUse);
            }
            
            String videoFormatsInPkg = rootNode.getAttribute("videoformats");
            if(videoFormatsInPkg != null) {
                boolean[] formatsInPkg = MLearnUtils.formatListToBooleans(
                        MLearnUtils.VIDEOFORMAT_NAMES, videoFormatsInPkg);
                boolean[] formatsToUse = MLearnUtils.cutUnsupportedFormats(
                        deviceSupportedMediaTypes, MLearnUtils.VIDEOFORMAT_NAMES, formatsInPkg);
                videoFormatToUse = MLearnUtils.getPreferredFormat(formatsToUse);
            }
            
            int displayWidth = Display.getInstance().getDisplayWidth();
            int displayHeight = Display.getInstance().getDisplayHeight();
            screenResToUse = MLearnUtils.getScreenIndex(displayWidth, 
                    displayHeight, supportedResolutions);
            
            boolean thisIsRTL = false;
            if(lang != null) {
                lang = lang.substring(0, 2);
                for(int i = 0; i < RTLLANGS.length; i++) {
                    if(lang.equals(RTLLANGS[i])) {
                        thisIsRTL = true;
                    }
                }
            }
            this.currentIsRTL = thisIsRTL;
            
            InputStreamReader reader = (InputStreamReader)parsers[RET_INPUTREADER];
            reader.close();
                    
            Vector pageVec = rootNode.findChildrenByTagName("page", true);
            int numPages = pageVec.size();
            cache.pages = new TOCCachePage[numPages];
            
            for(int i = 0; i < numPages; i++) {
                XmlNode currentPageNode = (XmlNode)pageVec.elementAt(i);
                cache.pages[i] = new TOCCachePage();
                cache.pages[i].href = currentPageNode.getAttribute("href");
                cache.pages[i].title = currentPageNode.getAttribute("title");
                
                Vector pageDevices = currentPageNode.findChildrenByTagName("idevice", true);
                int numDevices = pageDevices.size();
                cache.pages[i].idevices = new String[numDevices][3];
                for(int j = 0; j< numDevices; j++) {
                    XmlNode curNode = (XmlNode)pageDevices.elementAt(j);
                    cache.pages[i].idevices[j][TOCCachePage.DEV_ID] = curNode.getAttribute("id");
                    cache.pages[i].idevices[j][TOCCachePage.DEV_TITLE] = curNode.getAttribute("title");
                }
            }
            
            //now set the previous / next hrefs
            for(int i = 0; i < numPages; i++) {
                if(i > 0) {
                    cache.pages[i].prevHref = cache.pages[i-1].href;
                }
                
                if(i < numPages - 1) {
                    cache.pages[i].nextHref = cache.pages[i+1].href;
                }
            }
            
            currentURL = URL;
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    
    /**
     * Get rid of the list object
     */
    public void disposeList() {
        this.tocList = null;
    }

    /**
     * Gets the current list
     * @return current table of contents list
     */
    public List getList() {
        return tocList;
    }


    /**
     * Provides a LWUIT Form to be shown for the table of contents of the
     * current package
     * 
     * @return Form with an option to select each page.
     */
    public Form getForm() {
        if(tocList == null) {
            tocForm = new Form("Table of Contents");
            tocList = new List();
            tocList.setRenderer(new DefaultListCellRenderer(false));
            
            int numItems = cache.pages.length;

            for(int i = 0; i < numItems; i++) {
                String pageTitle = cache.pages[i].title;
                tocList.getModel().addItem(pageTitle);
            }

            tocForm.addComponent(tocList);
            hostMidlet.navListener.addMenuCommandsToForm(tocForm);
        }
        
        return tocForm;
    }


}
