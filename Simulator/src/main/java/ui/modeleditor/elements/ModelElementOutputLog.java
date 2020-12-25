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
import java.awt.Point;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import language.Language;
import simulator.editmodel.EditModel;
import ui.images.Images;
import ui.modeleditor.ModelClientData;
import ui.modeleditor.ModelSequences;
import ui.modeleditor.ModelSurface;
import ui.modeleditor.ModelSurfacePanel;
import ui.modeleditor.coreelements.ModelElement;
import ui.modeleditor.coreelements.ModelElementBox;
import ui.modeleditor.coreelements.ModelElementMultiInSingleOutBox;
import ui.modeleditor.descriptionbuilder.ModelDescriptionBuilder;
import ui.modeleditor.fastpaint.Shapes;

/**
 * Schreibt Daten in die Logausgabe.
 * @author Alexander Herzog
 */
public class ModelElementOutputLog extends ModelElementMultiInSingleOutBox implements ElementNoRemoteSimulation {
	/**
	 * Ausgabemodi f�r die einzelnen Eintr�ge
	 * @see ModelElementOutputLog#getModes()
	 * @see ModelElementOutputLog#getModeNameDescriptions()
	 */
	public enum OutputMode {
		/** Gibt die Systemzeit aus */
		MODE_TIMESTAMP,

		/** Gibt einen Text aus (siehe <code>data</code>) */
		MODE_TEXT,

		/** Gibt einen Tabulator aus */
		MODE_TABULATOR,

		/** Gibt einen Zeilenumbruch aus */
		MODE_NEWLINE,

		/** Berechnet einen Ausdruck und gibt das Ergebnis aus (siehe <code>data</code>) */
		MODE_EXPRESSION,

		/** Gibt den Namen des Kundentyps aus */
		MODE_CLIENT,

		/** Gibt die bisherige Wartezeit des Kunden als Zahl aus */
		MODE_WAITINGTIME_NUMBER,

		/** Gibt die bisherige Wartezeit des Kunden als Zeit aus */
		MODE_WAITINGTIME_TIME,

		/** Gibt die bisherige Transferzeit des Kunden als Zahl aus */
		MODE_TRANSFERTIME_NUMBER,

		/** Gibt die bisherige Transferzeit des Kunden als Zeit aus */
		MODE_TRANSFERTIME_TIME,

		/** Gibt die bisherige Bedienzeit des Kunden als Zahl aus */
		MODE_PROCESSTIME_NUMBER,

		/** Gibt die bisherige Bedienzeit des Kunden als Zeit aus */
		MODE_PROCESSTIME_TIME,

		/** Gibt die bisherige Verweilzeit des Kunden als Zahl aus */
		MODE_RESIDENCETIME_NUMBER,

		/** Gibt die bisherige Verweilzeit des Kunden als Zeit aus */
		MODE_RESIDENCETIME_TIME,

		/** Gibt eine dem Kunden zugeordnete Zeichenkette aus */
		MODE_STRING
	}

	/**
	 * Liste mit den Modi der Ausgabeelemente
	 * @see #getModes()
	 */
	private List<OutputMode> mode;

	/**
	 * Liste mit den zus�tzlichen Daten der Ausgabeelemente
	 * @see #getData()
	 */
	private List<String> data;

	/**
	 * Konstruktor der Klasse
	 * @param model	Modell zu dem dieses Element geh�ren soll (kann sp�ter nicht mehr ge�ndert werden)
	 * @param surface	Zeichenfl�che zu dem dieses Element geh�ren soll (kann sp�ter nicht mehr ge�ndert werden)
	 */
	public ModelElementOutputLog(final EditModel model, final ModelSurface surface) {
		super(model,surface,Shapes.ShapeType.SHAPE_DOCUMENT);
		mode=new ArrayList<>();
		data=new ArrayList<>();
	}

