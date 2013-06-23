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

package com.toughra.mlearnplayer.idevices;

import com.sun.lwuit.html.DocumentInfo;
import java.io.InputStream;
import java.io.IOException;

/**
 * Interface to implement when you want to have a sunhandler to EXERequestHandler.
 * 
 * Add to the EXERequestHandler with addSubhandler and then implement
 * handleSubRequest
 * 
 * @author mike
 */
public interface EXERequestSubHandler {
    
    /*
     * When a request matches this sub handler this method will be called
     */
    public InputStream handleSubRequest(DocumentInfo di) throws IOException;
}
