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
package net.webcalc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import language.Language;
import mathtools.MultiTable;
import mathtools.Table;
import net.calc.SimulationServer;
import simulator.AnySimulator;
import simulator.Simulator;
import simulator.StartAnySimulator;
import simulator.editmodel.EditModel;
import simulator.statistics.Statistics;
import systemtools.statistics.StatisticViewerReport;
import tools.DateTools;
import tools.SetupData;
import ui.parameterseries.ParameterCompareRunner;
import ui.parameterseries.ParameterCompareSetup;
import ui.statistics.StatisticsPanel;
import xml.XMLTools;
import xml.XMLTools.FileType;

/**
 * Diese Klasse bildet einen einzelnen Rechen-Task innerhalb
 * eines {@link CalcWebServer}-Objekts ab.
 * @author Alexander Herzog
 * @see CalcWebServer
 */
public class CalcFuture {
	/**
	 * Status des Rechen-Tasks
	 * @author Alexander Herzog
	 * @see CalcFuture#getStatus()
	 */
	public enum Status {
		/**
		 * Der Task wartet noch.
		 */
		WAITING(0),

		/**
		 * Der Task wird gerade ausgef�hrt.
		 */
		PROCESSING(1),

		/**
		 * Die Ausf�hrung des Tasks ist beendet, war aber nicht erfolgreich.
		 */
		DONE_ERROR(2),

		/**
		 * Die Ausf�hrung des Tasks wurde erfolgreich abgeschlossen.
		 * Statistikdaten stehen zur Verf�gung.
		 */
		DONE_SUCCESS(3);

		/**
		 * ID zur Identifikation des Status
		 */
		public final int id;

		/**
		 * Konstruktor des Enum
		 * @param id	ID zur Identifikation des Status
		 */
		Status(final int id) {
			this.id=id;
		}
	}

	/**
	 * Art der Simulation
	 * @author Alexander Herzog
	 *
	 */
	public enum SimulationType {
		/** Einfaches Modell */
		MODEL,
		/** Parameterreihe */
		PARAMETER_SERIES
	}

	/** Sichert den Zugriff auf den Status und die Nachrichten ab */
	private final ReentrantLock lock;
	/** ID des Tasks zur sp�teren Identifikation in der Liste aller Tasks */
	private final long id;
	/** System-Zeitpunkt an dem die Anfrage einging (d.h. an dem der Konstruktor aufgerufen wurde) */
	private final long requestTime;
	/** Festgelegtes Modell (darf <code>null</code> sein); im Fall eines festen Modells erfolgt nur noch eine Parametrisierung */
	private final EditModel originalModel;
	/** Eingabedaten (werden bereits im Konstruktor erfasst, aber die Modell-Erstellung daraus erfolgt erst sp�ter) */
	private final byte[] input;
	/** Geladene Tabelle zum Parametrisieren des Modells */
	private final MultiTable inputTable;
	/** Dateiname der geladenen Tabelle zum Parametrisieren des Modells */
	private final String inputTableName;
	/** IP-Adresse des entfernten Klienten */
	private final String ip;
	/** Aktueller Ausf�hrungsstatus */
	private Status status;
	/** Art der Simulation */
	private SimulationType simulationType;
	/** Liste der w�hrend der Verarbeitung aufgetretenen Meldungen */
	private final List<String> messages;
	/** Ausgabe-Statistikdaten */
	private Statistics statistics;
	/** Ergebnisse der Simulation in Bin�rform ({@link #getBytes()}) */
	private byte[] zip;
	/** Dateityp f�r die Ergebnis-Bin�rform-Daten */
	private XMLTools.FileType fileType;

	/** Simulator (nur w�hrend der Ausf�hrung einer normalen Simulation ungleich <code>null</code>) */
	private volatile AnySimulator simulator=null;
	/** Parameterreihensimulator (nur w�hrend der Ausf�hrung einer Parameterreihensimulation unlgeich <code>null</code>) */
	private volatile ParameterCompareRunner runner=null;

