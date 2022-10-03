package org.openstreetmap.josm.plugins.lshapedterrace;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

import javax.swing.JPanel;

public class LShapedTerracePreviewPanel extends JPanel {
	
	static final int SIZE = 150;
	
	private LShapedTerraceInputDialog dialog;
	
	public LShapedTerracePreviewPanel(LShapedTerraceInputDialog dialog) {
		super();
		setBackground(Color.BLACK);
		this.dialog = dialog;
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(SIZE, SIZE);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		// construct an L-shaped preview shape
		double cx = 0.5 - dialog.getCutoutDepth();
		double cy = 0.5 - dialog.getCutoutWidth();
		Point2D.Double pts[] = {
			// points for house shape preview [0-6]
			new Point2D.Double(cx, cy),
			new Point2D.Double(0.5, cy),
			new Point2D.Double(0.5, -0.5),
			new Point2D.Double(-0.5, -0.5),
			new Point2D.Double(-0.5, 0.5),
			new Point2D.Double(cx, 0.5),
			new Point2D.Double(cx, cy),
			// placeholder points for continuation arrow [7+]
			new Point2D.Double(-0.125, -0.33),
			new Point2D.Double(0, -0.5),
			new Point2D.Double(0, -0.125),
			new Point2D.Double(0, -0.5),
			new Point2D.Double(0.125, -0.33)
		};

		// transform outline points only
		double hx = dialog.getHouseDepth();
		double hy = dialog.getHouseWidth();
		double max = Math.max(hx, hy);
		hx = hx / max;
		hy = hy / max;
		for(int i=0; i<7; i++) {
			// flip according to swapBackFront and swapLeftRight
			double x = pts[i].x;
			double y = pts[i].y;
			if (dialog.getSwapBackFront() ^ dialog.getSwapEnds()) {
				x = -x;
			}
			if (dialog.getSwapLeftRight()) {
				y = -y;
			}
			// scale for houseWidth & houseDepth
			x = x * hx;
			y = y * hy;
			// shrink down to make room for arrow
			x = x * 0.5;
			y = y * 0.5 + 0.25;
			pts[i].setLocation(x,y);
		}

		// transform all points
		double bearing = dialog.getBearing();
		if (dialog.getSwapEnds()) {
			bearing += Math.PI;
		}
		double s = Math.sin(bearing);
		double c = Math.cos(bearing);
		for(int i=0; i<pts.length; i++) {
			// rotate points by bearing (+180° for swapEnds)
			double x = pts[i].x;
			double y = pts[i].y;
			double xr = x*c - y*s;
			double yr = x*s + y*c;
			// scale and translate points to panel size
			x = (xr * 0.9 + 0.5) * SIZE;
			y = (yr * 0.9 + 0.5) * SIZE;
			pts[i].setLocation(x,y);
		}

		// draw outline and arrow
		g.setColor(Color.WHITE);
		for(int i=0; i<pts.length-1; i++) {
			if (i == 6) {
				g.setColor(Color.ORANGE);
			} else {
				g.drawLine((int)pts[i].x, (int)pts[i].y, (int)pts[i+1].x, (int)pts[i+1].y);
			}
		}
	}
	
	public void redraw() {
		this.repaint();
	}

};