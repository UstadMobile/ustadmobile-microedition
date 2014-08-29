/*
 * Ustad Mobile Micro Edition 
 * Copyright 2011-2014 UstadMobile Inc
 * www.ustadmobile.com
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
import org.json.me.JSONException;
import org.json.me.JSONObject;



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
    
    /** The system clock time when activity on the current page started */
    public long currentPageStartTime = 0;
            
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
    
    public static final String versionInfo = "0.9.9b";
    
    /** Set the RTL Mode on the basis of the package language */
    public static final int RTLMODE_PACKAGE = 0;
    
    /** Set the RTL Mode on the basis of the settings for the app */
    public static final int RTLMODE_SETTINGS = 1;
    
    /** Decide on RTL on the basis of settings language or package language */
    public static int rtlMode = RTLMODE_SETTINGS;
    
    /** If an idevice needs some to be focused after a form is shown - set me here*/
    public Component focusMeAfterFormShows;
    
    /** The main xAPI server that we talk to for all operations - _MUST_ include port hostname:port only*/
    //#ifndef SERVER
    public final static String masterServer = "svr2.ustadmobile.com:8007";
    //#endif
    
    /** The main Course and Cloud server that we talk to for all operations - _MUST_ include port hostname:port only*/
    //#ifndef SERVER
    public final static String cloudServer = "umcloud1.ustadmobile.com:8010";
    //#endif
    
    //#ifdef SERVER
    //#expand public final static String masterServer = "%SERVER%";
    //#endif
    
    //#ifndef LOGINSKIP
    public final static boolean canSkipLogin = true;
    //#endif
    
    /** If server login skip is allowed or not */
    //#ifdef LOGINSKIP
    //#expand public final static boolean canSkipLogin = %LOGINSKIP%;
    //#endif
    
    /** Controls if it is possible to navigate or not - e.g. 
     * when System.currentTime - lastNavTime < transitionTime do nothing
     */
    long lastNavTime = 0;
    
    /** Lock to use for thread safety purposes */
    Object navigateLock = new Object();
    
    
    /**
     * Not really used - using EXEStrMgr instead
     * @see EXEStrMgr
     * 
     * @param msg 
     */
    public void logMsg(String msg) {
        System.out.println(msg);
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

        
        EXEStrMgr mgr = EXEStrMgr.getInstance(this);
        
        EXEStrMgr.lg(10, "Log file opened by Ustad Mobile ");
        EXEStrMgr.writeDebugInfo();
        
        navListener = new NavigationActionListener(this);
        
        
        //check to see about running the teacher server
        MLServerThread.getInstance().checkServer();
                
        try {
            ///make sure that we are doing TextFields correctly
            ServerLoginForm.setTextFieldDefaults();
            
            Display.getInstance().setBidiAlgorithm(true);
            Resources r = Resources.open("/theme2.res");
            UIManager.getInstance().setThemeProps(r.getTheme("Makeover"));

            EXEStrMgr.lg(10, "Ustad Mobile version: " 
                    + MLearnPlayerMidlet.versionInfo);
            EXEStrMgr.lg(11, "Loaded theme");
            
            //setup locale management
            EXEStrMgr.getInstance().initLocale();
            EXEStrMgr.lg(11, "Loaded locale");
            
            //#ifdef CRAZYDEBUG
//#             EXEStrMgr.lg(69, "Loading base image");
            //#endif
            loadingImg = r.getImage("loadingb64");
            
            //#ifdef CRAZYDEBUG
//#             EXEStrMgr.lg(69, "Loaded base image");
            //#endif
        }catch(Exception e) {
            EXEStrMgr.lg(310, e.toString());
        }
        
        menuFrm = new MLearnMenu(this);
        
        //#ifdef CRAZYDEBUG
//#         EXEStrMgr.lg(69, "Instantiated Menu");
        //#endif
        
        myTOC = new EXETOC(this);
        
        EXEStrMgr.lg(11, "Created menu and table of contents");
        
        currentPackageURI = "/mxml1";
        
        
        //TODO: If not logged in show login form
        String autoOpenContentItem = System.getProperty("com.ustadmobile.packagedir");
        
        if(EXEStrMgr.getInstance().getCloudUser() == null && autoOpenContentItem == null) {
            showLoginForm();
            EXEStrMgr.lg(11, "Showed login form");
        }else {
            contentBrowser = new ContentBrowseForm(this);
            contentBrowser.makeForm();
            contentBrowser.show();
            
            if(autoOpenContentItem != null) {
                String packageDir = contentBrowser.getPathForAutoOpenItem(autoOpenContentItem);
                openPackageDir(packageDir, true);
            }
            
            EXEStrMgr.lg(11, "Show content browser");
        }
        
        pushThread = new MLObjectPusher();
        pushThread.start(); 
        
        
        CompatibilityEngine.doDetection();
        
        /*
         * This is Nokia specific code that is used to stop the lights from dimming
         */
        if(CompatibilityEngine.nokiaUI) {
            com.nokia.mid.ui.DeviceControl.setLights(0, 100);
            new Thread(this).start();
        }
        
    }
    
    public void showLoginForm() {
        showLoginForm(false);
    }
    
    /**
     * 
     * @param forceLogout if true will nullify current cloud credentials
     */
    public void showLoginForm(boolean forceLogout) {
        if(forceLogout) {
            EXEStrMgr.getInstance().doCloudLogout();
        }
        ServerLoginForm loginForm = new ServerLoginForm(this);
        loginForm.addActionListener(this);
        loginForm.show();
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
            if(state != Player.UNREALIZED && state != Player.CLOSED) {
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
            if(CompatibilityEngine.isNokiaUI()) {
                //com.nokia.mid.ui.DeviceControl.setLights(0, 100);
            }
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
            EXEStrMgr.lg(12, "Last position attempting to open " + lastPos);
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
                EXEStrMgr.lg(311, "Error attempting to load last page : " + lastPos, e);
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
        boolean isMedia = URL.endsWith("mp3") || URL.endsWith("3gp") || URL.endsWith("wav") || URL.endsWith("mpg") || URL.endsWith("mp4");
        
        EXEStrMgr.lg(13, "Opening inputstream for " + URL);
        
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
     * @param menuItemsToShow array representing which items to show
     */
    public void showMenu(boolean[] menuItemsToShow) {
        menuFrm.updateFieldsFromPrefs();
        menuFrm.show(menuItemsToShow);
    }
    
    /**
     * Returns Menu
     * @return MLearnMenu in use
     */
    public MLearnMenu getMenu(){
        return menuFrm;
    }
    
    /*
     * Show the main options menu
     */
    public void showMenu() {
        menuFrm.updateFieldsFromPrefs();
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
     * Record the TinCan 
     * @param nextPageHref - The next page href that is to be shown.
     */
    public void recordPageTinCan(String nextPageHref) {
        //if going to a new page and we have a time record on the last page
        if(currentHref != null && !currentHref.equals(nextPageHref)) {
            long duration = System.currentTimeMillis() - currentPageStartTime;
            TOCCachePage cPg = myTOC.cache.getPageByHref(currentHref);
            JSONObject actorObj = EXEStrMgr.getInstance().getTinCanActor();
            if(actorObj != null && cPg != null) {
                String pageName = cPg.href.substring(0, cPg.href.lastIndexOf('.'));
                int slashPos = currentPackageURI.lastIndexOf('/', currentPackageURI.length() -1);
                String folderName = currentPackageURI.substring(slashPos+1);
                String prefix = MLearnUtils.TINCAN_PREFIX + "/" + folderName;
                JSONObject tinCanStmt = UMTinCan.makePageViewStmt(prefix, 
                        pageName, cPg.title, "en-US", duration, actorObj);
                EXEStrMgr.getInstance().queueTinCanStmt(tinCanStmt);   
            }
        }
        
        //page change is taking place or first page is being viewed
        if(currentPageStartTime == 0 || !currentHref.equals(nextPageHref)) {
            currentPageStartTime = System.currentTimeMillis();
        }
    }
    
    /**
     * For use with Idevices - get the currently playing pagename
     * 
     * @return folderName/Pagename
     */
    public String getTinCanPage() {
        String pageName = this.currentHref.substring(0, 
                this.currentHref.lastIndexOf('.'));
        int slashPos = currentPackageURI.lastIndexOf('/', 
                currentPackageURI.length() -1);
        String folderName = currentPackageURI.substring(slashPos+1);
        return folderName + "/" + pageName;
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
        recordPageTinCan(myHref);
        
        Object[] pageIdeviceInfo = myTOC.getPageIdeviceList(currentPackageURI + "/exetoc.xml",
                myHref);
        ideviceIdList = (String[])pageIdeviceInfo[EXETOC.PAGELIST_IDEVICEIDS];
        ideviceTitleList = (String[])pageIdeviceInfo[EXETOC.PAGELIST_IDEVICETITLES];
        TOCCachePage cPg = myTOC.cache.getPageByHref(myHref);
        this.nextHref = cPg.nextHref;
        this.prevHref = cPg.prevHref;                

        int index = (goLast == false) ? 0 : ideviceIdList.length -1;
        EXEStrMgr.lg(14, "Showing idevice for page " + pageHref + " : " + index);

        
        
        if(ideviceIdList.length == 0) {
            //this needs to show an idevice (blank html or something should do)
            Idevice blankHTMLDevice = new HTMLIdevice(this, " ");
            showIdevice(blankHTMLDevice, null);
        }else {
            showIdevice(myHref, ideviceIdList[index]);
        }
        
        
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
     * Checks to see if we should accept navigation commands (avoid issues with people pushing
     * buttons too fast)
     * 
     * @return true if ok to go, false otherwise
     */
    private final boolean canNavigateNow() {
        return (System.currentTimeMillis() - lastNavTime) > transitionTime;
    }
    
    /**
     * 
     * Goes increment devices along to show the next (or previous) idevice
     * @param increment how many idevices to move along (can be +ve/-ve)
     * 
     */
    public void showNextDevice(int increment) {
        if(!canNavigateNow()) {
            System.out.println("Blocking navigate because something already happening");
            return; 
        }
        
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
                EXEStrMgr.lg(15, "Package to package nav starting ");
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
                    currentPkgId = myTOC.getColPkgId(nextColIndex);
                    if(currentPackageURI.endsWith("/")) {
                        currentPackageURI = currentPackageURI.substring(0, currentPackageURI.length() -1);
                        EXEStrMgr.lg(16, "Set current package URI to " + currentPackageURI);
                    }
                    myTOC.tocList = null;
                    
                    if(autoPageOpen) {
                        loadTOC(true);
                        boolean goLast = false;
                        String pageURL = null;
                        if(increment > 0) {
                             pageURL = myTOC.getPageHref(0);
                        }else {
                            pageURL = myTOC.getPageHref(myTOC.getNumPages()-1);
                            goLast = true;
                        }
                        
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
        
        lastNavTime = System.currentTimeMillis();
        
    }

    
    /**
     * Stop the currently running idevice
     */
    void stopCurrentIdevice() {
        if(currentIdeviceObj != null) {
            currentIdeviceObj.stop();
            
            
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
                EXEStrMgr.lg(17, "Showing idevice form for " + ideviceId);
                
                //save where we are if we are using a collection
                deviceForm.show();
                if(focusMeAfterFormShows != null) {
                    Component currentFocus = deviceForm.getFocused();
                    deviceForm.setFocused(null);
                    deviceForm.setFocused(focusMeAfterFormShows);
                    focusMeAfterFormShows = null;
                }
                
                MLearnUtils.printFreeMem(this, "show " + device.getClass().getName());
                

                //tell the device to start anything that it wants to run - media -etc
                device.start();
                MLearnUtils.printFreeMem(this, "startedr " + device.getClass().getName());
                currentIdeviceObj = device;
            }
        }catch(Exception e) {
            e.printStackTrace();
            EXEStrMgr.lg(312, e.toString());
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
        
        currentHref = pageHREF;
        
        try {
            EXEStrMgr.lg(18, "Loading idevice now for " + pageHREF + ":" + ideviceId);
            InputStream inStream = getInputStreamReaderByURL(currentPackageURI + "/"
                    + pageHREF);
            XmlNode cachedData =  null;
            
            Idevice device = IdeviceFactory.makeIdevice(inStream, ideviceId, this, cachedData);
            
            //Save the current location of the student
            if(currentColId != null && pageHREF != null && this.currentHref != null) {
                String lastURLRecordVal = this.currentPkgId + "/" + pageHREF;
                EXEStrMgr.getInstance().setPref("lastpage."
                        + currentColId, lastURLRecordVal);
                EXEStrMgr.lg(19, "Recorindg lastpage." + currentColId + " as " + lastURLRecordVal);
            }

            
            showIdevice(device, ideviceId);
        }catch(Exception e) {
            EXEStrMgr.lg(313, "exception whilst loading idevice " + pageHREF + ":" + ideviceId + e.toString());
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
        }else if(src instanceof ServerLoginForm) {
            //dispose of the src
            contentBrowser = new ContentBrowseForm(this);
            contentBrowser.makeForm();
            contentBrowser.show();
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
        }else if(fileNameLower.endsWith(".mp4")) {
            return "video/mp4";
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
            return playMedia(posFbURIs[MLearnUtils.nextRandom(r, posFbURIs.length)]);
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
            return playMedia(negFbURIs[MLearnUtils.nextRandom(r, negFbURIs.length)]);
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
        
        //now determine which one to use...
        String newMediaURI = MLearnUtils.reworkMediaURI(myTOC.audioFormatToUse, 
                MLearnUtils.AUDIOFORMAT_NAMES, mediaURI);
        if(newMediaURI != null) {
            mediaURI = newMediaURI;
        }
        System.out.println("Actually going to play " + mediaURI);
        
        String mediaType = getContentType(mediaURI);
        
        
        
        mediaURI = currentPackageURI + "/" + mediaURI;
        
        String mediaLoc = MLearnUtils.connectorToFile(mediaURI);
        
        
        if(mediaEnabled) {
            try {
                //playerInputStream = getInputStreamReaderByURL(mediaURI);
                
                //player = Manager.createPlayer(playerInputStream, mediaType);
                
                
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
                EXEStrMgr.lg(314, "Error creating player for " + mediaLoc + 
                        " type " + mediaType + " " + e.toString());
                if(player != null) {
                    try {
                        player.deallocate();
                    }catch(Exception e1) {
                        EXEStrMgr.lg(314, "exception deallocating faulty player " + mediaLoc + 
                        " type " + mediaType + " " + e1.toString());
                    }
                    
                    try {
                        player.close();
                    }catch(Exception e2) {
                        EXEStrMgr.lg(314, "exception closing faulty player " + mediaLoc + 
                        " type " + mediaType + " " + e2.toString());
                    }
                }
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
        if(event.equals(PlayerListener.END_OF_MEDIA)) {
            if(this.player != null) {
                if(this.player.getState() != Player.CLOSED && this.player.getState() != Player.UNREALIZED) {
                    try { this.player.stop(); }
                    catch(Exception e) {
                        EXEStrMgr.lg(315, "exception attempting to stop player: " + e.toString());
                    }
                    try { Thread.sleep(300); } 
                    catch(InterruptedException e) {}
                }
                
                this.player.close();
                
                try {
                    this.player.deallocate();
                }catch(Exception e) {
                    e.printStackTrace();
                    EXEStrMgr.lg(315, "Exception deallocating player: " + e.toString());
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

