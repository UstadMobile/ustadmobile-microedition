package net.paiwastoon.mlearnplayer;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.plaf.UIManager;
import com.sun.lwuit.util.Resources;
import com.sun.lwuit.*;
import com.sun.lwuit.animations.CommonTransitions;
import com.sun.lwuit.html.HTMLCallback;
import com.sun.lwuit.html.HTMLComponent;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.microedition.io.Connector;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.midlet.*;
import net.paiwastoon.mlearnplayer.idevices.EXERequestHandler;
import net.paiwastoon.mlearnplayer.xml.XmlNode;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
//#ifdef NOKIA
 import java.util.TimeZone;
//#endif 

//#ifdef SAMSUNG
//@ import java.util.Stack;
//#endif

/**
 * @author mike
 */
public class MLearnPlayerMidlet extends MIDlet implements ActionListener{

    //Base folder name for the learning package that the user is currently running
    public String currentPackageURI;

    public EXETOC myTOC = null;

    private com.sun.lwuit.Form TOCForm = null;

    //the id of the idevice that the user is currently on
    public String currentIdevice = null;
    
    public int currentIdeviceIndex = -1;

    //a reference to the idevice currently shown
    public Idevice currentIdeviceObj = null;

    //the idevices on the current page
    protected String[] ideviceIdList;

    //the titles that are on the current page
    private String[] ideviceTitleList;

    /** Href of the currently loaded page */
    public String currentHref;
    
    //next page href to show
    public String nextHref;
    //prev page href to show
    public String prevHref;
    
    //the cached next and previous links for the url given by nextPrevLinksCachedForURL
    //public String[] cachedURLNextPrevLinks;
    
    public String nextPrevLinksCachedForURL;
    
    //XmlNode of XmlData for the next node to show
    public XmlNode cachedNextNode;
    
    public String cachedIdeviceId;
    
    //If the cached data is ready
    public boolean cachedNextNodeReady = false;
    
    //Thread that will run to cache the next
    private CacheNextIdeviceThread cacheNodeThread;

    //media player
    private Player player;
    
    //the browsers for opening content
    private ContentBrowseForm contentBrowser = null;
    
    //the in game menu form
    MLearnMenu menuFrm;
    
    //the current idevice form
    Form deviceForm;
    
    //Default time to show feedback (in ms)
    public int fbTime = 10000;
    
    public long currentMediaLength = -1;
    
    //Default Shortcut keys
    final int KEY_NEXT = 42;
    final int KEY_PREV = 35;
    
    final int NEXT_HREF = 0;
    final int NEXT_DEVICEID = 1;
    final String firstDevice = ":FIRST";
    final String lastDevice = ":LAST";
    
    public static final boolean mediaEnabled = true;
    
