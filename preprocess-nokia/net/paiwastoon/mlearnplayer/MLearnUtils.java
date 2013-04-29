/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.paiwastoon.mlearnplayer;

import com.sun.lwuit.Graphics;
import com.sun.lwuit.Image;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import java.util.Random;
import java.util.Vector;
import net.paiwastoon.mlearnplayer.xml.XmlNode;

/**
 *
 * @author mike
 */
public class MLearnUtils {
    
    public static final int  RESIZE_MAXPROPORTION = 0;
    
    public static final int  RESIZE_BOUND_X = 1;
    
    public static final int  RESIZE_BOUND_Y = 2;
    
    public static final int  RESIZE_FILL = 3;
    
    
    /**
     * Gets the text of the first node to match a given name - useful for
     * &lt;propname&gt;Text val&lt;/propname&gt;
     * @param parent
     * @param nodeName
     * @return 
     */
    public static String getTextProperty(XmlNode parent, String nodeName) {
        XmlNode node = (XmlNode)parent.findChildrenByTagName(nodeName, true).elementAt(0);
        return node.getTextChildContent(0);
    }
    
    
    /**
     * Will fire an action event to all listeners in the vector
     * Every element should be an action listener
     * 
     * @param listeners 
     */
    public static void fireActionEventToAll(Vector listeners, ActionEvent evt) {
        if(listeners == null) {
            return;
        }
        
        int numListeners = listeners.size();
        for(int i = 0; i < numListeners; i++) {
            ((ActionListener)listeners.elementAt(i)).actionPerformed(evt);
        }
    }
    
    /**
     * Gets the index in the array of a given item.  Uses strict (object) 
     * equality not .equals - e.g. must refer to exact same object
     * 
     * @param arr Array to look in - single dimensional array
     * @param obj
     * @return 
     */
    public static int indexInArray(Object arr, Object obj) {
        Object[] objArr = (Object[])arr;
        int numItems = objArr.length;
        for(int i = 0; i < numItems; i++) {
            if(objArr[i] == obj) {
                return i;
            }
        }
        
        return -1;
    }
    
    public static void shuffleArray(Object array) {
        Object[] arrObj = (Object[])array;
        Random r = new Random();
        int numItems = arrObj.length;
        for(int i = 0; i < numItems; i++) {
            int randomPos = r.nextInt(numItems);
            Object tmp = arrObj[randomPos];
            arrObj[randomPos] = arrObj[i];
            arrObj[i] = tmp;
        }
    }
    
    /**
     * Resize an image
     * 
     * @param src
     * @param width
     * @param height
     * @param strategy - one of the strategy flags - right now we just run fill
     * @return 
     */
    public static Image resizeImage(Image src, int width, int height, int strategy) {
        Image res = Image.createImage(width, height);
        Graphics g = res.getGraphics();
        g.drawImage(src, 0, 0, width, width);
        return res;
    }
    
}
