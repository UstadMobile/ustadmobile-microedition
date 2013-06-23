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


import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.plaf.UIManager;
import com.sun.lwuit.util.Resources;
import com.sun.lwuit.*;
import com.sun.lwuit.animations.CommonTransitions;
import com.sun.lwuit.html.HTMLCallback;
import com.sun.lwuit.html.HTMLComponent;
import com.sun.lwuit.layouts.BorderLayout;
import com.sun.lwuit.plaf.Border;
import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.VolumeControl;
import javax.microedition.midlet.*;
import com.toughra.mlearnplayer.datatx.MLObjectPusher;
import com.toughra.mlearnplayer.datatx.MLServerThread;
import com.toughra.mlearnplayer.idevices.EXERequestHandler;
import com.toughra.mlearnplayer.idevices.HTMLIdevice;
import com.toughra.mlearnplayer.xml.XmlNode;

//#ifdef NOKIA
//# import com.nokia.mid.ui.*;
//#endif 


/**
 * 
 * This is the main MIDLet that really runs the show of Ustad Mobil.  
 * 
 * @author mike
 */
public class MLearnPlayerMidlet extends MIDlet implements ActionListener, Runnable, PlayerListener{

    /**Base folder name for the learning package that the user is currently running*/
    public String currentPackageURI;

    /** The table of contents and idevice/href cache*/
    public EXETOC myTOC = null;

    /** The Table of contents form */
    private com.sun.lwuit.Form TOCForm = null;

    /**the id of the idevice that the user is currently on*/
    public String currentIdevice = null;
    
    /**The index of the current idevice in the page*/
    public int currentIdeviceIndex = -1;

    /*a reference to the idevice currently shown*/
    public Idevice currentIdeviceObj = null;

    /*the idevices on the current page*/
    protected String[] ideviceIdList;

    /*the titles that are on the current page*/
    private String[] ideviceTitleList;

    /** Href of the currently loaded page */
    public String currentHref;
    
    /**next page href to show*/
    public String nextHref;
    
    /*prev page href to show*/
    public String prevHref;
    
    /*the current collection id (as defined by packagefolder/execollectionid flat file*/
    public String currentColId;
    
    /*The current package ID - normally the folder name of the package*/
    public String currentPkgId;
    
    
    
    
    //the cached next and previous links for the url given by nextPrevLinksCachedForURL
    //public String[] cachedURLNextPrevLinks;
    /** Cached next and previous links*/
    public String nextPrevLinksCachedForURL;
    
   
    /** 
     * The ID of the cached idevice (e.g. next device) if it has been cached,
     * otherwise null
     */
    public String cachedIdeviceId;
    
    

    /** Media player object to be used*/
    private Player player;
    
    /** InputStream used for the player as required*/
    private InputStream playerInputStream;
    
    /** The browse form used to select the first piece of content*/
    public ContentBrowseForm contentBrowser = null;
    
    /** The main options menu */
    MLearnMenu menuFrm;
    
    /**the current idevice form*/
    Form deviceForm;
    
    /** The form showing right now (be it TOC,device, or collection) */
    public Form curFrm;
    
    /**Default time to show feedback (in ms)*/
    public int fbTime = 3500;
    
    /** The length of the currently playing media*/
    public long currentMediaLength = -1;
    
    /** Default shortcut keycode - for next (*) button */
    public static final int KEY_NEXT = 42;
    
    /** Default shortcut keycode - for previous (#) button */
    public static final int KEY_PREV = 35;
    
    /** Command ID for going next HREF*/
    final int NEXT_HREF = 0;
    
    /** Command ID for going to next idevice*/
    final int NEXT_DEVICEID = 1;
    
    /** String used with method to indicate we should open the first idevice
     * in a package*/
    final String firstDevice = ":FIRST";
    
    /** String used with method to indicate we should open the last idevice
     * in a package
     */
    final String lastDevice = ":LAST";
    
    /*
     * The preprocessor = if the preprocessor is media capable set to the static
     * variable as required.  If this is set to false then any calls to play
     * media will be ignored.
     */
    //#ifdef MEDIAENABLED
//#    public static final boolean mediaEnabled = true;
    //#else
    public static final boolean mediaEnabled = false;
    //#endif
    
    /** Array of sound file names that can be used for positive feedback*/
    public String[] posFbURIs;
    
    /** Array of sound file names that can be used for negative feedback*/
    public String[] negFbURIs;
    
    /**time for maintenance thread to sleep*/
    final int tickTime = 2000;
    
    /**transition time for animated stuff - e.g. slide to slide*/
    public final int transitionTime = 1000;
    
    /** time to wait before/after transition before starting an idevice*/
    public final int transitionWait = 100;
    
    /** currently unused */
    public NavigationActionListener navListener;
    
