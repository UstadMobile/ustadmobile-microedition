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
import com.toughra.mlearnplayer.EXEStrMgr;
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
 * This Idevice implements in J2ME the Cloze activity from EXE (fill in the blank)
 * format.  It does so by generating an HTML form and using the HTMLComponent.
 * 
 * A Request Handler attached to the component will then check the answers that
 * were provided and change the background color to green or red according to if
 * the answer provided by the student was correct or incorrect.
 * 
 * When doing a mobile export EXE can also specify the format type of the data
 * to be entered.  This has a system for specifying number only input which
 * is pretty handy on cell phones to use the number pad directly instead of 
 * going through all the keys.
 * 
 * The HTMLComponent will be made with an HTMLCallback method that will listen
 * for when the user is entering answers and then fire an event when the link
 * at the bottom of the form is clicked that will check the answers and refresh
 * the page.
 * 
 * @author mike
 */
public class ClozeIdevice  extends Idevice implements EXERequestSubHandler{

    /** The HTML generated for the form to show */
    String formHTML;
    
    /** the LWUIT form that represents this idevice*/
    Form frm;
    
    /** the correct words that have been made blank*/
    String[] words;
    
    /** the HTMLComponent being used to show this form*/
    HTMLComponent comp;
    
    /** The answers of the learner as we currently know them */
    String[] currentAnswers;
    
    /** the Your score is x / y feedback to show */
    String scoreStr;
    
    /** Input Formats that are used on the text fields in the HTML form */
    String[][] wordFormats;
    
    /**The prefix of the fieldname used in the htmlForm */
    final String prefix = "clozeBlank";
    
