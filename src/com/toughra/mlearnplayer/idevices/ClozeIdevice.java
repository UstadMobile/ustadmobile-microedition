/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.toughra.mlearnplayer.idevices;

import com.sun.lwuit.Component;
import com.sun.lwuit.Container;
import com.sun.lwuit.Dialog;
import com.sun.lwuit.Form;
import com.sun.lwuit.Label;
import com.sun.lwuit.TextArea;
import com.sun.lwuit.TextField;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.html.DefaultHTMLCallback;
import com.sun.lwuit.html.DocumentInfo;
import com.sun.lwuit.html.HTMLComponent;
import com.sun.lwuit.html.HTMLElement;
import com.sun.lwuit.layouts.BorderLayout;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Vector;
import com.toughra.mlearnplayer.Idevice;
import com.toughra.mlearnplayer.MLearnPlayerMidlet;
import com.toughra.mlearnplayer.MLearnUtils;
import com.toughra.mlearnplayer.xml.XmlNode;



/**
 *
 * @author mike
 */
public class ClozeIdevice  extends Idevice implements EXERequestSubHandler{

    
    String formHTML;
    
    Form frm;
    
    String[] words;
    
    HTMLComponent comp;
    
    String[] currentAnswers;
    
    String scoreStr;
    
    //Input formats to use
    String[][] wordFormats;
    
    //The prefix of the fieldname used 
    final String prefix = "clozeBlank";
    
    public ClozeIdevice(MLearnPlayerMidlet hostMidlet, XmlNode nodeData)  {
        
        super(hostMidlet);
        formHTML = MLearnUtils.getTextProperty(nodeData, "formhtml");
        scoreStr = MLearnUtils.getTextProperty(nodeData, "scoretext");
        Vector wordChldrn = nodeData.findChildrenByTagName("word", true);
        int numWords = wordChldrn.size();
        hadCorrect = MLearnUtils.makeBooleanArray(false, numWords);
        
        words = new String[numWords];
        currentAnswers = new String[numWords];
        for(int i = 0; i < numWords; i++) {
            words[i] = ((XmlNode)wordChldrn.elementAt(i)).getAttribute("value");
            currentAnswers[i] = "";
        }
        
        wordFormats = new String[numWords][];
        Vector wordFormatChildren = nodeData.findChildrenByTagName("wordformat", true);
        for(int i = 0; i < numWords; i++) {
            String fmtValStr = ((XmlNode)wordFormatChildren.elementAt(i)).getAttribute("value");
            String[] wordFormat = expandWordFormats(fmtValStr);
            wordFormats[i] = wordFormat;
        }
    }
    
    /**
     * Converts a , separated list of formats (e.g. ABC,123) 
     * into a string array
     * 
     * @param formats
     * @return 
     */
    private String[] expandWordFormats(String formats) {
        Vector formatsFound = new Vector();
        int startPos = 0;
        for(int i = 0; i < formats.length(); i++) {
            if(formats.charAt(i) == ',' || i == (formats.length() - 1)) {
                int lastPos = i;
                //if at the end of the string - inc - there is no comma
                if(i == formats.length() - 1) {
                    lastPos = i + 1;
                }
                String newFmt = formats.substring(startPos, lastPos);
                
                startPos = i + 1;
                formatsFound.addElement(newFmt);
            }
        }
        
        String[] retVal = new String[formatsFound.size()];
        formatsFound.copyInto(retVal);
        return retVal;
    }
    
    public int getMode() {
        return Idevice.MODE_LWUIT_FORM;
    }
    
    public InputStream handleSubRequest(DocumentInfo di) throws IOException {
        String msg = "<html><body>Got your form mate</body></html>";
        String formParams = di.getParams();
        System.out.println("Form params: " + formParams);
        
        try {
            return new ByteArrayInputStream(msg.getBytes("UTF-8"));
        }catch(UnsupportedEncodingException e) {
            throw new IOException(e.toString());
        }
        
    }
    

    public Form getForm() {
        if(frm == null) {
            //change stuff for textfield
            final char RTL_ARABICNUM_BEGIN = '\u06F0';
            final char RTL_ARABICNUM_END = '\u06F9';
            
            Hashtable numbers = new Hashtable();
            for(int iter = 0 ; iter < 10 ; iter++) {
                char theChar = (char)(RTL_ARABICNUM_BEGIN + iter);
                numbers.put(new Integer('0' + iter), "" + theChar);
            }
            
            TextField.addInputMode("123", numbers, false);
            
            ClozeHTMLCallback cb = new ClozeHTMLCallback();
            cb.clozeDev = this;
            comp = hostMidlet.makeHTMLComponent(formHTML, cb);
            
            //make the form submit come back to us
            EXERequestHandler handler = (EXERequestHandler)comp.getRequestHandler();
            handler.addSubHandler("idevice://current/ideviceclozeform", this);
            
            frm = new Form();
            frm.setLayout(new BorderLayout());
            frm.addComponent(BorderLayout.CENTER, comp);
            comp.setEventsEnabled(true);
            
        }
        
        return frm;
    }
    
