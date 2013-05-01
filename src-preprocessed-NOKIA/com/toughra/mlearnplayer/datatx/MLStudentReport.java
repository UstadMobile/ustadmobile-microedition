/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.toughra.mlearnplayer.datatx;
import com.sun.lwuit.Button;
import com.sun.lwuit.Label;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.table.TableLayout;
import java.io.InputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import com.sun.lwuit.Command;
import com.sun.lwuit.Form;
import java.util.*;
import com.toughra.mlearnplayer.EXEStrMgr;
import com.toughra.mlearnplayer.MLearnMenu;
import com.toughra.mlearnplayer.MLearnPlayerMidlet;
import com.toughra.mlearnplayer.MLearnUtils;

/**
 *
 * @author mike
 */
public class MLStudentReport extends Form implements ActionListener{
    
    Form studentDataFrm;
    
    Hashtable studentData;
    
    Hashtable pkgCombos;
    
    MLearnMenu bkMenu;
    
    Hashtable studentNames;
    
    String[] studentIds;
    
    int numStudents;
    
    int currentStudent;
    
    Command cmdNext;
    static final int CMDNEXTID = 2;
    
    Command cmdPrev;
    static final int CMDPREVID = 4;
    
    public static MLStudentReport genReport(MLearnMenu menu,int sinceDayOffset, int numDaysBack) {
        String dirName = EXEStrMgr.getInstance().getPref("basefolder") 
                        + "/logrx";
        Hashtable pkgComboGen = new Hashtable();
        Hashtable ht = MLStudentReport.makeDataTable(dirName, pkgComboGen, sinceDayOffset, numDaysBack);

        //do a dump of the combo hashtables
        Enumeration e3 = pkgComboGen.elements();
        while(e3.hasMoreElements()) {
            Hashtable item = (Hashtable)e3.nextElement();
            MLLogParser.dumpHT(item);
        }

        Enumeration studNames = MLServerThread.getInstance().getStudentNames().keys();
        String name1 = studNames.nextElement().toString();
        
        MLStudentReport repForm = new MLStudentReport(menu, ht, pkgComboGen);
        repForm.setupForm(name1);
        repForm.show();
        
        return repForm;
    }
    
    
    public MLStudentReport(MLearnMenu bkMenu, Hashtable studentData, Hashtable pkgCombos) {
        this.bkMenu = bkMenu;
        this.studentData = studentData;
        this.pkgCombos = pkgCombos;
        
        cmdNext = new Command(">", CMDNEXTID);
        cmdPrev = new Command("<", CMDPREVID);
        
        addKeyListener(MLearnPlayerMidlet.KEY_NEXT, this);
        addKeyListener(MLearnPlayerMidlet.KEY_PREV, this);
    }
    
    public void setupForm() {
        //find out the students who have data on this system and their names
        studentNames = MLServerThread.getInstance().getStudentNames();
        numStudents = studentNames.size();
        Enumeration studentIdE = studentNames.keys();
        studentIds = new String[numStudents];
        
        int count = 0;
        while(studentIdE.hasMoreElements()) {
            studentIds[count] = studentIdE.nextElement().toString();
            count++;
        }
    }
    
    public void setupForm(String firstStudentName) {
        setupForm();
        makeForm(this, firstStudentName);
    }
    
    private int safeGet(Hashtable ht, String key) {
        Object obj = ht.get(key);
        if(obj != null && obj instanceof Integer) {
            return ((Integer)obj).intValue();
        }
        
        return 0;
    }
    
