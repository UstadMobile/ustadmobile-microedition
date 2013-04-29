/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.paiwastoon.mlearnplayer;

import java.util.Hashtable;

/**
 *
 * @author mike
 */
public class TOCCache {
    
    public TOCCachePage[] pages;
    
    private Hashtable hrefPgTable;
    
    public TOCCache() {
        
    }
    
    public void mkTable() {
        hrefPgTable = new Hashtable();
        int numPages = pages.length;
        for(int i = 0; i < numPages; i++) {
            hrefPgTable.put(pages[i].href, pages[i]);
        }
    }
    
    public int pgIndex(TOCCachePage pg) {
        int numPages = pages.length;
        for(int i = 0; i < numPages; i++) {
            if(pages[i] == pg) {
                return i;
            }
        }
        
        return -1;
    }
    
    
    /**
     * Lookup a cached page reference by href
     * 
     * @param href
     * @return 
     */
    public TOCCachePage getPageByHref(String href) {
        if(hrefPgTable == null) {
            mkTable();
        }
        
        Object retVal = hrefPgTable.get(href);
        if(retVal == null) {
            return null;//e.g. page not found
        }
        return (TOCCachePage)retVal;
    }
    
}

