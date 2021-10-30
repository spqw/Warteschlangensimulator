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
package ui.modeleditor.elements;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.concurrent.Semaphore;

import javax.swing.Icon;

import org.apache.commons.math3.util.FastMath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import language.Language;
import mathtools.NumberTools;
import mathtools.TimeTools;
import simulator.editmodel.EditModel;
import simulator.editmodel.FullTextSearch;
import simulator.runmodel.SimulationData;
import simulator.simparser.ExpressionCalc;
import tools.DateTools;
import ui.images.Images;
import ui.modeleditor.ModelClientData;
import ui.modeleditor.ModelSequences;
import ui.modeleditor.ModelSurface;
import ui.modeleditor.coreelements.ModelElement;
import ui.modeleditor.coreelements.ModelElementPosition;
import ui.modeleditor.fastpaint.Shapes;
import ui.modeleditor.outputbuilder.HTMLOutputBuilder;
import ui.modeleditor.outputbuilder.SpecialOutputBuilder;
import ui.tools.FlatLaFHelper;

/**
 * Zeigt w�hrend der Animation das Ergebnis eines Rechnenausdrucks als Text an.
 * @author Alexander Herzog
 */
public class ModelElementAnimationTextValue extends ModelElementPosition implements ElementWithAnimationDisplay {
	/**
	 * Standard-Textfarbe
	 * @see #color
	 */
	private static final Color DEFAULT_COLOR=Color.BLACK;

	/**
	 * Wie soll der Ausgabetext formatiert werden?
	 * @author Alexander Herzog
	 * @see ModelElementAnimationTextValue#getMode()
	 * @see ModelElementAnimationTextValue#setMode(ModeExpression)
	 */
	public enum ModeExpression {
		/**
		 * Berechnet einen Ausdruck und zeigt diesen als Zahlenwert an.
		 */
		MODE_EXPRESSION_NUMBER,

		/**
		 * Berechnet einen Ausdruck und zeigt diesen als Prozentwert an.
		 */
		MODE_EXPRESSION_PERCENT,

		/**
		 * Berechnet einen Ausdruck und zeigt diesen als Zeitangabe an.
		 */
		MODE_EXPRESSION_TIME,

		/**
		 * Zeigt die Tage, Stunden, Minuten und Sekunden seit Simulationsstart an.
		 */
		MODE_TIME,

		/**
		 * Zeigt einen Datumswert an, der sich relativ zum Simulationsstart ergibt.
		 */
		MODE_DATE
	}

	/**
	 *  Wie soll der Ausgabetext formatiert werden?
	 *  @see #getMode()
	 *  @see #setMode(ModeExpression)
	 *  @see ModeExpression
	 */
	private ModeExpression mode=ModeExpression.MODE_EXPRESSION_NUMBER;

	/**
	 * Rechenausdruck
	 * @see #getExpression()
	 * @see #setExpression(String)
	 */
	private String expression="123";

	/**
	 * Anzahl an anzuzeigenden Nachkommastellen
	 * @see #getDigits()
	 * @see #setDigits(int)
	 */
	private int digits=1;

	/**
	 * Unix-Zeitstempel (in Sekunden) der dem Start der Simulation entspricht
	 * @see #getDateZero()
	 * @see #setDateZero(long)
	 */
	private long dateZero=0;

	/**
	 * Sichert ab, dass Simulations- und Zeichenthread
	 * nicht gleichzeitig auf {@link #simTextValue},
	 * {@link #simTextValueLong} und {@link #simTextValueDouble} zugreifen.
	 */
	private Semaphore drawLock=new Semaphore(1);

	/**
	 * W�hrend der Animation anzuzeigender Text
	 */
	private String simTextValue=null;

	/**
	 * W�hrend der Animation anzuzeigende Uhrzeit
	 */
	private long simTextValueLong;

	/**
	 * W�hrend der Animation anzuzeigendes Datum
	 */
	private double simTextValueDouble;

	/**
	 * Zu verwendende Schriftart
	 * @see #getFontFamily()
	 * @see #setFontFamily(ui.modeleditor.elements.FontCache.FontFamily)
	 */
	private FontCache.FontFamily fontFamily=FontCache.defaultFamily;

	/**
	 * Schriftgr��e
	 * @see #getTextSize()
	 * @see #setTextSize(int)
	 */
	private int textSize=14;

	/**
	 * Ausgabe des Textes im Fettdruck
	 * @see #getTextBold()
	 * @see #setTextBold(boolean)
	 */
	private boolean bold;

	/**
	 * Ausgabe des Textes im Kursivdruck
	 * @see #getTextItalic()
	 * @see #setTextItalic(boolean)
	 */
	private boolean italic;

	/**
	 * Textfarbe
	 * @see #getColor()
	 * @see #setColor(Color)
	 */
	private Color color=DEFAULT_COLOR;

	/**
	 * F�llfarbe des Kastens (kann <code>null</code> sein f�r transparent)
	 * @see #getFillColor()
	 * @see #setFillColor(Color)
	 */
	private Color fillColor=null;

	/**
	 * Deckkraft der Hintergrundfarbe
	 * @see #getFillAlpha()
	 * @see #setFillAlpha(double)
	 */
	private double fillAlpha=1.0;

