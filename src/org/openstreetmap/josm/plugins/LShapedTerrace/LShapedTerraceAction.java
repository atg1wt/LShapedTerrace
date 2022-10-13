package org.openstreetmap.josm.plugins.lshapedterrace;

import java.awt.event.ActionEvent;
import java.awt.Container;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;

public final class LShapedTerraceAction extends JosmAction {
	
	// objects extracted from selection
	private Way outline;
	private Way template;

	// values extracted from selection for use in terracing
	private LatLon outlinePoints[];
	private double outlineWidth;
	private double outlineDepth;
	private double outlineBearing;
	private double cutoutWidth;
	private double cutoutDepth;
	private double houseProportion;

	public LShapedTerraceAction() {
		super(
			"L-shaped terrace",	// menu text
			"l_terrace.png",	// icon filename
			"Converts a rectangular building to a row of L-shaped buildings",	// tooltip
			null,	// shortcut
			false,	// register action for toolbar prefs?
			false	// install layer & selection change adapters?
		);
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		if (decomposeSelection()) {
			getValuesFromWays();
			doTerracing();
		}
	}

	// Get an outline and optionally a template from the user's selection
	// Returns true if successful, false on error
	private boolean decomposeSelection() {
		outline = null;
		template = null;
		Collection<OsmPrimitive> sel = getLayerManager().getEditDataSet().getSelected();
		Iterator<OsmPrimitive> iter = sel.iterator();
		while (iter.hasNext()) {
			OsmPrimitive prim = iter.next();
			if (prim instanceof Way) {
				Way way = (Way)prim;
				if (way.isClosed()) {
					int nodeCount = way.getRealNodesCount();
					if (nodeCount == 4) {
						if (outline == null) {
							outline = way;
						} else {
							return alertUser("Please select only one rectangle");
						}
					}
					if (nodeCount == 6) {
						if (template == null) {
							template = way;
						} else {
							return alertUser("Please select only one template");
						}
					}		
				}
			}
		}
		if (outline == null) {
			return alertUser("Please select a rectangle, and optionally a template");
		}
		return true;
	}

	private boolean alertUser(String message) {
		new ExtendedDialog(MainApplication.getMainFrame(), "L-Terrace: selection not valid", new String[] {"OK"})
			.setIcon(JOptionPane.INFORMATION_MESSAGE)
			.setContent(message)
			.setButtonIcons(new String[] {"ok"})
			.showDialog();
		return false;
	}

	private void getValuesFromWays() {
		// get the four corners of the outline, measure edge lengths
		outlinePoints = new LatLon[4];
		for (int i=0; i<4; i++) {
			outlinePoints[i] = outline.getNode(i).getCoor();
		}
		outlineWidth = outlinePoints[0].greatCircleDistance(outlinePoints[1]);
		outlineDepth = outlinePoints[0].greatCircleDistance(outlinePoints[3]);
		// if necessary, reorder points so long edge is width, short edge is depth
		if (outlineDepth > outlineWidth) {
			reorderOutlinePoints(1, 2, 3, 0);
			double tmp = outlineWidth; outlineWidth = outlineDepth; outlineDepth = tmp;
		}
		// make sure the outline is clockwise, so preview graphics are correctly oriented
		double diff = outlinePoints[0].bearing(outlinePoints[3]) - outlinePoints[0].bearing(outlinePoints[1]);
		if (diff < 0) {
			diff += Math.PI * 2;
		}
		if (diff > Math.PI) {
			// swap front and back edges to reverse winding
			reorderOutlinePoints(3, 2, 1, 0);
		}
		// get a bearing along the long edge of the outline
		outlineBearing = outlinePoints[0].bearing(outlinePoints[1]);

		// set cutout sizes from prefs or default

		// if template Way available, override cutout sizes with measured values
		if (template == null) {
			this.houseProportion = 0.5;
			this.cutoutWidth = Config.getPref().getDouble("plugins.lshapedterrace.cutoutwidth", 0.3);
			this.cutoutDepth = Config.getPref().getDouble("plugins.lshapedterrace.cutoutdepth", 0.5);
		} else {
			// get edge lengths and note the longest
			double edges[] = new double[6];
			int longestEdge = 0;
			double longestDist = 0;
			for (int i=0; i<6; i++) {
				double d = template.getNode(i).getCoor().greatCircleDistance( template.getNode(i+1).getCoor() );
				edges[i] = d;
				if (d > longestDist) {
					longestDist = d;
					longestEdge = i;
				}
			}
			// which way to longest adjacent edge? prepare to step over array in that direction
			int nextEdge = (longestEdge+1) % 6;
			int prevEdge = (longestEdge+5) % 6;
			int step = (edges[nextEdge] > edges[prevEdge]) ? 1 : 5;
			// get overall depth from longest edge
			int currentEdge = longestEdge;
			double totalDepth = longestDist;
			// get overall width from next edge
			currentEdge = (currentEdge+step) % 6;
			double totalWidth = edges[currentEdge];
			// get cutout width, two edges later
			currentEdge = (currentEdge+step+step) % 6;
			this.cutoutWidth = edges[currentEdge] / totalWidth;
			// next = cutout depth from next edge
			currentEdge = (currentEdge+step) % 6;
			this.cutoutDepth = edges[currentEdge] / totalDepth;
			this.houseProportion = totalWidth / totalDepth;
		}
		
	}

