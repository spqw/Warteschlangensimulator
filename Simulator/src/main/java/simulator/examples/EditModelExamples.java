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
package simulator.examples;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.w3c.dom.Document;

import language.Language;
import mathtools.Table;
import simulator.editmodel.EditModel;
import simulator.editmodel.EditModelDark;
import systemtools.MsgBox;
import tools.SetupData;
import ui.EditorPanel;
import ui.commandline.CommandBenchmark;
import ui.modeleditor.ModelSurface;
import ui.modeleditor.ModelSurfacePanel;
import ui.modeleditor.coreelements.ModelElement;
import ui.modeleditor.elements.ModelElementAnimationBar;
import ui.modeleditor.elements.ModelElementAnimationBarChart;
import ui.modeleditor.elements.ModelElementAnimationBarStack;
import ui.modeleditor.elements.ModelElementAnimationLineDiagram;
import ui.modeleditor.elements.ModelElementAnimationRecord;
import ui.modeleditor.elements.ModelElementSub;
import ui.tools.FlatLaFHelper;

/**
 * Liefert Beispielmodelle
 * @author Alexander Herzog
 */
public class EditModelExamples {
	/**
	 * Kategorien f�r die Beispiele
	 */
	public enum ExampleType {
		/**
		 * Standardbeispiele
		 */
		TYPE_DEFAULT,

		/**
		 * Beispiele, die sich auf reale Modelle bzw. Fragen beziehen
		 */
		TYPE_REAL_MODELS,

		/**
		 * Beispiele, die bestimmte Modellierungseigenschaften verdeutlichen
		 */
		TYPE_PROPERTIES,

		/**
		 * Beispiele zum Vergleich verschiedener Steuerungsstrategien
		 */
		TYPE_COMPARE,

		/**
		 * Beispiele, die mathematische Zusammenh�nge verdeutlichen
		 */
		TYPE_MATH
	}

	/**
	 * Schl�sselw�rter f�r die Beispiele
	 */
	public enum ExampleKeyWord {
		/**
		 * Batch-Ank�nfte oder -Bedienungen
		 */
		BATCH(()->Language.tr("Examples.KeyWords.Batch")),

		/**
		 * Priorit�ten (inkl. FIFO/LIFO)
		 */
		PRIORITIES(()->Language.tr("Examples.KeyWords.Priorities")),

		/**
		 * Routing der Kunden
		 */
		ROUTING(()->Language.tr("Examples.KeyWords.Routing")),

		/**
		 * Zeitpl�ne (sowohl f�r Ank�nfte als auch Schichtpl�ne)
		 */
		SCHEDULES(()->Language.tr("Examples.KeyWords.Schedules")),

		/**
		 * Push- und Pull-Strategien
		 */
		PUSH_PULL(()->Language.tr("Examples.KeyWords.PushPull")),

		/**
		 * Transporte
		 */
		TRANSPORT(()->Language.tr("Examples.KeyWords.Transport")),

		/**
		 * Darstellung mathematischer Zusammenh�nge
		 */
		MATH(()->Language.tr("Examples.KeyWords.Math"));

		/**
		 * Callback zum Abruf des Namens in der aktuellen Sprache
		 */
		private final Supplier<String> nameGetter;

		/**
		 * Konstruktor des Enum
		 * @param nameGetter	Callback zum Abruf des Namens in der aktuellen Sprache
		 */
		ExampleKeyWord(final Supplier<String> nameGetter) {
			this.nameGetter=nameGetter;
		}

		/**
		 * Liefert den Namen es Enum-Eintrags.
		 * @return	Name es Enum-Eintrags
		 */
		public String getName() {
			return nameGetter.get();
		}

		/**
		 * Liefert eine Liste mit allen Schl�sselw�rtern und dem Begriff "Alle" der Liste vorangestellt.
		 * @return	Liste mit allen Schl�sselw�rtern zzgl. "Alle"
		 */
		public static String[] getNames() {
			final List<String> list=Stream.of(values()).map(keyWord->keyWord.getName()).collect(Collectors.toList());
			list.add(0,Language.tr("Examples.KeyWords.All"));
			return list.toArray(new String[0]);
		}
	}

	/**
	 * Liste mit den Beispielen.
	 * @see #addExample(String[], String, ExampleType, ExampleKeyWord...)
	 */
	private final List<Example> list;

	/**
	 * Konstruktor der Klasse<br>
	 * Diese Klasse stellt nur statische Hilfsroutinen zur Verf�gung und kann nicht instanziert werden.
	 */
	private EditModelExamples() {
		list=new ArrayList<>();
		addExamples();
	}

