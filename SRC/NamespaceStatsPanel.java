//NonLocalPanel
package com.apelon.namespaceplugin;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.apache.log4j.Category;

import com.apelon.apps.dts.editor.modules.DTSEditorModuleMgr;
import com.apelon.beans.dts.data.NamespaceEntry;
import com.apelon.beans.dts.plugin.DTSAppManager;
import com.apelon.dts.client.DTSQuery;
import com.apelon.dts.client.namespace.Namespace;
import com.apelon.dts.client.namespace.NamespaceType;
import com.apelon.namespaceplugin.StatFactory.Attribute;
import com.apelon.namespaceplugin.StatFactory.StatObj;

/**
 * main (tab) panel for NonLocal Properties Creation
 * <p>
 * Copyright (c) 2019 Apelon, Inc. All rights reserved.
 * @since 4.7.1
 */
class NamespaceStatsPanel extends JPanel  {
	
	private boolean DEBUG = true;
	
	final static int WIDTH = 450;
	final static int HEIGHT = 600;

    private DTSEditorModuleMgr moduleMgr;
    private JTextArea textArea;
    private JCheckBox allBox;
    private JButton runButton;
    private TitledBorder outputBorder;
    private JComboBox <NamespaceEntry> spaceCombo;
    //private BufferedWriter outWriter;
    //private final static String SPACE_FILE_NAME = "NSStatSpace.xml";

    NamespaceStatsPanel(DTSEditorModuleMgr mgr) {
    	super(new BorderLayout());
    	moduleMgr = mgr;
    	JPanel topPanel = new JPanel(new BorderLayout());
    	Border title = BorderFactory.createTitledBorder(" Select Namespace ");
    	Border margin = new EmptyBorder(5, 5, 5, 5 );		//add an empty border as a “margin”
    	topPanel.setBorder(new CompoundBorder(title, margin));	//”nest” the borders
    	JPanel panel = new JPanel(new GridBagLayout());
        addComponent(panel, new JLabel("Namespace:", SwingConstants.LEFT), 0, 0, 1, 0.0, 0.0, 
											new Insets(5, 10, 3, 5));	//top, left, bottom, right)
    	spaceCombo = new JComboBox<NamespaceEntry>(); 	//we could add an action listener to the combo to catch a selection, but I’d prefer to make the user press the Run button instead 
        addComponent(panel, spaceCombo, 1, 0, 1, 1.0, 0.0, new Insets(5, 0, 5, 10));	//top, left, bottom, right)
        allBox = new JCheckBox("Include non-local attributes");
        addComponent(panel, allBox, 0, 1, 2, 1.0, 0.0, new Insets(5, 10, 0, 10));	//top, left, bottom, right)
    	topPanel.add(panel, "Center");
    	panel = new JPanel();		//flow layout works here
    	runButton = new JButton("Run Report");
    	panel.add(runButton);		//this will center the button
		//addComponent(panel, runButton, 0, 3, 2, 1.0, 0.0, new Insets(10, 10, 10, 10));
    	topPanel.add(panel, "South");
    	runButton.addActionListener(new ActionListener() { 
    	    public void actionPerformed (ActionEvent ae) {
    	    	  runReport();
    	    }
    	});
    	add(topPanel, "North");
    	//now add the text area
    	JPanel textPanel = new JPanel(new BorderLayout());
    	outputBorder = BorderFactory.createTitledBorder(" Output ");
    	textPanel.setBorder(outputBorder);
    	textArea = new JTextArea();
    	textArea.setWrapStyleWord(true);	//this causes a “word wrap” style, alternately you could have a horizontal scroll bar
        JScrollPane scroll = new JScrollPane(textArea,
        	JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);	//we use word wrapping
    	textPanel.add(scroll, "Center");
    	add(textPanel, "Center");		//the scroll pane wraps the text area for scrolling
    	setPreferredSize(new Dimension(WIDTH, HEIGHT));	//preferred width, height of main panel
        if (mgr!=null) {
        	//plug-in
            enableModule(mgr.getServerConnection()!=null);
        }
        else {
        	//standalone, connection dialog will enable
            enableModule(false);  
        }
    }

    //helper method to add a single height component to a panel using GridBagLayout
    private void addComponent(JPanel panel, JComponent comp, int x, int y, int width, double weightx, double weighty, Insets insets) {
    	//GridBagConstraints(int x, int y, int width, int height, double wtx, double wty, int anchor, int fill, Insets insets, int ipadx, int ipady)
    	panel.add(comp, new GridBagConstraints(x, y, width, 1, weightx, weighty, 
    				GridBagConstraints.EAST, GridBagConstraints.BOTH, insets, 0, 0));
    }
    