    /**
     * Constructor
     * 
     * @param hostMidlet MLearnPlayerMidlet we are using
     * @param nodeData the XML node data that has been loaded
     * 
     */
    public ClozeIdevice(MLearnPlayerMidlet hostMidlet, XmlNode nodeData)  {
        super(hostMidlet);
        formHTML = MLearnUtils.getTextProperty(nodeData, "formhtml");
        scoreStr = MLearnUtils.getTextProperty(nodeData, "scoretext");
        Vector wordChldrn = nodeData.findChildrenByTagName("word", true);
        int numWords = wordChldrn.size();
        
        hadCorrect = MLearnUtils.makeBooleanArray(false, numWords);
        hasAttempted = MLearnUtils.makeBooleanArray(false, numWords);
        
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

    public void start() {
        super.start(); //To change body of generated methods, choose Tools | Templates.
    }

    public void stop() {
        super.stop(); //To change body of generated methods, choose Tools | Templates.
        
        EXEStrMgr.lg(this, //idevice
                0, //question id
                getTimeOnDevice(), //time on device in ms
                MLearnUtils.countBoolValues(hadCorrect, true), //num correctly answered
                correctFirst, //num answered correct first attempt
                MLearnUtils.countBoolValues(hasAttempted, true), //num questions attempted
                LOGDEVCOMPLETE, //verb
                0, //score
                0, //maxScorePossible
                "",//answer given 
                "");//remarks
    }
    
    

    public String getDeviceTypeName() {
        return "cloze";
    }
    
    
    
    /**
     * Converts a , separated list of formats (e.g. ABC,123) 
     * into a string array
     * 
     * @param formats comma separated list of input formats
     * @return String[] array e.g. {"ABC", "123'}
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
    
    /**
     * says that this a Idevice.MODE_LWUIT_FORM type idevice
     * 
     * @return Idevice.MODE_LWUIT_FORM
     */
    public int getMode() {
        return Idevice.MODE_LWUIT_FORM;
    }
    
    
    /**
     * handleSubRequest was used to try and look through params.  Unfortunately
     * because of a bug in LWUITs HTMLComponent when the input format is set the 
     * parameters do not actually get set properly.  Therefor we are using
     * the HTMLCallback to find this info
     * 
     * @param di DocumentInfo
     * @return InputStream with a blank HTML Document
     * @throws IOException 
     */
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
    

    /**
     * Main idevice method that will generate the form that is going to be used
     * It will setup the HTMLComponent and create an HTMLCallback object for it
     * to monitor user input and get an event when the user clicks the check
     * answers button
     * 
     * @return Form for idevice
     */
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
    
    /**
     * Checks the answers and makes up new HTML by modifying the existing HTML,
     * inserting a background-color according to if the answer is correct or not
     * 
     */
    public void checkAnswers() {
        StringBuffer newHTML = new StringBuffer(formHTML);
        StringBuffer answerLogLine = new StringBuffer();
        
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
            boolean isCorrect = MLearnUtils.equalsIgnoreCase(words[i], currentAnswers[i]);
            if(!currentAnswers[i].equals("")) {
                hasAttempted[i] = true;
            }
            
            if(hadCorrect[i] == false && isCorrect == true) {
                hadCorrect[i] = true;
            }
            
            answerLogLine.append("Blank ").append(i+1).append(" \'").append(currentAnswers[i]).append('\'');
            if(isCorrect) {
                //correct
                newAttribs += "style='background-color: green' ";
                numCorrect++;
                answerLogLine.append(" (Correct)");
            }else {
                //wrong
                answerLogLine.append(" (Incorrect wanted '").append(words[i]).append("')");
                newAttribs += "style='background-color: red' ";
            }
            
            if(i < (words.length - 1)) {
                answerLogLine.append(',');
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
        EXEStrMgr.lg(this, 0, 0, 0, 0, 0, EXEStrMgr.VERB_ANSWERED, numCorrect, words.length, answerLogLine.toString(), "");
    }
    
    /**
     * Update the answer given by the user
     * 
     * @param index the word index
     * @param answer the answer now entered by the user
     */
    void updateUserAnswer(int index, String answer) {
        currentAnswers[index] = answer;
    }
    
    /**
     * Utility method for logging
     * @return #correctly answered first attempt, #correctly answered, #blanks total
     */
    public int[] getScores() {
        return new int[] { correctFirst, MLearnUtils.countBoolValues(hadCorrect, true), 
            words.length };
    }
    
    /**
     * This is a quiz style device
     * 
     * @return Idevice.LOG_QUIZ
     */
    public int getLogType() {
        return LOG_QUIZ;
    }
    
}
/**
 * This HTMLCallback is used to listen for when the user has entered an answer
 * and figure out 
 * @author mike
 */
class ClozeHTMLCallback extends DefaultHTMLCallback{
    
    /** The ClozeIdevice that we are working for */
    ClozeIdevice clozeDev;

    
    /**
     * This is a hackish method that we use to set the input formats on text fields.
     * 
     * All textfields are set to have a value of clozeBankINDEXNUM.  This is then
     * recognized, and the TextField then has it's input formats set
     * 
     * We are using this because if we use the standard wap attribute/value pairs
     * the events will not be fired and the parameter values will not be stored
     * because of a bug in HTMLComponent in LWUIT.
     * 
     * @param c LWUIT container to dig through
     * @return Vector of all child components in the container
     */
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
    
    /**
     * This is listened for so that we can get the components, set the correct
     * input formats and wipe out the clozeBlankINDEX part of the value in the 
     * textfield
     * 
     * @param htmlC HTMLComponent concerned
     * @param status DefaultHTMLCallBack.STATUS_ field
     * @param url URL selected
     */
    public void pageStatusChanged(HTMLComponent htmlC, int status, String url) {
        
        if(status == STATUS_DISPLAYED) {
            int cCount = htmlC.getComponentCount();
        
            Vector allComponents = getChildComponents(htmlC);
            int x = 0;
        }
        
        super.pageStatusChanged(htmlC, status, url); //To change body of generated methods, choose Tools | Templates.
    }

    
    /**
     * Does nothing
     * 
     * @param ae
     * @param htmlc
     * @param htmle 
     */
    public void actionPerformed(ActionEvent ae, HTMLComponent htmlc, HTMLElement htmle) {

        System.out.println("EVENT ACTION PERFORMED");
        
    }

    /**
     * This is used to listen for when the user has entered a new value in a 
     * textfield
     * 
     * @param type unused
     * @param index unused
     * @param htmlC concerned HTMLComponent
     * @param textField concerned TextField
     * @param element concerned HTMLElement containing the id attribute
     */
    public void dataChanged(int type, int index, HTMLComponent htmlC, TextField textField, HTMLElement element) {
        String id = element.getAttribute("id");
        String val = textField.getText();
        
        //spot if this is a clozeBlank field - if so update the answers from the user known
        if(id.startsWith(clozeDev.prefix)) {
            int wordNum = Integer.parseInt(id.substring(clozeDev.prefix.length()));
            clozeDev.updateUserAnswer(wordNum, val);
        }
        super.dataChanged(type, index, htmlC, textField, element);
    }

    /**
     * Action Handler for when the link at the bottom to check answers gets
     * clicked.  Will call the checkAnswers method and then update the form
     * 
     * @param htmlC concerned HTMLComponent
     * @param url URL currently showing
     * @return true to indicate continue with the event
     */
    public boolean linkClicked(HTMLComponent htmlC, String url) {
        System.out.println("Clicked link for " + url);
        if(url.endsWith("#")) {
            clozeDev.checkAnswers();
        }
        return true;
    }
    
    

}
