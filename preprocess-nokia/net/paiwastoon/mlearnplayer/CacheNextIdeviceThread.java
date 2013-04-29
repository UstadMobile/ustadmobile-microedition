/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.paiwastoon.mlearnplayer;

import java.io.InputStream;

/**
 *
 * @author mike
 */
public class CacheNextIdeviceThread extends Thread{
    
    private MLearnPlayerMidlet hostMidlet;
    
    private String href;
    
    private String ideviceId;
    
    public CacheNextIdeviceThread(MLearnPlayerMidlet hostMidlet, String href, String ideviceId) {
        this.hostMidlet = hostMidlet;
        this.href = href;
        this.ideviceId = ideviceId;
        this.hostMidlet.cachedIdeviceId = this.ideviceId;
        setPriority(MIN_PRIORITY);
    }
    
    public void run() {
        try {
            try { Thread.sleep(500); }
            catch(InterruptedException e) {}
            hostMidlet.cachedNextNodeReady = false;
            System.out.println("Caching next XmlNode");
            String pageURL = hostMidlet.currentPackageURI + "/" + href;
            InputStream inStream = hostMidlet.getInputStreamReaderByURL(pageURL);
            hostMidlet.cachedNextNode = IdeviceFactory.getXmlNode(inStream, ideviceId, hostMidlet);
            
            TOCCachePage cPg = hostMidlet.myTOC.cache.getPageByHref(href);
            //hostMidlet.cachedURLNextPrevLinks = new String[] { cPg.nextHref, cPg.prevHref };
            hostMidlet.nextPrevLinksCachedForURL = pageURL;
            
            hostMidlet.cachedNextNodeReady = true;
            System.out.println("Cache complete");
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
}
