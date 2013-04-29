/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.paiwastoon.mlearnplayer.idevices;
import com.sun.lwuit.*;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.layouts.BoxLayout;
import com.sun.lwuit.table.TableLayout;
import java.io.InputStream;
import java.util.Vector;
import net.paiwastoon.mlearnplayer.*;
import net.paiwastoon.mlearnplayer.xml.XmlNode;


/**
 *
 * @author mike
 */
public class MissingLetterIdevice extends Idevice implements ActionListener {

    String positiveFb;
    
    String negativeFb;
    
    String completeFb;
    
    Form mainFrm;
    
    Container buttonContainer;
    
    Label imageLabel;
    
    //the current level
    public int currentIndex;
    
    MissingLetterLevel[] levels;
    
    long freeMem;
    
    int attemptCount = 0;
    
    public MissingLetterIdevice(MLearnPlayerMidlet host, XmlNode data ) {
        super(host);
        
        Vector levelVector = data.findChildrenByTagName("missingletterelmt", true);
        
        int numLevels = levelVector.size();
        levels = new MissingLetterLevel[numLevels];
        
        for(int i = 0; i < numLevels; i++) {
            XmlNode currentLevel = (XmlNode)levelVector.elementAt(i);
            levels[i] = new MissingLetterLevel(currentLevel.getAttribute("imgsrc"),
                    currentLevel.findFirstChildByTagName("choices", true).getTextChildContent(0));
        }
        
        positiveFb = data.findFirstChildByTagName("correctfeedback", true).getTextChildContent(0);
        negativeFb = data.findFirstChildByTagName("negativefeedback", true).getTextChildContent(0);
        completeFb = data.findFirstChildByTagName("completeoverlay", true).getTextChildContent(0);
    }
    
    
    
    public void showLevel(int levelNum) {
        freeMem = MLearnUtils.checkFreeMem();
        buttonContainer.removeAll();
        attemptCount = 0;
        
        for(int i = 0; i < levels[levelNum].choices.length; i++) {
            Button b = new Button(new Command(levels[levelNum].choices[i]));
            b.addActionListener(this);
            buttonContainer.addComponent(b);
        }
        
        try {
            InputStream reader = hostMidlet.getInputStreamReaderByURL(
                    levels[levelNum].imgURL);
            imageLabel.setIcon(Image.createImage(reader));
            reader.close();
        }catch(Exception e) {
            System.err.println("Error showing message");
            e.printStackTrace();
        }
        
        currentIndex = levelNum;
        
    }

    public void actionPerformed(ActionEvent ae) {
        String cmdStr = ae.getCommand().getCommandName();
        FeedbackDialog fbDialog = new FeedbackDialog(hostMidlet);
        boolean wasCorrect = false;
        if(cmdStr.equals(levels[currentIndex].correctChoice)) {
            fbDialog.showFeedback(mainFrm, positiveFb, true);
            wasCorrect = true;
            if(attemptCount == 0) {
                correctFirst++;
            }
        }else {
            attemptCount++;
            fbDialog.showFeedback(mainFrm, negativeFb, false);
        }
        
        if(wasCorrect) {
            if(currentIndex < levels.length - 1) {
                try { Thread.sleep(1000); }
                catch(InterruptedException e) {}
                showLevel(currentIndex + 1);
            }else {
                //fbDialog.showFeedback(mainFrm, completeFb, true);
            }
        }
    }
    
    
    
    public int getMode() {
        return Idevice.MODE_LWUIT_FORM;
    }

    public void dispose() {
        super.dispose();
    }

    public Form getForm() {
        //make buttons
        if(mainFrm == null) {
            mainFrm = new Form();
            buttonContainer = new Container();
            buttonContainer.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
            TableLayout tLayout = new TableLayout(1,2);
            mainFrm.setLayout(tLayout);
            TableLayout.Constraint buttonConst = tLayout.createConstraint();
            buttonConst.setWidthPercentage(30);
            mainFrm.addComponent(buttonConst, buttonContainer);
            imageLabel = new Label();
            TableLayout.Constraint imgConst = tLayout.createConstraint();
            imgConst.setWidthPercentage(70);
            mainFrm.addComponent(imgConst, imageLabel);
        }
        
        return mainFrm;
    }

    public void start() {
        showLevel(0);
    }

    public void stop() {
        
    }
    
    
    
}
class MissingLetterLevel {
    
    String imgURL;
    
    String[] choices;
    
    String correctIndex;
    
    static final String sepStr = ",";
    
    String correctChoice;
    
    MissingLetterLevel(String imgURL, String choiceList) {
        this.imgURL = imgURL;
        Vector choicesVector = new Vector();
        int nextIndex = -1;
        int lastIndex = 0;
        
        //check the last character, trim it off if required
        if(!choiceList.endsWith(sepStr)) {
            choiceList = choiceList + ",";
        }
        
        while((nextIndex = choiceList.indexOf(",", lastIndex)) != -1) {
            String thisToken = choiceList.substring(lastIndex, nextIndex);
            choicesVector.addElement(thisToken);
            lastIndex = nextIndex + 1;
        }
        
        choices = new String[choicesVector.size()];
        
        choicesVector.copyInto(choices);
        correctChoice = choices[0];
        MLearnUtils.shuffleArray(choices);
    }
    
    
}
