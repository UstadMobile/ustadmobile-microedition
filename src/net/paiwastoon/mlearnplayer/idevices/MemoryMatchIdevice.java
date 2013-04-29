/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.paiwastoon.mlearnplayer.idevices;

import net.paiwastoon.mlearnplayer.*;
import net.paiwastoon.mlearnplayer.xml.*;
import com.sun.lwuit.*;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.impl.LWUITImplementation;
import com.sun.lwuit.table.TableLayout;
import java.util.Vector;


/**
 *
 * @author mike
 */
public class MemoryMatchIdevice extends Idevice implements ActionListener{
    
    //whether those tiles that are matched are hidden
    boolean hideMatches;
    
    Image coverImage;
    
    String coverImgSrc;
    
    Form frm;
    
    MemoryMatchPair[] pairs;
    
    MemoryMatchCell[] cells;
    
    MemoryMatchCell selected;
    
    int numPairsMatched;
    
    //html of positive feedback
    String fbPos;
    
    //html of negative feedback
    String fbNeg;
    
    //temporary holding variable for feedback about to show
    String fbTmp;
    
    //cell width in pixels
    int cellWidth = 50;
    
    //cell height in pixels
    int cellHeight = 50;
    
    //number rows
    int rows = 2;
    
    //number cols
    int cols = 2;
    
    //the time that a cell will show before feedback displays
    int cellShowTime = 500;
    
    TableLayout tLayout;
    
    //whether or not to split the array into questions and answers
    boolean splitPairs = false;

    public MemoryMatchIdevice(MLearnPlayerMidlet host, XmlNode rootData) {
        super(host);
        
        XmlNode gameNd = rootData.findFirstChildByTagName("memmatch", true);
        coverImgSrc = gameNd.getAttribute("covertile");
        
        Vector pairNodes = gameNd.findChildrenByTagName("pair", true);
        pairs = new MemoryMatchPair[pairNodes.size()];
        
        try {
            coverImage = Image.createImage(hostMidlet.getInputStreamReaderByURL(coverImgSrc));
        }catch(Exception e) {
            //should not happen really...
            System.err.println("Bugger mem match cannot load " + coverImgSrc);
            coverImage = Image.createImage(100, 100, 0xff0000);
        }
        
        for(int i = 0; i < pairs.length; i++) {
            XmlNode pairNode = (XmlNode)pairNodes.elementAt(i);
            String qText = pairNode.findFirstChildByTagName("question", true).getTextChildContent(0);
            String aText = pairNode.findFirstChildByTagName("answer", true).getTextChildContent(0);
            pairs[i] = new MemoryMatchPair(this, host);
            pairs[i].aCell = pairs[i].mkCell(aText);
            pairs[i].qCell = pairs[i].mkCell(qText);
        }
        
        fbPos = gameNd.findFirstChildByTagName("positivefeedback", true).getTextChildContent(0);
        fbNeg = gameNd.findFirstChildByTagName("negativefeedback", true).getTextChildContent(0);
        rows = Integer.parseInt(gameNd.getAttribute("rows"));
        cols = Integer.parseInt(gameNd.getAttribute("cols"));
        
        if(gameNd.getAttribute("splitpairs").equalsIgnoreCase("true")) {
            splitPairs = true;
        }
    }
    
    

    public void actionPerformed(ActionEvent ae) {
        Component sCmp = ae.getComponent();
        if(sCmp != null && sCmp instanceof MemoryMatchCell) {
            MemoryMatchCell cell = (MemoryMatchCell)ae.getComponent();
            
            cell.changeState(MemoryMatchCell.STATE_SHOWING);
            
            if(selected == null) {
                selected = cell;
            }else {                
                //we have a pair selected - find out if this is a match
                boolean isMatch = selected.isMatch(cell);
                
                fbTmp = isMatch ? fbPos : fbNeg;
                if(isMatch) {
                    numPairsMatched++;
                }
                
                new MMatchFBThread(this).start();
                
                //TODO : Allow the user to see what they have selected before showing feedback
                if(!isMatch) {
                    new HideCellThread(cell, selected, this, hostMidlet.fbTime/3).start();
                }
                selected = null;
                
                if(numPairsMatched == pairs.length) {
                    //All done here
                    
                }
            }
        }
    }
    
