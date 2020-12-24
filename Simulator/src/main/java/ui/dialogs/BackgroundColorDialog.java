/**
 * Copyright 2020 Alexander Herzog
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.Serializable;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import language.Language;
import mathtools.NumberTools;
import systemtools.BaseDialog;
import systemtools.MsgBox;
import systemtools.SmallColorChooser;
import ui.help.Help;
import ui.images.Images;
import ui.modeleditor.ModelElementBaseDialog;
import ui.modeleditor.ModelSurface;
import ui.tools.ImageChooser;

/**
 * Dialog, der das Einstellen von Hintergrund- un Rasterfarbe erm�glicht.
 * @author Alexander Herzog
 */
public class BackgroundColorDialog extends BaseDialog {
	/**
	 * Serialisierungs-ID der Klasse
	 * @see Serializable
	 */
	private static final long serialVersionUID = 7680431280280416543L;

	/** Nutzerdefinierte Hintergrundfarbe aktiv? */
	private final JCheckBox backgroundCheck;
	/** Nutzerdefinierte Hintergrundfarbe ausw�hlen */
	private final SmallColorChooser backgroundColor;
	/** Nutzerdefinierte Rasterfarbe aktiv? */
	private final JCheckBox rasterCheck;
	/** Nutzerdefinierte Rasterfarbe ausw�hlen */
	private final SmallColorChooser rasterColor;
	/** Nutzerdefinierte Gradientenfarbe aktiv? */
	private final JCheckBox gradientCheck;
	/** Nutzerdefinierte Gradientenfarbe ausw�hlen */
	private final SmallColorChooser gradientColor;

	/** Hintergrundbild */
	private final ImageChooser backgroundImage;
	/** Skalierung f�r Hintergrundbild */
	private final JTextField backgroundImageScale;

	/**
	 * Konstruktor der Klasse
	 * @param owner	�bergeordnetes Element
	 * @param colors	Bisherige Farben (2-elementiges Array aus Hintergrund- und Rasterfarbe)
	 * @param image	Hintergrundbild (kann <code>null</code> sein)
	 * @param scale	Skalierung f�r das Hintergrundbild (muss gr��er als 0 sein)
	 * @param readOnly	Gibt an, ob die Einstellungen ver�ndert werden d�rfen
	 */
	public BackgroundColorDialog(final Component owner, final Color[] colors, final BufferedImage image, final double scale, final boolean readOnly) {
		super(owner,Language.tr("Window.BackgroundColor.Title"),readOnly);

		final JPanel content=createGUI(()->Help.topicModal(BackgroundColorDialog.this,"EditorColorDialog"));
		content.setLayout(new BorderLayout());
		final JTabbedPane tabs=new JTabbedPane();
		content.add(tabs,BorderLayout.CENTER);

		JPanel tabOuter;
		JPanel tab;
		JPanel line, cell;

		/* Tab "Farben" */
		tabs.addTab(Language.tr("Window.BackgroundColor.Tab.Color"),tabOuter=new JPanel(new BorderLayout()));
		tabOuter.add(tab=new JPanel(),BorderLayout.NORTH);
		tab.setLayout(new BoxLayout(tab,BoxLayout.PAGE_AXIS));

		final Color c1=(colors!=null && colors.length>=2 && colors[0]!=null)?colors[0]:ModelSurface.DEFAULT_BACKGROUND_COLOR;
		final Color c2=(colors!=null && colors.length>=2 && colors[1]!=null)?colors[1]:ModelSurface.DEFAULT_RASTER_COLOR;
		final Color c3=(colors!=null && colors.length>=3 && colors[2]!=null)?colors[2]:null;

		tab.add(line=new JPanel(new FlowLayout(FlowLayout.LEFT)),BorderLayout.CENTER);

		/* Hintergrundfarbe */
		line.add(cell=new JPanel(new BorderLayout()));
		cell.add(backgroundCheck=new JCheckBox(Language.tr("Window.BackgroundColor.UserBackground"),!c1.equals(ModelSurface.DEFAULT_BACKGROUND_COLOR)),BorderLayout.NORTH);
		backgroundCheck.setEnabled(!readOnly);
		cell.add(backgroundColor=new SmallColorChooser(c1),BorderLayout.CENTER);
		backgroundColor.setEnabled(!readOnly);
		backgroundColor.addClickListener(e->backgroundCheck.setSelected(true));

		/* Rasterfarbe */
		line.add(cell=new JPanel(new BorderLayout()));
		cell.add(rasterCheck=new JCheckBox(Language.tr("Window.BackgroundColor.UserRaster"),!c2.equals(ModelSurface.DEFAULT_RASTER_COLOR)),BorderLayout.NORTH);
		rasterCheck.setEnabled(!readOnly);
		cell.add(rasterColor=new SmallColorChooser(c2),BorderLayout.CENTER);
		rasterColor.setEnabled(!readOnly);
		rasterColor.addClickListener(e->rasterCheck.setSelected(true));

		/* Farbverlauf */
		line.add(cell=new JPanel(new BorderLayout()));
		cell.add(gradientCheck=new JCheckBox(Language.tr("Window.BackgroundColor.UseGradient"),c3!=null),BorderLayout.NORTH);
		gradientCheck.setEnabled(!readOnly);
		cell.add(gradientColor=new SmallColorChooser(c3==null?Color.WHITE:c3),BorderLayout.CENTER);
		gradientColor.setEnabled(!readOnly);
		gradientColor.addClickListener(e->gradientCheck.setSelected(true));

		/* Tab "Hintergrundbild" */
		tabs.addTab(Language.tr("Window.BackgroundColor.Tab.Image"),tabOuter=new JPanel(new BorderLayout()));
		tabOuter.add(tab=new JPanel(),BorderLayout.NORTH);
		tab.setLayout(new BoxLayout(tab,BoxLayout.PAGE_AXIS));
		tab.add(backgroundImage=new ImageChooser(image,null));
		backgroundImage.setPreferredSize(new Dimension(0,500));

		final Object[] data=ModelElementBaseDialog.getInputPanel(Language.tr("Window.BackgroundColor.ImageScale")+":",NumberTools.formatNumberMax(scale),7);
		tab.add((JPanel)data[0]);
		backgroundImageScale=(JTextField)data[1];
		backgroundImageScale.addKeyListener(new KeyListener() {
			@Override public void keyTyped(KeyEvent e) {checkData(false);}
			@Override public void keyReleased(KeyEvent e) {checkData(false);}
			@Override public void keyPressed(KeyEvent e) {checkData(false);}
		});

		/* Icons auf Tabs */
		tabs.setIconAt(0,Images.EDIT_BACKGROUND_COLOR.getIcon());
		tabs.setIconAt(1,Images.EDIT_BACKGROUND_IMAGE.getIcon());

		/* Dialog starten */
		pack();
		setLocationRelativeTo(getOwner());
	}

