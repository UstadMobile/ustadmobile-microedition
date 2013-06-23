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
package com.toughra.mlearnplayer.datatx;
import com.sun.lwuit.*;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.layouts.BoxLayout;
import java.util.Hashtable;
import com.toughra.mlearnplayer.EXEStrMgr;
import com.toughra.mlearnplayer.MLearnMenu;
import com.toughra.mlearnplayer.MLearnUtils;

/**
 * Used to request a report to be made - however this is actually not in use
 * because it's pretty damn slow - this is better done in the cloud.
 * 
 * @author mike
 */
public class MLReportRequest extends Form implements ActionListener{
    
    public static int[] sinceOptions = {0, 7, 14, 30};
    
    MLearnMenu hostMenu;
    
    Command cmdCancel;
    static final int CMDCANCEL = 20;
    
    Command cmdReport;
    static final int CMDREPORT = 21;
    
    ComboBox sinceComboBox;
    
    TextField numDaysTextField;
    
    public MLReportRequest(MLearnMenu hostMenu){
        this.hostMenu = hostMenu;
        
        cmdCancel = new Command(MLearnUtils._("Cancel"), CMDCANCEL);
        cmdReport = new Command(MLearnUtils._("Get Report"), CMDREPORT);
    }
    
    public void setupForm() {
        String[] sinceOptionsStr = new String[sinceOptions.length];
        for(int i = 0; i< sinceOptions.length; i++) {
            if(sinceOptions[i] == 0) {
                sinceOptionsStr[i] = MLearnUtils._("now");
            }else {
                sinceOptionsStr[i] = sinceOptions[i] + " days ago";
            }
        }
        
        setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        
        
        Label sinceLabel = new Label(MLearnUtils._("Since:"));
        addComponent(sinceLabel);
        sinceComboBox = new ComboBox(sinceOptionsStr);
        addComponent(sinceComboBox);
        
        Label numDaysLabel = new Label(MLearnUtils._("Going back how many days?"));
        addComponent(numDaysLabel);
        
        numDaysTextField = new TextField();
        numDaysTextField.setText("7");
        addComponent(numDaysTextField);
        
        Button cancelButton = new Button(cmdCancel);
        cancelButton.addActionListener(this);
        addComponent(cancelButton);
        
        Button report1 = new Button(cmdReport);
        report1.addActionListener(this);
        addComponent(report1);
    }

    public void actionPerformed(ActionEvent ae) {
        int cmdId = ae.getCommand().getId();
        switch(cmdId) {
            case CMDCANCEL:
                hostMenu.show();
                break;
            case CMDREPORT:
                int sinceTime = sinceOptions[sinceComboBox.getSelectedIndex()];
                int backDays = Integer.parseInt(numDaysTextField.getText());

                MLStudentReport.genReport(hostMenu, sinceTime, backDays);
        }

    }
    
    
}
