/**
 * Copyright 2021 Alexander Herzog
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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import org.apache.commons.math3.util.FastMath;
import org.apache.jena.ext.com.google.common.base.Objects;

import mathtools.NumberTools;
import ui.modeleditor.coreelements.ModelElementBox;
import ui.tools.FlatLaFHelper;

/**
 * Erm�glicht das Zeichnen von Beschriftungen an eine Animationsdiagramm-Y-Achse
 * @author Alexander Herzog
 */
public class AxisDrawer {
	/**
	 * Welche Werte sollen an der Achse angezeigt werden?
	 */
	public enum Mode {
		/** Keine Werte anzeigen */
		OFF(0),
		/** Nur Minimum und Maximum anzeigen */
		MIN_MAX(1),
		/** Minimum, Zwischenwerte und Maximum anzeigen */
		FULL(2);

		/**
		 * Nummer des Modus (zum Speichern)
		 */
		public final int nr;

		/**
		 * Konstruktor des Enum
		 * @param nr	Nummer des Modus
		 */
		Mode(final int nr) {
			this.nr=nr;
		}

		/**
		 * Liefert den zu einer Nummer zugeh�rigen Modus
		 * @param nr	Nummer
		 * @return	Zugeh�riger Modus (oder Fallback-Wert)
		 */
		public static Mode fromNr(final int nr) {
			for (Mode mode: values()) if (mode.nr==nr) return mode;
			return OFF;
		}

		/**
		 * Liefert den zu einer Nummer zugeh�rigen Modus
		 * @param nr	Nummer
		 * @return	Zugeh�riger Modus (oder Fallback-Wert)
		 */
		public static Mode fromNr(final String nr) {
			final Integer I=NumberTools.getInteger(nr);
			if (I==null) return OFF;
			return fromNr(I.intValue());
		}
	}

	/** Rotations-System f�r die Beschriftung der y-Achse */
	private final AffineTransform transformRotate;

	/**
	 * Aktueller Minimalwert
	 */
	private double minValue;

	/**
	 * Aktueller Maximalwert
	 */
	private double maxValue;

	/**
	 * Darstellungsmodus
	 */
	private Mode mode=Mode.OFF;

	/**
	 * M�ssen die Texte beim n�chsten Zeichnen aktualisiert werden?
	 * @see #setAxisValues(double, double, Mode, String)
	 * @see #prepare(Graphics2D, double, int)
	 */
	private boolean needUpdateText;

	/**
	 * Anzuzeigende Werte
	 * @see #prepare(Graphics2D, double, int)
	 */
	private String[] text;

	/**
	 * Breiten der anzuzeigenden Texte
	 * @see #prepare(Graphics2D, double, int)
	 */
	private int[] textWidth;

	/**
	 * Optionale Textbeschriftung f�r die Achse
	 */
	private String label;

	/**
	 * Breite f�r die optionale Textbeschriftung f�r die Achse
	 * @see #label
	 * @see #prepare(Graphics2D, double, int)
	 */
	private int labelWidth;

	/**
	 * Schriftfarbe
	 * @see #prepare(Graphics2D, double, int)
	 */
	private Color fontColor;

	/**
	 * Schriftart f�r die Achenbeschriftung
	 */
	private Font axisFont;

	/**
	 * Zoomfaktor zu dem {@link #axisFont} berechnet wurde
	 * @see #axisFont
	 */
	private double axisFontZoom;

	/**
	 * H�he der Schrift {@link #axisFont} �ber der Grundlinie
	 * @see #axisFont
	 */
	private int axisFontAscent;

	/**
	 * H�he der Schrift {@link #axisFont} unter der Grundlinie
	 * @see #axisFont
	 */
	private int axisFontDescent;

	/**
	 * Abstand zwischen zwei Wertebeschriftungen
	 */
	private static final int VALUE_STEP_WIDE=50;

	/**
	 * Konstruktor der Klasse
	 */
	public AxisDrawer() {
		transformRotate=new AffineTransform();
		transformRotate.rotate(Math.toRadians(-90));
	}

