/**
 * Copyright 2022 Alexander Herzog
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
import java.awt.Graphics;

import org.apache.commons.math3.util.FastMath;

/**
 * Einfacher Text-Renderer
 * @author Alexander Herzog
 * @see ModelElementText
 */
public class ModelElementTextRendererPlain extends ModelElementTextRenderer {
	/**
	 * Auszugebende Textzeilen
	 */
	private String[] lines;

	/**
	 * Schriftgr��e beim letzten Aufruf von {@link #setStyle(int, boolean, boolean, String, ui.modeleditor.elements.ModelElementText.TextAlign)}
	 * @see #setStyle(int, boolean, boolean, String, ui.modeleditor.elements.ModelElementText.TextAlign)
	 * @see #calcIntern(Graphics, double)
	 * @see #draw(Graphics, int, int, Color)
	 */
	private int fontSize=12;

	/**
	 * Text fett ausgeben?
	 * @see #setStyle(int, boolean, boolean, String, ui.modeleditor.elements.ModelElementText.TextAlign)
	 * @see #calcIntern(Graphics, double)
	 * @see #draw(Graphics, int, int, Color)
	 */
	private boolean bold;

	/**
	 * Text kursiv ausgeben?
	 * @see #setStyle(int, boolean, boolean, String, ui.modeleditor.elements.ModelElementText.TextAlign)
	 * @see #calcIntern(Graphics, double)
	 * @see #draw(Graphics, int, int, Color)
	 */
	private boolean italic;

	/**
	 * Schriftart
	 * @see #setStyle(int, boolean, boolean, String, ui.modeleditor.elements.ModelElementText.TextAlign)
	 * @see #calcIntern(Graphics, double)
	 * @see #draw(Graphics, int, int, Color)
	 */
	private String fontFamily="";

	/**
	 * Ausrichtung der Zeilen
	 * @see #setStyle(int, boolean, boolean, String, ui.modeleditor.elements.ModelElementText.TextAlign)
	 * @see #calcIntern(Graphics, double)
	 * @see #draw(Graphics, int, int, Color)
	 */
	private ModelElementText.TextAlign textAlign;

	/**
	 * Berechnete Schriftart
	 * @see #setStyle(int, boolean, boolean, String, ui.modeleditor.elements.ModelElementText.TextAlign)
	 * @see #calcIntern(Graphics, double)
	 * @see #draw(Graphics, int, int, Color)
	 */
	private Font font;

	/**
	 * H�he der Textzeile �ber der Grundlinie
	 * @see #calcIntern(Graphics, double)
	 * @see #draw(Graphics, int, int, Color)
	 */
	private int ascent;

	/**
	 * H�he einer einzelnen Zeile
	 * @see #calcIntern(Graphics, double)
	 * @see #draw(Graphics, int, int, Color)
	 */
	private int lineHeight;

	/**
	 * Konstruktor der Klasse
	 */
	public ModelElementTextRendererPlain() {
		/*
		 * Wird nur ben�tigt, um einen JavaDoc-Kommentar f�r diesen (impliziten) Konstruktor
		 * setzen zu k�nnen, damit der JavaDoc-Compiler keine Warnung mehr ausgibt.
		 */
	}

	@Override
	protected void processLines(String[] lines) {
		this.lines=lines;
	}

	@Override
	public void setStyle(final int fontSize, final boolean bold, final boolean italic, final String fontFamily, final ModelElementText.TextAlign textAlign) {
		if (fontSize==this.fontSize && bold==this.bold && italic==this.italic && fontFamily.equals(this.fontFamily) && textAlign==this.textAlign) return;
		this.fontSize=fontSize;
		this.bold=bold;
		this.italic=italic;
		this.fontFamily=fontFamily;
		this.textAlign=textAlign;
		setNeedRecalc();
	}

	@Override
	protected void calcIntern(Graphics graphics, double zoom) {
		int style=Font.PLAIN;
		if (bold) style+=Font.BOLD;
		if (italic) style+=Font.ITALIC;
		font=FontCache.getFontCache().getFont(fontFamily,style,(int)Math.round(fontSize*zoom));

		graphics.setFont(font);

		width=0;
		height=0;
		ascent=graphics.getFontMetrics().getAscent();
		lineHeight=ascent+graphics.getFontMetrics().getDescent();

		for (String line: lines) {
			width=FastMath.max(width,graphics.getFontMetrics().stringWidth(line));
			height+=lineHeight;
		}
	}

	@Override
	protected void drawIntern(final Graphics graphics, int x, int y) {
		y+=ascent;

		graphics.setFont(font);
		FontMetrics metrics=null;
		int lineWidth;

		for (String line: lines) {
			switch (textAlign) {
			case LEFT:
				graphics.drawString(line,x,y);
				break;
			case CENTER:
				if (metrics==null) metrics=graphics.getFontMetrics();
				lineWidth=metrics.stringWidth(line);
				graphics.drawString(line,x+(width-lineWidth)/2,y);
				break;
			case RIGHT:
				if (metrics==null) metrics=graphics.getFontMetrics();
				lineWidth=metrics.stringWidth(line);
				graphics.drawString(line,x+(width-lineWidth),y);
				break;
			default:
				graphics.drawString(line,x,y);
				break;
			}
			y+=lineHeight;
		}
	}
}
