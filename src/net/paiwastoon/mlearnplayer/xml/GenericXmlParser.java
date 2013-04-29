/*
 * Standard generic XML parser with one addition - parse selective capability.
 * This allows you to set an element to look for to star parsing and will
 * skip content until that element is found.
 * 
 */

package net.paiwastoon.mlearnplayer.xml;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;


/**
 *
 * @author mike
 */
public class GenericXmlParser {
    public XmlNode parseXML(KXmlParser parser,
            boolean ignoreWhitespaces) throws Exception {
        parser.next();


        return _parse(parser, ignoreWhitespaces);
    }

    public XmlNode parseXMLSelective(KXmlParser parser,
            boolean ignoreWhitespaces,  String startTagName, String startTagAttribName,
            String startTagAttribVal) throws Exception {
        parser.next();
        XmlNode myNode = _parseFrom(parser, ignoreWhitespaces, startTagName, startTagAttribName, startTagAttribVal);
        return myNode;
    }
    
    XmlNode _parseFrom(KXmlParser parser,
            boolean ignoreWhitespaces, String startTagName, String startTagAttribName, 
            String startTagAttribVal) throws Exception {
        
        //once we find the matching node then go and use the normal _parse
        XmlNode node = new XmlNode(XmlNode.ELEMENT_NODE);
        boolean foundNode = false;
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new Exception("Illegal XML state: "
                    + parser.getName() + ", " + parser.getEventType());
        } else {
            node.nodeName = parser.getName();
            if(node.nodeName.equalsIgnoreCase(startTagName)) {
                for (int i = 0; i < parser.getAttributeCount(); i++) {
                    node.setAttribute(parser.getAttributeName(i), parser.getAttributeValue(i));
                }
                
                String attribNameVal = node.getAttribute(startTagAttribName);
                if(attribNameVal != null && attribNameVal.equals(startTagAttribVal)) {
                    foundNode = true;
                    //Now we have found the start of the document that we are interested in
                    parser.next();

                    while (parser.getEventType() != XmlPullParser.END_TAG) {
                        if (parser.getEventType() == XmlPullParser.START_TAG) {
                            node.addChild(_parse(parser, ignoreWhitespaces));
                        } else if (parser.getEventType() == XmlPullParser.TEXT) {
                            if (!ignoreWhitespaces || !parser.isWhitespace()) {
                                XmlNode child = new XmlNode(XmlNode.TEXT_NODE);

                                child.nodeValue = parser.getText();

                                node.addChild(child);
                            }
                        }
                        parser.next();
                    }
                    return node;
                }
                
            }

        }

        if(foundNode == false) {
            //look for child nodes to go through
            parser.next();
            while (parser.getEventType() != XmlPullParser.END_TAG) {
                if (parser.getEventType() == XmlPullParser.START_TAG) {
                    XmlNode childResult = _parseFrom(parser, ignoreWhitespaces, startTagName, startTagAttribName, startTagAttribVal);
                    if(childResult != null) {
                        return childResult;
                    }
                }
                parser.next();
            }
        }
        
        //nothing found ... dang...
        return null;
    }

    XmlNode _parse(KXmlParser parser,
            boolean ignoreWhitespaces) throws Exception {
        XmlNode node = new XmlNode(XmlNode.ELEMENT_NODE);

        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new Exception("Illegal XML state: "
                    + parser.getName() + ", " + parser.getEventType());
        } else {
            node.nodeName = parser.getName();

            for (int i = 0; i < parser.getAttributeCount(); i++) {
                node.setAttribute(parser.getAttributeName(i), parser.getAttributeValue(i));
            }

            parser.next();

            while (parser.getEventType() != XmlPullParser.END_TAG) {
                if (parser.getEventType() == XmlPullParser.START_TAG) {
                    node.addChild(_parse(parser, ignoreWhitespaces));
                } else if (parser.getEventType() == XmlPullParser.TEXT) {
                    if (!ignoreWhitespaces || !parser.isWhitespace()) {
                        XmlNode child = new XmlNode(XmlNode.TEXT_NODE);

                        child.nodeValue = parser.getText();

                        node.addChild(child);
                    }
                } 
                parser.next();
            }
        }
        return node;
    }
}