	/**
	 * Liefert eine Liste mit Beschreibungen zu den Ausgabemodi
	 * @return	Liste mit Beschreibungen zu den Ausgabemodi
	 * @see OutputMode
	 */
	public String[] getModeNameDescriptions() {
		return new String[] {
				Language.tr("Surface.Output.XML.Element.TypeDescription.TimeStamp"),
				Language.tr("Surface.Output.XML.Element.TypeDescription.Text"),
				Language.tr("Surface.Output.XML.Element.TypeDescription.Tabulator"),
				Language.tr("Surface.Output.XML.Element.TypeDescription.LineBreak"),
				Language.tr("Surface.Output.XML.Element.TypeDescription.Expression"),
				Language.tr("Surface.Output.XML.Element.TypeDescription.ClientType"),
				Language.tr("Surface.Output.XML.Element.TypeDescription.WaitingTimeNumber"),
				Language.tr("Surface.Output.XML.Element.TypeDescription.WaitingTime"),
				Language.tr("Surface.Output.XML.Element.TypeDescription.TransferTimeNumber"),
				Language.tr("Surface.Output.XML.Element.TypeDescription.TransferTime"),
				Language.tr("Surface.Output.XML.Element.TypeDescription.ProcessTimeNumber"),
				Language.tr("Surface.Output.XML.Element.TypeDescription.ProcessTime"),
				Language.tr("Surface.Output.XML.Element.TypeDescription.ResidenceTimeNumber"),
				Language.tr("Surface.Output.XML.Element.TypeDescription.ResidenceTime"),
				Language.tr("Surface.Output.XML.Element.TypeDescription.String")
		};
	}

	/**
	 * Icon, welches im "Element hinzuf�gen"-Dropdown-Men� angezeigt werden soll.
	 * @return	Icon f�r das Dropdown-Men�
	 */
	@Override
	public URL getAddElementIcon() {
		return Images.MODELEDITOR_ELEMENT_OUTPUT.getURL();
	}

	/**
	 * Tooltip f�r den "Element hinzuf�gen"-Dropdown-Men�-Eintrag.
	 * @return Tooltip f�r den "Element hinzuf�gen"-Dropdown-Men�eintrag
	 */
	@Override
	public String getToolTip() {
		return Language.tr("Surface.OutputLog.Tooltip");
	}

	/**
	 * Liefert die Liste mit den Modi der einzelnen Ausgabeelemente
	 * @return	Liste mit den Modi der Ausgabeelemente
	 */
	public List<OutputMode> getModes() {
		return mode;
	}

	/**
	 * Liefert die Liste mit den zus�tzlichen Daten der einzelnen Ausgabeelemente
	 * @return	Liste mit den zus�tzlichen Daten der Ausgabeelemente
	 */
	public List<String> getData() {
		return data;
	}

	/**
	 * �berpr�ft, ob das Element mit dem angegebenen Element inhaltlich identisch ist.
	 * @param element	Element mit dem dieses Element verglichen werden soll.
	 * @return	Gibt <code>true</code> zur�ck, wenn die beiden Elemente identisch sind.
	 */
	@Override
	public boolean equalsModelElement(ModelElement element) {
		if (!super.equalsModelElement(element)) return false;
		if (!(element instanceof ModelElementOutputLog)) return false;

		if (mode.size()!=((ModelElementOutputLog)element).mode.size()) return false;
		if (data.size()!=((ModelElementOutputLog)element).data.size()) return false;
		for (int i=0;i<mode.size();i++) if (!((ModelElementOutputLog)element).mode.get(i).equals(mode.get(i))) return false;
		for (int i=0;i<data.size();i++) if (!((ModelElementOutputLog)element).data.get(i).equals(data.get(i))) return false;

		return true;
	}