	/**
	 * Konstruktor der Klasse <code>ModelElementTextValue</code>
	 * @param model	Modell zu dem dieses Element geh�ren soll (kann sp�ter nicht mehr ge�ndert werden)
	 * @param surface	Zeichenfl�che zu dem dieses Element geh�ren soll (kann sp�ter nicht mehr ge�ndert werden)
	 */
	public ModelElementAnimationTextValue(final EditModel model, final ModelSurface surface) {
		super(model,surface,new Dimension(0,0),Shapes.ShapeType.SHAPE_RECTANGLE);
		useSizeOnCompare=false;
	}

	/**
	 * Icon, welches im "Element hinzuf�gen"-Dropdown-Men� angezeigt werden soll.
	 * @return	Icon f�r das Dropdown-Men�
	 */
	@Override
	public Icon getAddElementIcon() {
		return Images.MODELEDITOR_ELEMENT_ANIMATION_TEXT_VALUE.getIcon();
	}

	@Override
	public boolean isVisualOnly() {
		return true;
	}

	/**
	 * Tooltip f�r den "Element hinzuf�gen"-Dropdown-Men�-Eintrag.
	 * @return Tooltip f�r den "Element hinzuf�gen"-Dropdown-Men�eintrag
	 */
	@Override
	public String getToolTip() {
		return Language.tr("Surface.AnimationText.Tooltip");
	}

	/**
	 * Liefert den aktuellen Anzeigemodus
	 * @return	Aktueller Anzeigemodus
	 * @see ModeExpression
	 */
	public ModeExpression getMode() {
		return mode;
	}

	/**
	 * Stellt den aktuellen Anzeigemodus ein
	 * @param mode	Neuer Anzeigemodus
	 * @see ModeExpression
	 */
	public void setMode(final ModeExpression mode) {
		this.mode=mode;
		fireChanged();
	}

	/**
	 * Liefert im Falle, dass Modus <code>MODE_EXPRESSION_NUMBER</code> oder
	 * <code>MODE_EXPRESSION_PERCENT</code> gew�hlt sind, den aktuellen Ausdruck.
	 * @return	Aktueller Ausdruck
	 */
	public String getExpression() {
		return expression;
	}

	/**
	 * Stellt den Ausdruck ein, der im Falle von <code>MODE_EXPRESSION_NUMBER</code> oder
	 * <code>MODE_EXPRESSION_PERCENT</code> ausgerechnet werden soll.
	 * @param expression	Neuer Ausdruck
	 */
	public void setExpression(final String expression) {
		if (expression!=null) {
			this.expression=expression;
			fireChanged();
		}
	}

	/**
	 * Gibt an, wie viele Nachkommastellen angezeigt werden sollen.
	 * @return	Anzahl an anzuzeigenden Nachkommastellen
	 */
	public int getDigits() {
		return digits;
	}

	/**
	 * Stellt in, wie viele Nachkommastellen angezeigt werden sollen.
	 * @param digits	Anzahl an anzuzeigenden Nachkommastellen
	 */
	public void setDigits(final int digits) {
		if (digits>=0 && digits<=15) this.digits=digits;
	}

	/**
	 * Liefert den Unix-Zeitstempel (in Sekunden) der dem Start der Simulation entsprechen soll.
	 * @return	Unix-Zeitstempel (in Sekunden) der dem Start der Simulation entspricht
	 */
	public long getDateZero() {
		return Math.max(0,dateZero);
	}

	/**
	 * Stellt den Unix-Zeitstempel (in Sekunden) der dem Start der Simulation entsprechen soll ein.
	 * @param dateZero	Unix-Zeitstempel (in Sekunden) der dem Start der Simulation entspricht
	 */
	public void setDateZero(long dateZero) {
		this.dateZero=Math.max(0,dateZero);
	}

	/**
	 * Liefert die momentan eingestellte Schriftart
	 * @return	Aktuelle Schriftart
	 */
	public FontCache.FontFamily getFontFamily() {
		return fontFamily;
	}

	/**
	 * Stellt die zu verwendende Schriftart ein
	 * @param fontFamily	Neue Schriftart
	 */
	public void setFontFamily(FontCache.FontFamily fontFamily) {
		if (fontFamily!=null) this.fontFamily=fontFamily;
		fireChanged();
	}

	/**
	 * Liefert die aktuelle Gr��e der Schrift
	 * @return	Aktuelle Schriftgr��e
	 */
	public int getTextSize() {
		return textSize;
	}

	/**
	 * Stellt die Schriftgr��e ein
	 * @param textSize	Neue Schriftgr��e
	 */
	public void setTextSize(final int textSize) {
		this.textSize=FastMath.max(6,FastMath.min(128,textSize));
		fireChanged();
	}

	/**
	 * Liefert die Angabe, ob der Text fett gedruckt werden soll.
	 * @return	Ausgabe des Textes im Fettdruck
	 */
	public boolean getTextBold() {
		return bold;
	}

	/**
	 * Stellt ein, ob der Text fett gedruckt werden soll.
	 * @param bold	Angabe, ob der Text fett gedruckt werden soll
	 */
	public void setTextBold(final boolean bold) {
		if (this.bold==bold) return;
		this.bold=bold;
		fireChanged();
	}

