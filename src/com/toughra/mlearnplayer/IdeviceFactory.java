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

import com.toughra.mlearnplayer.xml.XmlNode;
import com.toughra.mlearnplayer.xml.GenericXmlParser;
import com.toughra.mlearnplayer.idevices.MediaSlide;
import com.toughra.mlearnplayer.idevices.ClozeIdevice;
import com.toughra.mlearnplayer.idevices.MCQIdevice;
import com.toughra.mlearnplayer.idevices.MissingLetterIdevice;
import com.toughra.mlearnplayer.idevices.MemoryMatchIdevice;
import com.toughra.mlearnplayer.idevices.MathDrillIdevice;
import com.toughra.mlearnplayer.idevices.HTMLIdevice;
import com.toughra.mlearnplayer.idevices.SortIdevice;
import com.toughra.mlearnplayer.idevices.HangmanIdevice;
import com.toughra.mlearnplayer.idevices.SlideShowIdevice;
import java.io.*;
import org.kxml2.io.KXmlParser;

/**
 * This class is used to take an XML idevice input stream and turn it into the 
 * appropriate type of idevice
 * 
 * @author mike
 */
public class IdeviceFactory {

        
        public static XmlNode getXmlNode(InputStream input, String ideviceId, MLearnPlayerMidlet host) throws Exception{
            GenericXmlParser gParser = new GenericXmlParser();
            KXmlParser parser = new KXmlParser();
            InputStreamReader reader = new InputStreamReader(input, "UTF-8");
            parser.setInput(reader);
            XmlNode ideviceNode = gParser.parseXMLSelective(parser, true, "idevice", "id", ideviceId);
            reader.close();
            return ideviceNode;
        }
        
        /**
         * Overloaded method
         * 
         * @param input
         * @param ideviceId
         * @param host
         * @return
         * @throws Exception 
         */
        public static Idevice makeIdevice(InputStream input, String ideviceId, MLearnPlayerMidlet host) throws Exception{
            return makeIdevice(input, ideviceId, host, null);
        }
        
    
	/**
	 * Returns an idevice object given the required inputstream
         * and the device id to look for.  Will check through the types of
         * devices it knows about and instantiate the correct class of object.
         * 
	 * @param input Input XML stream
	 * @param ideviceId id of the idevice itself one is looking for
         * @param host - the host midlet
         * @param ideviceNode XmlNode data for the idevice if it is already available (e.g. it was cached)
	 * @return
	 */
	public static Idevice makeIdevice(InputStream input, String ideviceId, MLearnPlayerMidlet host, XmlNode ideviceNode) throws Exception{
            MLearnUtils.checkFreeMem();
            if(ideviceNode == null) {
                ideviceNode = getXmlNode(input, ideviceId, host);
            }
            
            String deviceType = ideviceNode.getAttribute("type");
            
            Idevice deviceToReturn  = null;
            if(deviceType.equals("multichoice")) {
                deviceToReturn = new MCQIdevice(host, ideviceNode);
            }else if(deviceType.equals("mediaslide")) {
                deviceToReturn = new MediaSlide(host, ideviceNode);
            }else if(deviceType.equals("html")) {
                deviceToReturn = new HTMLIdevice(host, ideviceNode);
            }else if(deviceType.equals("slideshow")){
                deviceToReturn = new SlideShowIdevice(host, ideviceNode);
            }else if(deviceType.equals("hangman")) {
                deviceToReturn = new HangmanIdevice(host, ideviceNode);
            }else if(deviceType.equals("sort")) {
                deviceToReturn = new SortIdevice(host, ideviceNode);
            }else if(deviceType.equals("cloze")) {
                deviceToReturn = new ClozeIdevice(host, ideviceNode);
            } else if(deviceType.equals("missingletter")) {
                deviceToReturn = new MissingLetterIdevice(host, ideviceNode);
            }else if(deviceType.equals("memorymatch")) {
                deviceToReturn = new MemoryMatchIdevice(host, ideviceNode);
            }else if(deviceType.equals("mathdrill2")) {
                deviceToReturn = new MathDrillIdevice(host, ideviceNode);
            }else {
                HTMLIdevice device = new HTMLIdevice(host, "");
                device.htmlStr = "<html><body>&nbsp;</body></html>";
                deviceToReturn = device;                
            }
            
            deviceToReturn.ideviceId = ideviceId;
            deviceToReturn.deviceType = deviceType;

            //could not find the type of idevice claimed here
            MLearnUtils.printFreeMem(host, "Created idevice" + deviceType);
            return deviceToReturn;
	}
}