	/**
	 * Stellt den Minimal- und den Maximalwert ein.
	 * @param min	Minimalwert
	 * @param max	Maximalwert
	 * @param mode	Darstellungsmodus
	 * @param label	Beschriftung f�r die Achse (kann <code>null</code> oder leer sein)
	 */
	public void setAxisValues(final double min, final double max, Mode mode, String label) {
		if (mode==null) mode=Mode.OFF;
		if (min==max) mode=Mode.OFF;
		if (label!=null && label.trim().isEmpty()) label=null;
		if (minValue==min && maxValue==max && this.mode==mode && Objects.equal(label,this.label)) return;
		minValue=min;
		maxValue=max;
		this.mode=mode;
		this.label=label;
		needUpdateText=true;
		fontColor=FlatLaFHelper.isDark()?Color.LIGHT_GRAY:Color.BLACK;
	}

	/**
	 * Bereitet die Darstellung der Texte vor (Berechnung der Schriftarten usw.)
	 * @param graphics	<code>Graphics</code>-Objekt in das das Element eingezeichnet werden soll
	 * @param zoom	Zoomfaktor (zur Berechnung der Fontgr��e)
	 * @param range	Zeichenbreite bzw. H�he (zur Berechnung der Anzahl an Zwischenschritten)
	 */
	private void prepare(final Graphics2D graphics, final double zoom, final int range) {
		boolean needUpdateTextWidth=false;

		/* Farbe einstellen */
		graphics.setColor(fontColor);

		/* Font einstellen */
		if (axisFont==null || axisFontZoom!=zoom) {
			axisFont=new Font(ModelElementBox.DEFAULT_FONT_TYPE,Font.PLAIN,(int)FastMath.round(11*zoom));
			axisFontZoom=zoom;
			graphics.setFont(axisFont);
			final FontMetrics fontMetrics=graphics.getFontMetrics();
			axisFontAscent=fontMetrics.getAscent();
			axisFontDescent=fontMetrics.getDescent();
			needUpdateTextWidth=true;
		}
		graphics.setFont(axisFont);

		/* Zahlen an Achsen vorbereiten */
		if (mode!=Mode.OFF) {
			/* Anzahl an Zwischenschritten */
			final int steps;
			if (mode==Mode.FULL) {
				steps=(int)Math.round((range/zoom)/VALUE_STEP_WIDE)+1;
			} else {
				steps=2;
			}

			/* Texte wenn n�tig berechnen */
			if (text==null || text.length!=steps) {
				text=new String[steps];
				needUpdateText=true;
			}
			if (needUpdateText) {
				needUpdateTextWidth=true;
				boolean ok=false;
				int digits=1;
				while (!ok) {
					ok=true;
					for (int i=0;i<steps;i++) {
						final double value=minValue+(maxValue-minValue)*i/(steps-1);
						final String s=NumberTools.formatNumber(value,digits);
						if (digits<3) {
							for (int j=0;j<i;j++) if (text[j].equals(s)) {ok=false; break;}
							if (!ok) {digits++; break;}
						}
						text[i]=s;
					}
				}
				needUpdateText=false;
			}

			/* Textbreiten wenn n�tig neu berechnen */
			if (textWidth==null || textWidth.length!=steps) {
				textWidth=new int[steps];
				needUpdateTextWidth=true;
			}
			if (needUpdateTextWidth) {
				final FontMetrics fontMetrics=graphics.getFontMetrics();
				for (int i=0;i<steps;i++) {
					textWidth[i]=fontMetrics.stringWidth(text[i]);
				}
			}
		}

		/* Achsenbeschriftung vorbereiten */
		if (label!=null) {
			if (needUpdateTextWidth) {
				final FontMetrics fontMetrics=graphics.getFontMetrics();
				labelWidth=fontMetrics.stringWidth(label);
			}
		}

		needUpdateText=false;
	}

	/**
	 * Maximale Breite der Wertetexte
	 * (zur Bestimmung der x-Position der Textbeschriftung der y-Achse)
	 * @see #drawY(Graphics2D, double, Rectangle)
	 */
	private int lastMaxTextWidth;

