//NonLocalPropertiesMgr
package com.apelon.modules.dts.editor.namespaceplugin;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import com.apelon.beans.dts.plugin.connection.DtsConnectionListener;
import com.apelon.modules.dts.editor.plugin.AbstractModuleManager;

/**
 * Entry class for DTS Namespace Stats module.
 * <p>
 * Copyright (c) 2019 Apelon, Inc. All rights reserved.
 * @since 4.7.1
 * @version 4.7.1
 */
public class NamespaceStatsPropertiesMgr extends AbstractModuleManager implements DtsConnectionListener {
	
	
    private static final boolean DEBUG = false;
    
    public NamespaceStatsPropertiesMgr() {
    	super(NamespacePlugin.NAME, NamespacePlugin.VERSION);
    }
    
    /** Override to return module menu items. */
    /*
    protected JMenuItem[] getMenuItems(String menuName) {
		return new JMenuItem[]{ getWizardToolItem() };
    }*/
    
    /** Module Architecture. Return whether a module component can be used as a layout panel. */
    @Override
    public boolean isLayoutComponent(String name) {
    	return false;
    }
    
    /** get plug-in menu items */
    @Override
    protected JMenuItem[] getOptionsMenuItems() {
    	return new JMenuItem[] {  getMainItem() };
    }
    
    @Override
    protected JMenuItem[] getHelpMenuItems() {
        return new JMenuItem[] { getAboutItem() };
    }
    
    private JMenuItem getMainItem() {
    	JMenuItem item = buildMenuItem("New "+NamespacePlugin.NAME, -1, "Open New "+NamespacePlugin.NAME, null, null, null, 
    	           new ActionListener() {
                       public void actionPerformed (ActionEvent ae) {
                           moduleSelected(ae);
                       }
    	});
    	item.setEnabled(false);
    	return item;
    }
    	
    private JMenuItem getAboutItem() {
    	return buildMenuItem("About "+NamespacePlugin.NAME, -1, "Show "+NamespacePlugin.NAME+" About Dialog", null, null, null,
    	           new ActionListener() {
                       public void actionPerformed(ActionEvent ae) {
                    	   showAboutPanel(NamespacePlugin.NAME, NamespacePlugin.VERSION, NamespacePlugin.YEAR);
                       }
    	});
    }
    

    /** run the wizard */
    private void moduleSelected(ActionEvent ae) {
        System.out.println("NamespaceStats.starting");
        JPanel panel = new NamespaceStatsPanel(moduleMgr);
        JDialog dialog = moduleMgr.createDialog(panel, NamespacePlugin.NAME, 0, 0);
        //                                        NonLocalPanel.WIDTH, 
        //                                        NonLocalPanel.HEIGHT);
        dialog.setModal(false);
        //allow for capture
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
		 
	 }
	 
}