    public void checkAnswers() {
        StringBuffer newHTML = new StringBuffer(formHTML);
        
        Component[] allComps = new Component[comp.getComponentCount()];
        for(int i = 0; i < allComps.length; i++) {
            allComps[i] = comp.getComponentAt(i);
        }
        int y = 0;
        
        int numCorrect = 0;
        for(int i = 0; i < words.length; i++){
            HTMLElement element = (HTMLElement)comp.getDOM().getElementById(
                    "clozeBlank" + i);
            String elementId = "clozeBlank" + i;
            String idStr = "id=\"" + elementId + "\"";
            
            
            int idPos = newHTML.toString().indexOf(idStr);
            
            //now delete the old value="" string
            int valueStart = idPos + idStr.length() + 1;
            
            newHTML.delete(valueStart, valueStart+8);
            String newAttribs = " value=\"" + elementId + ":" + currentAnswers[i] + "\" ";
            boolean isCorrect = words[i].equals(currentAnswers[i]);
            if(hadCorrect[i] == false && isCorrect == true) {
                hadCorrect[i] = true;
            }
            
            if(isCorrect) {
                //correct
                newAttribs += "style='background-color: green' ";
                numCorrect++;
            }else {
                //wrong
                newAttribs += "style='background-color: red' ";
            }
            newHTML.insert(idPos, newAttribs);
        }
        
        if(correctFirst == -1) {
            correctFirst = numCorrect;
        }
        
        EXERequestHandler handler = (EXERequestHandler)comp.getRequestHandler();
        handler.htmlStr = newHTML.toString();
        comp.setPage("idevice://current/");
        
        Dialog dlg = new Dialog();
        String msg = scoreStr + numCorrect + " / " + words.length;
        dlg.addComponent(new Label(msg));
        dlg.setTimeout(hostMidlet.fbTime);
        dlg.showDialog();
        
    }
    
    void updateUserAnswer(int index, String answer) {
        currentAnswers[index] = answer;
    }
    
    public int[] getScores() {
        return new int[] { correctFirst, MLearnUtils.countBoolValues(hadCorrect, true), 
            words.length };
    }
    
    public int getLogType() {
        return LOG_QUIZ;
    }
    
}
class ClozeHTMLCallback extends DefaultHTMLCallback{
    
    ClozeIdevice clozeDev;

    
    
    private Vector getChildComponents(Container c) {
        int numChildren = c.getComponentCount();
        Vector retVal = new Vector();
        for(int i = 0; i < numChildren; i++) {
            Component cChild = c.getComponentAt(i);
            retVal.addElement(cChild);
            if(cChild instanceof TextField) {
                TextField tf = (TextField)cChild;
                String txtNow = tf.getText();
                String valNow = "";
                String fieldName = "";
                if(txtNow.startsWith("clozeBlank")) {
                    //find what the field name is
                    int colonPos = txtNow.indexOf(':');
                    if(colonPos != -1) {
                        valNow = txtNow.substring(colonPos + 1);
                        fieldName = txtNow.substring(0, colonPos);
                    }else {
                        fieldName = txtNow;
                    }
                }
                tf.setText(valNow);
                int wordNum = Integer.parseInt(fieldName.substring(clozeDev.prefix.length()));
                tf.setInputModeOrder(clozeDev.wordFormats[wordNum]);
            }
            if(cChild instanceof Container) {
                Vector subChildren = getChildComponents((Container)cChild);
                retVal.addElement(subChildren);
            }
        }
        
        return retVal;
    }
    
    public void pageStatusChanged(HTMLComponent htmlC, int status, String url) {
        
        if(status == STATUS_DISPLAYED) {
            int cCount = htmlC.getComponentCount();
        
            Vector allComponents = getChildComponents(htmlC);
            int x = 0;
        }
        
        super.pageStatusChanged(htmlC, status, url); //To change body of generated methods, choose Tools | Templates.
    }

    
    
    public void actionPerformed(ActionEvent ae, HTMLComponent htmlc, HTMLElement htmle) {

        System.out.println("EVENT ACTION PERFORMED");
        
    }

    public void dataChanged(int type, int index, HTMLComponent htmlC, TextField textField, HTMLElement element) {
        String id = element.getAttribute("id");
        String val = textField.getText();
        
        if(id.startsWith(clozeDev.prefix)) {
            int wordNum = Integer.parseInt(id.substring(clozeDev.prefix.length()));
            clozeDev.updateUserAnswer(wordNum, val);
        }
        super.dataChanged(type, index, htmlC, textField, element);
    }

    /*
    public String fieldSubmitted(HTMLComponent htmlC, TextArea ta, String actionURL, String id, String value, int type, String errorMsg) {
        System.out.println("submit: " +  value);
        HTMLElement el = clozeDev.comp.getDOM();
        String realVal = ta.getText();
        Vector inputChildren = clozeDev.comp.getDOM().getDescendantsByTagName("input");
        
        String prefix = "clozeBlank";
        for(int i = 0; i < inputChildren.size(); i++) {
            HTMLElement currentChild = (HTMLElement)inputChildren.elementAt(i);
            String childId = currentChild.getAttribute("id");
            if(childId != null && childId.startsWith(prefix)) {
                int wordNum = Integer.parseInt(childId.substring(prefix.length()));
                String childValue = currentChild.getAttribute("value");
                clozeDev.updateUserAnswer(wordNum, childValue);
            }
        }
        return super.fieldSubmitted(htmlC, ta, actionURL, id, value, type, errorMsg);
    }
    */

    public boolean linkClicked(HTMLComponent htmlC, String url) {
        System.out.println("Clicked link for " + url);
        if(url.endsWith("#")) {
            clozeDev.checkAnswers();
        }
        return true;
    }
    
    

}