	/**
	 * �bertr�gt die Einstellungen von dem angegebenen Element auf dieses.
	 * @param element	Element, von dem alle Einstellungen �bernommen werden sollen
	 */
	@Override
	public void copyDataFrom(ModelElement element) {
		super.copyDataFrom(element);
		if (element instanceof ModelElementOutputLog) {
			mode.addAll(((ModelElementOutputLog)element).mode);
			data.addAll(((ModelElementOutputLog)element).data);
		}
	}

	/**
	 * Erstellt eine Kopie des Elements
	 * @param model	Modell zu dem das kopierte Element geh�ren soll.
	 * @param surface	Zeichenfl�che zu der das kopierte Element geh�ren soll.
	 * @return	Kopiertes Element
	 */
	@Override
	public ModelElementOutputLog clone(final EditModel model, final ModelSurface surface) {
		final ModelElementOutputLog element=new ModelElementOutputLog(model,surface);
		element.copyDataFrom(this);
		return element;
	}

	/**
	 * Name des Elementtyps f�r die Anzeige im Kontextmen�
	 * @return	Name des Elementtyps
	 */
	@Override
	public String getContextMenuElementName() {
		return Language.tr("Surface.OutputLog.Name");
	}

	/**
	 * Liefert die Bezeichnung des Typs des Elemente (zur Anzeige in der Element-Box)
	 * @return	Name des Typs
	 */
	@Override
	public String getTypeName() {
		return Language.tr("Surface.OutputLog.Name.Short");
	}

	/**
	 * Vorgabe-Hintergrundfarbe f�r die Box
	 * @see #getTypeDefaultBackgroundColor()
	 */
	private static final Color defaultBackgroundColor=new Color(230,230,230);