	/**
	 * Konstruktor der Klasse
	 * @param id	ID des Tasks zur sp�teren Identifikation in der Liste aller Tasks
	 * @param input	Eingabedatei (wird sofort gelesen und danach bei der eigentlichen Ausf�hrung nicht mehr ben�tigt)
	 * @param ip	IP-Adresse des entfernten Klienten
	 * @param origFileName	Optional (kann also <code>null</code> sein) der Remote-Dateiname
	 * @param model	Festgelegtes Modell (darf <code>null</code> sein); im Fall eines festen Modells erfolgt nur noch eine Parametrisierung
	 */
	public CalcFuture(final long id, final File input, final String ip, final String origFileName, final EditModel model) {
		this.id=id;
		this.ip=ip;
		originalModel=model;
		if (originalModel==null) {
			this.input=loadFile(input);
			inputTable=null;
			inputTableName=null;
		} else {
			this.input=null;
			final MultiTable table=new MultiTable();
			if (origFileName==null) {
				if (table.load(input)) inputTable=table; else inputTable=null;
				inputTableName="data";
			} else {
				final Table.SaveMode mode=Table.getSaveModeFromFileName(new File(origFileName),true,false);
				if (table.load(input,mode)) inputTable=table; else inputTable=null;
				inputTableName=origFileName;
			}
		}

		messages=new ArrayList<>();
		lock=new ReentrantLock();
		requestTime=System.currentTimeMillis();

		if (input==null) {
			setStatus(Status.DONE_ERROR);
			addMessage(Language.tr("CalcWebServer.LoadError"));
		} else {
			setStatus(Status.WAITING);
			addMessage(Language.tr("CalcWebServer.LoadOk")+" - "+DateTools.formatUserDate(System.currentTimeMillis(),true));
		}
	}

	/**
	 * L�dt eine Datei als Bin�rdaten
	 * @param input	Zu ladende Datei
	 * @return	Bin�rdaten oder im Fehlerfall <code>null</code>
	 */
	private byte[] loadFile(final File input) {
		if (input==null) return null;

		try (FileInputStream fileInput=new FileInputStream(input)) {
			try (DataInputStream data=new DataInputStream(fileInput)) {
				int size=data.available();
				final byte[] result=new byte[size];
				if (data.read(result)<size) return null;
				return result;
			}
		} catch (IOException e) {return null;}
	}

	/**
	 * Liefert die im Konstruktor �bergebene ID zur�ck.
	 * @return	ID des Tasks
	 */
	public long getId() {
		return id;
	}