	/**
	 * Liefert die Angabe, ob der Text kursiv gedruckt werden soll.
	 * @return	Ausgabe des Textes im Kursivdruck
	 */
	public boolean getTextItalic() {
		return italic;
	}

	/**
	 * Stellt ein, ob der Text kursiv gedruckt werden soll.
	 * @param italic	Angabe, ob der Text kursiv gedruckt werden soll
	 */
	public void setTextItalic(final boolean italic) {
		if (this.italic==italic) return;
		this.italic=italic;
		fireChanged();
	}

	/**
	 * Liefert die aktuelle Textfarbe
	 * @return	Aktuelle Textfarbe
	 */
	public Color getColor() {
		return color;
	}

	/**
	 * Stellt die Textfarbe ein
	 * @param color	Textfarbe
	 */
	public void setColor(final Color color) {
		if (color!=null) this.color=color;
	}

	/**
	 * Liefert die aktuelle F�llfarbe des Kastens
	 * @return	Aktuelle F�llfarbe des Kastens (kann <code>null</code> sein f�r transparent)
	 */
	public Color getFillColor() {
		return fillColor;
	}

	/**
	 * Stellt die F�llfarbe des Kastens ein
	 * @param color	F�llfarbe des Kastens (oder <code>null</code> f�r transparent)
	 */
	public void setFillColor(final Color color) {
		fillColor=color;
		fireChanged();
	}

	/**
	 * Liefert die Deckkraft der Hintergrundfarbe.
	 * @return	Deckkraft der Hintergrundfarbe (Wert zwischen 0 und 1 jeweils einschlie�lich)
	 */
	public double getFillAlpha() {
		return fillAlpha;
	}

	/**
	 * Stellt die Deckkraft der Hintergrundfarbe ein.
	 * @param fillAlpha	Deckkraft der Hintergrundfarbe (Wert zwischen 0 und 1 jeweils einschlie�lich)
	 */
	public void setFillAlpha(double fillAlpha) {
		this.fillAlpha=Math.max(0,Math.min(1,fillAlpha));
		fireChanged();
	}

	/**
	 * �berpr�ft, ob das Element mit dem angegebenen Element inhaltlich identisch ist.
	 * @param element	Element mit dem dieses Element verglichen werden soll.
	 * @return	Gibt <code>true</code> zur�ck, wenn die beiden Elemente identisch sind.
	 */
	@Override
	public boolean equalsModelElement(ModelElement element) {
		if (!super.equalsModelElement(element)) return false;
		if (!(element instanceof ModelElementAnimationTextValue)) return false;
		final ModelElementAnimationTextValue otherText=(ModelElementAnimationTextValue)element;

		if (mode!=otherText.mode) return false;

		if (mode==ModeExpression.MODE_EXPRESSION_NUMBER || mode==ModeExpression.MODE_EXPRESSION_PERCENT || mode==ModeExpression.MODE_EXPRESSION_TIME) {
			if (!expression.equals(otherText.expression)) return false;
			if (digits!=otherText.digits) return false;
		}

		if (mode==ModeExpression.MODE_DATE) {
			if (dateZero!=otherText.dateZero) return false;
		}

		if (!otherText.color.equals(color)) return false;
		if (fontFamily!=otherText.fontFamily) return false;
		if (textSize!=otherText.textSize) return false;
		if (bold!=otherText.bold) return false;
		if (italic!=otherText.italic) return false;
		if (fillColor!=otherText.fillColor) return false;
		if (fillAlpha!=otherText.fillAlpha) return false;

		return true;
	}

	/**
	 * �bertr�gt die Einstellungen von dem angegebenen Element auf dieses.
	 * @param element	Element, von dem alle Einstellungen �bernommen werden sollen
	 */
	@Override
	public void copyDataFrom(ModelElement element) {
		super.copyDataFrom(element);
		if (element instanceof ModelElementAnimationTextValue) {
			final ModelElementAnimationTextValue copySource=(ModelElementAnimationTextValue)element;
			mode=copySource.mode;
			expression=copySource.expression;
			digits=copySource.digits;
			dateZero=copySource.dateZero;
			fontFamily=copySource.fontFamily;
			textSize=copySource.textSize;
			bold=copySource.bold;
			italic=copySource.italic;
			color=copySource.color;
			fillColor=copySource.fillColor;
			fillAlpha=copySource.fillAlpha;
		}
	}

	/**
	 * Erstellt eine Kopie des Elements
	 * @param model	Modell zu dem das kopierte Element geh�ren soll.
	 * @param surface	Zeichenfl�che zu der das kopierte Element geh�ren soll.
	 * @return	Kopiertes Element
	 */
	@Override
	public ModelElementAnimationTextValue clone(final EditModel model, final ModelSurface surface) {
		final ModelElementAnimationTextValue element=new ModelElementAnimationTextValue(model,surface);
		element.copyDataFrom(this);
		return element;
	}