	/**
	 * Liefert den Namen des Beispielmodells das f�r Benchmarks verwendet werden soll.
	 * @return	Benchmark-Beispielmodell
	 * @see CommandBenchmark
	 */
	public static String getBenchmarkExampleName() {
		return Language.tr("Examples.SystemDesign");
	}

	/**
	 * Liefert den Namen einer Gruppe
	 * @param group	Gruppe deren Name bestimmt werden soll
	 * @return	Name der angegebenen Gruppe
	 */
	public static String getGroupName(final ExampleType group) {
		switch (group) {
		case TYPE_DEFAULT: return Language.tr("Examples.Type.Simple");
		case TYPE_PROPERTIES: return Language.tr("Examples.Type.Properties");
		case TYPE_COMPARE: return Language.tr("Examples.Type.Compare");
		case TYPE_REAL_MODELS: return Language.tr("Examples.Type.Real");
		case TYPE_MATH: return Language.tr("Examples.Type.Mathematics");
		default: return Language.tr("Examples.Type.Unknown");
		}
	}

	/**
	 * Liefert die Namen der Beispiele in einer bestimmten Gruppe
	 * @param group	Gruppe f�r die die Beispiele aufgelistet werden sollen
	 * @return	Liste der Namen der Beispiele in der gew�hlten Gruppe
	 */
	public static List<String> getExampleNames(final ExampleType group) {
		return getExampleNames(group,null);
	}

	/**
	 * Liefert die Namen der Beispiele in einer bestimmten Gruppe
	 * @param group	Gruppe f�r die die Beispiele aufgelistet werden sollen
	 * @param keyWord	Zus�tzliches Schl�sselwort, welches ein Beispiel beinhalten muss, um zur�ckgeliefert zu werden (darf <code>null</code> sein, dann ist die Schl�sselwort-Filterung inaktiv)
	 * @return	Liste der Namen der Beispiele in der gew�hlten Gruppe
	 */
	public static List<String> getExampleNames(final ExampleType group, final ExampleKeyWord keyWord) {
		final EditModelExamples examples=new EditModelExamples();
		final List<String> result=new ArrayList<>();
		for (Example example: examples.list) if (example.type==group && (keyWord==null || example.keyWords.contains(keyWord))) result.add(example.names[0]);
		return result;
	}

	/**
	 * F�gt ein Beispiel zu der Liste der Beispiele hinzu
	 * @param names	Namen f�r das Beispiel in den verschiedenen Sprachen
	 * @param file	Beispieldateiname
	 * @param type	Gruppe in die das Beispiel f�llt
	 * @param keyWords	Optionale Schl�sselw�rter f�r das Beispiel
	 */
	private void addExample(final String[] names, final String file, final ExampleType type, final ExampleKeyWord... keyWords) {
		list.add(new Example(names,file,type,new HashSet<>(Arrays.asList(keyWords))));
	}