	/**
	 * Stellt den aktuellen Status ein
	 * @param status	Neuer Status
	 * @see #status
	 * @see #getStatus()
	 */
	private void setStatus(final Status status) {
		lock.lock();
		try {
			this.status=status;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Liefert den aktuellen Status des Tasks.
	 * @return	Aktueller Status
	 * @see Status
	 */
	public Status getStatus() {
		lock.lock();
		try {
			return status;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Liefert den Zeitstempel an dem der Task angelegt wurde
	 * @return	Zeitstempel des Anlegens des Tasks
	 */
	public long getRequestTime() {
		return requestTime;
	}

	/**
	 * Liefert die im Konstruktor �bergebene IP des entfernten Clienten zur�ck.
	 * @return	IP des entfernten Clienten
	 */
	public String getIP() {
		return ip;
	}

	/**
	 * F�gt eine Nachricht zu der Liste der Nachrichten hinzu
	 * @param message	Neue Nachricht
	 * @see #messages
	 * @see #getMessages()
	 */
	private void addMessage(final String message) {
		lock.lock();
		try {
			messages.add(message.replace("\n"," "));
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Liefert die Liste der Meldungen die sich bislang ergeben haben.
	 * @return	Liste der Meldungen (kann leer sein, aber nie <code>null</code>)
	 */
	public String[] getMessages() {
		lock.lock();
		try {
			return messages.toArray(new String[0]);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * F�gt eine Fehlermeldung zu der Liste der Nachrichten hinzu
	 * und setzt den Fehler-Status
	 * @param message	Fehlermeldung
	 * @see #messages
	 * @see #getMessages()
	 * @see Status#DONE_ERROR
	 */
	private void setError(final String message) {
		addMessage(message);
		setStatus(Status.DONE_ERROR);
	}

	/**
	 * Erstellt ein Ausgabe-Ergebnis bereit.
	 * @param statistics	Ausgabe-Statistikdaten
	 * @param output	Ergebnisse in Bin�rform
	 * @param fileType	Dateityp der Bin�rform-Ergebnisse
	 * @param message	Meldung bzw. Abschluss-Status
	 */
	private void setResult(final Statistics statistics, final ByteArrayOutputStream output, final XMLTools.FileType fileType, final String message) {
		this.statistics=statistics;
		setResult(output,fileType,message);
	}

	/**
	 * Erstellt ein Ausgabe-Ergebnis bereit.
	 * @param output	Ergebnisse in Bin�rform
	 * @param fileType	Dateityp der Bin�rform-Ergebnisse
	 * @param message	Meldung bzw. Abschluss-Status
	 */
	private void setResult(final ByteArrayOutputStream output, final XMLTools.FileType fileType, final String message) {
		addMessage(message);

		final ByteArrayOutputStream result=new ByteArrayOutputStream();
		try (GZIPOutputStream compressor=new GZIPOutputStream(result)) {
			compressor.write(output.toByteArray());
		} catch (IOException e) {
			addMessage(e.getMessage());
			setStatus(Status.DONE_ERROR);
			return;
		}
		zip=result.toByteArray();
		this.fileType=fileType;
		setStatus(Status.DONE_SUCCESS);
	}

	/**
	 * Liefert das Ergebnis der Simulation in Bin�rform
	 * @return	Bytes oder <code>null</code>, wenn noch keine Ergebnisse vorliegen
	 */
	public byte[] getBytes() {
		if (zip==null) return null;

		final ByteArrayOutputStream output=new ByteArrayOutputStream();
		try (GZIPInputStream decompressor=new GZIPInputStream(new ByteArrayInputStream(zip))) {
			while (decompressor.available()>0) {
				byte[] data=new byte[decompressor.available()];
				decompressor.read(data);
				output.write(data);
			}
		} catch (IOException e) {
			return null;
		}

		return output.toByteArray();
	}

	/**
	 * Liefert den Dateityp der Bin�rform-Daten
	 * @return	Dateityp der Bin�rform-Daten
	 * @see #getBytes()
	 */
	public XMLTools.FileType getXMLFileType() {
		return fileType;
	}

	/**
	 * F�hrt die Simulation eines einzelnen Modells durch.
	 * @param model	Zu simulierendes Modell
	 */
	private void runModel(final EditModel model) {
		if (!StartAnySimulator.isRemoveSimulateable(model)) {
			setError(SimulationServer.PREPARE_NO_REMOTE_MODEL);
			return;
		}

		final StartAnySimulator starter=new StartAnySimulator(model,null,null,Simulator.logTypeFull);
		final String prepareError=starter.prepare();
		if (prepareError!=null) {setError(prepareError); return;}
		addMessage(Language.tr("CalcWebServer.Simulation.Start")+" - "+DateTools.formatUserDate(System.currentTimeMillis(),true));
		simulator=starter.start();
		try {
			final Statistics statistics=simulator.getStatistic();
			final ByteArrayOutputStream output=new ByteArrayOutputStream();
			final XMLTools.FileType fileType=SetupData.getSetup().defaultSaveFormatStatistics.fileType;
			statistics.saveToStream(output,fileType);
			setResult(statistics,output,fileType,Language.tr("CalcWebServer.Simulation.Finished")+" - "+DateTools.formatUserDate(System.currentTimeMillis(),true));
		} finally {
			simulator=null;
		}
	}

	/**
	 * F�hrt eine Parameterreihensimulation durch.
	 * @param setup	Parameterreihen-Konfiguration
	 */
	private void runSeries(final ParameterCompareSetup setup) {
		runner=new ParameterCompareRunner(null,null,msg->addMessage(msg));
		try {
			final String error=runner.check(setup);
			if (error!=null) {setError(error); return;}
			runner.start();
			if (runner.waitForFinish()) {
				final ByteArrayOutputStream output=new ByteArrayOutputStream();
				final XMLTools.FileType fileType=SetupData.getSetup().defaultSaveFormatParameterSeries.fileType;
				setup.saveToStream(output,fileType);
				setResult(output,fileType,Language.tr("CalcWebServer.Simulation.Finished")+" - "+DateTools.formatUserDate(System.currentTimeMillis(),true));
			} else {
				setError(Language.tr("CalcWebServer.Simulation.Failed"));
			}
		} finally {
			runner=null;
		}
	}

	/**
	 * F�hrt die Verarbeitung aus.<br>
	 * Diese Methode kann �ber einen anderen Thread ausgef�hrt werden.
	 */
	public void run() {
		if (status!=Status.WAITING || (input==null && originalModel==null)) {
			setStatus(Status.DONE_ERROR);
			return;
		}
		setStatus(Status.PROCESSING);

		if (originalModel==null) {
			/* Normale Betriebsart, Modell oder Parameterreihen-Setup laden */

			final EditModel model=new EditModel();
			if (model.loadFromStream(new ByteArrayInputStream(input),FileType.AUTO)==null) {
				simulationType=SimulationType.MODEL;
				runModel(model);
				return;
			}

			final ParameterCompareSetup series=new ParameterCompareSetup(null);
			if (series.loadFromStream(new ByteArrayInputStream(input),FileType.AUTO)==null) {
				simulationType=SimulationType.PARAMETER_SERIES;
				runSeries(series);
				return;
			}
		} else {
			/* Festes Modell, nur Parameter laden */

			if (inputTable==null) {
				setStatus(Status.DONE_ERROR);
				return;
			}

			final EditModel changedEditModel=originalModel.modelLoadData.changeModel(originalModel,inputTable,inputTableName,true);
			if (changedEditModel==null) {
				simulationType=SimulationType.MODEL;
				runModel(originalModel);
				return;
			}

			simulationType=SimulationType.MODEL;
			changedEditModel.modelLoadData.setActive(false);
			runModel(changedEditModel);
			return;
		}

		setError(Language.tr("CalcWebServer.Simulation.UnknownFormat"));
	}

	/**
	 * Handelt es sich bei dem Rechen-Task um eine normale Simulation und wurde
	 * diese erfolgreich abgeschlossen, so kann �ber diese Methode das zugeh�rige
	 * Statistikobjekt abgefragt werden.
	 * @return	Statistik-Ergebnis-Objekt f�r den Rechen-Task
	 */
	public Statistics getStatistics() {
		return statistics;
	}

	/**
	 * html-Daten f�r den interaktiven Statistik-Viewers
	 * werden in diesem Objekt f�r weitere Abfragen �ber
	 * {@link #getStatisticsViewer()} vorgehalten.
	 * @see #getStatisticsViewer()
	 */
	private byte[] viewerData=null;

	/**
	 * Erstellt einen html-js-basierenden Statistik-Viewer (sofern es sich um eine
	 * normale Simulation handelt und diese erfolgreich abgeschlossen wurde) und liefert
	 * diesen als html-Daten zur�ck
	 * @return	html-Daten eines interaktiven Statistik-Viewers
	 * @see #getStatistics()
	 */
	public byte[] getStatisticsViewer() {
		if (viewerData!=null) return viewerData;
		if (statistics==null) return null;
		final StatisticsPanel panel=new StatisticsPanel(1);
		panel.setStatistics(statistics,false);
		final StatisticViewerReport viewer=new StatisticViewerReport(panel.getStatisticNodeRoot(),statistics,statistics.editModel.name,0,null);
		try (ByteArrayOutputStream stream=new ByteArrayOutputStream()) {
			viewer.writeReportHTMLApp(stream);
			return viewerData=stream.toByteArray();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Liefert einen Text zu dem aktuellen Status.
	 * @return	Text zu dem aktuellen Status
	 * @see #getStatus()
	 */
	private String getStatusText() {
		if (status==null) return Language.tr("CalcWebServer.Status.Unknown");
		switch (status) {
		case DONE_ERROR: return Language.tr("CalcWebServer.Status.Done_error");
		case DONE_SUCCESS: return Language.tr("CalcWebServer.Status.Done_success");
		case PROCESSING: return Language.tr("CalcWebServer.Status.Processing");
		case WAITING: return Language.tr("CalcWebServer.Status.Waiting");
		default: return Language.tr("CalcWebServer.Status.Unknown");
		}
	}

	/**
	 * Liefert die Meldungen in JSON-formatierter Form zur�ck.
	 * @return	JSON-formatierte Meldungen
	 * @see #getStatusJSON()
	 */
	private String jsonFormatMessages() {
		final StringBuilder result=new StringBuilder();
		result.append("[");
		for (int i=0;i<messages.size();i++) {
			if (i>0) result.append(",");
			result.append("\"");
			result.append(messages.get(i));
			result.append("\"");
		}
		result.append("]");
		return result.toString();
	}

	/**
	 * Liefert den aktuellen Status des Task in Form eines JSON-Objektes.
	 * @return	Status als JSON-Objekt
	 */
	public String getStatusJSON() {
		final String viewable=(this.status==Status.DONE_SUCCESS && simulationType==SimulationType.MODEL)?"1":"0";

		final StringBuilder status=new StringBuilder();
		status.append("{\n");
		status.append("  \"id\": \""+id+"\",\n");
		status.append("  \"time\": \""+DateTools.formatUserDate(requestTime,true)+"\",\n");
		status.append("  \"status\": \""+this.status.id+"\",\n");
		status.append("  \"statusText\": \""+getStatusText()+"\",\n");
		status.append("  \"viewable\": \""+viewable+"\",\n");
		status.append("  \"client\": \""+ip+"\",\n");
		status.append("  \"messages\": "+jsonFormatMessages()+"\n");
		status.append("}");
		return status.toString();
	}

	/**
	 * Bricht die Verarbeitung des Tasks ab bzw. stellt ein,
	 * dass beim sp�teren Aufruf von {@link #run()} keine
	 * Verarbeitung mehr stattfindet.
	 */
	public void cancel() {
		setError(Language.tr("CalcWebServer.Simulation.Canceled")+" - "+DateTools.formatUserDate(System.currentTimeMillis(),true));

		if (simulator!=null) simulator.cancel();
		if (runner!=null) runner.cancel();
	}
}