    /** controls main run thread - should almost always be true*/
    boolean running = true;
    
    /** the main volume - used when instantiating players (from 0-100) */
    public int volume = 80;
    
    /** Animated image for a loading icon*/
    Image loadingImg;
    
    /** Dialog to house animated loading icon*/
    Dialog loadingDialog;
    
    /** Control if the debug log is enabled */
    boolean debugLogEnabled = true;
    
    /** Not really used now*/
    String debugLogFile = "log.txt";
    /** Not really used now*/
    PrintStream debugStrm;
    
    /** Localization resource */
    public Resources localeRes;
    /** Hashtable of keys to lookup to find string*/
    public Hashtable localeHt;
    
    /** Reference to self*/
    static MLearnPlayerMidlet hostMidlet;
    
    /**the thread running to do bluetooth push to teacher phone*/
    MLObjectPusher pushThread;
    
    /**whether or not to just open the next chapter's first page right away*/
    boolean autoPageOpen = true;
    
    /**whether we have already done an auto open on return*/
    boolean returnPosDone = false;
    
    public static final String versionInfo = "V: 0.9.02 (16/June/2013)";
    
    /**
     * Not really used - using EXEStrMgr instead
     * @see EXEStrMgr
     * 
     * @param msg 
     */
    public void logMsg(String msg) {
        System.out.println(msg);
        /*if(debugStrm == null && currentPackageURI != null && currentPackageURI.length() > 8) {
                OutputStream out = null;
                try {
                    String printOutDest = currentPackageURI + "/" + debugLogFile;
                    System.out.println("Attempt to open for writing debug log: " + printOutDest);
                    FileConnection fileCon = (FileConnection)Connector.open(printOutDest, 
                            Connector.READ_WRITE);
                    if(fileCon.exists()) {
                        fileCon.truncate(0);
                    }else {
                        fileCon.create();
                    }
                    
                    out = fileCon.openOutputStream();
                }catch (Exception e) {
                    e.printStackTrace();
                }
                debugStrm = new PrintStream(out);
        }
        
        if(debugStrm != null) {
            debugStrm.println(msg);
            debugStrm.flush();
        }
        * 
        */
    }
    
    /**
     * Give a reference to self
     * @return the running MIDlet
     */
    public static MLearnPlayerMidlet getInstance() {
        return hostMidlet;
    }
    
    /**
     * startApp will:
     *  Load theme (/theme2.res from jar file) - see build.xml
     *  Load locale res (/localisation.res from jar file) - see build.xml
     *  Setup forms and action listeners for them
     *  Display the ContentBrowseForm to allow the user to select which learning
     *  package they want to start with.
     */
    public void startApp() {
        hostMidlet = this;//uses for getInstance

        com.sun.lwuit.Display.init(this);
        navListener = new NavigationActionListener(this);
        EXEStrMgr mgr = EXEStrMgr.getInstance(this);
        
        //check to see about running the teacher server
        MLServerThread.getInstance().checkServer();
                
        try {
            Display.getInstance().setBidiAlgorithm(true);
            Resources r = Resources.open("/theme2.res");
            UIManager.getInstance().setThemeProps(r.getTheme("Makeover"));

            EXEStrMgr.getInstance().p("Ustad Mobile version: " + this.versionInfo, EXEStrMgr.DEBUG);
            EXEStrMgr.getInstance().p("Loaded theme", 1);
            
            //setup locale management
            EXEStrMgr.getInstance().initLocale();
            
            loadingImg = r.getImage("loadingb64");
        }catch(Exception e) {
            EXEStrMgr.po(e, "Exception doing init loading");
        }
        
        menuFrm = new MLearnMenu(this);
        myTOC = new EXETOC(this);
        
        currentPackageURI = "/mxml1";
        
        contentBrowser = new ContentBrowseForm(this);
        contentBrowser.makeForm();
        contentBrowser.show();
        
        pushThread = new MLObjectPusher();
        pushThread.start(); 
        
        /*
         * This is Nokia specific code that is used to stop the lights from dimming
         */
        //#ifdef NOKIA
//#             DeviceControl.setLights(0, 100);
//#     new Thread(this).start();
        //#endif
    }
    
    /**
     * Set the volume being used, and if a player is running right now, update
     * the volume for it
     * @param vol int from 0-100 for volume
     */
    public void setVolume(int vol) {
        this.volume = vol;
        if(this.player != null) {
            int state = player.getState();
            if(state != player.UNREALIZED && state != player.CLOSED) {
                ((VolumeControl)player.getControl("VolumeControl")).setLevel(vol);
            }
        }
    }
    