	/**
	 * Bestimmt den anzuzeigenden Text oder die �berschrift.
	 * @param getTitle	Text (<code>false</code>) oder �berschrift (<code>true</code>) liefern
	 * @return	Anzuzeigender Text oder �berschrift
	 */
	private String getDisplayText(final boolean getTitle) {
		if (getTitle) {
			if (surface==null) return Language.tr("Surface.AnimationText.Type.Property");
			else return getName();
		}

		if (surface==null) return Language.tr("Surface.AnimationText.Type.Value");
		drawLock.acquireUninterruptibly();
		try {
			if (simTextValue!=null) return simTextValue;
		} finally {
			drawLock.release();
		}
		switch (mode) {
		case MODE_EXPRESSION_NUMBER: return Language.tr("Surface.AnimationText.Type.Number");
		case MODE_EXPRESSION_PERCENT: return Language.tr("Surface.AnimationText.Type.PercentValue");
		case MODE_EXPRESSION_TIME: return Language.tr("Surface.AnimationText.Type.TimeValue");
		case MODE_TIME: return Language.tr("Surface.AnimationText.Type.SimulationTime");
		case MODE_DATE: return Language.tr("Surface.AnimationText.Type.Date");
		default: return Language.tr("Surface.AnimationText.Type.Error");
		}
	}

	/**
	 * Schriftgr��e beim letzten Aufruf von {@link #drawToGraphics(Graphics, Rectangle, double, boolean)}
	 * @see #drawToGraphics(Graphics, Rectangle, double, boolean)
	 */
	private int lastTextSize=-1;

	/**
	 * Zoomfaktor beim letzten Aufruf von {@link #drawToGraphics(Graphics, Rectangle, double, boolean)}
	 * @see #drawToGraphics(Graphics, Rectangle, double, boolean)
	 */
	private double lastZoomFont=-1;

	/**
	 * Schriftausgestaltung (fett, kursiv) beim letzten Aufruf von {@link #drawToGraphics(Graphics, Rectangle, double, boolean)}
	 * @see #drawToGraphics(Graphics, Rectangle, double, boolean)
	 */
	private double lastStyleFont=-1;

	/**
	 * Schriftart beim letzten Aufruf von {@link #drawToGraphics(Graphics, Rectangle, double, boolean)}
	 * @see #drawToGraphics(Graphics, Rectangle, double, boolean)
	 */
	private FontCache.FontFamily lastFamily=null;

	/**
	 * In {@link #drawToGraphics(Graphics, Rectangle, double, boolean)} generierte
	 * Schriftart f�r den Text
	 * @see #drawToGraphics(Graphics, Rectangle, double, boolean)
	 */
	private Font lastFontMain;

	/**
	 * In {@link #drawToGraphics(Graphics, Rectangle, double, boolean)} generierte
	 * Schriftart f�r den Titel
	 * @see #drawToGraphics(Graphics, Rectangle, double, boolean)
	 */
	private Font lastFontTitle;

	/**
	 * Farbe f�r den Titel �ber dem eigentlichen Text
	 * @see #drawToGraphics(Graphics, Rectangle, double, boolean)
	 */
	private Color titleColor;

	/**
	 * F�llfarbe beim letzten Aufruf von {@link #drawToGraphics(Graphics, Rectangle, double, boolean)}
	 * @see #lastFillAlpha
	 * @see #lastComputedFillColor
	 */
	private Color lastFillColor=null;

	/**
	 * Deckkraft beim letzten Aufruf von {@link #drawToGraphics(Graphics, Rectangle, double, boolean)}
	 * @see #lastFillColor
	 * @see #lastComputedFillColor
	 */
	private double lastFillAlpha=0.0;

	/**
	 * Berechnete F�llfarbe beim letzten Aufruf von {@link #drawToGraphics(Graphics, Rectangle, double, boolean)}
	 * @see #lastFillColor
	 * @see #lastFillAlpha
	 */
	private Color lastComputedFillColor=null;