	/**
	 * Pr�ft, ob die eingegebenen Daten in Ordnung sind.
	 * @param showErrorMessages	Wird hier <code>true</code> �bergeben, so wird eine Fehlermeldung ausgegeben, wenn die Daten nicht in Ordnung sind.
	 * @return	Gibt <code>true</code> zur�ck, wenn die Daten in Ordnung sind.
	 */
	private boolean checkData(final boolean showErrorMessages) {
		boolean ok=true;

		final Double D=NumberTools.getPositiveDouble(backgroundImageScale,true);
		if (D==null) {
			if (showErrorMessages) {
				MsgBox.error(this,Language.tr("Window.BackgroundColor.ImageScale.ErrorTitle"),String.format(Language.tr("Window.BackgroundColor.ImageScale.ErrorInfo"),backgroundImageScale.getText()));
				return false;
			}
			ok=false;
		}

		return ok;
	}

	@Override
	protected boolean checkData() {
		return checkData(true);
	}

	/**
	 * Liefert im Falle, dass der Dialog per "Ok" geschlossen wurde die neueingestellten Farben
	 * @return	3-elementiges Array aus Hintergrund-, Raster und gradienten Hintergrundfarbe (welche <code>null</code> sein kann) oder <code>null</code>, wenn der Dialog abgebrochen wurde.
	 */
	public Color[] getColors() {
		if (getClosedBy()!=CLOSED_BY_OK) return null;
		Color c1=ModelSurface.DEFAULT_BACKGROUND_COLOR;
		if (backgroundCheck.isSelected()) c1=backgroundColor.getColor();
		Color c2=ModelSurface.DEFAULT_RASTER_COLOR;
		if (rasterCheck.isSelected()) c2=rasterColor.getColor();
		Color c3=(gradientCheck.isSelected())?gradientColor.getColor():null;
		return new Color[] {c1,c2,c3};
	}

	/**
	 * Liefert das eingestellte Hintergrundbild.
	 * @return	Hintergrundbild (kann <code>null</code> sein)
	 */
	public BufferedImage getImage() {
		if (getClosedBy()!=CLOSED_BY_OK) return null;
		return backgroundImage.getImage();
	}

	/**
	 * Liefert den Skalierungsfaktor f�r das Hintergrundbild.
	 * @return	Skalierungsfaktor f�r das Hintergrundbild
	 */
	public double getScale() {
		return NumberTools.getPositiveDouble(backgroundImageScale,true);
	}
}