    /**
     * the main run thread - only sleeps and wakes up the lights for Nokias
     */
    public void run() {
        while(running) {
            try {Thread.sleep(tickTime); }
            catch(InterruptedException e) {}
            //#ifdef NOKIA
//#         DeviceControl.setLights(0, 100);
            //#endif
        }
    }
    
    /**
     * Opens a package (folder containing exetoc.xml) and shows the user the
     * list of pages in it if showTOC is true
     * 
     * @param dirName - The dirname containing the exetoc.xml file
     * @param showTOC - True if we should show table of contents (default) false otherwise
     */
    public void openPackageDir(String dirName, boolean showTOC) {
        //contentBrowser = null;
        currentPackageURI = dirName.substring(0, dirName.length() - 1);
        int slashPos = currentPackageURI.lastIndexOf('/', dirName.length() -1);
        currentPkgId = currentPackageURI.substring(slashPos+1);
        
        System.out.println("Package URI: " + currentPackageURI);
        if(showTOC) {
            showTOC();
        }
        
      
        logMsg("started");
    }
    
    /**
     * Overloaded function
     * 
     * @param dirName - The dirname containing the exetoc.xml file
     */
    public void openPackageDir(String dirName) {
        openPackageDir(dirName, true);
    }
    
    
    /**
     * Opens a collection dir (containing execollection.xml) .
     * If autoPageOpen is true then show the last page that the learner
     * was using from before.
     * 
     * @param dirName 
     */
    public void openCollectionDir(String dirName) {
        //dirname comes with trailing /
        stopCurrentIdevice();
        myTOC.colBaseHREF = dirName.substring(0, dirName.length() - 1);
        myTOC.readCollection(dirName + "execollection.xml");
        Form colForm = myTOC.getCollectionForm();
        curFrm = colForm;
        currentColId = MLearnUtils.getCollectionID(dirName);
        colForm.show();
        
        //check and see if there is a saved last position
        String lastPos = EXEStrMgr.getInstance().getPref("lastpage."
                + currentColId);
        if(lastPos != null && returnPosDone == false) {
            //we need to go to where they last were...
            EXEStrMgr.po("Last position attempting to open " + lastPos, EXEStrMgr.DEBUG);
            try {
                returnPosDone = true;
                int slashPos = lastPos.indexOf('/');
                String lastPkg = lastPos.substring(0, slashPos);
                String lastHref = lastPos.substring(slashPos + 1);

                String pkgDir = myTOC.colBaseHREF + "/" + lastPkg + "/";
                hostMidlet.openPackageDir(pkgDir, false);
                loadTOC(true);

                hostMidlet.loadPage(lastHref);
            }catch(Exception e) {
                EXEStrMgr.po("Error attempting to load last page : " + e.toString(), EXEStrMgr.DEBUG);
            }
        }
        returnPosDone = true;
        
    }
    
    
    /**
     * Utility method that could be used to request opening a FileConnection
     * resource a few times.
     * 
     * @param URL URL to pass to the FileConnection object
     * @return the InputStream once its open
     * @throws IOException 
     */
    public InputStream retryGetFile(String URL) throws IOException {
        IOException lastException = null;
        int maxTries = 3;
        int retryWait = 200;
        for(int attmpt = 0; attmpt < maxTries; attmpt++) {
            try {
                InputStream is = Connector.openInputStream(URL);
                return is;
            }catch(IOException e) {
                e.printStackTrace();
                logMsg("Exception attempting to open fs attempt " + attmpt + ": " + e.toString());
                try {Thread.sleep(retryWait ); }
                catch(InterruptedException ie) {}
                System.gc();
                lastException = e;
            }
        }
        throw lastException;
    }
    
    
    /**
     * Returns an input stream by checking the URL.  
     * URL starts file:// - will use IOWatcher to preload the file contents
     *  if this is not a media file ending with mp3, 3gp or wav
     * If URL starts file:// and is a media file will return FileConnection.openInputStream
     * If URL starts with / will return getClass().getResourceAsStream
     * Else will interpret as a relative link, prefix the currentPackageURI, and apply
     * the same logic as if for file://
     * 
     * @param URL URL as above
     * @return InputStream for the URL
     * @throws IOException if there is an IOException dealing with the file system
     */
    public InputStream getInputStreamReaderByURL(String URL) throws IOException{
        boolean isMedia = URL.endsWith("mp3") || URL.endsWith("3gp") || URL.endsWith("wav") || URL.endsWith("mpg");
        
        EXEStrMgr.po("Opening inputstream for " + URL, EXEStrMgr.DEBUG);
        
        if(URL.startsWith("file://")) {
            if(isMedia) {
                //logMsg("Opening " + URL + " Direct");
                return Connector.openInputStream(URL);
            }else {
                //logMsg("Opening " + URL + " through Byte Array");
                return IOWatcher.makeWatchedInputStream(URL);
            }
        }else if (URL.startsWith("/")) {
            return getClass().getResourceAsStream(URL);
        }else {
            //this is a relative link
            System.out.println("Opening File" + URL);
            if(isMedia) {
                //logMsg("Opening " + URL + " direct");
                return Connector.openInputStream(currentPackageURI + "/" + URL);
            }else {
                //logMsg("Opening " + URL + " through Byte Array");
                return IOWatcher.makeWatchedInputStream(currentPackageURI + "/" + URL);
            }
        }
    }
    
