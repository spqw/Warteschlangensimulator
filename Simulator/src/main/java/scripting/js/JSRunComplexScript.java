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
package scripting.js;

import java.util.function.Consumer;

import language.Language;
import simulator.editmodel.EditModel;
import simulator.statistics.Statistics;

/**
 * Javascript-Interpreter zur Durchf�hrung von mehreren verkn�pften Simulationsl�ufen
 * @author Alexander Herzog
 */
public class JSRunComplexScript {
	/**
	 * Callback f�r Ausgaben
	 */
	private final Consumer<String> outputCallback;

	/**
	 * Ausgangsmodell
	 */
	private final EditModel model;

	/**
	 * Pfad zur zugeh�rigen Modelldatei (als Basis f�r relative Pfade in Ausgabeelementen)
	 */
	private final String editModelPath;

	/**
	 * Ver�ndertes Modell
	 * @see #getChangedModel()
	 */
	private EditModel modelChanged;

	/**
	 * Stellt das Javascript-"Model"-Objekt zur Verf�gung.
	 * @see #run(String)
	 */
	private JSRunComplexScriptModel modelJS;

	/**
	 * Stellt das Javascript-"Statistics"-Objekt zur Verf�gung.
	 * @see #run(String)
	 */
	private JSCommandXML statisticsJS;


	/**
	 * Stellt das Javascript-"FileOutput"-Objekt zur Verf�gung.
	 * @see #run(String)
	 */
	private JSCommandOutput fileJS;

	/**
	 * Erfolg der letzten Skriptausf�hrung
	 * @see #getLastSuccess()
	 */
	private boolean lastSuccess;

	/**
	 * Ausgabe des Skriptes
	 * @see #getResults()
	 */
	private String lastResults;

	/**
	 * Konstruktor der Klasse
	 * @param model	Ausgangsmodell
	 * @param editModelPath	Pfad zur zugeh�rigen Modelldatei (als Basis f�r relative Pfade in Ausgabeelementen)
	 * @param outputCallback	Callback f�r Ausgaben
	 */
	public JSRunComplexScript(final EditModel model, final String editModelPath, final Consumer<String> outputCallback) {
		this.model=model;
		this.editModelPath=editModelPath;
		this.outputCallback=outputCallback;
		this.modelChanged=model;
		lastSuccess=false;
		lastResults="";
	}

	/**
	 * Liefert das ver�nderte Modell.
	 * @return	Ver�ndertes Modell
	 */
	public EditModel getChangedModel() {
		return modelChanged;
	}

	/**
	 * Liefert den Pfad zur zugeh�rigen Modelldatei (als Basis f�r relative Pfade in Ausgabeelementen).
	 * @return	Pfad zur zugeh�rigen Modelldatei (als Basis f�r relative Pfade in Ausgabeelementen)
	 */
	public String getEditModelPath() {
		return editModelPath;
	}

	/**
	 * Setzt ein neues ver�ndertes Modell.
	 * @param model	Ver�ndertes Modell
	 */
	public void setChangedModel(final EditModel model) {
		if (model==null) modelChanged=this.model.clone(); else modelChanged=model.clone();
	}

	/**
	 * Stellt die Statistikdaten, die im JS-Skript zur Verf�gung stehen sollen, ein
	 * @param statistics	Statistikdaten f�r die Nutzung im JS-Skript
	 */
	public void setStatistics(final Statistics statistics) {
		this.statisticsJS.setXML(statistics.saveToXMLDocument(),statistics.loadedStatistics);
	}

	/**
	 * F�hrt ein Skript aus
	 * @param script	Auszuf�hrendes Skript
	 * @return	Liefert im Erfolgsfall <code>true</code>
	 */
	public boolean run(final String script) {
		final JSBuilder builder=new JSBuilder(1000*86_400,outputCallback);
		builder.addBinding("System",new JSCommandSystem());
		builder.addBinding("Output",new JSCommandOutput(builder.output,false));
		builder.addBinding("Model",modelJS=new JSRunComplexScriptModel(builder.output,this));
		builder.addBinding("Statistics",statisticsJS=new JSCommandXML(builder.output,null,null,true));
		builder.addBinding("FileOutput",fileJS=new JSCommandOutput(builder.output,true));

		final JSEngine runner=builder.build();
		if (runner==null) {
			lastResults=String.format(Language.tr("Statistics.Filter.EngineInitError"),builder.engineName.name);
			return false;
		}

		if (!runner.initScript(script)) {
			lastResults=Language.tr("Statistics.Filter.ScriptInitError");
			return false;
		}
		lastSuccess=runner.run();
		lastResults=runner.getResult();
		return lastSuccess;
	}

	/**
	 * Liefert die Ausgabe des Skriptes.
	 * @return	Ausgabe des Skriptes
	 */
	public String getResults() {
		return lastResults;
	}

	/**
	 * Gibt an, ob die letzte Skriptausf�hrung erfolgreich war.
	 * @return	Erfolg der letzten Skriptausf�hrung
	 */
	public boolean getLastSuccess() {
		return lastSuccess;
	}

	/**
	 * Bricht die Skriptausf�hrung ab.
	 */
	public synchronized void cancel() {
		modelJS.cancel();
		statisticsJS.cancel();
		fileJS.cancel();
	}
}
