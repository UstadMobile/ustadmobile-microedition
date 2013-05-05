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
        
        public static Idevice makeIdevice(InputStream input, String ideviceId, MLearnPlayerMidlet host) throws Exception{
            return makeIdevice(input, ideviceId, host, null);
        }
        
    
	/**
	 * 
	 * @param input Input XML stream
	 * @param ideviceId id of the idevice itself one is looking for
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