	/**
	 * Zeichnet das Element in ein <code>Graphics</code>-Objekt
	 * @param graphics	<code>Graphics</code>-Objekt in das das Element eingezeichnet werden soll
	 * @param drawRect	Tats�chlich sichtbarer Ausschnitt
	 * @param zoom	Zoomfaktor
	 * @param showSelectionFrames	Rahmen anzeigen, wenn etwas ausgew�hlt ist
	 */
	@Override
	public void drawToGraphics(final Graphics graphics, final Rectangle drawRect, final double zoom, final boolean showSelectionFrames) {
		final String text=getDisplayText(false);
		final String title=getDisplayText(true);

		int style=Font.PLAIN;
		if (bold) style+=Font.BOLD;
		if (italic) style+=Font.ITALIC;
		if (lastFamily!=fontFamily || textSize!=lastTextSize || zoom!=lastZoomFont || style!=lastStyleFont || lastFontMain==null || lastFontTitle==null) {
			lastFontMain=FontCache.getFontCache().getFont(fontFamily,style,(int)FastMath.round(textSize*zoom));
			lastFontTitle=FontCache.getFontCache().getFont(FontCache.defaultFamily,0,(int)FastMath.round(9*zoom));
			lastFamily=fontFamily;
			lastTextSize=textSize;
			lastZoomFont=zoom;
			lastStyleFont=style;
		}

		if (fillColor!=lastFillColor || fillAlpha!=lastFillAlpha) {
			if (fillColor==null) {
				lastComputedFillColor=null;
			} else {
				lastComputedFillColor=new Color(fillColor.getRed(),fillColor.getGreen(),fillColor.getBlue(),Math.max(0,Math.min(255,((int)Math.round(255*fillAlpha)))));
			}
			lastFillColor=fillColor;
			lastFillAlpha=fillAlpha;
		}

		int width;
		int height;
		if (title.trim().isEmpty()) {
			graphics.setFont(lastFontMain);
			width=graphics.getFontMetrics().stringWidth(text);
			height=graphics.getFontMetrics().getAscent()+graphics.getFontMetrics().getDescent();
		} else {
			graphics.setFont(lastFontTitle);
			width=graphics.getFontMetrics().stringWidth(title);
			height=graphics.getFontMetrics().getAscent()+graphics.getFontMetrics().getDescent();
			graphics.setFont(lastFontMain);
			width=FastMath.max(width,graphics.getFontMetrics().stringWidth(text));
			height+=graphics.getFontMetrics().getAscent()+graphics.getFontMetrics().getDescent();
		}
		final Point point=getPosition(true);

		int w=(int)FastMath.round(width/zoom);
		int h=(int)FastMath.round(height/zoom);
		if (getSize().width!=w || getSize().height!=h) setSize(new Dimension(w,h));

		setClip(graphics,drawRect,null);

		if (lastComputedFillColor!=null) {
			graphics.setColor(lastComputedFillColor);
			graphics.fillRect(point.x,point.y,w,h);
			graphics.setColor(color);
		}

		int x=(int)FastMath.round(point.x*zoom);
		if (title.trim().isEmpty()) {
			graphics.setColor(color);
			graphics.setFont(lastFontMain);
			int y=(int)FastMath.round(point.y*zoom)+graphics.getFontMetrics().getAscent();
			graphics.drawString(text,x,y);
		} else {
			if (titleColor==null) titleColor=FlatLaFHelper.isDark()?EditModel.BLACK_COLOR_IN_DARK_MODE:Color.BLACK;
			graphics.setColor(titleColor);
			graphics.setFont(lastFontTitle);
			int y=(int)FastMath.round(point.y*zoom)+graphics.getFontMetrics().getAscent();
			graphics.drawString(title,x,y);
			y+=graphics.getFontMetrics().getDescent();

			graphics.setColor(color);
			graphics.setFont(lastFontMain);
			y+=graphics.getFontMetrics().getAscent();
			graphics.drawString(text,x,y);
		}

		if (isSelected() && showSelectionFrames) {
			drawRect(graphics,drawRect,zoom,Color.GREEN,2,null,2);
		} else {
			if (isSelectedArea() && showSelectionFrames) drawRect(graphics,drawRect,zoom,Color.BLUE,2,null,2);
		}
	}

	/**
	 * Name des Elementtyps f�r die Anzeige im Kontextmen�
	 * @return	Name des Elementtyps
	 */
	@Override
	public String getContextMenuElementName() {
		return Language.tr("Surface.AnimationText.Name");
	}

	/**
	 * Liefert ein <code>Runnable</code>-Objekt zur�ck, welches aufgerufen werden kann, wenn die Eigenschaften des Elements ver�ndert werden sollen.
	 * @param owner	�bergeordnetes Fenster
	 * @param readOnly	Wird dieser Parameter auf <code>true</code> gesetzt, so wird die "Ok"-Schaltfl�che deaktiviert
	 * @param clientData	Kundendaten-Objekt
	 * @param sequences	Fertigungspl�ne-Liste
	 * @return	<code>Runnable</code>-Objekt zur Einstellung der Eigenschaften oder <code>null</code>, wenn das Element keine Eigenschaften besitzt
	 */
	@Override
	public Runnable getProperties(final Component owner, final boolean readOnly, final ModelClientData clientData, final ModelSequences sequences) {
		return ()->{
			new ModelElementAnimationTextValueDialog(owner,ModelElementAnimationTextValue.this,readOnly);
		};
	}

	/**
	 * Liefert den jeweiligen xml-Element-Namen f�r das Modell-Element
	 * @return	xml-Element-Namen, der diesem Modell-Element zugeordnet werden soll
	 */
	@Override
	public String[] getXMLNodeNames() {
		return Language.trAll("Surface.AnimationText.XML.Root");
	}

