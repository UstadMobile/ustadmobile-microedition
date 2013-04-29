/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.paiwastoon.mlearnplayer.datatx;
import com.sun.lwuit.*;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.layouts.BoxLayout;
import java.util.Hashtable;
import net.paiwastoon.mlearnplayer.EXEStrMgr;
import net.paiwastoon.mlearnplayer.MLearnMenu;
import net.paiwastoon.mlearnplayer.MLearnUtils;

/**
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
