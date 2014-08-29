/*
 * Ustad Mobile (Micro Edition App)
 * 
 * Copyright 2011-2014 UstadMobile Inc. All rights reserved.
 * www.ustadmobile.com
 *
 * Ustad Mobile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version with the following additional terms:
 * 
 * All names, links, and logos of Ustad Mobile and Toughra Technologies FZ
 * LLC must be kept as they are in the original distribution.  If any new
 * screens are added you must include the Ustad Mobile logo as it has been
 * used in the original distribution.  You may not create any new
 * functionality whose purpose is to diminish or remove the Ustad Mobile
 * Logo.  You must leave the Ustad Mobile logo as the logo for the
 * application to be used with any launcher (e.g. the mobile app launcher).
 * 
 * If you want a commercial license to remove the above restriction you must
 * contact us and purchase a license without these restrictions.
 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 
 * Ustad Mobile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
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