    /**
     * Overload of makeHTMLComponent (String, HTMLCallback)
     * 
     * 
     * @param htmlStr
     * @return 
     */
    public HTMLComponent makeHTMLComponent(String htmlStr) {
        return makeHTMLComponent(htmlStr, null);
    }
    
    /**
     * This makes an HTMLComponent with a given HTML String.  It will use a 
     * RequestHandler so that images etc. will be loaded from the current
     * package path.
     * 
     * If the package is running Right to Left then a dir tag will be set by
     * the RequestHandler to make the HTML right to left.
     * 
     * @param htmlStr HTML String to show (do not include &ltbody&gt; &lt;html&gt; etc.
     * @param callback HTMLCallback object if desired.  Otherwise leave null
     * 
     * @return HTMLComponent that will display the given HTML code.
     */
    public HTMLComponent makeHTMLComponent(String htmlStr, HTMLCallback callback) {
        HTMLComponent htmlComp = new HTMLComponent(new EXERequestHandler(this, 
                htmlStr));
        htmlComp.setIgnoreCSS(false);
        htmlComp.setShowImages(true);
        Font bodyFont = Font.getBitmapFont("bodyFont");
        Font titleFont = Font.getBitmapFont("titleFont");
        htmlComp.setDefaultFont("arial.12", bodyFont);
        //HTMLComponent.addFont("arial.60", bodyFont);
        if(callback != null) {
            htmlComp.setHTMLCallback(callback);
        }
        
        htmlComp.setPage("idevice://current/");
        
        return htmlComp;
    }
    
    /**
     * Shows the main options menu (continue, repeat, about, settings, etc)
     */
    public void showMenu() {
        menuFrm.show();
    }
    
    /**
     * See overloaded function
     */
    public void loadTOC() {
        loadTOC(false);
    }
    