	/**
	 * Liefert die Vorgabe-Hintergrundfarbe f�r die Box
	 * @return	Vorgabe-Hintergrundfarbe f�r die Box
	 */
	@Override
	public Color getTypeDefaultBackgroundColor() {
		return defaultBackgroundColor;
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
			new ModelElementOutputLogDialog(owner,ModelElementOutputLog.this,readOnly);
		};
	}

	/**
	 * F�gt optionale Men�punkte zu einem "Folgestation hinzuf�gen"-Untermen� hinzu, welche
	 * es erm�glichen, zu dem aktuellen Element passende Folgestationen hinzuzuf�gen.
	 * @param parentMenu	Untermen� des Kontextmen�s, welches die Eintr�ge aufnimmt
	 * @param addNextStation	Callback, das aufgerufen werden kann, wenn ein Element zur Zeichenfl�che hinzugef�gt werden soll
	 */
	@Override
	protected void addNextStationContextMenuItems(final JMenu parentMenu, final Consumer<ModelElementBox> addNextStation) {
		NextStationHelper.nextStationsData(this,parentMenu,addNextStation);
	}

	/**
	 * F�gt optional weitere Eintr�ge zum Kontextmen� hinzu
	 * @param owner	�bergeordnetes Element
	 * @param popupMenu	Kontextmen� zu dem die Eintr�ge hinzugef�gt werden sollen
	 * @param surfacePanel	Zeichenfl�che
	 * @param point	Punkt auf den geklickt wurde
	 * @param readOnly	Wird dieser Parameter auf <code>true</code> gesetzt, so k�nnen �ber das Kontextmen� keine �nderungen an dem Modell vorgenommen werden
	 */
	@Override
	protected void addContextMenuItems(final Component owner, final JPopupMenu popupMenu, final ModelSurfacePanel surfacePanel, final Point point, final boolean readOnly) {
		if (addRemoveEdgesContextMenuItems(popupMenu,readOnly)) popupMenu.addSeparator();
	}

	/**
	 * Liefert den jeweiligen xml-Element-Namen f�r das Modell-Element
	 * @return	xml-Element-Namen, der diesem Modell-Element zugeordnet werden soll
	 */
	@Override
	public String[] getXMLNodeNames() {
		return Language.trAll("Surface.OutputLog.XML.Root");
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

		for (int i=0;i<Math.min(mode.size(),data.size());i++) {
			node.appendChild(sub=doc.createElement(Language.trPrimary("Surface.Output.XML.Element")));

			String type="";
			switch (mode.get(i)) {
			case MODE_TIMESTAMP: type=Language.tr("Surface.Output.XML.Element.Type.TimeStamp"); break;
			case MODE_TEXT: type=Language.tr("Surface.Output.XML.Element.Type.Text"); break;
			case MODE_TABULATOR: type=Language.tr("Surface.Output.XML.Element.Type.Tabulator"); break;
			case MODE_NEWLINE: type=Language.tr("Surface.Output.XML.Element.Type.LineBreak"); break;
			case MODE_EXPRESSION: type=Language.tr("Surface.Output.XML.Element.Type.Expression"); break;
			case MODE_CLIENT: type=Language.tr("Surface.Output.XML.Element.Type.ClientType"); break;
			case MODE_WAITINGTIME_NUMBER: type=Language.tr("Surface.Output.XML.Element.Type.WaitingTimeNumber"); break;
			case MODE_WAITINGTIME_TIME: type=Language.tr("Surface.Output.XML.Element.Type.WaitingTime"); break;
			case MODE_TRANSFERTIME_NUMBER: type=Language.tr("Surface.Output.XML.Element.Type.TransferTimeNumber"); break;
			case MODE_TRANSFERTIME_TIME: type=Language.tr("Surface.Output.XML.Element.Type.TransferTime"); break;
			case MODE_PROCESSTIME_NUMBER: type=Language.tr("Surface.Output.XML.Element.Type.ProcessTimeNumber"); break;
			case MODE_PROCESSTIME_TIME: type=Language.tr("Surface.Output.XML.Element.Type.ProcessTime"); break;
			case MODE_RESIDENCETIME_NUMBER: type=Language.tr("Surface.Output.XML.Element.Type.ResidenceTimeNumber"); break;
			case MODE_RESIDENCETIME_TIME: type=Language.tr("Surface.Output.XML.Element.Type.ResidenceTime"); break;
			case MODE_STRING: type=Language.tr("Surface.Output.XML.Element.Type.String"); break;
			}
			sub.setAttribute(Language.trPrimary("Surface.Output.XML.Element.Type"),type);
			if (!data.get(i).isEmpty()) sub.setAttribute(Language.trPrimary("Surface.Output.XML.Element.Data"),data.get(i));
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

		if (Language.trAll("Surface.Output.XML.Element",name)) {
			final String m=Language.trAllAttribute("Surface.Output.XML.Element.Type",node);
			final String d=Language.trAllAttribute("Surface.Output.XML.Element.Data",node);
			OutputMode index=OutputMode.MODE_TIMESTAMP;
			boolean ok=false;
			if (Language.trAll("Surface.Output.XML.Element.Type.TimeStamp",m)) {index=OutputMode.MODE_TIMESTAMP; ok=true;}
			if (Language.trAll("Surface.Output.XML.Element.Type.Text",m)) {index=OutputMode.MODE_TEXT; ok=true;}
			if (Language.trAll("Surface.Output.XML.Element.Type.Tabulator",m)) {index=OutputMode.MODE_TABULATOR; ok=true;}
			if (Language.trAll("Surface.Output.XML.Element.Type.LineBreak",m)) {index=OutputMode.MODE_NEWLINE; ok=true;}
			if (Language.trAll("Surface.Output.XML.Element.Type.Expression",m)) {index=OutputMode.MODE_EXPRESSION; ok=true;}
			if (Language.trAll("Surface.Output.XML.Element.Type.ClientType",m)) {index=OutputMode.MODE_CLIENT; ok=true;}
			if (Language.trAll("Surface.Output.XML.Element.Type.WaitingTimeNumber",m)) {index=OutputMode.MODE_WAITINGTIME_NUMBER; ok=true;}
			if (Language.trAll("Surface.Output.XML.Element.Type.WaitingTime",m)) {index=OutputMode.MODE_WAITINGTIME_TIME; ok=true;}
			if (Language.trAll("Surface.Output.XML.Element.Type.TransferTimeNumber",m)) {index=OutputMode.MODE_TRANSFERTIME_NUMBER; ok=true;}
			if (Language.trAll("Surface.Output.XML.Element.Type.TransferTime",m)) {index=OutputMode.MODE_TRANSFERTIME_TIME; ok=true;}
			if (Language.trAll("Surface.Output.XML.Element.Type.ProcessTimeNumber",m)) {index=OutputMode.MODE_PROCESSTIME_NUMBER; ok=true;}
			if (Language.trAll("Surface.Output.XML.Element.Type.ProcessTime",m)) {index=OutputMode.MODE_PROCESSTIME_TIME; ok=true;}
			if (Language.trAll("Surface.Output.XML.Element.Type.ResidenceTimeNumber",m)) {index=OutputMode.MODE_RESIDENCETIME_NUMBER; ok=true;}
			if (Language.trAll("Surface.Output.XML.Element.Type.ResidenceTime",m)) {index=OutputMode.MODE_RESIDENCETIME_TIME; ok=true;}
			if (Language.trAll("Surface.Output.XML.Element.Type.String",m)) {index=OutputMode.MODE_STRING; ok=true;}
			if (!ok) return String.format(Language.tr("Surface.XML.AttributeSubError"),Language.trPrimary("Surface.Output.XML.Element.Type"),name,node.getParentNode().getNodeName());
			mode.add(index);
			data.add(d);
			return null;
		}

		return null;
	}

	@Override
	public String getHelpPageName() {
		return "ModelElementOutputLog";
	}

	/**
	 * Erstellt eine Beschreibung f�r das aktuelle Element
	 * @param descriptionBuilder	Description-Builder, der die Beschreibungsdaten zusammenfasst
	 */
	@Override
	public void buildDescription(final ModelDescriptionBuilder descriptionBuilder) {
		super.buildDescription(descriptionBuilder);

		final String[] modeDesciptions=getModeNameDescriptions();
		for (int i=0;i<mode.size();i++) {
			final OutputMode m=mode.get(i);
			final String value;
			String text="";
			switch (m) {
			case MODE_TIMESTAMP: text=modeDesciptions[0]; break;
			case MODE_TEXT: text=modeDesciptions[1]; break;
			case MODE_TABULATOR: text=modeDesciptions[2]; break;
			case MODE_NEWLINE: text=modeDesciptions[3]; break;
			case MODE_EXPRESSION: text=modeDesciptions[4]; break;
			case MODE_CLIENT: text=modeDesciptions[5]; break;
			case MODE_WAITINGTIME_NUMBER: text=modeDesciptions[6]; break;
			case MODE_WAITINGTIME_TIME: text=modeDesciptions[7]; break;
			case MODE_TRANSFERTIME_NUMBER: text=modeDesciptions[8]; break;
			case MODE_TRANSFERTIME_TIME: text=modeDesciptions[9]; break;
			case MODE_PROCESSTIME_NUMBER: text=modeDesciptions[10]; break;
			case MODE_PROCESSTIME_TIME: text=modeDesciptions[11]; break;
			case MODE_RESIDENCETIME_NUMBER: text=modeDesciptions[12]; break;
			case MODE_RESIDENCETIME_TIME: text=modeDesciptions[13]; break;
			case MODE_STRING: text=modeDesciptions[14]; break;
			}

			if (m==OutputMode.MODE_TEXT || m==OutputMode.MODE_EXPRESSION || m==OutputMode.MODE_STRING) value=text+": "+data.get(i); else value=text;
			descriptionBuilder.addProperty(Language.tr("ModelDescription.Output.Property"),value,1000);
		}
	}
}