    /** enable namespace combo after connection */
    public void enableModule(boolean b) {
        if (b) spaceCombo.setModel(new DefaultComboBoxModel<NamespaceEntry>(getNamespaces()));
        else spaceCombo.setModel(new DefaultComboBoxModel<NamespaceEntry>());
    }
    
    private void runReport() {
    	NamespaceEntry entry = (NamespaceEntry)spaceCombo.getSelectedItem();
    	if(entry == null) return;
    	Namespace space = entry.getNamespace();
    	boolean isOntylog = space.getNamespaceType().equals(NamespaceType.ONTYLOG) || 
							space.getNamespaceType().equals(NamespaceType.ONTYLOG_EXTENSION);
    	outputBorder.setTitle(" "+space.getName()+" ");
    	System.out.println(entry.getNamespace().getName());
    	setBusy(true);
    	textArea.setText("");
    	try {
    		HashMap<Attribute,ArrayList<StatObj>> maps = StatFactory.getStats(space.getId(), allBox.isSelected());
    		writeObjects("Basic Data", maps.get(Attribute.BASIC));
    		writeObjects("Concept Properties", maps.get(Attribute.CONCEPT_PROPS));
    		writeObjects("Concept Synonyms", maps.get(Attribute.CONCEPT_SYNONYMS));
    		if (isOntylog) {
        		writeObjects("Concept Kinds", maps.get(Attribute.CONCEPT_KINDS));
        		writeObjects("Concept Roles", maps.get(Attribute.CONCEPT_ROLES));
        		writeObjects("Inverse Concept Roles", maps.get(Attribute.CONCEPT_INV_ROLES));
    		}
    		writeObjects("Concept Associations", maps.get(Attribute.CONCEPT_ASSNS));
    		writeObjects("Inverse Concept Associations", maps.get(Attribute.CONCEPT_INV_ASSNS));
    		writeObjects("Term Properties", maps.get(Attribute.TERM_PROPS));
    		writeObjects("Term Inverse Synonyms", maps.get(Attribute.TERM_INV_SYNONYMS));
    		writeObjects("Term Associations", maps.get(Attribute.TERM_ASSNS));
    		writeObjects("Inverse Term Associations", maps.get(Attribute.TERM_INV_ASSNS));
    	
    	} catch (Exception e) {
    		e.printStackTrace();
    		JOptionPane.showMessageDialog(this, e.getMessage(), "Stat Error", JOptionPane.ERROR_MESSAGE);
    	}
    	finally {
    		SwingUtilities.invokeLater(new Runnable() {
    			public void run() {
    				textArea.scrollRectToVisible(new Rectangle(0, 0, 1, 1));
    			}
    		});
    		setBusy(false);
    	}
    	
    }
    
    private void writeObjects(String header, ArrayList<StatObj> list) {
		textArea.append(header+"\n");
		for (StatObj obj : list) {
			textArea.append("   "+obj.value+"="+obj.localCount+" "+
								(obj.allCount<0?"":"("+(obj.allCount-obj.localCount)+")")+"\n");
		}
    	textArea.append("\n");
    }
    
  //create an XML file from ‘lines’, returns true if successful
    private boolean createXMLFile(String filename, String[] lines) {
              PrintWriter outWriter = null;	//define it first so it can be referenced in the catch block
              try {
                          //create the file, this will overwrite if it already exists
    	  outWriter = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
    	  outWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");	//add the xml header
    	  //now write the data lines
    	  for (String line : lines) {
    	   	outWriter.println(line); //repeat for each line
        }
        outWriter.close();
        System.out.println("Wrote file"+(new File(filename)).getAbsolutePath());
        return true;
        } catch (Exception e) {
              e.printStackTrace();	//for debugging
        if (outWriter!=null) outWriter.close();
        return false;
         }
    }


    NamespaceEntry[] getNamespaces(){
        NamespaceEntry[] entries = new NamespaceEntry[0];
        DTSQuery queryMgr = DTSAppManager.getQuery();
        try {
            Namespace[] spaces = queryMgr.getNamespaceQuery().getNamespaces();
            int n = spaces.length;
            entries = new NamespaceEntry[n];
            for (int i=0; i < n; i++) {
                entries[i] = new NamespaceEntry(spaces[i]);
            }
            Arrays.sort(entries, 0, entries.length, new Comparator<NamespaceEntry>() {
                public int compare(NamespaceEntry a, NamespaceEntry b) {
                    return a.toString().compareToIgnoreCase(b.toString());
                }
            });
        } catch (Exception e) {
            Category.getInstance("nonlocalproperties").error(e);
            e.printStackTrace();
        }
        return entries;
    }
	
    /**************************************************/
    //cursor methods
    protected void setBusy(boolean b) {
    	if (b) {
    		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    	}
    	else {
    		setCursor(Cursor.getDefaultCursor());
    	}
    }
    

}

