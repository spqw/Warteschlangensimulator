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
package ui.statistics;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.io.Serializable;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

import language.Language;
import mathtools.distribution.tools.FileDropperData;
import scripting.js.JSRunDataFilter;
import scripting.js.JSRunDataFilterTools;
import simulator.statistics.Statistics;
import systemtools.MsgBox;
import tools.SetupData;
import ui.images.Images;
import ui.script.ScriptEditorAreaBuilder;
import ui.script.ScriptEditorPanel;
import ui.script.ScriptPopup;
import ui.script.ScriptTools;

/**
 * Erm�glicht die Filterung der Ergebnisse mit Hilfe von Javascript.
 * @author Alexander Herzog
 * @see StatisticViewerFastAccessBase
 * @see StatisticViewerFastAccess
 */
public class StatisticViewerFastAccessJS extends StatisticViewerFastAccessBase {
	/**
	 * Serialisierungs-ID der Klasse
	 * @see Serializable
	 */
	private static final long serialVersionUID = 7766458667440808352L;

	/** Eingabefeld f�r das Skript */
	private RSyntaxTextArea filter;
	/** Zuletzt gespeichertes Skript */
	private String lastSavedFilterText;
	/** Zuletzt ausgef�hrtes Skript */
	private String lastInterpretedFilterText;
	/** Ergebnis der Ausf�hrung von {@link #lastInterpretedFilterText} */
	private String lastInterpretedFilterResult;
	/** Javascript-Interpreter zur Filterung von Statistikausgaben */
	private JSRunDataFilter dataFilter;

	/**
	 * Konstruktor der Klasse
	 * @param helpFastAccess	Hilfe f�r Schnellzugriff-Seite
	 * @param helpFastAccessModal	Hilfe f�r Schnellzugriff-Dialog
	 * @param statistics	Statistik-Objekt, dem die Daten entnommen werden sollen
	 * @param resultsChanged	Runnable das aufgerufen wird, wenn sich die Ergebnisse ver�ndert haben
	 */
	public StatisticViewerFastAccessJS(final Runnable helpFastAccess, final Runnable helpFastAccessModal, final Statistics statistics, final Runnable resultsChanged) {
		super(helpFastAccess,helpFastAccessModal,statistics,resultsChanged,true);

		/* Filtertext */
		final ScriptEditorAreaBuilder builder=new ScriptEditorAreaBuilder(ScriptPopup.ScriptMode.Javascript,false,e->process(false));
		builder.addAutoCompleteFeatures(ScriptEditorPanel.featuresFilter);
		builder.addFileDropper(e->{
			final FileDropperData data=(FileDropperData)e.getSource();
			final File file=data.getFile();
			if (file.isFile()) {
				if (discardFilterOk(getParent())) {
					final String script=JSRunDataFilterTools.loadText(file);
					if (script!=null) {filter.setText(script); process(true); lastSavedFilterText=script;}
				}
				data.dragDropConsumed();
			}
		});
		final RTextScrollPane scrollFilter;
		add(scrollFilter=new RTextScrollPane(filter=builder.get()));
		scrollFilter.setLineNumbersEnabled(true);

		/* Filtertext laden */
		filter.setText(SetupData.getSetup().filterJavascript);
		process(true);
		lastSavedFilterText="";
	}

	@Override
	protected Icon getIcon() {
		return Images.SCRIPT_MODE_JAVASCRIPT.getIcon();
	}

	@Override
	protected void addXML(final String selector) {
		final String oldFilter=filter.getText();
		final String newCommand=String.format("Output.println(Statistics.xml(\"%s\"));",selector.replace("\"","\\\""));
		filter.setText(oldFilter+(oldFilter.endsWith("\n")?"":"\n")+newCommand);
		process(false);
	}

	@Override
	public void setStatistics(final Statistics statistics) {
		super.setStatistics(statistics);
		dataFilter=new JSRunDataFilter(statistics.saveToXMLDocument(),statistics.loadedStatistics);
	}