	/**
	 * Speichert die Eigenschaften des Modell-Elements als Untereintr�ge eines xml-Knotens
	 * @param doc	�bergeordnetes xml-Dokument
	 * @param node	�bergeordneter xml-Knoten, in dessen Kindelementen die Daten des Objekts gespeichert werden sollen
	 */
	@Override
	protected void addPropertiesDataToXML(final Document doc, final Element node) {
		super.addPropertiesDataToXML(doc,node);

		Element sub;

		/* Schriftart */
		if (fontFamily!=FontCache.defaultFamily) {
			sub=doc.createElement(Language.trPrimary("Surface.AnimationText.XML.FontFamily"));
			node.appendChild(sub);
			sub.setTextContent(fontFamily.name);
		}

		/* Schriftgr��e, fett/kursiv */
		sub=doc.createElement(Language.trPrimary("Surface.AnimationText.XML.FontSize"));
		node.appendChild(sub);
		sub.setTextContent(""+textSize);
		if (bold) sub.setAttribute(Language.trPrimary("Surface.AnimationText.XML.FontSize.Bold"),"1");
		if (italic) sub.setAttribute(Language.trPrimary("Surface.AnimationText.XML.FontSize.Italic"),"1");

		/* Textfarbe */
		sub=doc.createElement(Language.trPrimary("Surface.AnimationText.XML.Color"));
		node.appendChild(sub);
		sub.setTextContent(EditModel.saveColor(color));

		/* Ausgabemodus */
		sub=doc.createElement(Language.trPrimary("Surface.AnimationText.XML.Mode"));
		node.appendChild(sub);

		switch (mode) {
		case MODE_EXPRESSION_NUMBER:
			sub.setAttribute(Language.trPrimary("Surface.AnimationText.XML.Mode.Type"),Language.trPrimary("Surface.AnimationText.XML.Mode.Type.Number"));
			break;
		case MODE_EXPRESSION_PERCENT:
			sub.setAttribute(Language.trPrimary("Surface.AnimationText.XML.Mode.Type"),Language.trPrimary("Surface.AnimationText.XML.Mode.Type.Percent"));
			break;
		case MODE_EXPRESSION_TIME:
			sub.setAttribute(Language.trPrimary("Surface.AnimationText.XML.Mode.Type"),Language.trPrimary("Surface.AnimationText.XML.Mode.Type.TimeValue"));
			break;
		case MODE_TIME:
			sub.setAttribute(Language.trPrimary("Surface.AnimationText.XML.Mode.Type"),Language.trPrimary("Surface.AnimationText.XML.Mode.Type.Time"));
			break;
		case MODE_DATE:
			sub.setAttribute(Language.trPrimary("Surface.AnimationText.XML.Mode.Type"),Language.trPrimary("Surface.AnimationText.XML.Mode.Type.Date"));
			break;
		}
		if (mode==ModeExpression.MODE_EXPRESSION_NUMBER || mode==ModeExpression.MODE_EXPRESSION_PERCENT || mode==ModeExpression.MODE_EXPRESSION_TIME) {
			sub.setTextContent(expression);
			if (digits!=1) sub.setAttribute(Language.trPrimary("Surface.AnimationText.XML.Digits"),""+digits);
		}
		if (mode==ModeExpression.MODE_DATE) {
			sub.setTextContent(NumberTools.formatLongNoGrouping(dateZero));
		}

		/* Hintergrund */
		if (fillColor!=null) {
			sub=doc.createElement(Language.trPrimary("Surface.AnimationText.XML.BackgroundColor"));
			node.appendChild(sub);
			sub.setTextContent(EditModel.saveColor(fillColor));
			if (fillAlpha<1) sub.setAttribute(Language.trPrimary("Surface.AnimationText.XML.BackgroundColor.Alpha"),NumberTools.formatSystemNumber(fillAlpha));
		}
	}

