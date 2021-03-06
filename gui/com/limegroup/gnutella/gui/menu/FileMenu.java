package com.limegroup.gnutella.gui.menu;

import javax.swing.Action;
import javax.swing.JMenuItem;

import org.limewire.util.OSUtils;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.actions.FileMenuActions;

/**
 * Handles all of the contents of the file menu in the menu bar.
 */
final class FileMenu extends AbstractMenu {

	/**
	 * Creates a new <tt>FileMenu</tt>, using the <tt>key</tt> 
	 * argument for setting the locale-specific title and 
	 * accessibility text.
	 *
	 * @param key the key for locale-specific string resources unique
	 *            to the menu
	 */
	FileMenu() {
	    super(I18n.tr("&File"));
		
	    MENU.add(createMenuItem(new FileMenuActions.SendFileAction()));
	    MENU.addSeparator();
	    MENU.add(createMenuItem(new FileMenuActions.OpenMagnetTorrentAction()));
		MENU.add(createMenuItem(new FileMenuActions.CreateTorrentAction()));
	    
		if(!OSUtils.isMacOSX()) {
			MENU.addSeparator();
			MENU.add(createMenuItem(new FileMenuActions.CloseAction()));
			MENU.add(createMenuItem(new FileMenuActions.ExitAction()));
		}
	}

	/**
	 * Returns a new <tt>JMenuItem</tt> instance that is configured from
	 * the action.
	 */
	private JMenuItem createMenuItem(Action action) {
		JMenuItem menuItem = new JMenuItem(action);
		return menuItem;
	}
}
