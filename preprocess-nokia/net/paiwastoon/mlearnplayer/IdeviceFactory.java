package net.paiwastoon.mlearnplayer;

import net.paiwastoon.mlearnplayer.xml.*;
import net.paiwastoon.mlearnplayer.idevices.*;
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
            } else {
                HTMLIdevice device = new HTMLIdevice(host, null);
                device.htmlStr = "<html><body>idevice: " + deviceType + "not supported</body></html>";
                deviceToReturn = device;                
            }

            //could not find the type of idevice claimed here
            return deviceToReturn;
	}
}