	/**
	 * L�dt eine einzelne Einstellung des Modell-Elements aus einem einzelnen xml-Element.
	 * @param name	Name des xml-Elements
	 * @param content	Inhalt des xml-Elements als Text
	 * @param node	xml-Element, aus dem das Datum geladen werden soll
	 * @return	Tritt ein Fehler auf, so wird die Fehlermeldung als String zur�ckgegeben. Im Erfolgsfall wird <code>null</code> zur�ckgegeben.
	 */
	@Override
	protected String loadProperty(final String name, final String content, final Element node) {
		String error=super.loadProperty(name,content,node);
		if (error!=null) return error;

		/* Schriftgr��e, fett/kursiv */
		if (Language.trAll("Surface.AnimationText.XML.FontSize",name)) {
			Integer I;
			I=NumberTools.getNotNegativeInteger(content);
			if (I==null) return String.format(Language.tr("Surface.XML.ElementSubError"),name,node.getParentNode().getNodeName());
			textSize=I;
			bold=(Language.trAllAttribute("Surface.AnimationText.XML.FontSize.Bold",node).equals("1"));
			italic=(Language.trAllAttribute("Surface.AnimationText.XML.FontSize.Italic",node).equals("1"));
			return null;
		}

		/* Schriftart */
		if (Language.trAll("Surface.AnimationText.XML.FontFamily",name)) {
			fontFamily=FontCache.getFontCache().getFamilyFromName(content);
			return null;
		}

		/* Textfarbe */
		if (Language.trAll("Surface.AnimationText.XML.Color",name) && !content.trim().isEmpty()) {
			color=EditModel.loadColor(content);
			if (color==null) return String.format(Language.tr("Surface.XML.ElementSubError"),name,node.getParentNode().getNodeName());
			return null;
		}

		/* Ausgabemodus */
		if (Language.trAll("Surface.AnimationText.XML.Mode",name)) {
			final String art=Language.trAllAttribute("Surface.AnimationText.XML.Mode.Type",node);
			mode=null;
			if (Language.trAll("Surface.AnimationText.XML.Mode.Type.Number",art)) mode=ModeExpression.MODE_EXPRESSION_NUMBER;
			if (Language.trAll("Surface.AnimationText.XML.Mode.Type.Percent",art)) mode=ModeExpression.MODE_EXPRESSION_PERCENT;
			if (Language.trAll("Surface.AnimationText.XML.Mode.Type.TimeValue",art)) mode=ModeExpression.MODE_EXPRESSION_TIME;
			if (Language.trAll("Surface.AnimationText.XML.Mode.Type.Time",art)) mode=ModeExpression.MODE_TIME;
			if (Language.trAll("Surface.AnimationText.XML.Mode.Type.Date",art)) mode=ModeExpression.MODE_DATE;
			if (mode==null) return String.format(Language.tr("Surface.XML.AttributeSubError"),Language.trPrimary("Surface.AnimationText.XML.Mode.Type"),name,node.getParentNode().getNodeName());
			if (mode==ModeExpression.MODE_EXPRESSION_NUMBER || mode==ModeExpression.MODE_EXPRESSION_PERCENT || mode==ModeExpression.MODE_EXPRESSION_TIME) {
				expression=content;
				final String digitsText=Language.trAllAttribute("Surface.AnimationText.XML.Digits",node);
				if (!digitsText.isEmpty()) {
					final Integer I=NumberTools.getNotNegativeInteger(digitsText);
					if (I==null || I.intValue()>15) return String.format(Language.tr("Surface.XML.AttributeSubError"),Language.trPrimary("Surface.AnimationText.XML.Digits"),name,node.getParentNode().getNodeName());
					digits=I.intValue();
				}
			}
			if (mode==ModeExpression.MODE_DATE) {
				final Long L=NumberTools.getNotNegativeLong(content);
				if (L==null) return String.format(Language.tr("Surface.XML.AttributeSubError"),Language.trPrimary("Surface.AnimationText.XML.Digits"),name,node.getParentNode().getNodeName());
				dateZero=L.longValue();
			}
			return null;
		}

		/* Hintergrund */
		if (Language.trAll("Surface.AnimationText.XML.BackgroundColor",name) && !content.trim().isEmpty()) {
			final Color color=EditModel.loadColor(content);
			if (color==null) return String.format(Language.tr("Surface.XML.ElementSubError"),name,node.getParentNode().getNodeName());
			fillColor=color;
			final String alpha=Language.trAllAttribute("Surface.AnimationText.XML.BackgroundColor.Alpha",node);
			if (!alpha.trim().isEmpty()) {
				final Double D=NumberTools.getDouble(alpha);
				if (D==null || D<0 || D>1) return String.format(Language.tr("Surface.XML.AttributeSubError"),Language.trPrimary("Surface.AnimationText.XML.BackgroundColor.Alpha"),name,node.getParentNode().getNodeName());
				fillAlpha=D;
			}
			return null;
		}

		return null;
	}

	/**
	 * Rechenausdruck der w�hrend der Animation ausgewertet
	 * werden soll, um den darzustellenden Wert zu erhalten.
	 * @see #initAnimation(SimulationData)
	 * @see #calcExpression(SimulationData)
	 */
	private ExpressionCalc animationExpression;

	/**
	 * Wertet {@link #animationExpression} aus und liefert
	 * den zu zeichnenden Wert zur�ck.
	 * @param simData	Simulationsdatenobjekt
	 * @return	Darzustellender Wert
	 */
	private double calcExpression(final SimulationData simData) {
		if (animationExpression==null) return 0.0;
		simData.runData.setClientVariableValues(null);
		return animationExpression.calcOrDefault(simData.runData.variableValues,simData,null,0);
	}

	/**
	 * Tempor�rer {@link StringBuilder} zum
	 * Umwandeln von Zahlen in Zeichenketten
	 * @see #updateSimulationData(SimulationData, boolean)
	 */
	private StringBuilder animationSB;

	/**
	 * Unterschiede zwischen angezeigten und aktuellem Wert,
	 * die in Abh�ngigkeit von der Anzahl an Nachkommastellen
	 * kein Neuzeichnen erfordern.
	 * @see #updateSimulationData(SimulationData, boolean)
	 */
	private static final double[] maxDelta=new double[] {
			FastMath.pow(10,0),
			FastMath.pow(10,-1),
			FastMath.pow(10,-2),
			FastMath.pow(10,-3),
			FastMath.pow(10,-4),
			FastMath.pow(10,-5),
			FastMath.pow(10,-6),
			FastMath.pow(10,-7),
			FastMath.pow(10,-8),
			FastMath.pow(10,-9),
			FastMath.pow(10,-10),
			FastMath.pow(10,-11),
			FastMath.pow(10,-12),
			FastMath.pow(10,-13),
			FastMath.pow(10,-14),
			FastMath.pow(10,-15),
			FastMath.pow(10,-16),
			FastMath.pow(10,-17),
			FastMath.pow(10,-18),
			FastMath.pow(10,-19),
			FastMath.pow(10,-20)
	};