	/**
	 * Zeichnet die y-Achsenbeschriftung
	 * @param graphics	<code>Graphics</code>-Objekt in das das Element eingezeichnet werden soll
	 * @param zoom	Zoomfaktor
	 * @param rectangle	Gem�� dem Zoomfaktor umgerechneter sichtbarer Bereich f�r das Diagramm
	 */
	public void drawY(final Graphics2D graphics, final double zoom, final Rectangle rectangle) {
		if (mode==Mode.OFF && label==null) return;
		prepare(graphics,zoom,rectangle.height);

		if (mode!=Mode.OFF) {
			final int maxI=text.length-1;
			final int x=rectangle.x-(int)Math.round(zoom);
			final int y1=rectangle.y+rectangle.height;
			final int y2=rectangle.y+axisFontAscent;

			for (int i=0;i<=maxI;i++) {
				graphics.drawString(text[i],x-textWidth[i],y1+(y2-y1)*i/maxI);
			}
		}

		if (label!=null) {
			final AffineTransform transformDefault=graphics.getTransform();
			graphics.transform(transformRotate);

			if (mode==Mode.OFF) {
				lastMaxTextWidth=0;
			} else {
				for (int w: textWidth) lastMaxTextWidth=Math.max(lastMaxTextWidth,w);
			}
			final int x=rectangle.x-3*(int)Math.round(zoom)-lastMaxTextWidth-axisFontDescent;
			final int y=rectangle.y+rectangle.height/2+labelWidth/2;
			graphics.drawString(label,-y,x);

			graphics.setTransform(transformDefault);
		}
	}

	/**
	 * Zeichnet die x-Achsenbeschriftung
	 * @param graphics	<code>Graphics</code>-Objekt in das das Element eingezeichnet werden soll
	 * @param zoom	Zoomfaktor
	 * @param rectangle	Gem�� dem Zoomfaktor umgerechneter sichtbarer Bereich f�r das Diagramm
	 */
	public void drawX(final Graphics2D graphics, final double zoom, final Rectangle rectangle) {
		if (mode==Mode.OFF && label==null) return;
		prepare(graphics,zoom,rectangle.width);

		if (mode!=Mode.OFF) {
			final int maxI=text.length-1;
			final int x1=rectangle.x;
			final int x2=rectangle.x+rectangle.width-textWidth[maxI];
			final int y=rectangle.y+rectangle.height+axisFontAscent;

			for (int i=0;i<=maxI;i++) {
				graphics.drawString(text[i],x1+(x2-x1)*i/maxI,y);
			}
		}

		if (label!=null) {
			final int y;
			if (mode==Mode.OFF) {
				y=rectangle.y+rectangle.height+axisFontAscent;
			} else {
				y=rectangle.y+rectangle.height+(axisFontAscent+axisFontDescent)+axisFontAscent;
			}
			final int x=rectangle.x+rectangle.width/2-labelWidth/2;
			graphics.drawString(label,x,y);
		}
	}

	/**
	 * Zeichnet die y-Achsenbeschriftung (von oben nach unten)
	 * @param graphics	<code>Graphics</code>-Objekt in das das Element eingezeichnet werden soll
	 * @param zoom	Zoomfaktor
	 * @param rectangle	Gem�� dem Zoomfaktor umgerechneter sichtbarer Bereich f�r das Diagramm
	 */
	public void drawYInvers(final Graphics2D graphics, final double zoom, final Rectangle rectangle) {
		double d;
		d=minValue; minValue=maxValue; maxValue=d;
		drawY(graphics,zoom,rectangle);
		d=minValue; minValue=maxValue; maxValue=d;
	}

	/**
	 * Zeichnet die x-Achsenbeschriftung (von rechts nach links)
	 * @param graphics	<code>Graphics</code>-Objekt in das das Element eingezeichnet werden soll
	 * @param zoom	Zoomfaktor
	 * @param rectangle	Gem�� dem Zoomfaktor umgerechneter sichtbarer Bereich f�r das Diagramm
	 */
	public void drawXInvers(final Graphics2D graphics, final double zoom, final Rectangle rectangle) {
		double d;
		d=minValue; minValue=maxValue; maxValue=d;
		drawX(graphics,zoom,rectangle);
		d=minValue; minValue=maxValue; maxValue=d;
	}
}