	/**
	 * F�hrt das Skript aus
	 * @param forceProcess	Verarbeitung erzwingen (<code>true</code>) auch wenn sich das Skript seit der letzten Ausf�hrung nicht ver�ndert hat?
	 */
	public void process(final boolean forceProcess) {
		final String text=filter.getText();
		if (lastInterpretedFilterText!=null && text.equals(lastInterpretedFilterText) && lastInterpretedFilterResult!=null && !forceProcess) {
			setResults(lastInterpretedFilterResult);
			return;
		}
		lastInterpretedFilterText=text;

		if (text.trim().isEmpty()) {
			lastInterpretedFilterResult="";
		} else {
			if (dataFilter==null) dataFilter=new JSRunDataFilter(statistics.saveToXMLDocument(),statistics.loadedStatistics);
			dataFilter.run(text);
			lastInterpretedFilterResult=dataFilter.getResults();
		}
		setResults(lastInterpretedFilterResult);

		SetupData setup=SetupData.getSetup();
		setup.filterJavascript=filter.getText();
		setup.saveSetupWithWarning(null);
	}

	/**
	 * Speichert das Skript oder die Ergebnisse in einer Datei.
	 * @param parentFrame	�bergeordnetes Fenster (zur Ausrichtung des Dialogs)
	 * @param text	Zu speichernder Text
	 * @param isJS	Skript (<code>true</code>) oder Ausgabetext (<code>false</code>)
	 * @return	Liefert <code>true</code>, wenn der Text erfolgreich gespeichert werden konnte
	 */
	private boolean saveTextToFile(Component parentFrame, String text, final boolean isJS) {
		final String fileName;
		if (isJS) {
			fileName=ScriptTools.selectJSSaveFile(parentFrame,null,null);
		} else {
			fileName=ScriptTools.selectTextSaveFile(parentFrame,null,null);
		}
		if (fileName==null) return false;
		final File file=new File(fileName);

		if (file.exists()) {
			if (!MsgBox.confirmOverwrite(getParent(),file)) return false;
		}

		return JSRunDataFilterTools.saveText(text,file,false);
	}

	/**
	 * L�dt ein Skript aus einer Datei.
	 * @param parentFrame	�bergeordnetes Fenster (zur Ausrichtung des Dialogs)
	 * @param isJS	Skript (<code>true</code>) oder Ausgabetext (<code>false</code>)
	 * @return	Liefert im Erfolgsfall den geladenen Text
	 */
	private String loadTextFromFile(Container parentFrame, final boolean isJS) {
		final String fileName;
		if (isJS) {
			fileName=ScriptTools.selectJSFile(parentFrame,null,null);
		} else {
			fileName=ScriptTools.selectTextFile(parentFrame,null,null);
		}
		if (fileName==null) return null;
		final File file=new File(fileName);

		return JSRunDataFilterTools.loadText(file);
	}

	/**
	 * Darf das aktuelle Skript verworfen werden (ggf. Nutzer fragen) ?
	 * @param parentFrame	�bergeordnetes Fenster (zur Ausrichtung des Dialogs)
	 * @return	Liefert <code>true</code>, wenn das Skript verworfen werden darf
	 */
	private boolean discardFilterOk(Container parentFrame) {
		if (filter.getText().equals(lastSavedFilterText)) return true;

		switch (MsgBox.confirmSave(getParent(),Language.tr("Filter.Save.Title"),Language.tr("Filter.Save.Info"))) {
		case JOptionPane.YES_OPTION:
			if (!saveTextToFile(parentFrame,filter.getText(),true)) return false;
			return true;
		case JOptionPane.NO_OPTION: return true;
		case JOptionPane.CANCEL_OPTION: return false;
		default: return false;
		}
	}

	@Override
	protected void commandNew() {
		if (!discardFilterOk(getParent())) return;
		if (!filter.getText().equals(lastSavedFilterText)) lastSavedFilterText=filter.getText();
		filter.setText("");
		process(true);
	}

	@Override
	protected void commandLoad() {
		if (!discardFilterOk(getParent())) return;
		String s=loadTextFromFile(getParent(),true);
		if (s!=null) {filter.setText(s); process(true); lastSavedFilterText=s;}
	}

	@Override
	protected void commandSave() {
		if (saveTextToFile(getParent(),filter.getText(),true)) lastSavedFilterText=filter.getText();
	}

	@Override
	protected void commandTools(final JButton sender) {
		/* alt: new ListPopup(sender,helpFastAccessModal).popupFull(statistics,filter,()->process(),false); */
		final ScriptPopup popup=new ScriptPopup(sender,statistics.editModel,statistics,ScriptPopup.ScriptMode.Javascript,helpFastAccessModal);
		popup.addFeatures(ScriptEditorPanel.featuresFilter);
		popup.build();
		popup.show(filter,()->process(false));
	}
}
