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

import com.toughra.mlearnplayer.xml.XmlNode;
import com.toughra.mlearnplayer.FeedbackDialog;
import com.toughra.mlearnplayer.Idevice;
import com.toughra.mlearnplayer.MLearnPlayerMidlet;
import com.toughra.mlearnplayer.MLearnUtils;
import com.sun.lwuit.*;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.table.TableLayout;
import com.toughra.mlearnplayer.EXEStrMgr;
import java.util.Random;
import java.util.Vector;


/**
 * Idevice that implements the MemoryMatch exercise.  Forms a grid of 
 * cells that the user has to match between.  Cells can only be shown
 * one pair at a time.
 * 
 * @author mike
 */
public class MemoryMatchIdevice extends Idevice implements ActionListener{
    
    /**whether those tiles that are matched are hidden*/
    boolean hideMatches;
    
    /** the cover image to use before a tile is revealed*/
    Image coverImage;
    
    //** image source for cover image */
    String coverImgSrc;
    
    /** The main form*/
    Form frm;
    
    /** Pair objects that represent which ones are actually matches */
    MemoryMatchPair[] pairs;
    
    /** The cells that we are using in the grid*/
    MemoryMatchCell[] cells;
    
    /** the currently selected cell*/
    MemoryMatchCell selected;
    
    /** number of pairs matched so far */
    int numPairsMatched;
    
    /**html of positive feedback*/
    String fbPos;
    
    /**html of negative feedback*/
    String fbNeg;
    
    /**temporary holding variable for feedback about to show*/
    String fbTmp;
    
    /**cell width in pixels*/
    int cellWidth = 50;
    
    /**cell height in pixels*/
    int cellHeight = 50;
    
    /**number rows*/
    int rows = 2;
    
    /**number cols*/
    int cols = 2;
    
    /*the time that a cell will show before feedback displays (in ms)*/
    int cellShowTime = 500;
    
    /**LayoutManager to use */
    TableLayout tLayout;
    
    /**whether or not to split the array into questions and answers*/
    boolean splitPairs = false;
    
    /** Running counter of how many attempts got made*/
    int numMatchAttempts = 0;

    /**
     * Constructor
     * 
     * @param host our midlet host
     * @param rootData the XML data that we need
     */
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

    public String getDeviceTypeName() {
        return "memmatch";
    }
    
    
    
    

    /**
     * Event handler that deals with when the main button is pushed
     * 
     * @param ae ActionEvent
     */
    public void actionPerformed(ActionEvent ae) {
        Component sCmp = ae.getComponent();
        if(sCmp != null && sCmp instanceof MemoryMatchCell) {
            MemoryMatchCell cell = (MemoryMatchCell)ae.getComponent();
            
            cell.changeState(MemoryMatchCell.STATE_SHOWING);
            
            if(selected == null) {
                selected = cell;
            }else {                
                numMatchAttempts++;
                
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
    
    
    /**
     * Show the feedback
     */
    public void doFeedback() {
        FeedbackDialog fbDialog = new FeedbackDialog(hostMidlet);
        fbDialog.showFeedback(frm, fbTmp);
    }
    
    

    /**
     * Dispose of everything
     */
    public void dispose() {
        super.dispose();
        frm = null;
        coverImage = null;
    }

    /**
     * Shuffle an array (because java.util.Collections is not in j2me)
     * 
     * @param array Array to shuffle
     * @param start starting pos
     * @param length length to shuffle through
     */
    public static void shuffleCells(MemoryMatchCell[] arrObj, int start, int length) {
        
        Random r = new Random();
        
        for(int i = start; i < (start + length); i++) {
            int randomOffset = r.nextInt(length);
            int rIndex = randomOffset + start;
            MemoryMatchCell tmp = arrObj[rIndex];
            arrObj[rIndex] = arrObj[i];
            arrObj[i] = tmp;
        }
    }
    
    /**
     * Returns the main LWUIT form to use
     * 
     * @return main LWUIT Form
     */
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
                shuffleCells(cells, 0, numPairs);
                shuffleCells(cells, numPairs, numPairs);
                
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

    /**
     * Start the idevice
     */
    public void start() {
        super.start();
        numPairsMatched = 0;
    }

    /**
     * Stop the idevice
     */
    public void stop() {
        super.stop();
        //for the answer verb
        EXEStrMgr.lg(this, //idevice
                0, //question id
                0, //time on device in ms
                0, //num correctly answered
                0, //num answered correct first attempt
                0, //num questions attempted
                EXEStrMgr.VERB_ANSWERED, //verb
                numPairsMatched, //score
                pairs.length, //maxScorePossible
                Idevice.BLANK,//answer given 
                Idevice.BLANK);//remarks
        
        //for the special idevicecomplete verb
        EXEStrMgr.lg(this, //idevice
                0, //question id
                getTimeOnDevice(), //time on device in ms
                numPairsMatched, //num correctly answered
                numPairsMatched, //num answered correct first attempt
                numPairsMatched, //num questions attempted
                EXEStrMgr.VERB_ANSWERED, //verb
                0, //score
                0, //maxScorePossible
                Idevice.BLANK,//answer given 
                Idevice.BLANK);//remarks
        
    }

    
    /**
     * This is a quiz style idevice
     * @return Idevice.LOG_QUIZ
     */
    public int getLogType() {
        return Idevice.LOG_QUIZ;
    }

    //NOTE: this is not strictly speaking correct - does not count incorrect guesses as this is somewhat random
    //TODO: Add a numGuesses to the additional string in the log
    /**
     * Provides the quiz score data for logging
     * @return array of quiz scores
     */
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