	@Override
	public boolean updateSimulationData(final SimulationData simData, final boolean isPreview) {
		String s=null;
		double d;
		long l;
		switch (mode) {
		case MODE_EXPRESSION_NUMBER:
			if (isPreview) return false;
			d=calcExpression(simData);
			if (simTextValue!=null && Math.abs(simTextValueDouble-d)<maxDelta[digits+1]) return false;
			if (animationSB==null) animationSB=new StringBuilder();
			s=NumberTools.formatNumber(d,digits,animationSB);
			simTextValueDouble=d;
			break;
		case MODE_EXPRESSION_PERCENT:
			if (isPreview) return false;
			d=calcExpression(simData);
			if (simTextValue!=null && Math.abs(simTextValueDouble-d)<maxDelta[digits+4]) return false;
			if (animationSB==null) animationSB=new StringBuilder();
			s=NumberTools.formatPercent(d,digits,animationSB);
			simTextValueDouble=d;
			break;
		case MODE_EXPRESSION_TIME:
			d=calcExpression(simData);
			if (simTextValue!=null && Math.abs(simTextValueDouble-d)<0.5) return false;
			s=TimeTools.formatLongTime(Math.floor(d));
			simTextValueDouble=d;
			break;
		case MODE_TIME:
			l=simData.currentTime/1000;
			if (simTextValue!=null && simTextValueLong==l) return false;
			s=TimeTools.formatLongTime(l);
			simTextValueLong=l;
			break;
		case MODE_DATE:
			l=simData.currentTime/1000;
			if (simTextValue!=null && simTextValueLong==l) return false;
			s=DateTools.formatUserDate((l+dateZero)*1000);
			simTextValueLong=l;
			break;
		default:
			return false;
		}

		drawLock.acquireUninterruptibly();
		try {
			simTextValue=s;
		} finally {
			drawLock.release();
		}

		return true;
	}

	@Override
	public void initAnimation(SimulationData simData) {
		animationExpression=new ExpressionCalc(simData.runModel.variableNames);
		if (animationExpression.parse(expression)>=0) animationExpression=null;
	}

	@Override
	public String getHelpPageName() {
		return "ModelElementAnimationText";
	}

	/**
	 * Liefert die Javascript-Daten f�r die Station zur Ausgabe des Modells als HTML-Datei
	 * @param outputBuilder	Builder, der die Gesamtdaten aufnehmen soll
	 * @return	Javascript-Daten f�r die Station
	 */
	private String getHTMLText(final HTMLOutputBuilder outputBuilder) {
		final StringBuilder sb=new StringBuilder();

		sb.append("function drawAnimationTextValue(p,labelText,text,fontSize,fontBold,fontItalic,color) {\n");

		sb.append("  context.textAlign=\"left\";\n");
		sb.append("  context.textBaseline=\"hanging\";\n");

		sb.append("  context.font=\"9px Verdana,Lucida,sans-serif\";\n");
		sb.append("  context.fillStyle=\"black\";\n");
		sb.append("  context.fillText(labelText,p.x,p.y);\n");

		sb.append("  context.font=\"\"+fontSize+\"px Verdana,Lucida,sans-serif\";\n");
		sb.append("  if (fontBold) context.font=\"bold \"+context.font;\n");
		sb.append("  if (fontItalic) context.font=\"italic \"+context.font;\n");
		sb.append("  context.fillStyle=color;\n");
		sb.append("  var lines=text.split(\"\\n\");\n");
		sb.append("  for (var i=0;i<lines.length;i++) {\n");
		sb.append("    context.fillText(lines[i],p.x,p.y+9+fontSize*i);\n");
		sb.append("  }\n");

		sb.append("}\n");

		return sb.toString();
	}

	/**
	 * Zeichnet das Element in einem {@link HTMLOutputBuilder}
	 * @param outputBuilder	Builder, der die Daten aufnehmen soll
	 */
	private void specialOutputHTML(final HTMLOutputBuilder outputBuilder) {
		outputBuilder.addJSUserFunction("drawAnimationTextValue",builder->getHTMLText(builder));

		final Point p=getPosition(true);
		final String text=HTMLOutputBuilder.encodeHTML(getDisplayText(false),true).replace("\n","\\n");
		final String title=HTMLOutputBuilder.encodeHTML(getDisplayText(true),true).replace("\n","\\n");

		outputBuilder.outputBody.append("drawAnimationTextValue({x: "+p.x+", y: "+p.y+"},\""+title+"\",\""+text+"\","+textSize+","+(bold?"true":"false")+","+(italic?"true":"false")+",\""+HTMLOutputBuilder.colorToHTML(color)+"\");\n");
	}

	/**
	 * Zeichnet das Element in einem {@link SpecialOutputBuilder}
	 * @param outputBuilder	Builder, der die Daten aufnehmen soll
	 */
	@Override
	public void specialOutput(final SpecialOutputBuilder outputBuilder) {
		if (outputBuilder instanceof HTMLOutputBuilder) specialOutputHTML((HTMLOutputBuilder)outputBuilder);
	}

	@Override
	public void search(final FullTextSearch searcher) {
		super.search(searcher);

		if (mode==ModeExpression.MODE_EXPRESSION_NUMBER || mode==ModeExpression.MODE_EXPRESSION_PERCENT || mode==ModeExpression.MODE_EXPRESSION_TIME) {
			searcher.testString(this,Language.tr("Editor.DialogBase.Search.OutputExpression"),expression,newExpression->{expression=newExpression;});
		}
		searcher.testInteger(this,Language.tr("Editor.DialogBase.Search.FontSize"),textSize,newFontSize->{if (newFontSize>0) textSize=newFontSize;});
	}
}