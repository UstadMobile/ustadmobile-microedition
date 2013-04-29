package net.paiwastoon.mlearnplayer.xml;

//see http://www.developer.nokia.com/Community/Wiki/How_to_parse_an_XML_file_in_Java_ME_with_kXML

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

public class XmlNode {
	public static final int TEXT_NODE = 0;
	public static final int ELEMENT_NODE = 1;
        
	public int nodeType = 0;
	public String nodeName = null;
	public String nodeValue = null;
	public Vector children = null;
	public Hashtable attributes = null;
        
        
        public String getTextChildContent(int childNum) {
            //TODO: Check me - childNum is not used
            return ((XmlNode)children.elementAt(0)).nodeValue.toString();
        }
        
        public XmlNode findFirstChildByTagName(String tagName, boolean recursive) {
            Vector childResults = findChildrenByTagName(tagName, recursive);
            if(childResults.size() > 0) {
                return (XmlNode)childResults.elementAt(0);
            }else {
                return null;
            }
        }

        
        
        /**
         * TODO: Make a maxmatch parameter so it can skip the rest
         */
        public Vector findChildrenByTagName(String tagName, boolean recursive) {
            Vector results = new Vector();
            int numChildren = children.size();
            
            XmlNode currentChild = null;
            for(int i = 0; i < numChildren; i++) {
                currentChild = (XmlNode)children.elementAt(i);
                if(currentChild.nodeType == ELEMENT_NODE) {
                    if(currentChild.nodeName.equals(tagName)) {
                        results.addElement(currentChild);
                    }

                    if(recursive) {
                        Vector subResults = currentChild.findChildrenByTagName(tagName, recursive);
                        int subResultsCount = subResults.size();
                        for(int subCount = 0; subCount < subResultsCount; subCount++) {
                            results.addElement(subResults.elementAt(subCount));
                        }
                    }
                }
            }

            return results;
        }
 
	public XmlNode(int nodeType)
	{
		this.nodeType = nodeType;
		this.children = new Vector();
		this.attributes = new Hashtable();
	}
	public String[] getAttributeNames()
	{
		String[] names = new String[attributes.size()];
 
		Enumeration e = attributes.keys();
 
		int i = 0;
 
		while(e.hasMoreElements())
		{
			names[i] = (String)e.nextElement();
 
			i++;
		}
		return names;
	}
	public void setAttribute(String key, String value)
	{
		attributes.put(key, value);
	}
        
        public boolean hasAttribute(String key){
            return attributes.containsKey(key);
        }
        
	public String getAttribute(String key)
	{
		return (String)attributes.get(key);
	}
 
	public void addChild(XmlNode child)
	{
		this.children.addElement(child);
	}
}