	/**
	 * F�gt alle Beispiele zu der Liste der Beispiele hinzu.
	 * @see #list
	 */
	private void addExamples() {
		/* Standardbeispiele */
		addExample(Language.trAll("Examples.ErlangC"),"ErlangC1.xml",ExampleType.TYPE_DEFAULT);

		/* Beispiele, die sich auf reale Modelle bzw. Fragen beziehen */
		addExample(Language.trAll("Examples.Callcenter"),"Callcenter.xml",ExampleType.TYPE_REAL_MODELS,ExampleKeyWord.ROUTING);
		addExample(Language.trAll("Examples.Restaurant"),"Restaurant.xml",ExampleType.TYPE_REAL_MODELS);
		addExample(Language.trAll("Examples.Baustellenampel"),"Baustellenampel.xml",ExampleType.TYPE_REAL_MODELS);
		addExample(Language.trAll("Examples.EmergencyDepartment"),"EmergencyDepartment.xml",ExampleType.TYPE_REAL_MODELS);
		addExample(Language.trAll("Examples.MultiStageProduction"),"MultiStageProduction.xml",ExampleType.TYPE_REAL_MODELS);

		/* Beispiele, die bestimmte Modellierungseigenschaften verdeutlichen */
		addExample(Language.trAll("Examples.ClientTypePriorities"),"Kundentypen.xml",ExampleType.TYPE_PROPERTIES,ExampleKeyWord.PRIORITIES);
		addExample(Language.trAll("Examples.ImpatientClientsAndRetry"),"Warteabbrecher.xml",ExampleType.TYPE_PROPERTIES,ExampleKeyWord.ROUTING);
		addExample(Language.trAll("Examples.SharedResources"),"SharedResources.xml",ExampleType.TYPE_PROPERTIES);
		addExample(Language.trAll("Examples.LimitedNumberOfClientsAtAStation"),"Variable.xml",ExampleType.TYPE_PROPERTIES,ExampleKeyWord.PUSH_PULL);
		addExample(Language.trAll("Examples.OperatorsAsSimulationObjects"),"BedienerAlsSimulationsobjekte.xml",ExampleType.TYPE_PROPERTIES);
		addExample(Language.trAll("Examples.Transport"),"Transport.xml",ExampleType.TYPE_PROPERTIES,ExampleKeyWord.TRANSPORT);
		addExample(Language.trAll("Examples.Transporter"),"Transporter.xml",ExampleType.TYPE_PROPERTIES,ExampleKeyWord.TRANSPORT);
		addExample(Language.trAll("Examples.CombiningOrdersAndItems"),"MultiSignalBarrier.xml",ExampleType.TYPE_PROPERTIES);
		addExample(Language.trAll("Examples.Batch"),"Batch.xml",ExampleType.TYPE_PROPERTIES,ExampleKeyWord.BATCH);
		addExample(Language.trAll("Examples.Failure"),"Failure.xml",ExampleType.TYPE_PROPERTIES);
		addExample(Language.trAll("Examples.SetUpTimes"),"SetUpTimes.xml",ExampleType.TYPE_PROPERTIES);
		addExample(Language.trAll("Examples.Rework"),"Rework.xml",ExampleType.TYPE_PROPERTIES,ExampleKeyWord.ROUTING);
		addExample(Language.trAll("Examples.HoldJS"),"HoldJS.xml",ExampleType.TYPE_PROPERTIES,ExampleKeyWord.PUSH_PULL);
		addExample(Language.trAll("Examples.RestrictedBuffer"),"RestriktierterPuffer.xml",ExampleType.TYPE_PROPERTIES,ExampleKeyWord.PUSH_PULL);
		addExample(Language.trAll("Examples.Analog"),"Analog.xml",ExampleType.TYPE_PROPERTIES);
		addExample(Language.trAll("Examples.Jockeying"),"Jockeying.xml",ExampleType.TYPE_PROPERTIES,ExampleKeyWord.ROUTING);
		addExample(Language.trAll("Examples.QueueingDiscipline"),"QueueingDiscipline.xml",ExampleType.TYPE_PROPERTIES);
		addExample(Language.trAll("Examples.Shiftplan"),"Shiftplan.xml",ExampleType.TYPE_PROPERTIES,ExampleKeyWord.SCHEDULES);
		addExample(Language.trAll("Examples.ArrivalAndServiceBatch"),"ArrivalAndServiceBatch.xml",ExampleType.TYPE_PROPERTIES);
		addExample(Language.trAll("Examples.BatchTransport"),"BatchTransport.xml",ExampleType.TYPE_PROPERTIES,ExampleKeyWord.BATCH,ExampleKeyWord.TRANSPORT);
		addExample(Language.trAll("Examples.IntervalInterArrivalTimes"),"IntervalInterArrivalTimes.xml",ExampleType.TYPE_PROPERTIES,ExampleKeyWord.SCHEDULES);
		addExample(Language.trAll("Examples.ClosedQueueingNetwork"),"ClosedQueueingNetwork.xml",ExampleType.TYPE_PROPERTIES);
		addExample(Language.trAll("Examples.WorkerWakeUp"),"WorkerWakeUp.xml",ExampleType.TYPE_PROPERTIES,ExampleKeyWord.SCHEDULES);

		/* Beispiele zum Vergleich verschiedener Steuerungsstrategien */
		addExample(Language.trAll("Examples.SystemDesign"),"Vergleiche2.xml",ExampleType.TYPE_COMPARE,ExampleKeyWord.ROUTING);
		addExample(Language.trAll("Examples.SystemDesignWithControl"),"Vergleiche3.xml",ExampleType.TYPE_COMPARE,ExampleKeyWord.ROUTING);
		addExample(Language.trAll("Examples.PushAndPullProduction"),"PushPull.xml",ExampleType.TYPE_COMPARE,ExampleKeyWord.PUSH_PULL);
		addExample(Language.trAll("Examples.PushAndPullProductionMultiBarriers"),"PushPullMulti.xml",ExampleType.TYPE_COMPARE,ExampleKeyWord.PUSH_PULL);
		addExample(Language.trAll("Examples.PushPullThroughput"),"PushPullThroughput.xml",ExampleType.TYPE_COMPARE,ExampleKeyWord.PUSH_PULL);
		addExample(Language.trAll("Examples.ChangeResourceCountCompare"),"ChangeResourceCountCompare.xml",ExampleType.TYPE_COMPARE);
		addExample(Language.trAll("Examples.DelayJS"),"DelayJS.xml",ExampleType.TYPE_COMPARE);
		addExample(Language.trAll("Examples.ParallelSerial"),"ParallelSerial.xml",ExampleType.TYPE_COMPARE);
		addExample(Language.trAll("Examples.FIFO-LIFO-Switch"),"FIFO-LIFO-Switch.xml",ExampleType.TYPE_COMPARE,ExampleKeyWord.PRIORITIES);
		addExample(Language.trAll("Examples.SetUpTimeReduction"),"SetUpTimeReduction.xml",ExampleType.TYPE_COMPARE,ExampleKeyWord.ROUTING);
		addExample(Language.trAll("Examples.DurchlaufzeitenVersusDurchsatz"),"DurchlaufzeitenVersusDurchsatz.xml",ExampleType.TYPE_COMPARE,ExampleKeyWord.PUSH_PULL);
		addExample(Language.trAll("Examples.EconomyOfScale"),"EconomyOfScale.xml",ExampleType.TYPE_COMPARE,ExampleKeyWord.PRIORITIES);
		addExample(Language.trAll("Examples.LocalVersusGlobalWarehouse"),"LocalVersusGlobalWarehouse.xml",ExampleType.TYPE_COMPARE,ExampleKeyWord.PUSH_PULL);

		/* Beispiele, die mathematische Zusammenh�nge verdeutlichen */
		addExample(Language.trAll("Examples.LawOfLargeNumbers"),"GesetzDerGro�enZahlen.xml",ExampleType.TYPE_MATH,ExampleKeyWord.MATH);
		addExample(Language.trAll("Examples.Galton"),"Galton.xml",ExampleType.TYPE_MATH,ExampleKeyWord.MATH);
		addExample(Language.trAll("Examples.CoefficientOfVariation"),"CoefficientOfVariation.xml",ExampleType.TYPE_MATH,ExampleKeyWord.MATH);
		addExample(Language.trAll("Examples.PASTA"),"PASTA.xml",ExampleType.TYPE_MATH,ExampleKeyWord.MATH);
		addExample(Language.trAll("Examples.ZentralerGrenzwertsatz"),"ZentralerGrenzwertsatz.xml",ExampleType.TYPE_MATH,ExampleKeyWord.MATH);
		addExample(Language.trAll("Examples.BusStoppParadoxon"),"BusStoppParadoxon.xml",ExampleType.TYPE_MATH,ExampleKeyWord.MATH);
		addExample(Language.trAll("Examples.RandomNumberGenerators"),"RandomNumberGenerators.xml",ExampleType.TYPE_MATH,ExampleKeyWord.MATH);
	}