    public void doFeedback() {
        FeedbackDialog fbDialog = new FeedbackDialog(hostMidlet);
        fbDialog.showFeedback(frm, fbTmp);
    }
    
    

    public void dispose() {
        super.dispose();
        frm = null;
        coverImage = null;
    }

    public Form getForm() {
        if(frm == null) {
            frm = new Form();
            cells = new MemoryMatchCell[pairs.length*2];
            tLayout = new TableLayout(rows, cols);
            frm.setLayout(tLayout);
            
            if(splitPairs) {
                int numPairs = pairs.length;
                for(int i = 0; i < pairs.length; i++) {
                    cells[i] = pairs[i].qCell;
                    cells[i+numPairs] = pairs[i].aCell;
                }
                Object[] cellArr = (Object[])cells;
                
                //shuffle each part separately
                MLearnUtils.shuffleArray(cellArr, 0, numPairs);
                MLearnUtils.shuffleArray(cellArr, numPairs, numPairs);
                
            }else {
                for(int i = 0; i < pairs.length; i++) {
                    cells[i*2] = pairs[i].aCell;
                    cells[(i*2)+1] = pairs[i].qCell;
                }
                MLearnUtils.shuffleArray(cells);
            }
            
            for(int r = 0; r < rows; r++) {
                for(int c = 0; c < cols; c++) {
                    TableLayout.Constraint tlc = tLayout.createConstraint(r, c);
                    
                    //index is sequential from top left to bottom right (by col, then row)
                    int index = (r * cols) + c;
                    frm.addComponent(tlc, cells[index]);
                }
            }
        }
        
        return frm;
    }

    public void start() {
        super.start();
        numPairsMatched = 0;
    }

    public void stop() {
        super.stop();
    }

    public int getLogType() {
        return Idevice.LOG_QUIZ;
    }

    //NOTE: this is not strictly speaking correct - does not count incorrect guesses as this is somewhat random
    //TODO: Add a numGuesses to the additional string in the log
    public int[] getScores() {
        return new int[] {
            numPairsMatched, numPairsMatched, pairs.length
        };
    }
    
   
    
    
    public int getMode() {
        return Idevice.MODE_LWUIT_FORM;
    }
    
    
    
}
class MemoryMatchPair {
    
    MemoryMatchIdevice idevice;
    
    MLearnPlayerMidlet midlet;
    
    MemoryMatchPair(MemoryMatchIdevice idevice, MLearnPlayerMidlet midlet) {
        this.idevice = idevice;
        this.midlet = midlet;
    }
    
    public MemoryMatchCell mkCell(String txt) {
        MemoryMatchCell cell =  new MemoryMatchCell(idevice, midlet, idevice.coverImage, txt, this);
        cell.addActionListener(idevice);
        return cell;
    }
    
    //question cell
    MemoryMatchCell qCell;
    //answer cell
    MemoryMatchCell aCell;
}

class HideCellThread extends Thread {
    
    private MemoryMatchCell c1;
    private MemoryMatchCell c2;
    private MemoryMatchIdevice device;
    int sleepTime;
    
    public HideCellThread(MemoryMatchCell c1, MemoryMatchCell c2, MemoryMatchIdevice device, int sleepTime) {
        this.c1 = c1;
        this.c2 = c2;
        this.device = device;
        this.sleepTime = sleepTime;
    }
    
    public void run() {
        try { Thread.sleep(sleepTime); }
        catch(InterruptedException e) {}
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            public void run() {
                c1.changeState(MemoryMatchCell.STATE_COVERED);
                c2.changeState(MemoryMatchCell.STATE_COVERED);
            }
        });
    }
}


class MMatchFBThread extends Thread {
    
    MemoryMatchIdevice idevice;
    
    MMatchFBThread(MemoryMatchIdevice idevice) {
        this.idevice = idevice;
    }
    
    public void run() {
        try { Thread.sleep(idevice.cellShowTime); }
        catch(InterruptedException e) {}
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            public void run() {
                idevice.doFeedback();
            }
        });
    }
}
