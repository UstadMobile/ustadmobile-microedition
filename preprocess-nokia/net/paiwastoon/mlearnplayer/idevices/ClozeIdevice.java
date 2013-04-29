/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.paiwastoon.mlearnplayer.idevices;

import com.sun.lwuit.Component;
import com.sun.lwuit.Dialog;
import com.sun.lwuit.Form;
import com.sun.lwuit.Label;
import com.sun.lwuit.TextArea;
import com.sun.lwuit.TextField;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.html.DefaultHTMLCallback;
import com.sun.lwuit.html.HTMLComponent;
import com.sun.lwuit.html.HTMLElement;
import java.util.Vector;
import net.paiwastoon.mlearnplayer.Idevice;
import net.paiwastoon.mlearnplayer.MLearnPlayerMidlet;
import net.paiwastoon.mlearnplayer.MLearnUtils;
import net.paiwastoon.mlearnplayer.xml.XmlNode;


/**
 *
 * @author mike
 */
public class ClozeIdevice  extends Idevice{
    
    String formHTML;
    
    Form frm;
    
    String[] words;
    
    HTMLComponent comp;
    
    String[] currentAnswers;
    
    String scoreStr;

    public ClozeIdevice(MLearnPlayerMidlet hostMidlet, XmlNode nodeData) {
        super(hostMidlet);
        formHTML = MLearnUtils.getTextProperty(nodeData, "formhtml");
        scoreStr = MLearnUtils.getTextProperty(nodeData, "scoretext");
        Vector wordChldrn = nodeData.findChildrenByTagName("word", true);
        int numWords = wordChldrn.size();
        words = new String[numWords];
        currentAnswers = new String[numWords];
        for(int i = 0; i < numWords; i++) {
            words[i] = ((XmlNode)wordChldrn.elementAt(i)).getAttribute("value");
            currentAnswers[i] = "";
        }
    }
    
    public int getMode() {
        return Idevice.MODE_LWUIT_FORM;
    }

    public Form getForm() {
        if(frm == null) {
            ClozeHTMLCallback cb = new ClozeHTMLCallback();
            cb.clozeDev = this;
            comp = hostMidlet.makeHTMLComponent(formHTML, cb);
            comp.setEventsEnabled(true);
            frm = new Form();
            frm.addComponent(comp);
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
            String idStr = "id=\"clozeBlank" +i + "\"";
            
            int idPos = newHTML.toString().indexOf(idStr);
            
            //now delete the old value="" string
            int valueStart = idPos + idStr.length() + 1;
            
            newHTML.delete(valueStart, valueStart+8);
            String newAttribs = " value=\"" + currentAnswers[i] + "\" ";
            
            if(words[i].equals(currentAnswers[i])) {
                //correct
                newAttribs += "style='background-color: green' ";
                numCorrect++;
            }else {
                //wrong
                newAttribs += "style='background-color: red' ";
            }
            newHTML.insert(idPos, newAttribs);
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
    
}

class ClozeHTMLCallback extends DefaultHTMLCallback{
    
    ClozeIdevice clozeDev;

    public void actionPerformed(ActionEvent ae, HTMLComponent htmlc, HTMLElement htmle) {

        System.out.println("EVENT");
        
    }

    public void dataChanged(int type, int index, HTMLComponent htmlC, TextField textField, HTMLElement element) {
        String id = element.getAttribute("id");
        String val = textField.getText();
        String prefix = "clozeBlank";
        if(id.startsWith(prefix)) {
            int wordNum = Integer.parseInt(id.substring(prefix.length()));
            clozeDev.updateUserAnswer(wordNum, val);
        }
        super.dataChanged(type, index, htmlC, textField, element);
    }

    public String fieldSubmitted(HTMLComponent htmlC, TextArea ta, String actionURL, String id, String value, int type, String errorMsg) {
        System.out.println("submit: " +  value);
        return super.fieldSubmitted(htmlC, ta, actionURL, id, value, type, errorMsg);
    }

    public boolean linkClicked(HTMLComponent htmlC, String url) {
        System.out.println("Clicked link for " + url);
        if(url.endsWith("#")) {
            clozeDev.checkAnswers();
        }
        return true;
    }
    
    

}
