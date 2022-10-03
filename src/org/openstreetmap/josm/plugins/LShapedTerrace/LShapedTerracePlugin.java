package org.openstreetmap.josm.plugins.lshapedterrace;

import javax.swing.JMenuItem;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.Destroyable;

public class LShapedTerracePlugin extends Plugin implements Destroyable {

	private JosmAction action = new LShapedTerraceAction();
	private JMenuItem menuItem;

	// initialise plugin
	public LShapedTerracePlugin(PluginInformation info) {
		super(info);
		menuItem = MainMenu.add(MainApplication.getMenu().moreToolsMenu, action);
	}

	@Override
	public void destroy() {
		MainApplication.getMenu().moreToolsMenu.remove(menuItem);
	}

}