	/**
	 * Liefert eine Liste mit allen verf�gbaren Beispielen.
	 * @return	Liste mit allen verf�gbaren Beispielen
	 */
	public static String[] getExamplesList() {
		final EditModelExamples examples=new EditModelExamples();
		return examples.list.stream().map(example->example.names[0]).toArray(String[]::new);
	}

	/**
	 * Liefert den Index des Beispiels basieren auf dem Namen
	 * @param name	Name des Beispiels zu dem der Index bestimmt werden soll
	 * @return	Index des Beispiels oder -1, wenn es kein Beispiel mit diesem Namen gibt
	 * @see #getExampleByIndex(Component, int)
	 */
	public static int getExampleIndexFromName(final String name) {
		if (name==null || name.isEmpty()) return -1;
		final EditModelExamples examples=new EditModelExamples();

		for (int i=0;i<examples.list.size();i++) {
			for (String test: examples.list.get(i).names) if (name.trim().equalsIgnoreCase(test)) return i;
		}
		return -1;
	}

	/**
	 * Liefert ein bestimmtes Beispiel �ber seine Nummer aus der Namesliste (<code>getExamplesList()</code>)
	 * @param owner	�bergeordnetes Elementes (zum Ausrichten von Fehlermeldungen). Wird hier <code>null</code> �bergeben, so werden Fehlermeldungen auf der Konsole ausgegeben
	 * @param index	Index des Beispiels, das zur�ckgeliefert werden soll
	 * @return	Beispiel oder <code>null</code>, wenn sich der Index au�erhalb des g�ltigen Bereichs befindet
	 * @see EditModelExamples#getExamplesList()
	 */
	public static EditModel getExampleByIndex(final Component owner, final int index) {
		final EditModelExamples examples=new EditModelExamples();
		if (index<0 || index>=examples.list.size()) return null;
		final String fileName=examples.list.get(index).file;

		final EditModel editModel=new EditModel();
		try (InputStream in=EditModelExamples.class.getResourceAsStream("examples_"+Language.tr("Numbers.Language")+"/"+fileName)) {
			final String error=editModel.loadFromStream(in);
			if (error!=null) {
				if (owner==null) {
					System.out.println(error);
				} else {
					MsgBox.error(owner,Language.tr("XML.LoadErrorTitle"),error);
				}
				return null;
			}
			processDiagramColors(editModel.surface);
			return editModel;
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Stellt einen Farbverlauf f�r die Zeichenfl�chendiagramme ein.
	 * @param surface	Zeichenfl�che auf der die Diagramme aktualisiert werden sollen
	 */
	private static void processDiagramColors(final ModelSurface surface) {
		for (ModelElement element: surface.getElements()) {
			if (element instanceof ModelElementSub) processDiagramColors(((ModelElementSub)element).getSubSurface());
			if (element instanceof ModelElementAnimationLineDiagram) processLineDiagramColors((ModelElementAnimationLineDiagram)element);
			if (element instanceof ModelElementAnimationBar) processBarColors((ModelElementAnimationBar)element);
			if (element instanceof ModelElementAnimationBarStack) processBarStackColors((ModelElementAnimationBarStack)element);
			if (element instanceof ModelElementAnimationBarChart) processBarChartColors((ModelElementAnimationBarChart)element);
			if (element instanceof ModelElementAnimationRecord) processRecordColors((ModelElementAnimationRecord)element);
		}
	}

	/**
	 * Bisherige, durch einen Farbverlauf zu ersetzende Diagrammhintergrundfarbe
	 * @see #processLineDiagramColors(ModelElementAnimationLineDiagram)
	 * @see #processBarColors(ModelElementAnimationBar)
	 * @see #processBarStackColors(ModelElementAnimationBarStack)
	 * @see #processBarChartColors(ModelElementAnimationBarChart)
	 * @see #processRecordColors(ModelElementAnimationRecord)
	 */
	private static final Color DEFAULT_DIAGRAM_BACKGROUND_COLOR=new Color(240,240,240);

	/**
	 * Stellt statt der Standardhintergrundfarbe einen Farbverlauf in einem Zeichenfl�chen-Liniendiagramm ein.
	 * @param element	Zeichenfl�chen-Liniendiagramm
	 */
	private static void processLineDiagramColors(final ModelElementAnimationLineDiagram element) {
		if (DEFAULT_DIAGRAM_BACKGROUND_COLOR.equals(element.getBackgroundColor()) && element.getGradientFillColor()==null) {
			element.setBackgroundColor(Color.WHITE);
			element.setGradientFillColor(new Color(230,230,250));
		}
	}

	/**
	 * Stellt statt der Standardhintergrundfarbe einen Farbverlauf in einem Zeichenfl�chen-Balken ein.
	 * @param element	Zeichenfl�chen-Balken
	 */
	private static void processBarColors(final ModelElementAnimationBar element) {
		if (DEFAULT_DIAGRAM_BACKGROUND_COLOR.equals(element.getBackgroundColor()) && element.getGradientFillColor()==null) {
			element.setBackgroundColor(Color.WHITE);
			element.setGradientFillColor(new Color(230,230,250));
		}
	}

	/**
	 * Stellt statt der Standardhintergrundfarbe einen Farbverlauf in einem Zeichenfl�chen gestapeltem Balken ein.
	 * @param element	Zeichenfl�chen gestapeltem Balken
	 */
	private static void processBarStackColors(final ModelElementAnimationBarStack element) {
		if (DEFAULT_DIAGRAM_BACKGROUND_COLOR.equals(element.getBackgroundColor()) && element.getGradientFillColor()==null) {
			element.setBackgroundColor(Color.WHITE);
			element.setGradientFillColor(new Color(230,230,250));
		}
	}

	/**
	 * Stellt statt der Standardhintergrundfarbe einen Farbverlauf in einem Zeichenfl�chen-Balkendiagramm ein.
	 * @param element	Zeichenfl�chen-Balkendiagramm
	 */
	private static void processBarChartColors(final ModelElementAnimationBarChart element) {
		if (DEFAULT_DIAGRAM_BACKGROUND_COLOR.equals(element.getBackgroundColor()) && element.getGradientFillColor()==null) {
			element.setBackgroundColor(Color.WHITE);
			element.setGradientFillColor(new Color(230,230,250));
		}
	}

	/**
	 * Stellt statt der Standardhintergrundfarbe einen Farbverlauf in einem Zeichenfl�chen-Wertaufzeichungsdiagramm ein.
	 * @param element	Zeichenfl�chen-Wertaufzeichungsdiagramm
	 */
	private static void processRecordColors(final ModelElementAnimationRecord element) {
		if (DEFAULT_DIAGRAM_BACKGROUND_COLOR.equals(element.getBackgroundColor()) && element.getGradientFillColor()==null) {
			element.setBackgroundColor(Color.WHITE);
			element.setGradientFillColor(new Color(230,230,250));
		}
	}

	/**
	 * Pr�ft, ob das �bergebene Modell mit einem der Beispielmodelle �bereinstimmt.<br>
	 * Es werden dabei alle Sprachen f�r den Vergleich herangezogen.
	 * @param editModel	Model, bei dem gepr�ft werden soll, ob es mit einem der Beispiele �bereinstimmt
	 * @return	Index des Beispielmodells oder -1, wenn das zu pr�fende Modell mit keinem der Beispielmodelle �bereinstimmt
	 */
	public static int equalsIndex(final EditModel editModel) {
		final EditModelExamples examples=new EditModelExamples();

		for (int i=0;i<examples.list.size();i++) for (String lang: Language.getLanguages()) {
			final EditModel testModel=new EditModel();
			try (InputStream in=EditModelExamples.class.getResourceAsStream("examples_"+lang+"/"+examples.list.get(i).file)) {
				testModel.loadFromStream(in);
				processDiagramColors(testModel.surface);
				if (testModel.equalsEditModel(editModel)) return i;
				EditModelDark.processModel(testModel,EditModelDark.ColorMode.LIGHT,EditModelDark.ColorMode.DARK);
				if (testModel.equalsEditModel(editModel)) return i;
			} catch (IOException e) {return -1;}
		}
		return -1;
	}

	/**
	 * F�gt eine Gruppe zu dem Men� hinzu.
	 * @param owner	�bergeordnetes Elementes (zum Ausrichten von Fehlermeldungen). Wird hier <code>null</code> �bergeben, so werden Fehlermeldungen auf der Konsole ausgegeben
	 * @param menu	Men�, in dem die Beispiele als Unterpunkte eingef�gt werden sollen
	 * @param listener	Listener, der mit einem Modell aufgerufen wird, wenn dieses geladen werden soll.
	 * @param group	Beispielgruppe
	 * @see #addToMenu(Component, JMenu, Consumer)
	 */
	private void addGroupToMenu(final Component owner, final JMenu menu, final Consumer<EditModel> listener, final ExampleType group) {
		final JMenuItem caption=new JMenuItem(getGroupName(group));
		menu.add(caption);
		Font font=caption.getFont();
		font=new Font(font.getName(),Font.BOLD,font.getSize());
		caption.setFont(font);
		caption.setEnabled(false);
		caption.setForeground(Color.BLACK);

		list.stream().filter(example->example.type==group).sorted((e1,e2)->String.CASE_INSENSITIVE_ORDER.compare(e1.names[0],e2.names[0])).forEach(example->{
			final JMenuItem item=new JMenuItem(example.names[0]);
			item.addActionListener(e->{
				final EditModel editModel=new EditModel();
				try (InputStream in=EditModelExamples.class.getResourceAsStream("examples_"+Language.tr("Numbers.Language")+"/"+example.file)) {
					final String error=editModel.loadFromStream(in);
					if (error!=null) {
						if (owner==null) {
							System.out.println(error);
						} else {
							MsgBox.error(owner,Language.tr("XML.LoadErrorTitle"),error);
						}
						return;
					}
					processDiagramColors(editModel.surface);
					if (FlatLaFHelper.isDark()) EditModelDark.processModel(editModel,EditModelDark.ColorMode.LIGHT,EditModelDark.ColorMode.DARK);
					if (listener!=null) listener.accept(editModel);
				} catch (IOException e1) {}
			});
			menu.add(item);
		});
	}

	/**
	 * F�gt eine Gruppe als Untermen� zu dem Men� hinzu.
	 * @param owner	�bergeordnetes Elementes (zum Ausrichten von Fehlermeldungen). Wird hier <code>null</code> �bergeben, so werden Fehlermeldungen auf der Konsole ausgegeben
	 * @param menu	Men�, in dem die Beispiele �ber ein Untermen� als Punkte eingef�gt werden sollen
	 * @param listener	Listener, der mit einem Modell aufgerufen wird, wenn dieses geladen werden soll.
	 * @param group	Beispielgruppe
	 * @see #addToMenu(Component, JMenu, Consumer)
	 */
	private void addGroupToSubMenu(final Component owner, final JMenu menu, final Consumer<EditModel> listener, final ExampleType group) {
		final JMenu sub=new JMenu(getGroupName(group));
		menu.add(sub);

		list.stream().filter(example->example.type==group).sorted((e1,e2)->String.CASE_INSENSITIVE_ORDER.compare(e1.names[0],e2.names[0])).forEach(example->{
			final JMenuItem item=new JMenuItem(example.names[0]);
			item.addActionListener(e->{
				final EditModel editModel=new EditModel();
				try (InputStream in=EditModelExamples.class.getResourceAsStream("examples_"+Language.tr("Numbers.Language")+"/"+example.file)) {
					final String error=editModel.loadFromStream(in);
					if (error!=null) {
						if (owner==null) {
							System.out.println(error);
						} else {
							MsgBox.error(owner,Language.tr("XML.LoadErrorTitle"),error);
						}
						return;
					}
					processDiagramColors(editModel.surface);
					if (FlatLaFHelper.isDark()) EditModelDark.processModel(editModel,EditModelDark.ColorMode.LIGHT,EditModelDark.ColorMode.DARK);
					if (listener!=null) listener.accept(editModel);
				} catch (IOException e1) {}
			});
			sub.add(item);
		});
	}

	/**
	 * F�gt alle Beispiele zu einem Men� hinzu
	 * @param owner	�bergeordnetes Elementes (zum Ausrichten von Fehlermeldungen). Wird hier <code>null</code> �bergeben, so werden Fehlermeldungen auf der Konsole ausgegeben
	 * @param menu	Men�, in dem die Beispiele als Unterpunkte eingef�gt werden sollen
	 * @param listener	Listener, der mit einem Modell aufgerufen wird, wenn dieses geladen werden soll.
	 */
	public static void addToMenu(final Component owner, final JMenu menu, final Consumer<EditModel> listener) {
		final EditModelExamples examples=new EditModelExamples();
		final Dimension screenSize=Toolkit.getDefaultToolkit().getScreenSize();

		boolean lastWasFullMenu=(menu.getItemCount()>0);
		for (ExampleType type: ExampleType.values()) {
			final long count=examples.list.stream().filter(example->example.type==type).count();
			boolean useSubMenu=true;
			if (count==1) useSubMenu=false;
			if (count<=5 && screenSize.height>1080) useSubMenu=false;
			if (count<=8 && screenSize.height>1200) useSubMenu=false;
			if (useSubMenu) {
				if (lastWasFullMenu) menu.addSeparator();
				examples.addGroupToSubMenu(owner,menu,listener,type);
			} else {
				menu.addSeparator();
				examples.addGroupToMenu(owner,menu,listener,type);
			}
			lastWasFullMenu=!useSubMenu;
		}
	}

	/**
	 * Pr�ft, dass sich die Modelle durch eine �nderung der Systemsprache inhaltlich nicht �ndern
	 * und gibt die Ergebnisse �ber <code>System.out</code> aus.
	 * @param names	Namen der Sprachen
	 * @param setLanguage	Runnables, um die Sprache umzustellen
	 */
	public static void runLanguageTest(final String[] names, final Runnable[] setLanguage) {
		String error;

		for (String exampleName: EditModelExamples.getExamplesList()) {
			System.out.println("Teste Beispiel \""+exampleName+"\"");

			setLanguage[0].run();

			final EditModel example=EditModelExamples.getExampleByIndex(null,EditModelExamples.getExampleIndexFromName(exampleName));
			if (example==null) continue;
			Document doc=example.saveToXMLDocument();

			if (!example.equalsEditModel(example.clone())) {
				System.out.println("  Modell ist nicht mit direkter Kopie von sich identisch.");
				continue;
			}

			EditModel model;

			boolean abort=false;
			for (int index=1;index<names.length;index++) {
				setLanguage[index].run();

				model=new EditModel();
				error=model.loadFromXML(doc.getDocumentElement());
				if (error!=null) {
					System.out.println("  Fehler beim Laden des "+names[index-1]+" Modells im "+names[index]+" Setup:");
					System.out.println("  "+error);
					abort=true;
					break;
				}

				if (!model.equalsEditModel(example)) {
					System.out.println("  "+names[index]+" Modell und Ausgangsmodell sind nicht mehr identisch.");
					abort=true;
					break;
				}

				doc=model.saveToXMLDocument();
			}
			if (abort) continue;

			setLanguage[0].run();

			final EditModel finalModel=new EditModel();
			error=finalModel.loadFromXML(doc.getDocumentElement());
			if (error!=null) {
				System.out.println("  Fehler beim Laden des "+names[names.length-1]+" Modells im "+names[0]+" Setup:");
				System.out.println("  "+error);
				continue;
			}

			if (!finalModel.equalsEditModel(example)) {
				System.out.println("  Konvertiertes Modell entspricht nicht mehr dem Ausgangsmodell.");
				continue;
			}
		}
	}

	/**
	 * Pr�ft, ob sich die Modelle durch eine �nderung der Systemsprache inhaltlich nicht �ndern
	 * und gibt die Ergebnisse �ber <code>System.out</code> aus.
	 */
	public static void runLanguageTestAll() {
		final SetupData setup=SetupData.getSetup();
		final String saveLanguage=setup.language;

		final String[] names=Language.getLanguages();
		final Runnable[] changers=Arrays.asList(names).stream().map(name->(Runnable)()->setup.setLanguage(name)).toArray(Runnable[]::new);
		runLanguageTest(names,changers);

		setup.setLanguage(saveLanguage);
		System.out.println("done.");
	}

	/**
	 * Liefert eine Liste aller Beispiele
	 * @return	Liste aller Beispiele
	 */
	public static List<Example> getList() {
		final EditModelExamples examples=new EditModelExamples();
		return examples.list;
	}

	/**
	 * Beispielmodell
	 * @author Alexander Herzog
	 */
	public static class Example {
		/**
		 * Namen f�r das Beispiel in den verschiedenen Sprachen
		 */
		public final String[] names;

		/**
		 * Beispieldateiname
		 */
		public final String file;

		/**
		 * Gruppe in die das Beispiel f�llt
		 */
		public final ExampleType type;

		/**
		 * Menge der optionalen Schl�sselw�rter f�r das Beispiel
		 */
		public final Set<ExampleKeyWord> keyWords;

		/**
		 * Konstruktor der Klasse
		 * @param names	Namen f�r das Beispiel in den verschiedenen Sprachen
		 * @param file	Beispieldateiname
		 * @param type	Gruppe in die das Beispiel f�llt
		 * @param keyWords	Menge der optionalen Schl�sselw�rter f�r das Beispiel
		 */
		private Example(final String[] names, final String file, final ExampleType type, final Set<ExampleKeyWord> keyWords) {
			this.names=names;
			this.file=file;
			this.type=type;
			this.keyWords=keyWords;
		}
	}

	/**
	 * Speichert Bilder f�r alle Beispielmodelle
	 * @param language	Sprache
	 * @param parentFolder	Ausgabeverzeichnis (die Dateinamen werden automatisch gew�hlt)
	 * @param out	Ausgabestream f�r Meldungen (darf nicht <code>null</code> sein)
	 * @return	Gibt an, ob die Bilder erfolgreich erstellt werden konnten
	 */

	public static boolean saveImages(final String language, final File parentFolder, final PrintStream out) {
		final File folder=new File(parentFolder,language);
		if (!folder.isDirectory()) {
			if (!folder.mkdirs()) {
				out.println("error mkdir "+folder.toString());
				return false;
			}
		}

		final SetupData setup=SetupData.getSetup();
		final String saveLanguage=setup.language;
		setup.setLanguage(language);

		try {
			final StringBuilder info=new StringBuilder();
			for (Example example: getList()) {
				final String name=example.names[0];
				final String file="ExampleModel_"+example.file.replace(".xml",".png");

				if (out!=null) out.println("writing \""+name+"\"");

				final EditModel editModel=getExampleByIndex(null,getExampleIndexFromName(name));
				final ModelSurfacePanel surfacePanel=new ModelSurfacePanel();
				surfacePanel.setSurface(editModel,editModel.surface,editModel.clientData,editModel.sequences);
				final String error=EditorPanel.exportModelToFile(editModel,null,surfacePanel,new File(folder,file),null,true);
				if (error!=null && out!=null) out.println(error);

				info.append("## "+name+"\n");
				info.append("!["+example.names[0]+"](Images/"+file+")\n\n");
			}
			Table.saveTextToFile(info.toString(),new File(folder,"info.md"));
		} finally {
			setup.setLanguage(saveLanguage);
		}

		SwingUtilities.invokeLater(()->{
			for (Frame frame: Frame.getFrames()) if (frame instanceof JFrame) {
				((JFrame)frame).setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
				frame.dispose();
			}
		});

		return true;
	}
}