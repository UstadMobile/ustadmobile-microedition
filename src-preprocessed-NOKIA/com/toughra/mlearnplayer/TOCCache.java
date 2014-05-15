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

import java.util.Hashtable;

/**
 * Table of contents cache handler
 * 
 * @author mike
 */
public class TOCCache {
   
    /** The pages contained within this package */
    public TOCCachePage[] pages;
    
    /** Mapping of href name to the cached page object*/
    private Hashtable hrefPgTable;
    
    /**
     * Constructor (empty)
     */
    public TOCCache() {
        
    }
    
    /**
     * Go through and put all the pages into in pages into hrefPgTable
     * this.pages must be set first.  This is generally done by the host 
     * midlet
     */
    public void mkTable() {
        hrefPgTable = new Hashtable();
        int numPages = pages.length;
        for(int i = 0; i < numPages; i++) {
            hrefPgTable.put(pages[i].href, pages[i]);
        }
    }
    
    /**
     * Get the index of a given page
     * 
     * @param pg - page to find index for
     * @return index of the page in array pages
     */
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
     * @return TOCCachePage for that href.
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

