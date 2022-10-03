package org.openstreetmap.josm.plugins.lshapedterrace;

import java.awt.GridBagLayout;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;

public class LShapedTerraceInputDialog extends ExtendedDialog {

	static final private int SLIDERMAX = 100;

	private double outlineWidth;
	private double outlineDepth;
	private double cutoutWidth;
	private double cutoutDepth;
	private double terraceBearing;
	private boolean swapBackFront;
	private boolean swapLeftRight;
	private boolean swapEnds;
	private int firstNumber;
	private int houseCount;
	private int interpolation;
	
	private LShapedTerracePreviewPanel previewPanel;

	public LShapedTerraceInputDialog(double initialOutlineWidth, double initialOutlineDepth, double outlineBearing, double initialCutoutWidth, double initialCutoutDepth, double houseProportion) {
		super(
			MainApplication.getMainFrame(),	// parent
			"L-Shaped Terrace Options",	// title
			new String[] {"OK", "Cancel"},	// buttons[]
			true	// modal
		);

		// copy parameters to object properties so they can be getted later
		outlineWidth = initialOutlineWidth;
		outlineDepth = initialOutlineDepth;
		cutoutWidth = initialCutoutWidth;
		cutoutDepth = initialCutoutDepth;
		terraceBearing = outlineBearing;

		swapBackFront = false;
		swapLeftRight = false;
		swapEnds = false;
		firstNumber = 0;
		houseCount = (int)Math.round(outlineWidth / outlineDepth / houseProportion);
		interpolation = Config.getPref().getInt("plugins.lshapedterrace.interpolation", 1);
		
		setButtonIcons( new String[] {"ok", "cancel"} );

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new GridBagLayout());

		JLabel widthLabel = new JLabel( sliderCaption("Cutout width", cutoutWidth) );
		JSlider widthSlider = new JSlider(0, SLIDERMAX, (int)(cutoutWidth * SLIDERMAX));
		widthSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent ev) {
				cutoutWidth = (double)widthSlider.getValue() / SLIDERMAX;
				widthLabel.setText( sliderCaption("Cutout width", cutoutWidth) );
				previewPanel.redraw();
			}
		});

		JLabel depthLabel = new JLabel( sliderCaption("Cutout depth", cutoutDepth) );
		JSlider depthSlider = new JSlider(0, SLIDERMAX, (int)(cutoutDepth * SLIDERMAX));
		depthSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent ev) {
				cutoutDepth = (double)depthSlider.getValue() / SLIDERMAX;
				depthLabel.setText( sliderCaption("Cutout depth", cutoutDepth) );
				previewPanel.redraw();
			}
		});
		
		JCheckBox swapBFCheckbox = new JCheckBox("Swap front/back");
		swapBFCheckbox.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent ev) {
				swapBackFront = swapBFCheckbox.isSelected();
				previewPanel.redraw();
			}
		});
		
		JCheckBox swapLRCheckbox = new JCheckBox("Swap left/right");
		swapLRCheckbox.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent ev) {
				swapLeftRight = swapLRCheckbox.isSelected();
				previewPanel.redraw();
			}
		});
				
		JCheckBox swapEndsCheckbox = new JCheckBox("Swap start/end");
		swapEndsCheckbox.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent ev) {
				swapEnds = swapEndsCheckbox.isSelected();
				previewPanel.redraw();
			}
		});
		
		JTextField numHousesTextField = new JTextField( Integer.toString(houseCount) );
		numHousesTextField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void changedUpdate(DocumentEvent ev) {
				update();
			}
			@Override
			public void removeUpdate(DocumentEvent ev) {
				update();
			}
			@Override
			public void insertUpdate(DocumentEvent ev) {
				update();
			}
			private void update() {
				try {
					int n = Integer.parseInt( numHousesTextField.getText() );
					if (n>0) {
						houseCount = n;
					}
				} catch(NumberFormatException ex) {
					// TODO: prevent submitting form with invalid houseCount
				}
				previewPanel.redraw();
			}
		});

		JTextField firstNumberTextField = new JTextField();
		firstNumberTextField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void changedUpdate(DocumentEvent ev) {
				update();
			}
			@Override
			public void removeUpdate(DocumentEvent ev) {
				update();
			}
			@Override
			public void insertUpdate(DocumentEvent ev) {
				update();
			}
			private void update() {
				try {
					int n = Integer.parseInt( firstNumberTextField.getText() );
					if (n>0) {
						firstNumber = n;
					} else {
						firstNumber = 0;
					}
				} catch(NumberFormatException ex) {
					firstNumber = 0;
				}
				previewPanel.redraw();
			}
		});
		
		String[] interpolationTypes = { "All", "Odd/Even" };
		JComboBox<String> cboInterpolate = new JComboBox<>(interpolationTypes);
		cboInterpolate.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent ev) {
				interpolation = 1 + cboInterpolate.getSelectedIndex();
			}
		});
		cboInterpolate.setSelectedIndex(interpolation - 1);

		previewPanel = new LShapedTerracePreviewPanel(this);

		mainPanel.add(widthLabel, GBC.std() );
		mainPanel.add(widthSlider, GBC.eol().fill(GBC.HORIZONTAL) );
		mainPanel.add(depthLabel, GBC.std() );
		mainPanel.add(depthSlider, GBC.eol().fill(GBC.HORIZONTAL) );

		mainPanel.add(swapBFCheckbox, GBC.std() );
		mainPanel.add(swapLRCheckbox, GBC.std() );
		mainPanel.add(swapEndsCheckbox, GBC.eol() );

		mainPanel.add(previewPanel, GBC.eol().anchor(GBC.CENTER) );

		mainPanel.add( new JLabel("Number of houses:"), GBC.std() );
		mainPanel.add(numHousesTextField,GBC.eol().fill(GBC.HORIZONTAL) );
		mainPanel.add( new JLabel("First house number:"), GBC.std() );
		mainPanel.add(firstNumberTextField, GBC.eol().fill(GBC.HORIZONTAL) );
		mainPanel.add( new JLabel("Interpolation:"), GBC.std() );
		mainPanel.add(cboInterpolate, GBC.eol().fill(GBC.HORIZONTAL) );

		setContent(mainPanel);
		setDefaultButton(1);
		
	}
	
	private String sliderCaption(String dimension, double fraction) {
		return String.format("%s: %.1f%%", dimension, fraction*100);
	}
	
	public double getHouseWidth() {
		return outlineWidth / (double)houseCount;
	}

	public double getHouseDepth() {
		return outlineDepth;
	}

	public double getBearing() {
		return terraceBearing;
	}

	public int getHouseCount() {
		return houseCount;
	}

	public double getCutoutWidth() {
		if (cutoutDepth == 0 || cutoutWidth == 0) {
			return 0;
		} else {
			return cutoutWidth;
		}
	}

	public double getCutoutDepth() {
		if (cutoutDepth == 0 || cutoutWidth == 0) {
			return 0;
		} else {
			return cutoutDepth;
		}
	}

	public boolean getSwapBackFront() {
		return swapBackFront;
	}

	public boolean getSwapLeftRight() {
		return swapLeftRight;
	}

	public boolean getSwapEnds() {
		return swapEnds;
	}

	public int getFirstNumber() {
		return firstNumber;
	}

	public int getInterpolation() {
		return interpolation;
	}

}

