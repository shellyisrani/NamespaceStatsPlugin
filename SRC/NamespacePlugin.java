
package com.apelon.modules.dts.editor.namespaceplugin;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.apelon.apelonserver.client.ServerConnection;
import com.apelon.apps.dts.editor.modules.DTSModuleConfig;
import com.apelon.beans.dts.plugin.DTSAppManager;
import com.apelon.dts.client.DTSQuery;
import com.apelon.modules.dts.editor.plugin.DTSConnectionDialog;
import com.apelon.modules.dts.editor.plugin.ModuleConfigUtility;

/**
 * standalone Namespace Stats plugin
 * <p>
 * Copyright (c) 2019 Apelon, Inc. All rights reserved.
 * @since 4.7.1
 */
public class NamespacePlugin extends JFrame  {
        
	static final String NAME = "Namespace Stats Plugin";
	static final String VERSION = "4.7.1";
	static final int YEAR = 2020;
	static final String CONFIG_FILE = "namespacestatsconfig.xml";
    
	private boolean DEBUG = true;
	
    //private NonLocalPanel panel;
	private NamespaceStatsPanel panel;
    private DTSModuleConfig config;
        
    public NamespacePlugin() {
        super(NAME);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.setProperty("org.xml.sax.driver", 
                           "org.apache.xerces.parsers.SAXParser");
        System.setProperty("java.encoding", "utf8"); 
        Container content = getContentPane();
        panel = new NamespaceStatsPanel(null);		//standalone use
        JPanel jpanel = new JPanel(new BorderLayout());
        jpanel.add(panel, "Center");
        content.add(jpanel, "Center");
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
        config = ModuleConfigUtility.getDTSModuleConfig(CONFIG_FILE);
    }
        
    public void requestFocus() {
        panel.requestFocus();
    }
    
    private void getConnection() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	String desc = "", user = "";
        		ServerConnection conn = null;
        		DTSConnectionDialog dialog = new DTSConnectionDialog(NamespacePlugin.this, config);
        		dialog.setVisible(true);
        		if (dialog.getStatus()==DTSConnectionDialog.CONNECT_OPTION) {
        			desc = dialog.getDescriptor();
        			conn = dialog.getConnection();
        			user = dialog.getEJBUser();
        			//psw = dialog.getEJBPassword();
        			//host = dialog.getEJBHost();
        			//port = dialog.getEJBPort();
        		}
        		dialog.dispose();
                if (conn==null) return;
                DTSQuery queryMgr = DTSAppManager.getQuery();
                try {
                    queryMgr.setConnection(conn);
                    //set parameters for background thread
                    //TODO DTSAppManager.setConnectionParameters(host, port, user, psw);
                } catch (Exception e) {
    				JOptionPane.showMessageDialog(NamespacePlugin.this, 
    						"Unable to set connection.", 
    						"Connect Error", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                    conn = null;
                }
                if (DEBUG) System.out.println("QueryManager is "+
                                              (queryMgr.isOpen()?"":"not ")+"open.");
                setConnectionStatus(conn!=null, desc);
                //set user for spec file
                DTSAppManager.setAttribute("dts.user", user);
            }
        });
    }

    private void setConnectionStatus(final boolean b, final String mess) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                panel.enableModule(b);
            }
        });
    }
    
    /********************************************************************************/
    private static void showFrame() {
        final NamespacePlugin module = new NamespacePlugin();
        module.setLocation(
                           (Toolkit.getDefaultToolkit().getScreenSize().width-
                        		   module.getPreferredSize().width)/2,
                           (Toolkit.getDefaultToolkit().getScreenSize().height-
                        		   module.getPreferredSize().height-50)/2);
        module.pack();
        module.setVisible(true);
        module.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        module.requestFocus();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { 
                module.getConnection();
            }
        });
    }
        

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() { showFrame(); }
        });
    }
}