    public void startApp() {
        com.sun.lwuit.Display.init(this);
        try {
            Resources r = Resources.open("/theme.res");
            UIManager.getInstance().setThemeProps(r.getTheme("Makeover"));
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        menuFrm = new MLearnMenu(this);
        myTOC = new EXETOC(this);
        
        currentPackageURI = "/mxml1";
        //showTOC();
        
        contentBrowser = new ContentBrowseForm(this);
        contentBrowser.makeForm();
        contentBrowser.show();
    }
    
    public void openPackageDir(String dirName) {
        //contentBrowser = null;
        currentPackageURI = dirName.substring(0, dirName.length() - 1);
        System.out.println("Package URI: " + currentPackageURI);
        showTOC();
    }
    
    public void openCollectionDir(String dirName) {
        //dirname comes with trailing /
        myTOC.colBaseHREF = dirName.substring(0, dirName.length() - 1);
        myTOC.readCollection(dirName + "execollection.xml");
        myTOC.getCollectionForm().show();
    }
    
    
    /**
     * Short utility method - if path starts with / - use stream method - otherwise
     * use file method
     * 
     * @param URL
     * @param callee - calling object ref
     * @return
     * @throws IOException 
     */
    public InputStream getInputStreamReaderByURL(String URL) throws IOException{
        if(URL.startsWith("file://")) {
            System.out.println("Opening file" + URL);
            return Connector.openInputStream(URL);
        }else if (URL.startsWith("/")) {
            return getClass().getResourceAsStream(URL);
        }else {
            //this is a relative link
            System.out.println("Opening File" + URL);
            return Connector.openInputStream(currentPackageURI + "/" + URL);
        }
    }
    
    public HTMLComponent makeHTMLComponent(String htmlStr) {
        return makeHTMLComponent(htmlStr, null);
    }
    
    public HTMLComponent makeHTMLComponent(String htmlStr, HTMLCallback callback) {
        HTMLComponent htmlComp = new HTMLComponent(new EXERequestHandler(this, 
                htmlStr));
        htmlComp.setIgnoreCSS(false);
        htmlComp.setShowImages(true);
        if(callback != null) {
            htmlComp.setHTMLCallback(callback);
        }
        
        htmlComp.setPage("idevice://current/");
        
        return htmlComp;
    }
    
    //shows the 'ingame' menu
    public void showMenu() {
        menuFrm.show();
    }
    

    public void showTOC() {
        stopCurrentIdevice();
        myTOC.readList(currentPackageURI + "/exetoc.xml");
        TOCForm = myTOC.getForm();
        myTOC.getList().addActionListener(this);
        TOCForm.show();
    }

    public void loadPage(String pageHref) {
        loadPage(pageHref, false);
    }
    
    public void loadPage(String pageHref, boolean goLast) {
        String myHref = (pageHref != null ? pageHref : this.currentHref);
        Object[] pageIdeviceInfo = myTOC.getPageIdeviceList(currentPackageURI + "/exetoc.xml",
                myHref);
        ideviceIdList = (String[])pageIdeviceInfo[EXETOC.PAGELIST_IDEVICEIDS];
        ideviceTitleList = (String[])pageIdeviceInfo[EXETOC.PAGELIST_IDEVICETITLES];
        if(ideviceIdList.length == 0) {
            Dialog myDialog = new Dialog("Empty Page");
            myDialog.addComponent(new Label("Sorry - Page has no content"));
            myDialog.show();
        }else {
            TOCCachePage cPg = myTOC.cache.getPageByHref(myHref);
            this.nextHref = cPg.nextHref;
            this.prevHref = cPg.prevHref;                
            
            int index = (goLast == false) ? 0 : ideviceIdList.length -1;
            showIdevice(myHref, ideviceIdList[index]);
        }
    }
    
 
    

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
     * TODO: Make this just refer to getNextIdeviceIdAndHref
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
                    myTOC.tocList = null;
                    showTOC();
                }
            }
            showTOC();
        }
    }

    void stopCurrentIdevice() {
        if(currentIdeviceObj != null) {
            currentIdeviceObj.stop();
            currentIdeviceObj.dispose();
            currentIdeviceObj = null;
            
            //think about this one...
            stopMedia(false);
            
        }
    }
    
    public void showIdevice(Idevice device, String ideviceId) {
        try {
            if(device.getMode() == Idevice.MODE_LWUIT_FORM) {
                if(!Display.isInitialized()) {
                    Display.init(this);
                }

                deviceForm = device.getForm();

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
                NavigationActionListener listener = new NavigationActionListener(this);
                listener.addMenuCommandsToForm(deviceForm);
                
                //Add listeners for navigation purposes
                deviceForm.addKeyListener(KEY_NEXT, this);
                deviceForm.addKeyListener(KEY_PREV, this);
                
                deviceForm.setTransitionOutAnimator(CommonTransitions.createSlide(
                            CommonTransitions.SLIDE_HORIZONTAL, true, 1000));
                deviceForm.show();
                

                //tell the device to start anything that it wants to run - media -etc
                device.start();
                currentIdeviceObj = device;
                
                /*
                //now get ready to cache the next one
                String[] nextDeviceInfo = getNextIDeviceIdAndHref(1);
                cacheNodeThread = new CacheNextIdeviceThread(this, nextDeviceInfo[NEXT_HREF],
                        nextDeviceInfo[NEXT_DEVICEID]);
                cacheNodeThread.start();
                */
            }
        }catch(Exception e) {
            Dialog errorDialog = new Dialog("Error showing idevice");
            String errorMessage = e.toString();
            if(e instanceof IOException) {
                IOException ioe = (IOException)e;
                errorMessage += "::" + ioe.getMessage();
            }else if(e instanceof RuntimeException) {
                RuntimeException re = (RuntimeException)e;
                
            }
            errorDialog.addComponent(new TextArea(errorMessage));
            errorDialog.show();
        }
    }
    
    public void showIdevice(String pageHREF, String ideviceId) {
        stopCurrentIdevice();
        
        try {
            InputStream inStream = getInputStreamReaderByURL(currentPackageURI + "/"
                    + pageHREF);
            XmlNode cachedData = (ideviceId.equals(cachedIdeviceId) && cachedNextNodeReady) ?
                    cachedNextNode : null;
            Idevice device = IdeviceFactory.makeIdevice(inStream, ideviceId, this, cachedData);
            showIdevice(device, ideviceId);
            
        }catch(Exception e) {
            e.printStackTrace();
        }
    }


    //user has selected from table of contents
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


    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
    }

    public void stopMedia() {
        stopMedia(true);
    }
    
    public void stopMedia(boolean doAutoGc) {
        if(player != null && mediaEnabled == true) {
            try{
                player.stop();
                player.close();
            }catch(Exception e) {
                e.printStackTrace();
            } finally {
                player = null;
                if(doAutoGc) { System.gc(); }
            }
        }
    }

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
    
    public String getCurrentPackageURI() {
        return currentPackageURI;
    }
    

    public long playMedia(String mediaURI) {
        stopMedia();
        System.out.println("asked to play: " + mediaURI);
        String mediaType = getContentType(mediaURI);
        mediaURI = currentPackageURI + "/" + mediaURI;
        if(mediaEnabled) {
            try {
                InputStream in = getInputStreamReaderByURL(mediaURI);
                player = Manager.createPlayer(in, mediaType);
                player.start();
                long playerTime = player.getDuration();
                this.currentMediaLength = playerTime;
                return playerTime;
            }catch(Exception e) {
                Form errorForm = new Form("Error creating player" + mediaURI + " : " + mediaType);
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

}

