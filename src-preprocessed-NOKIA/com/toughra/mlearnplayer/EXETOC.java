/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
    
    public static final int COL_TITLE = 0;
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
    
    String currentURL;
    
    /** If the current package loaded is right to left*/
    public boolean currentIsRTL;
    
    /**Language codes that are RTL*/
    static final String[] RTLLANGS = {"ar", "fa", "ps"};

    /* List for GUI */
    List tocList;

    /** */
    private MLearnPlayerMidlet hostMidlet;

    public EXETOC(MLearnPlayerMidlet hostMidlet) {
        this.hostMidlet = hostMidlet;
    }

    public int getNumPages() {
        return cache.pages.length;
    }
    
    public String getPageHref(int index) {
        return cache.pages[index].href;
    }
    
    /**
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
    
    public void clearCollection() {
        collection = null;
        colBaseDir = null;
        colTitle = null;
    }
    
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
     * Utility method to get a generic xml parser and the kxmlparser
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
            String lang = rootNode.getAttribute("xml:lang");
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

    
    public void disposeList() {
        this.tocList = null;
    }

    public List getList() {
        return tocList;
    }


    /**
     * Provides a LWUIT Form to be shown
     * @return
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