    /**
     * Load the table of contents
     * 
     * @param hide - Do not show the table of contents form itself immediately.  Do not set curFrm
     * 
     */ 
    public void loadTOC(boolean hide) {
        myTOC.readList(currentPackageURI + "/exetoc.xml");
        
        //now check for feedback sounds
        try {
            FileConnection con = (FileConnection)Connector.open(currentPackageURI);
            String prefixes[] = new String[] { "exesfx_good", "exesfx_wrong" };
            String extsn = ".mp3";
            
            for(int i = 0; i < prefixes.length; i++) {
                int c = 0;
                Vector foundFiles = new Vector();
                try {
                    boolean moreFiles = true;
                    do {
                        try {
                            String filename = prefixes[i] + c + extsn;Enumeration e = con.list(filename, true);
                            if(e.hasMoreElements()) {
                                //means this file exists
                                foundFiles.addElement(filename);
                                c++;
                            }else {
                                moreFiles = false;
                            }
                        }catch(Exception e) {
                            moreFiles = false;
                        }
                    }while(moreFiles);
                }catch(Exception e) {
                    System.out.println("Exception occured looking for audio files");
                }finally {
                    if(foundFiles.size() > 0) {
                        if(prefixes[i].equals("exesfx_good")) {
                            posFbURIs = new String[foundFiles.size()];
                            foundFiles.copyInto(posFbURIs);
                        }else {
                            negFbURIs = new String[foundFiles.size()];
                            foundFiles.copyInto(negFbURIs);
                        }
                    }
                }
            }
            con.close();
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        TOCForm = myTOC.getForm();
        myTOC.getList().addActionListener(this);
        if(!hide) {
            curFrm = TOCForm;
        }
    }
    
    /**
     * Used if we are using a loading dialog (now loads pretty quickly, so
     * not really used).  Will remove the loading dialog and show the toc 
     * form
     */
    public void tocReady() {
        if(loadingDialog != null) {
            loadingDialog.setVisible(false);
            loadingDialog.dispose();
            loadingDialog = null;
        }
        TOCForm.show();
    }
    

    /**
     * Generates a loading dialog
     * @return Dialog with animated loading image
     */
    public Dialog makeLoadingDialog() {
        Dialog dlg = new Dialog();
        Button loadingButton = new Button(loadingImg);
        loadingButton.getStyle().setBorder(Border.createEmpty());
        loadingButton.getSelectedStyle().setBorder(Border.createEmpty());
        
        dlg.addComponent(loadingButton);
        
        return dlg;
    }
    
    /**
     * Show table of contents form
     */
    public void showTOC() {
        stopCurrentIdevice();
        
        //show the loading screen
        
        loadingDialog = makeLoadingDialog();
        loadingDialog.showPacked(BorderLayout.CENTER, false);
        
        Thread loadThread = new Thread(new Runnable() {
            public void run() {
                loadTOC();
                Display.getInstance().callSeriallyAndWait(new Runnable() {
                   public void run() {
                       tocReady();
                   } 
                });
            }
        });
        
        loadThread.start();
        
    }

    /**
     * Load the specified page - show the first idevice in the list
     * 
     * @param pageHref href to load (e.g. pagename.xml)
     */
    public void loadPage(String pageHref) {
        loadPage(pageHref, false);
    }
    
    /**
     * Loads a given page.  If goLast is true, show the last idevice (e.g. reversing)
     * Otherwise - show the first idevice
     * 
     * @param pageHref href to laod - eg. pagename.xml
     * @param goLast if true show the last idevice on the page, otherwise show the first
     */
    public void loadPage(String pageHref, boolean goLast) {
        String myHref = (pageHref != null ? pageHref : this.currentHref);
        Object[] pageIdeviceInfo = myTOC.getPageIdeviceList(currentPackageURI + "/exetoc.xml",
                myHref);
        ideviceIdList = (String[])pageIdeviceInfo[EXETOC.PAGELIST_IDEVICEIDS];
        ideviceTitleList = (String[])pageIdeviceInfo[EXETOC.PAGELIST_IDEVICETITLES];
        TOCCachePage cPg = myTOC.cache.getPageByHref(myHref);
        this.nextHref = cPg.nextHref;
        this.prevHref = cPg.prevHref;                

        int index = (goLast == false) ? 0 : ideviceIdList.length -1;
        EXEStrMgr.po("Showing idevice for page " + pageHref + " : " + index, EXEStrMgr.DEBUG);

        if(ideviceIdList.length == 0) {
            //this needs to show an idevice (blank html or something should do)
            Idevice blankHTMLDevice = new HTMLIdevice(this, " ");
            showIdevice(blankHTMLDevice, null);
        }else {
            showIdevice(myHref, ideviceIdList[index]);
        }
        
        currentHref = myHref;
    }
    
 
    
    /**
     * Utility method that makes sure a stream gets opened as UTF-8
     * 
     * @param URL to open
     * @return InputStreamReader that will read UTF8
     * 
     * @throws IOException 
     */
    public InputStreamReader getUTF8StreamForURL(String URL) throws IOException {
        InputStreamReader reader = new InputStreamReader(this.getInputStreamReaderByURL(URL),
                "UTF-8");
        return reader;
    }
    
    /**
     * Get info about what is the next device to load
     * @param increment - how many idevices to go back/forth
     * 
     * @return 
     */
    public String[] getNextIDeviceIdAndHref(int increment) {
        String[] retValue = new String[2];
        int nextIdeviceIndex = currentIdeviceIndex + increment;
        
        //TODO: Change all "href" in tags to have .xml in it - confusing as heck otherwise
        if(nextIdeviceIndex >= 0 && nextIdeviceIndex < ideviceIdList.length) {
            retValue[NEXT_HREF] = this.currentHref;
            retValue[NEXT_DEVICEID] = ideviceIdList[this.currentIdeviceIndex + increment];
        }else if(nextIdeviceIndex >= ideviceIdList.length && this.nextHref != null) {
            retValue[NEXT_DEVICEID] = firstDevice;
            retValue[NEXT_HREF] = this.nextHref;
        }else if (nextIdeviceIndex < 0 && this.prevHref != null) {
            retValue[NEXT_DEVICEID] = lastDevice;
            retValue[NEXT_HREF] = this.prevHref;
        }
        
        return retValue;
    }
    
    /**
     * 
     * Goes increment devices along to show the next (or previous) idevice
     * @param increment how many idevices to move along (can be +ve/-ve)
     * 
     */
    public void showNextDevice(int increment) {
        stopCurrentIdevice();
        int nextIdeviceIndex = currentIdeviceIndex + increment;
        if(nextIdeviceIndex >= 0 && nextIdeviceIndex < ideviceIdList.length) {
            String nextIdeviceId = ideviceIdList[this.currentIdeviceIndex + increment];
            showIdevice(this.currentHref,nextIdeviceId);
        }else if(nextIdeviceIndex >= ideviceIdList.length && this.nextHref != null) {
            // go to the next page
            loadPage(this.nextHref);
        }else if (nextIdeviceIndex < 0 && this.prevHref != null) {
            loadPage(this.prevHref, true);
        }else {
            if(myTOC.collection != null) {
                EXEStrMgr.po("Package to package nav starting ", EXEStrMgr.DEBUG);
                int currentColIndex = myTOC.getCollectionIndex(currentPackageURI);
                int nextColIndex = currentColIndex;
                
                if(nextIdeviceIndex >= ideviceIdList.length && this.nextHref == null) {    
                    if(currentColIndex < (myTOC.collection.length -1)) {
                        nextColIndex = currentColIndex + 1;
                    }
                }else if(nextIdeviceIndex <= 0 && this.prevHref == null) {
                    if(currentColIndex >= 1) {
                        nextColIndex = currentColIndex -1;
                    }
                }
                
                if(nextColIndex != currentColIndex) {
                    currentPackageURI = myTOC.getColBaseHref(nextColIndex);
                    if(currentPackageURI.endsWith("/")) {
                        currentPackageURI = currentPackageURI.substring(0, currentPackageURI.length() -1);
                        EXEStrMgr.po("Set current package URI to " + currentPackageURI, EXEStrMgr.DEBUG);
                    }
                    myTOC.tocList = null;
                    
                    if(autoPageOpen) {
                        EXEStrMgr.po("auto page loading " + currentPackageURI, EXEStrMgr.DEBUG);
                        loadTOC(true);
                        boolean goLast = false;
                        String pageURL = null;
                        if(increment > 0) {
                             pageURL = myTOC.getPageHref(0);
                        }else {
                            pageURL = myTOC.getPageHref(myTOC.getNumPages()-1);
                            goLast = true;
                        }
                        EXEStrMgr.po("page url to be " + pageURL, EXEStrMgr.DEBUG);
                        
                        loadPage(pageURL, goLast);
                    }else {
                        showTOC();
                    }
                }
            }else {
                //if we have no collection to show and we're at the end go to 
                //table of contents.
                showTOC();
            }
        }
    }

    
    /**
     * Stop the currently running idevice
     */
    void stopCurrentIdevice() {
        if(currentIdeviceObj != null) {
            currentIdeviceObj.stop();
            EXEStrMgr.getInstance().l("", currentIdeviceObj);
            
            currentIdeviceObj.dispose();
            currentIdeviceObj = null;
            
            stopMedia(false);
            System.gc();
        }
    }
    
    /**
     * Show the idevice given by the device object
     * 
     * @param device - The idevice to show
     * @param ideviceId - the deviceid
     */
    public void showIdevice(Idevice device, String ideviceId) {
        try {
            
            if(device.getMode() == Idevice.MODE_LWUIT_FORM) {
                if(!Display.isInitialized()) {
                    Display.init(this);
                }
                
                MLearnUtils.checkFreeMem();
                
                deviceForm = device.getForm();
                curFrm = deviceForm;

                MLearnUtils.printFreeMem(this, "Got form from " + device.getClass().getName());
                
                //this is hear when we aren't really strictly showing a new idevice (e.g.
                // the slideshow device is just changing slides
                if(ideviceId != null) {
                    this.currentIdevice = ideviceId;
                    for (int i = 0; i < ideviceIdList.length; i++) {
                        if(ideviceIdList[i].equals(ideviceId)) {
                            currentIdeviceIndex = i;
                            break;
                        }
                    }
                }

                //adds the command to the form for menu items
                navListener.addMenuCommandsToForm(deviceForm);
                
                //Add listeners for navigation purposes
                deviceForm.addKeyListener(KEY_NEXT, this);
                deviceForm.addKeyListener(KEY_PREV, this);
                
                deviceForm.setTransitionOutAnimator(CommonTransitions.createSlide(
                            CommonTransitions.SLIDE_HORIZONTAL, true, transitionTime));
                EXEStrMgr.po("Showing idevice form for " + ideviceId, EXEStrMgr.DEBUG);
                
                //save where we are if we are using a collection
                deviceForm.show();
                
                MLearnUtils.printFreeMem(this, "show " + device.getClass().getName());
                

                //tell the device to start anything that it wants to run - media -etc
                device.start();
                MLearnUtils.printFreeMem(this, "startedr " + device.getClass().getName());
                currentIdeviceObj = device;
            }
        }catch(Exception e) {
            e.printStackTrace();
            Dialog errorDialog = new Dialog("Error showing idevice");
            String errorMessage = e.toString();
            logMsg("Exception showing idevice: " + errorMessage);
            if(e instanceof IOException) {
                IOException ioe = (IOException)e;
                errorMessage += "::" + ioe.getMessage();
            }else if(e instanceof RuntimeException) {
                RuntimeException re = (RuntimeException)e;
            }
            EXEStrMgr.po("Exception show device: " + e.toString(), EXEStrMgr.WARN);
            errorDialog.addComponent(new TextArea(errorMessage));
            errorDialog.show();
            
        }
    }
    
    /**
     * Show the idevice given by the id which is expected to be contained in
     * the file given by pageHREF.  Will search through the idevices given
     * and then instantiate an idevice by using the IdeviceFactory
     * 
     * @param pageHREF URL of page to load 
     * @param ideviceId deviceId to open
     */
    public void showIdevice(String pageHREF, String ideviceId) {
        stopCurrentIdevice();
        try {
            EXEStrMgr.po("Loading idevice now for " + pageHREF + ":" + ideviceId, EXEStrMgr.DEBUG);
            InputStream inStream = getInputStreamReaderByURL(currentPackageURI + "/"
                    + pageHREF);
            EXEStrMgr.po("got inputstream " + pageHREF + ":" + ideviceId, EXEStrMgr.DEBUG);
            
            XmlNode cachedData =  null;
            EXEStrMgr.po("Calling idevice factory" + pageHREF + ":" + ideviceId, EXEStrMgr.DEBUG);
            
            Idevice device = IdeviceFactory.makeIdevice(inStream, ideviceId, this, cachedData);
            EXEStrMgr.po("Generated idevice object " + pageHREF + ":" + ideviceId, EXEStrMgr.DEBUG);
            
            //Save the current location of the student
            if(currentColId != null && pageHREF != null && this.currentHref != null) {
                String lastURLRecordVal = this.currentPkgId + "/" + pageHREF;
                EXEStrMgr.getInstance().setPref("lastpage."
                        + currentColId, lastURLRecordVal);
                EXEStrMgr.po("Recorindg lastpage." + currentColId + " as " + lastURLRecordVal,
                        EXEStrMgr.DEBUG);
            }

            
            showIdevice(device, ideviceId);
            EXEStrMgr.po("Loading complete for idevice now for " + pageHREF + ":" + ideviceId, EXEStrMgr.DEBUG);
            
        }catch(Exception e) {
            EXEStrMgr.po(e, "exception whilst loading idevice ");
        }
    }


    /**
     * Main event handler - looks out for selections of a package from the current
     * table of contents list and when the user pushes the next or previous
     * buttons (default * and #)
     * @param evt 
     */
    public void actionPerformed(ActionEvent evt) {
        Object src = evt.getSource();
        String pageHref = null;
        if(src instanceof List) {
            List tocList = (List)src;
            int currentIndex = tocList.getSelectedIndex();
            pageHref = myTOC.getPageHref(currentIndex);
            myTOC.getList().removeActionListener(this);
            
            
            //System.gc();
            currentHref = pageHref;
            loadPage(pageHref);
        }else if(src instanceof Form) {
            int key = evt.getKeyEvent();
            if(key == KEY_NEXT) {
                showNextDevice(1);
            }else if(key == KEY_PREV) {
                showNextDevice(-1);
            }
        }
        
        //find out what

        int x = 0;
    }


    /**
     * Pause app - does nothing
     */
    public void pauseApp() {
    }

    /**
     * Ends the application - saves all preferences, logs, and stops any
     * server threads running
     * 
     * @param unconditional 
     */
    public void destroyApp(boolean unconditional) {
        MLObjectPusher.enabled = false;//make this thread quite
        EXEStrMgr.getInstance().saveAll();
    }

    /**
     * Stops any media currently playing - and runs garbage collect after
     */
    public void stopMedia() {
        stopMedia(true);
    }
    
    /**
     * Stops the media currently playing.  Closes and deallocates player.
     * 
     * @param doAutoGc If set true will call System.gc() after stopping media
     */
    public void stopMedia(boolean doAutoGc) {
        if(player != null && mediaEnabled == true) {
            try{
                MLearnUtils.checkFreeMem();
                player.stop();
                player.close();
                player.deallocate();
                player = null;
                logMsg("Played deallocated by stopmedia method");
                MLearnUtils.printFreeMem(this, "Stopped Media before gc");
            }catch(Exception e) {
                e.printStackTrace();
            } finally {
                if(doAutoGc) { 
                    System.gc(); 
                    MLearnUtils.printFreeMem(this, "Stopped Media after gc");
                }
            }
        }
    }

    /**
     * Utility method used with player - returns mime type for files ending
     * .mp3, .au, .wav, .3gp, .mpg
     * 
     * @param fileName
     * @return 
     */
    public static String getContentType(String fileName) {
        String fileNameLower = fileName.toLowerCase();
        if(fileNameLower.endsWith(".mp3")) {
            return "audio/mp3";
        }else if(fileNameLower.endsWith(".au")) {
            return "audio/basic";
        }else if(fileNameLower.endsWith(".wav")) {
            return "audio/X-wav";
        }else if(fileNameLower.endsWith(".mpg")) {
            return "video/mpeg";
        }else if(fileNameLower.endsWith(".3gp")) {
            return "video/3gpp";
        }


        //not known
        return null;
    }
    
    /**
     * Provides the current package uri base directory
     * @return Current package URI base directory
     */
    public String getCurrentPackageURI() {
        return currentPackageURI;
    }
    
    /**
     * Play the default sound that indicates a positive (e.g. correct answer)
     * event
     * 
     * @return the time of the sound in microseconds
     */
    public long playPositiveSound() {
        System.out.println("Play +ve");
        if(posFbURIs != null){
            Random r = new Random();
            return playMedia(posFbURIs[r.nextInt(posFbURIs.length)]);
        }
        
        return -1;
    }
    
    /**
     * Play the default negative sound
     * 
     * @return length of the sound in microseconds
     */
    public long playNegativeSound() {
        System.out.println("Play -ve");
        if(negFbURIs != null){
            Random r = new Random();
            return playMedia(negFbURIs[r.nextInt(negFbURIs.length)]);
        }
        
        return -1;
    }
    

    /**
     * Plays a sound file.  Will only run if mediaEnabled is set to true.
     * 
     * @param mediaURI URI of file to play e.g. file://localhost/file/foo.mp3
     * 
     * @return the length of the clip in microseconds
     */
    public long playMedia(String mediaURI) {
        stopMedia();
        MLearnUtils.checkFreeMem();
        System.out.println("asked to play: " + mediaURI);
        logMsg("asked to play: " + mediaURI);
        String mediaType = getContentType(mediaURI);
        mediaURI = currentPackageURI + "/" + mediaURI;
        
            String mediaLoc = MLearnUtils.connectorToFile(mediaURI);
        
        
        if(mediaEnabled) {
            try {
                //playerInputStream = getInputStreamReaderByURL(mediaURI);
                
                //player = Manager.createPlayer(playerInputStream, mediaType);
                
                
                logMsg("Creating player for " + mediaLoc);
                player = Manager.createPlayer(mediaLoc);
                player.realize();
                VolumeControl vc = (VolumeControl)player.getControl("VolumeControl");
                vc.setLevel(volume);
                player.start();
                long playerTime = player.getDuration();
                this.currentMediaLength = playerTime;
                player.addPlayerListener(this);
                MLearnUtils.printFreeMem(this, "Created player for " + mediaLoc);
                return playerTime;
            }catch(Exception e) {
                Form errorForm = new Form("Error creating player" + mediaLoc + " : " + mediaType);
                logMsg(e.toString() + " error for player " + mediaLoc);
                Label label = new Label(e.toString());
                errorForm.addComponent(label);
                errorForm.show();
                e.printStackTrace();
            }
        }else {
            System.out.println("Media disabled");
        }

        this.currentMediaLength = (1*1000*1000);
        //wait a second
        return (1*1000*1000);
    }

    /**
     * Automatically deallocate resources once the media file itself is finished
     * 
     * @param player The media player
     * @param event event string given by PlayerListener
     * @param eventData info about event
     */
    public void playerUpdate(Player player, String event, Object eventData) {
        logMsg("Player Update Event : '" + event + "'");
        if(event.equals(PlayerListener.END_OF_MEDIA)) {
            logMsg("Found end of Media");
            if(this.player != null) {
                if(this.player.getState() != Player.CLOSED && this.player.getState() != Player.UNREALIZED) {
                    logMsg("Attempting to stop player");
                    try { this.player.stop(); }
                    catch(Exception e) {
                        logMsg("exception attempting to stop player: " + e.toString());
                        e.printStackTrace();
                    }
                    try { Thread.sleep(300); } 
                    catch(InterruptedException e) {}
                }
                
                logMsg("Attempting to close and deallocate player");                
                this.player.close();
                
                
                logMsg("Attempting to deallocate now");
                
                try {
                    this.player.deallocate();
                }catch(Exception e) {
                    e.printStackTrace();
                    logMsg("Exception trying to deallocate player: " + e.toString());
                }
                this.player = null;
                
                try { Thread.sleep(500); }
                catch(InterruptedException e) {}
                
                
                logMsg("Player closed / deallocated by playerUpdated method");
            }else{
                logMsg("looked and player is null");
            }
            
            if(playerInputStream != null) {
                try { 
                    logMsg("Attempting to close playerInputStream");
                    playerInputStream.close(); 
                    logMsg("player input stream closed by playerUpdated method");
                } catch(IOException e) {
                    e.printStackTrace();
                }
                playerInputStream = null;
                
            }else {
                logMsg("PlayerInputStream is already null");
            }
        }
    }

    
    
}

