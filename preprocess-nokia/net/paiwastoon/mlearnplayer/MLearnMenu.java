/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.paiwastoon.mlearnplayer;

import com.sun.lwuit.*;
import com.sun.lwuit.events.*;
import com.sun.lwuit.layouts.BoxLayout;
import com.sun.lwuit.table.TableLayout;
import java.io.*;

/**
 *
 * @author mike
 */
public class MLearnMenu extends Form implements ActionListener{

    private MLearnPlayerMidlet host;
    
    String[] labels = {"Continue", "Repeat", "Back", "Contents",  "Collection", "Open Course", "Settings", "Quit"};
    
    private static final int CONTINUE = 0;
    
    private static final int REPEAT = 1;
    
    private static final int BACK = 2;
    
    private static final int CONTENTS = 3;
    
    private static final int COLLECTION = 4;
    
    private static final int OPENCOURSE = 5;
    
    private static final int SETTINGS = 6;
    
    private static final int QUIT = 7;
    
    
    Button[] buttons;
    
    Command[] cmds;
    
    BoxLayout bLayout;
    
    public MLearnMenu(MLearnPlayerMidlet host) {
        this.host = host;
        buttons = new Button[labels.length];
        cmds = new Command[labels.length];
        
        bLayout = new BoxLayout(BoxLayout.Y_AXIS) ;
        setLayout(bLayout);
        for(int i = 0; i < labels.length; i++) {
            InputStream imgIn = getClass().getResourceAsStream("/icons/menu-" + i + ".png");
            try {
                cmds[i] = new Command(labels[i], Image.createImage(imgIn));
                buttons[i] = new Button(cmds[i]);
                buttons[i].setTextPosition(RIGHT);
                buttons[i].addActionListener(this);
            }catch(IOException e) {
                e.printStackTrace();
                buttons[i] = new Button(labels[i]);
            }
            addComponent(buttons[i]);
        }
    }
    
    public void actionPerformed(ActionEvent ae) {
        Command cmd = ae.getCommand();
        if(cmd != null) {
            if(cmd.equals(cmds[CONTINUE])) {
                host.deviceForm.show();
            }else if(cmd.equals(cmds[REPEAT])) {
                host.showNextDevice(0);
            }else if(cmd.equals(cmds[BACK])) {
                host.showNextDevice(-1);
            }else if(cmd.equals(cmds[CONTENTS])) {
                host.showTOC();
            }else if(cmd.equals(cmds[COLLECTION])) {
                if(host.myTOC.collection != null) {
                    host.openCollectionDir(host.myTOC.colBaseHREF + "/");
                }
            }else if(cmd.equals(cmds[QUIT])) {
                host.destroyApp(true);
                host.notifyDestroyed();
            }
        }
    }
    
}