    public void makeForm(Form frm, String studentId) {
        frm.removeAll();
        String studentName = studentNames.get(studentId).toString();
        Hashtable studCombos = (Hashtable)pkgCombos.get(studentId);
        int numPkgCombos = studCombos.size();
        
        //make this according to number of packages
        TableLayout tLayout = new TableLayout((numPkgCombos*5)+3, 2);
        frm.setLayout(tLayout);
        
        Label nameHeader = new Label(studentName);
        TableLayout.Constraint nameHeadConst = tLayout.createConstraint();
        nameHeadConst.setHorizontalSpan(2);
        frm.addComponent(nameHeadConst, nameHeader);
        
        Enumeration pkgE = studCombos.keys();
        while(pkgE.hasMoreElements()) {
            String colPkgId = pkgE.nextElement().toString();
            String prefix = studentId + "/" + colPkgId;
            
            int time = safeGet(studentData, prefix + ":time");
            String[] storeKeys = MLLogParser.STOREKEYS;
            int[] scoreVals = new int[storeKeys.length];
            for(int i = 0; i < storeKeys.length; i++) {
                scoreVals[i] = safeGet(studentData, prefix + ":" + storeKeys[i]);
            }
            String pkgId = colPkgId.substring(colPkgId.indexOf('/')+1);
            
            Label pkgLabel = new Label(pkgId);
            TableLayout.Constraint pkgConst = tLayout.createConstraint();
            pkgConst.setHorizontalSpan(2);
            frm.addComponent(pkgConst, pkgLabel);
            
            Label timeLabel = new Label(MLearnUtils._("time"));
            TableLayout.Constraint tConst = tLayout.createConstraint();
            tConst.setWidthPercentage(30);
            frm.addComponent(tConst, timeLabel);
            
            int timeMin = ((time / 1000) / 60);
            
            
            Label timeVal = new Label(String.valueOf(timeMin)+"min");
            TableLayout.Constraint tvConst = tLayout.createConstraint();
            tConst.setWidthPercentage(70);
            frm.addComponent(timeVal);
            
            for(int i = 0; i < storeKeys.length; i++) {
                Label lbl = new Label(MLearnUtils._(storeKeys[i]));
                TableLayout.Constraint lblConst = tLayout.createConstraint();
                frm.addComponent(lblConst, lbl);
                
                Label valLbl = new Label(String.valueOf(scoreVals[i]));
                TableLayout.Constraint valConst = tLayout.createConstraint();
                frm.addComponent(valConst, valLbl);
            }
            
            
        }
        Button bkButton  = new Button(cmdPrev);
        bkButton.addActionListener(this);
        TableLayout.Constraint bkConst = tLayout.createConstraint();
        bkConst.setHorizontalAlign(LEFT);
        frm.addComponent(bkConst, bkButton);
        
        Button nxtButton = new Button(cmdNext);
        nxtButton.addActionListener(this);
        TableLayout.Constraint nxtConst = tLayout.createConstraint();
        nxtConst.setHorizontalAlign(RIGHT);
        frm.addComponent(nxtConst, nxtButton);
        
        
        Button menuBkButton = new Button("Back");
        TableLayout.Constraint menuConst = tLayout.createConstraint();
        menuConst.setHorizontalSpan(2);
        frm.addComponent(menuConst, menuBkButton);
        menuBkButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                bkMenu.show();
            }
        });
        frm.scrollComponentToVisible(nameHeader);
    }
    
    public static Hashtable makeDataTable(String dirName, Hashtable pkgComboTbl, int startSinceDays, int goBackDays) {
        Hashtable studentData = new Hashtable();
        
        
        java.util.Calendar startCal = java.util.Calendar.getInstance();
        long startSinceMs = (startSinceDays * 24 * 60 * 60 * 1000);
        Date startDate = new Date(System.currentTimeMillis() - (startSinceMs));
        startCal.setTime(startDate);
        
        long goBackMs = (goBackDays * 24 * 60 * 60 * 1000);
        long lastTimeMs = (System.currentTimeMillis() - startSinceMs - goBackMs);
        Date endDate = new Date(lastTimeMs);
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(endDate);
        
        try {
            FileConnection fcon = (FileConnection)Connector.open(dirName);
            Vector logNames = new Vector();
            Enumeration e = fcon.list();
            while(e.hasMoreElements()) {
                logNames.addElement(e.nextElement());
            }
            fcon.close();
            
            int numFiles = logNames.size();
            
            Calendar logCal = Calendar.getInstance();
            
            for(int i = 0; i< numFiles; i++) {
                /*
                 * First check and make sure that this log is in the date range
                 * we want to consider for the report
                 */
                String baseName = logNames.elementAt(i).toString();
                int dashPos = baseName.indexOf('-');//first dash
                String uuid = baseName.substring(0, dashPos);
                String dateStr = baseName.substring(dashPos+1);
                
                String yearStr = dateStr.substring(0, 4);
                String monStr = dateStr.substring(5, 7);
                String dayStr = dateStr.substring(8, 10);
                
                logCal.set(Calendar.YEAR, Integer.parseInt(yearStr));
                //TODO: this will need to be bumped up one
                logCal.set(Calendar.MONTH, Integer.parseInt(monStr));
                logCal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dayStr));
                EXEStrMgr.po("Log calendar = " + logCal.toString(), EXEStrMgr.DEBUG);
                if(logCal.after(startCal) || logCal.before(endCal)) {
                    EXEStrMgr.po("  Do not include this log", EXEStrMgr.DEBUG);
                    continue;
                }
                        
                String logName = dirName + "/" + logNames.elementAt(i).toString();
                fcon = (FileConnection)Connector.open(logName);
                InputStream fin = null;
                try {
                    fin = fcon.openInputStream();
                    MLLogParser.parseLog(fin, studentData, pkgComboTbl);
                }catch(Exception e2) {
                    EXEStrMgr.po("Exception parsing " + logName, EXEStrMgr.DEBUG);
                }finally {
                    try { fin.close(); fcon.close(); }
                    catch(Exception ig) {}
                }
            }
            
        }catch(Exception e) {
            
        }
        
        return studentData;
    }

    public void actionPerformed(ActionEvent ae) {
        int incBy = 0;
        
        if(ae.getSource() instanceof Button) {
            int cmdId = ae.getCommand().getId();
            if(cmdId == CMDNEXTID) {
                incBy = 1;
            }else if(cmdId == CMDPREVID) {
                incBy = -1;
            }
        }else if(ae.getSource() == this) {
            int key = ae.getKeyEvent();
            if(key == MLearnPlayerMidlet.KEY_NEXT) {
                incBy = 1;
            }else if(key == MLearnPlayerMidlet.KEY_PREV) {
                incBy = -1;
            }
        }
        
        
        
        int destIndex = currentStudent + incBy;
        if(destIndex >= 0 && destIndex < numStudents) {
            makeForm(this, studentIds[destIndex]);
            repaint();
            currentStudent = destIndex;
        }
    }
    
    
}