	private void reorderOutlinePoints(int i0, int i1, int i2, int i3) {
		LatLon newPoints[] = new LatLon[4];
		newPoints[0] = outlinePoints[i0];
		newPoints[1] = outlinePoints[i1];
		newPoints[2] = outlinePoints[i2];
		newPoints[3] = outlinePoints[i3];
		outlinePoints = newPoints;
	}

	private void doTerracing() {
		
		DataSet data;
		Node node[] = new Node[6];
		Collection<Command> commands = new LinkedList<>();
		
		// open dialog to get user input
		LShapedTerraceInputDialog dialog = new LShapedTerraceInputDialog(outlineWidth, outlineDepth, outlineBearing, cutoutWidth, cutoutDepth, houseProportion);
		dialog.showDialog();

		// early return on cancel/close
		if (dialog.getValue() != 1) {
			return;
		}

		// perform front/back swap
		if (dialog.getSwapBackFront()) {
			reorderOutlinePoints(3, 2, 1, 0);
		}
		
		// perform start/end swap
		if (dialog.getSwapEnds()) {
			reorderOutlinePoints(1, 0, 3, 2);
		}
		
		// get some values ready
		int houseCount = dialog.getHouseCount();
		double houseFrac = 1.0 / (double)houseCount;
		double cutWidth = dialog.getCutoutWidth();
		double cutDepth = dialog.getCutoutDepth();
		int houseNumber = dialog.getFirstNumber();
		int addNumber = dialog.getInterpolation();

		// store preferences that may have been altered in dialog
		Config.getPref().putInt("plugins.lshapedterrace.interpolation", addNumber);
		Config.getPref().putDouble("plugins.lshapedterrace.cutoutwidth", cutWidth);
		Config.getPref().putDouble("plugins.lshapedterrace.cutoutdepth", cutDepth);

		// give useful names to outline points
		LatLon frontStart = outlinePoints[0];
		LatLon frontEnd = outlinePoints[1];
		LatLon backEnd = outlinePoints[2];
		LatLon backStart = outlinePoints[3];
		
		// calculate midline
		LatLon midStart = backStart.interpolate(frontStart, cutDepth);
		LatLon midEnd = backEnd.interpolate(frontEnd, cutDepth);
		
		boolean backFirst = dialog.getSwapLeftRight();
		int nodeCount = (cutDepth == 0) ? 4 : 6;
		data = getLayerManager().getEditDataSet();
		TagMap tags = outline.getKeys();
		
		// first two points
		node[0] = new Node( backFirst ? backStart : midStart );
		node[1] = new Node( frontStart );
		commands.add( new AddCommand(data, node[0]) );
		commands.add( new AddCommand(data, node[1]) );

		for (int i=0; i<houseCount; i++) {
			double startFrac = houseFrac * i;
			double midFrac = startFrac + houseFrac * (backFirst ? 1.0 - cutWidth : cutWidth);
			double endFrac = startFrac + houseFrac;
			node[2] = new Node( frontStart.interpolate(frontEnd, endFrac) );
			if (backFirst) {
				node[3] = new Node( midStart.interpolate(midEnd, endFrac) );
				node[4] = new Node( midStart.interpolate(midEnd, midFrac) );
				node[5] = new Node( backStart.interpolate(backEnd, midFrac) );
			} else {
				node[3] = new Node( backStart.interpolate(backEnd, endFrac) );
				node[4] = new Node( backStart.interpolate(backEnd, midFrac) );
				node[5] = new Node( midStart.interpolate(midEnd, midFrac) );
			}
			// build a way from those nodes
			Way way = new Way();
			for (int j=0; j<nodeCount; j++) {
				way.addNode(node[j]);
			}
			way.addNode(node[0]);
			
			// do house numbering
			if (houseNumber > 0) {
				tags.put("addr:housenumber", Integer.toString(houseNumber));
				houseNumber += addNumber;
			}
			
			// apply tags to new way
			way.setKeys(tags);

			// add nodes and way to map
			commands.add( new AddCommand(data, node[2]) );
			commands.add( new AddCommand(data, node[3]) );
			if (cutDepth > 0) {
				commands.add( new AddCommand(data, node[4]) );
				commands.add( new AddCommand(data, node[5]) );
			}
			commands.add( new AddCommand(data, way) );
			
			// reuse these two nodes for next house
			node[0] = node[3];
			node[1] = node[2];

			// toggle the back/mid boolean
			backFirst = !backFirst;
		}

		// remove the outline, and nodes not used by anything else
		commands.add( new DeleteCommand(data, outline) );
		for (int i=0; i<4; i++) {
			Node delNode = outline.getNode(i);
			if (!delNode.isReferredByWays(2)) {
				commands.add( new DeleteCommand(data, delNode) );
			}
		}
		
		// finally, execute the commands!
		SequenceCommand seq = new SequenceCommand("L-Terrace", commands);
		UndoRedoHandler.getInstance().add(seq);
		
	}

}

