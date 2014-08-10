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

/**
 * Cached meta data for a page - includes previous/next href and the idevices
 * that are on the page
 * 
 * @author mike
 */
public class TOCCachePage {
    
    /** Position in idevices array of ideviceid*/
    public static final int DEV_ID = 0;
    
    /** Position in idevices array of the href*/
    public static final int DEV_HREF = 1;
    
    /** Position in idevices array of idevice title*/
    public static final int DEV_TITLE = 2;
    
    /** the next page href */
    public String nextHref;
    
    /** the previous page href*/
    public String prevHref;
    
    /** the href of this page*/
    public String href;
    
    /** the title of this page*/
    public String title;
    
    /** The tincan id of this page (if present) */
    public String tinCanID;
    
    /** The tincan activity definition of page (if present) */
    public String tinCanActivityDef;
    
    /**
     * Array in the form of
     * idevice[index] = [DEV_ID, DEV_HREF, DEV_TITLE]
     */
    public String[][] idevices;
    
}
