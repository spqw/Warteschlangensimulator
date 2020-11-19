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
package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileSystemView;

import org.apache.commons.math3.util.FastMath;

import language.Language;
import mathtools.NumberTools;
import mathtools.TimeTools;
import mathtools.distribution.swing.CommonVariables;
import mathtools.distribution.tools.DistributionTools;
import simcore.logging.CallbackLoggerData;
import simulator.Simulator;
import simulator.editmodel.EditModel;
import simulator.elements.RunModelAnimationViewer;
import simulator.logging.CallbackLoggerWithJS;
import simulator.runmodel.RunDataClient;
import simulator.runmodel.RunDataTransporter;
import simulator.runmodel.SimulationData;
import systemtools.MsgBox;
import systemtools.SetupBase;
import tools.ButtonRotator;
import tools.SetupData;
import tools.UsageStatistics;
import ui.dialogs.AnimationJSInfoDialog;
import ui.dialogs.ExpressionCalculatorDialog;
import ui.dialogs.NextEventsViewerDialog;
import ui.images.Images;
import ui.mjpeg.AnimationRecordWaitDialog;
import ui.mjpeg.MJPEGSystem;
import ui.mjpeg.VideoSystem;
import ui.modeleditor.ModelSurface;
import ui.modeleditor.ModelSurfaceAnimator;
import ui.modeleditor.ModelSurfaceAnimatorBase;
import ui.modeleditor.ModelSurfacePanel;
import ui.modeleditor.coreelements.ModelElement;
import ui.modeleditor.elements.ElementWithAnimationDisplay;
import ui.modeleditor.elements.ModelElementAnalogValue;
import ui.modeleditor.elements.ModelElementAnimationConnect;
import ui.modeleditor.elements.ModelElementConveyor;
import ui.modeleditor.elements.ModelElementSource;
import ui.modeleditor.elements.ModelElementSourceMulti;
import ui.modeleditor.elements.ModelElementSourceRecord;
import ui.modeleditor.elements.ModelElementSub;
import ui.modeleditor.elements.ModelElementTank;
import ui.modelproperties.ModelPropertiesDialog;

/**
 * Diese Klasse zeigt die Animation der Simulation in einem eingebetteten
 * <code>ModelSurfacePanel</code> an.
 * @author Alexander Herzog
 */
public class AnimationPanel extends JPanel implements RunModelAnimationViewer {
	/**
	 * Serialisierungs-ID der Klasse
	 * @see Serializable
	 */
	private static final long serialVersionUID = -4834682399637727518L;

	/**
	 * �bergeordnetes Fenster in dem sich das Panel befindet (zur Minimiert-Erkennung, um in diesem Fall bei Fernsteuerung dennoch Animationen auszul�sen)
	 */
	private final JFrame window;

	/**
	 * Referenz auf das Setup-Singleton.
	 */
	private final transient SetupData setup;

	/** Statuszeilen-Text zur Anzahl der aufgezeichneten Bilder */
	private final String infoMJPEG;
	/** Statuszeilen-Text mit Informationen zur Simulation (bei unbegrenzter Zeit) */
	private final String infoNoSum;
	/** Statuszeilen-Text mit Informationen zur Simulation (bei begrenzter Simulationszeit) */
	private final String infoSum;

	/** Gibt an, ob die Animation direkt nach der Initialisierung starten soll oder ob sich das System anf�nglich im Pausemodus befinden soll */
	private boolean startPaused;
	/** Ist <code>true</code>, wenn die Warm-up-Phase bei der Animation zun�chst als Simulation vorab ausgef�hrt werden soll */
	private boolean fastWarmUp;
	/** Besitzt das Modell Analogwert-Elemente? */
	private boolean hasAnalogElements;
	/** Besitzt das Modell Flie�b�nder? */
	private boolean hasConveyorElements;
	/** Editor-Modell */
	private transient EditModel model;
	/** Zeichenfl�che */
	private ModelSurfacePanel surfacePanel;
	/** Animationssystem */
	private transient ModelSurfaceAnimator surfaceAnimator;
	/** Sichert parallele Zugriffe auf {@link #simulator} ab. */
	private Semaphore simulatorLock=new Semaphore(1);
	/** Simulator f�r das Modell */
	private transient Simulator simulator;
	/** Vorgelagerter, im Konstruktor �bergebener optionaler Logger */
	private transient CallbackLoggerWithJS parentLogger;
	/** Logger, �ber den die Einzelschritt ausgaben angezeigt werden */
	private transient CallbackLoggerWithJS logger;
	/** L�uft die Animation momentan? */
	private boolean running;
	/** Runnable, das aufgerufen wird, wenn die Simulation beendet wurde */
	private Runnable animationDone;
	/** Runnable, das aufgerufen wird, wenn die Simulation ohne Animation zu Ende gef�hrt werden soll */
	private Runnable sendToSimulation;
	/** System zur Erzeugung einer MJPEG-Videodatei aus einzelnen Animationsbildern */
	private transient VideoSystem encoder;

	/** Ruft regelm��ig {@link #abortRunTest()} auf */
	private transient Timer timer;
	/** Wurde die Simulation erfolgreich beendet? ({@link #isSimulationSuccessful()}) */
	private boolean simulationSuccessful;
	/** Soll die Simulation abgebrochen werden? */
	private boolean abortRun;
	/** Soll die Simulation ohne Animationsausgabe zu Ende gef�hrt werden? ({@link #finishAsSimulation()}) */
	private boolean continueAsSimulation;
	/** Simulationsdatenobjekt */
	private transient SimulationData simData;
	/** Stellt sicher, dass {@link #simulator} nicht auf <code>null</code> gesetzt wird, w�hrend {@link #updateViewer(SimulationData)} l�uft */
	private Semaphore mutex;
	/** Zeitpunkt (bezogen auf die Simulationszeit) des letzten Aufrufs von {@link #delaySystem(SimulationData, int)} */
	private long lastTimeStep;
	/** Zeit f�r einen Zeitschritt ({@link #calculateMinimalTimeStep()}) */
	private double delaySystem;
	/** Tats�chliche Verz�gerung pro Animationsschritt (0..100); ist im Einzelschrittmodus 100, w�hrend {@link #delay} unver�ndert bleibt */
	private int delayInt;
	/** Verz�gerung pro Animationsschritt (0..100) */
	private int delay;
	/** Wird von {@link #animationDelayChanged()} auf <code>true</code> gesetzt und dann von {@link #delaySystem(SimulationData, int)} ausgewertet, wenn sich die Animationsgeschwindigkeit ge�ndert hat. */
	private boolean speedChanged;

	/** Listener f�r Klicks auf die verschiedenen Symbolleisten-Schaltfl�chen */
	private final transient ToolBarListener toolbarListener;

	/* Symbolleiste oben */

	/** Schaltfl�che "Beenden" */
	private final JButton buttonAbort;
	/** Schaltfl�che "Bild speichern" */
	private final JButton buttonScreenshot;
	/** Schaltfl�che "Simulation" (Animation als Simulation zu Ende f�hren) */
	private final JButton buttonSimulation;
	/** Schaltfl�che "Einstellungen" (zum Ausl�sen eines Popupmen�s) */
	private final JButton buttonTools;
	/** Schaltfl�che "Start"/"Pause" */
	private final JButton buttonPlayPause;
	/** Schaltfl�che "Einzelschritt" */
	private final JButton buttonStep;
	/** Schaltfl�che "Geschwindigkeit" (zum Ausl�sen eines Popupmen�s) */
	private final JButton buttonSpeed;

	/** Popupmen�punkt "Animationsstart" - "Animation sofort starten" */
	private JMenuItem menuStartModeRun;
	/** Popupmen�punkt "Animationsstart" - "Im Pause-Modus starten" */
	private JMenuItem menuStartModePause;
	/** Popupmen�punkt "Analoge Werte in Animation" - "Schnelle Animation" */
	private JMenuItem menuAnalogValuesFast;
	/** Popupmen�punkt "Analoge Werte in Animation" - "�nderungen exakt anzeigen (langsam)" */
	private JMenuItem menuAnalogValuesExact;
	/** Popupmen�punkt "Verzeichnis zum Speichern von Bildern" - "Im Nutzerverzeichnis" */
	private JMenuItem menuScreenshotModeHome;
	/** Popupmen�punkt "Verzeichnis zum Speichern von Bildern" - "Im ausgew�hlten Verzeichnis" */
	private JMenuItem menuScreenshotModeCustom;
	/** Popupmen�punkt "Logging-Daten im Einzelschrittmodus anzeigen" */
	private JCheckBoxMenuItem menuShowLog;

	/* Vertikale Symbolleiste */

	/** Schaltfl�che "Modell" */
	private final JButton buttonProperties;

	/* Statusleiste */

	/** Statusinformationen */
	private final JLabel statusBar;
	/** Fortschritt der Animation */
	private final JProgressBar progressBar;
	/** Aktueller Zoomfaktor */
	private JLabel labelZoom;
	/** Schaltfl�che f�r Zoomfaktor verringern */
	private JButton buttonZoomOut;
	/** Schaltfl�che f�r Zoomfaktor vergr��ern */
	private JButton buttonZoomIn;
	/** Schaltfl�che f�r Standard-Zoomfaktor */
	private JButton buttonZoomDefault;
	/** Schaltfl�che "Modell zentrieren" */
	private JButton buttonFindModel;

	/* Logging-Bereich unten */

	/** Logging-Ausgabe-Bereich */
	private final JPanel logArea;
	/** Textfeld zur Ausgabe der Logging-Ausgaben */
	private final JLabel logLabel;
	/** Aktueller Zeitwert auf den sich die Logging-Ausgaben beziehen */
	private long logTimeStamp;
	/** Wiederverwendbarer {@link StringBuilder} f�r die Logging-Ausgaben */
	private StringBuilder logText;
	/** Wiederverwendbarer {@link StringBuilder} f�r die Logging-Ausgaben ohne Formatierung */
	private StringBuilder logTextPlain;
	/** Welcher der verf�gbaren Logging-Ausgaben wird gerade angezeigt? (aktuell oder vorherige) */
	private int logTextDisplayIndex;
	/** Liste der vorherigen Logging-Ausgaben */
	private List<String> logTextHistory;
	/** Liste der vorherigen Logging-Ausgaben ohne Formatierung */
	private List<String> logTextHistoryPlain;
	/** Schaltfl�che "Vorherige Logging-Ausgabe" */
	private final JButton logPrevious;
	/** Schaltfl�che "N�chste Logging-Ausgabe" */
	private final JButton logNext;
	/** Schaltfl�che "Aktuelle Logging-Ausgabe" */
	private final JButton logCurrent;
	/** Schaltfl�che "Logging-Ausgabe kopieren" */
	private final JButton logCopy;
	/** Schaltfl�che "Ausdruck berechnen" */
	private final JButton logExpression;
	/** Schaltfl�che "Ergebnisse der Javascript-Skriptausf�hrung" */
	private final JButton logJS;
	/** Schaltfl�che "N�chste geplante Ereignisse" */
	private final JButton logEvents;

	/**
	 * Unter-Animator-Element ein, welches ebenfalls bei Animationsschritten benachrichtigt werden soll<br>
	 * (Kann <code>null</code> sein, wenn ein Untermodell-Fenster offen ist)
	 * @see #setSubViewer(RunModelAnimationViewer)
	 */
	private RunModelAnimationViewer subViewer;

	/**
	 * Konstruktor der Klasse
	 * @param window	�bergeordnetes Fenster in dem sich das Panel befindet (zur Minimiert-Erkennung, um in diesem Fall bei Fernsteuerung dennoch Animationen auszul�sen)
	 */
	public AnimationPanel(final JFrame window) {
		super();
		this.window=window;
		setup=SetupData.getSetup();

		infoMJPEG=", "+Language.tr("Animation.ImagesRecorded");
		infoNoSum=Language.tr("Animation.SimulatedTime.Unlimited");
		infoSum=Language.tr("Animation.SimulatedTime.Limited");

		model=null;
		mutex=new Semaphore(1);
		toolbarListener=new ToolBarListener();

		setLayout(new BorderLayout());

		/* Toolbar oben */
		final JToolBar toolBar=new JToolBar();
		add(toolBar,BorderLayout.NORTH);
		toolBar.setFloatable(false);

		buttonAbort=createToolbarButton(toolBar,Language.tr("Animation.Toolbar.Stop"),Language.tr("Animation.Toolbar.Stop.Info")+" ("+keyStrokeToString(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0))+")",Images.GENERAL_CANCEL.getIcon());
		toolBar.addSeparator();

		buttonScreenshot=createToolbarButton(toolBar,Language.tr("Animation.Toolbar.Image"),Language.tr("Animation.Toolbar.Image.Info"),Images.ANIMATION_SCREENSHOT.getIcon());
		updateScreenshotButtonHint();
		buttonSimulation=createToolbarButton(toolBar,Language.tr("Animation.Toolbar.Simulation"),Language.tr("Animation.Toolbar.Simulation.Info")+" ("+keyStrokeToString(KeyStroke.getKeyStroke(KeyEvent.VK_F5,0))+")",Images.SIMULATION.getIcon());
		buttonSimulation.setVisible(false);
		buttonTools=createToolbarButton(toolBar,Language.tr("Animation.Toolbar.Tools"),Language.tr("Animation.Toolbar.Tools.Info"),Images.GENERAL_TOOLS.getIcon());
		toolBar.addSeparator();

		buttonPlayPause=createToolbarButton(toolBar,Language.tr("Animation.Toolbar.Pause"),Language.tr("Animation.Toolbar.Pause.Info")+" ("+keyStrokeToString(KeyStroke.getKeyStroke(KeyEvent.VK_F6,0))+")",Images.ANIMATION_PAUSE.getIcon());
		buttonStep=createToolbarButton(toolBar,Language.tr("Animation.Toolbar.Step"),Language.tr("Animation.Toolbar.Step.Info")+" ("+keyStrokeToString(KeyStroke.getKeyStroke(KeyEvent.VK_F7,0))+")",Images.ANIMATION_STEP.getIcon());
		buttonStep.setEnabled(false);
		buttonSpeed=createToolbarButton(toolBar,Language.tr("Animation.Toolbar.Speed"),Language.tr("Animation.Toolbar.Speed.Info"),Images.ANIMATION_SPEED.getIcon());

		addUserButtons(toolBar);

		/* Area mit linkem Toolbar und Surface */
		final JPanel content=new JPanel(new BorderLayout());
		add(content,BorderLayout.CENTER);

		/* Toolbar links */
		final JToolBar leftToolBar=new JToolBar(SwingConstants.VERTICAL);
		leftToolBar.setFloatable(false);
		add(leftToolBar,BorderLayout.WEST);

		buttonProperties=createRotatedToolbarButton(leftToolBar,Language.tr("Editor.ModelProperties.Short"),Language.tr("Editor.ModelProperties.Info"),Images.MODEL.getIcon());

		/* Surface in der Mitte */
		content.add(new RulerPanel(surfacePanel=new ModelSurfacePanel(true,false),SetupData.getSetup().showRulers),BorderLayout.CENTER);
		surfacePanel.addZoomChangeListener(e->zoomChanged());

		/* Statusbar unten */
		final JPanel statusPanel=new JPanel(new BorderLayout());
		add(statusPanel,BorderLayout.SOUTH);

		final JPanel statusBarOuter=new JPanel(new BorderLayout());
		statusPanel.add(statusBarOuter,BorderLayout.CENTER);
		statusBarOuter.add(statusBar=new JLabel(""),BorderLayout.WEST);
		statusBar.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
		JPanel zoomArea=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));

		final JPanel progressBarOuter=new JPanel(new BorderLayout());
		statusBarOuter.add(progressBarOuter,BorderLayout.CENTER);
		progressBarOuter.setBorder(BorderFactory.createEmptyBorder(5,10,5,20));
		progressBarOuter.add(progressBar=new JProgressBar(),BorderLayout.CENTER);

		progressBar.setValue(0);

		statusPanel.add(zoomArea,BorderLayout.EAST);
		zoomArea.add(labelZoom=new JLabel("100% "));
		labelZoom.addMouseListener(new MouseAdapter() {
			@Override public void mousePressed(MouseEvent e) {showZoomContextMenu(labelZoom);}
		});
		labelZoom.setToolTipText(Language.tr("Editor.SetupZoom"));
		zoomArea.add(buttonZoomOut=createToolbarButton(null,"",Language.tr("Main.Menu.View.ZoomOut")+" ("+keyStrokeToString(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT,InputEvent.CTRL_DOWN_MASK))+")",Images.ZOOM_OUT.getIcon()));
		buttonZoomOut.setPreferredSize(new Dimension(20,20));
		buttonZoomOut.setBorderPainted(false);
		buttonZoomOut.setFocusPainted(false);
		buttonZoomOut.setContentAreaFilled(false);
		zoomArea.add(buttonZoomIn=createToolbarButton(null,"",Language.tr("Main.Menu.View.ZoomIn")+" ("+keyStrokeToString(KeyStroke.getKeyStroke(KeyEvent.VK_ADD,InputEvent.CTRL_DOWN_MASK))+")",Images.ZOOM_IN.getIcon()));
		buttonZoomIn.setPreferredSize(new Dimension(20,20));
		buttonZoomIn.setBorderPainted(false);
		buttonZoomIn.setFocusPainted(false);
		buttonZoomIn.setContentAreaFilled(false);
		zoomArea.add(buttonZoomDefault=createToolbarButton(null,"",Language.tr("Main.Menu.View.ZoomDefault")+" ("+keyStrokeToString(KeyStroke.getKeyStroke(KeyEvent.VK_MULTIPLY,InputEvent.CTRL_DOWN_MASK))+")",Images.ZOOM.getIcon()));
		buttonZoomDefault.setPreferredSize(new Dimension(20,20));
		buttonZoomDefault.setBorderPainted(false);
		buttonZoomDefault.setFocusPainted(false);
		buttonZoomDefault.setContentAreaFilled(false);
		zoomArea.add(buttonFindModel=createToolbarButton(null,"",Language.tr("Main.Menu.View.CenterModel")+" ("+keyStrokeToString(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0,InputEvent.CTRL_DOWN_MASK))+")",Images.ZOOM_CENTER_MODEL.getIcon()));
		buttonFindModel.setPreferredSize(new Dimension(20,20));
		buttonFindModel.setBorderPainted(false);
		buttonFindModel.setFocusPainted(false);
		buttonFindModel.setContentAreaFilled(false);

		statusPanel.add(logArea=new JPanel(new BorderLayout()),BorderLayout.SOUTH);
		logArea.setVisible(false);
		logArea.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		logArea.setBackground(Color.WHITE);
		logArea.add(logLabel=new JLabel(),BorderLayout.CENTER);
		logLabel.setBorder(BorderFactory.createEmptyBorder(2,5,2,5));
		final JToolBar logToolBar=new JToolBar(SwingConstants.VERTICAL);
		logArea.add(logToolBar,BorderLayout.EAST);
		logToolBar.setFloatable(false);
		logToolBar.add(Box.createVerticalGlue());
		logPrevious=createToolbarButton(logToolBar,"",Language.tr("Animation.Log.Previous"),Images.ARROW_UP.getIcon());
		logNext=createToolbarButton(logToolBar,"",Language.tr("Animation.Log.Next"),Images.ARROW_DOWN.getIcon());
		logCurrent=createToolbarButton(logToolBar,"",Language.tr("Animation.Log.Current"),Images.ARROW_DOWN_END.getIcon());
		logCopy=createToolbarButton(logToolBar,"",Language.tr("Animation.Log.Copy"),Images.EDIT_COPY.getIcon());
		logExpression=createToolbarButton(logToolBar,"",Language.tr("Animation.Log.Expression"),Images.ANIMATION_EVALUATE_EXPRESSION.getIcon());
		logJS=createToolbarButton(logToolBar,"",Language.tr("Animation.Log.JS"),Images.ANIMATION_EVALUATE_SCRIPT.getIcon());
		logEvents=createToolbarButton(logToolBar,"",Language.tr("Animation.Log.Events"),Images.ANIMATION_LIST_NEXT_EVENTS.getIcon());

		delay=setup.animationDelay*10;
		animationDelayChanged();

		final InputMap input=getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		input.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),"keyEscape");
		input.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5,0),"keyF5");
		input.put(KeyStroke.getKeyStroke(KeyEvent.VK_F6,0),"keyF6");
		input.put(KeyStroke.getKeyStroke(KeyEvent.VK_F7,0),"keyF7");
		addAction("keyEscape",e->{
			if (buttonAbort.isEnabled()) {
				closeRequest(); buttonAbort.setEnabled(false);
			}
		});
		addAction("keyF5",e->finishAsSimulation());
		addAction("keyF6",e->playPause());
		addAction("keyF7",e->step(false));
	}

	/**
	 * Generiert basierend auf einem Hotkey die Textbeschreibung f�r den Hotkey (z.B. zur Anzeige in Symbolleisten-Schaltfl�chen Tooltips)
	 * @param key	Hotkey
	 * @return	Textbeschreibung f�r den Hotkey
	 */
	private String keyStrokeToString(final KeyStroke key) {
		final int modifiers=key.getModifiers();
		final StringBuilder text=new StringBuilder();
		if (modifiers>0) {
			text.append(InputEvent.getModifiersExText(modifiers));
			text.append('+');
		}
		text.append(KeyEvent.getKeyText(key.getKeyCode()));
		return text.toString();
	}

	/**
	 * Legt eine Aktion an und f�gt diese in die {@link ActionMap} des Panels ein.
	 * @param name	Name der Aktion
	 * @param action	Auszuf�hrendes Callback beim Aufruf der Aktion
	 */
	private void addAction(final String name, final Consumer<ActionEvent> action) {
		getActionMap().put(name,new AbstractAction() {
			private static final long serialVersionUID=-6092283861324716876L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				if (action!=null) action.accept(e);
			}
		});
	}

	/**
	 * Erm�glicht das Hinzuf�gen weiterer Schaltfl�chen zur Symbolleiste in abgeleiteten Klassen
	 * @param toolbar	Toolbar des Panels
	 */
	protected void addUserButtons(final JToolBar toolbar) {}

	/**
	 * Erzeugt eine Schaltfl�che mit um 90� gegen den Uhrzeigersinn rotierter Beschriftung.
	 * @param toolbar	Symbolleiste in die die neue Schaltfl�che eingef�gt werden soll (kann <code>null</code> sein, dann wird die Schaltfl�che in keine Symbolleiste eingef�gt)
	 * @param title	Beschriftung der Schaltfl�che (darf nicht leer sein)
	 * @param hint	Tooltip f�r die Schaltfl�che (kann <code>null</code> sein)
	 * @param icon	Icon f�r die Schaltfl�che (kann <code>null</code> sein)
	 * @return	Neue Schaltfl�che
	 */
	private JButton createRotatedToolbarButton(final JToolBar toolbar, final String title, final String hint, final Icon icon) {
		ImageIcon rotatedIcon=null;

		if (icon instanceof ImageIcon) {
			final double scale=SetupData.getSetup().scaleGUI;
			if (scale!=1.0) {
				final int w=(int)Math.round(icon.getIconWidth()*scale);
				final int h=(int)Math.round(icon.getIconHeight()*scale);
				final Image temp=((ImageIcon)icon).getImage().getScaledInstance(w,h,Image.SCALE_SMOOTH);
				rotatedIcon=new ImageIcon(temp,"");
			} else {
				rotatedIcon=(ImageIcon)icon;
			}
		}

		final JButton button=new ButtonRotator.RotatedButton(title,rotatedIcon);

		if (toolbar!=null) toolbar.add(button);
		if (hint!=null) button.setToolTipText(hint);
		button.addActionListener(toolbarListener);

		return button;
	}

	/**
	 * Legt einen neuen Symbolleisten-Eintrag an
	 * @param toolbar	�bergeordnetes Symbolleisten-Element
	 * @param title	Name des neuen Symbolleisten-Eintrags
	 * @param hint	Zus�tzlich anzuzeigender Tooltip f�r den Symbolleisten-Eintrag (kann <code>null</code> sein, wenn kein Tooltip angezeigt werden soll)
	 * @param icon	Pfad zu dem Icon, das in dem Symbolleisten-Eintrag angezeigt werden soll (kann <code>null</code> sein, wenn kein Icon angezeigt werden soll)
	 * @return	Neu erstellter Symbolleisten-Eintrag
	 */
	protected final JButton createToolbarButton(final JToolBar toolbar, final String title, final String hint, final Icon icon) {
		JButton button=new JButton(title);
		if (toolbar!=null) toolbar.add(button);
		if (hint!=null) button.setToolTipText(hint);
		button.addActionListener(toolbarListener);
		if (icon!=null) button.setIcon(icon);
		return button;
	}

	/**
	 * Stellt die linke obere Ecke des sichtbaren Bereichs von {@link #surfacePanel} ein.
	 * @param position	Position der linken oberen Ecke
	 */
	private void setSurfacePosition(final Point position) {
		if (surfacePanel.getParent() instanceof JViewport) {
			JViewport viewport=(JViewport)surfacePanel.getParent();
			viewport.setViewPosition(position);
		}
	}

	/**
	 * Minimaler Zeitschritt aus Sicht einer Kundenquelle
	 * @param record	Kundenquelle
	 * @param oldMin	Bisheriger minimaler Zeitschritt
	 * @return	Neuer minimaler Zeitschritt
	 * @see #calculateMinimalTimeStep()
	 */
	private double calculateMinimalTimeStepFromRecord(final ModelElementSourceRecord record, final double oldMin) {
		final double mean=DistributionTools.getMean(record.getInterarrivalTimeDistribution());
		final long multiply=record.getTimeBase().multiply;
		if (mean>0 && mean*multiply<oldMin) return mean*multiply;
		return oldMin;
	}

	/**
	 * Berechnet einen minimalen Zeitschritt f�r die Animation.
	 * @see #delaySystem
	 */
	private void calculateMinimalTimeStep() {
		double min=86400;

		for (ModelElement element: model.surface.getElements()) {
			if (element instanceof ModelElementSource) {
				final ModelElementSourceRecord record=((ModelElementSource)element).getRecord();
				min=calculateMinimalTimeStepFromRecord(record,min);
			}
			if (element instanceof ModelElementSourceMulti) for (ModelElementSourceRecord record: ((ModelElementSourceMulti)element).getRecords()) {
				min=calculateMinimalTimeStepFromRecord(record,min);
			}
		}

		delaySystem=min/500;
	}

	/**
	 * Stellt das zu simulierende Modell ein. Der Simulator wird durch diese Methode gestartet, darf
	 * also nicht bereits vorher gestartet worden sein. Vor der Erstellung des Simulator-Objekts muss
	 * au�erdem zun�chst die <code>makeAnimationModel</code>-Methode auf das Editor-Modell angewandt werden.
	 * @param model	Editor-Modell (f�r die Animation des Simulationsverlaufs in einem {@link ModelSurfacePanel}-Objekt)
	 * @param simulator	Simulator f�r das Modell (darf noch nicht gestartet worden sein)
	 * @param logger	Logger, �ber den die Einzelschritt ausgaben angezeigt werden
	 * @param recordFile	Videodatei, in der die Animation aufgezeichnet werden soll
	 * @param paintTimeStamp	F�gt bei der Aufzeichnung in das Video den jeweils aktuellen Simulationszeit-Wert ein
	 * @param fastWarmUp	Ist <code>true</code>, wenn die Warm-up-Phase bei der Animation zun�chst als Simulation vorab ausgef�hrt werden soll
	 * @param zoom	Zoomfaktor f�r das Animations-Surface
	 * @param raster	Rasteranzeige auf dem Animations-Surface
	 * @param position	Position der linken oberen Ecke des Animations-Surface
	 * @param animationDone	Runnable, das aufgerufen wird, wenn die Simulation beendet wurde
	 * @param sendToSimulation	Runnable, das aufgerufen wird, wenn die Simulation ohne Animation zu Ende gef�hrt werden soll
	 * @param startPaused	Gibt an, ob die Animation direkt nach der Initialisierung starten soll oder ob sich das System anf�nglich im Pausemodus befinden soll
	 * @param startFullRecording	Wird die Animation im Pausemodus gestartet, so wird direkt der erste Schritt ausgef�hrt. �ber diese Funktion kann angegeben werden, dass dieser Schritt im vollst�ndigen Erfassungsmodus durchgef�hrt werden soll.
	 * @see #makeAnimationModel(EditModel)
	 */
	public void setSimulator(final EditModel model, final Simulator simulator, final CallbackLoggerWithJS logger, final File recordFile, final boolean paintTimeStamp, final boolean fastWarmUp, final double zoom, final ModelSurface.Grid raster, final Point position, final Runnable animationDone, final Runnable sendToSimulation, final boolean startPaused, final boolean startFullRecording) {
		this.model=model;
		this.startPaused=startPaused;
		this.fastWarmUp=fastWarmUp;

		if (!running) {
			simulatorLock.acquireUninterruptibly();
			try {
				this.simulator=null;
			} finally {
				simulatorLock.release();
			}
			playPause();
		}

		hasAnalogElements=hasAnalogElements(model);
		hasConveyorElements=hasConveyorElements(model);
		surfacePanel.setSurface(model,model.surface,model.clientData,model.sequences);
		surfaceAnimator=new ModelSurfaceAnimator(window,surfacePanel,model.animationImages,ModelSurfaceAnimator.AnimationMoveMode.MODE_MULTI,setup.useMultiCoreAnimation,setup.animateResources);

		if (recordFile==null) {
			encoder=null;
		} else {
			/* encoder=new VP8System(recordFile,surfaceAnimator.useAdditionalFrames()); */
			encoder=new MJPEGSystem(recordFile,surfaceAnimator.useAdditionalFrames());
			if (!encoder.isReady()) encoder=null;
		}
		surfaceAnimator.setRecordSystem(encoder,paintTimeStamp);

		surfacePanel.getSurface().setAnimatorPanel(this);
		surfacePanel.setZoom(zoom);
		zoomChanged();
		surfacePanel.setRaster(raster);
		surfacePanel.setColors(model.surfaceColors);
		setSurfacePosition(position);

		final Timer positionTimer=new Timer("AnimationPanelLayoutTimer",false);
		positionTimer.schedule(new TimerTask() {@Override public void run() {positionTimer.cancel(); setSurfacePosition(position);}},50,5000);

		surfaceAnimator.calcSurfaceSize();
		calculateMinimalTimeStep();

		this.animationDone=animationDone;
		this.sendToSimulation=sendToSimulation;
		buttonSimulation.setVisible(sendToSimulation!=null);
		simData=null;
		simulatorLock.acquireUninterruptibly();
		try {
			this.simulator=simulator;
		} finally {
			simulatorLock.release();
		}
		lastTimeStep=-1;

		this.parentLogger=logger;
		this.logger=logger;
		if (this.parentLogger!=null) this.parentLogger.setCallback(data->loggerCallback(data));
		logTimeStamp=-1;

		int clientCount=(int)(simulator.getCountClients()/1000);
		if (clientCount<=0) {
			progressBar.setVisible(false); /* Abbruch �ber Bedingung -> kein Fortschrittsbalken */
		} else {
			progressBar.setVisible(true);
			progressBar.setMaximum(Math.max(1,clientCount));
		}

		buttonAbort.setEnabled(true);
		abortRun=false;
		continueAsSimulation=false;
		simulationSuccessful=false;

		/* Ist n�tig, damit die kleinen Kundentyp-Icons auf den Stations-Shapes korrekt angezeigt werden. */
		surfacePanel.getSurface().initAfterLoad();

		if (startPaused && !fastWarmUp) {
			surfaceAnimator.setFullRecording(startFullRecording);
			running=true;
			playPause();
			if (this.logger!=null) this.logger.setActive(true);
			simulator.start(true);
		} else {
			simulator.start(false);
			if (logger!=null && logger.getNextLogger()==null) simulator.pauseLogging();
			timer=new Timer("AnimationCancelCheck",false);
			timer.schedule(new UpdateInfoTask(),100);
		}
	}

	/**
	 * Liefert den mit dem Panel verbundenen Simulator
	 * @return	Verbundener Simulator
	 */
	public Simulator getSimulator() {
		return simulator;
	}

	/**
	 * Besitzt das Modell Analogwert-Elemente?
	 * @param model	Modell
	 * @return	Liefert <code>true</code>, wenn das Modell Analogwert-Elemente besitzt
	 * @see #hasAnalogElements
	 */
	private boolean hasAnalogElements(final EditModel model) {
		for (ModelElement element: model.surface.getElements()) {
			if (element instanceof ModelElementAnalogValue) return true;
			if (element instanceof ModelElementTank) return true;
			if (element instanceof ModelElementSub) for (ModelElement sub: ((ModelElementSub)element).getSubSurface().getElements()) {
				if (sub instanceof ModelElementAnalogValue) return true;
				if (sub instanceof ModelElementTank) return true;
			}
		}
		return false;
	}

	/**
	 * Besitzt das Modell Flie�b�nder?
	 * @param model	Modell
	 * @return	Liefert <code>true</code>, wenn das Modell Flie�b�nder besitzt
	 * @see #hasConveyorElements
	 */
	private boolean hasConveyorElements(final EditModel model) {
		for (ModelElement element: model.surface.getElements()) {
			if (element instanceof ModelElementConveyor) return true;
			if (element instanceof ModelElementSub) for (ModelElement sub: ((ModelElementSub)element).getSubSurface().getElements()) {
				if (sub instanceof ModelElementConveyor) return true;
			}
		}
		return false;
	}

	/**
	 * Gibt es im Modell Elemente, die auf Animationsdaten reagieren (z.B. um die Darstellung zu aktualisieren)?
	 * @param model	Modell
	 * @return	Liefert <code>true</code>, wenn das Modell Elemente besitzt, die auf Animationsdaten reagieren
	 * @see ElementWithAnimationDisplay
	 */
	private boolean hasAnimationListener(final EditModel model) {
		for (ModelElement element: model.surface.getElements()) {
			if (element instanceof ElementWithAnimationDisplay) return true;
			if (element instanceof ModelElementSub) for (ModelElement sub: ((ModelElementSub)element).getSubSurface().getElements()) if (sub instanceof ElementWithAnimationDisplay) return true;
		}
		return false;
	}

	/**
	 * F�gt ein <code>ModelElementAnimationConnect</code>-Element zum Editor-Modell hinzu, um die Verkn�pfung
	 * aus der Simulation zu diesem Panel herzustellen.
	 * @param model	Editor-Modell, welches als Animation simuliert werden soll
	 * @return	Gibt <code>true</code> zur�ck, wenn die Warm-up-Phase bei der Animation zun�chst als Simulation vorab ausgef�hrt werden soll
	 * @see ModelElementAnimationConnect
	 */
	public boolean makeAnimationModel(final EditModel model) {
		/* Verkn�pfung zu Animationssystem hinzuf�gen */
		final ModelElementAnimationConnect animationConnect=new ModelElementAnimationConnect(model,model.surface);
		animationConnect.animationViewer=this;
		model.surface.add(animationConnect);

		/* Umgang mit der Warm-Up-Phase */
		if (model.warmUpTime<=0) return false; /* Kein Warm-Up definiert, also nix zu tun. */

		final SetupData.AnimationMode warmUpMode=setup.animationWarmUpMode;
		boolean skip=false;
		boolean fast=false;

		switch (warmUpMode) {
		case ANIMATION_WARMUP_NORMAL:
			/* alles so lassen */
			break;
		case ANIMATION_WARMUP_ASK:
			if (hasAnimationListener(model)) {
				final List<String> options=new ArrayList<>();
				final List<String> infos=new ArrayList<>();
				options.add(Language.tr("Animation.SkipWarmUp.OptionDefault"));
				options.add(Language.tr("Animation.SkipWarmUp.OptionSkip"));
				options.add(Language.tr("Animation.SkipWarmUp.OptionFast"));
				infos.add(Language.tr("Animation.SkipWarmUp.OptionDefault.Info"));
				infos.add(Language.tr("Animation.SkipWarmUp.OptionSkip.Info"));
				infos.add(Language.tr("Animation.SkipWarmUp.OptionFast.Info"));
				final int result=MsgBox.options(this,Language.tr("Animation.SkipWarmUp.Title"),String.format("<html><body>"+Language.tr("Animation.SkipWarmUp")+"</body></html>",NumberTools.formatLong(FastMath.round(model.warmUpTime*model.clientCount))),options.toArray(new String[0]),infos.toArray(new String[0]));
				switch (result) {
				case 0:	/* nicht �ndern */ break;
				case 1: skip=true; break;
				case 2: fast=true; break;
				}
			}
			break;
		case ANIMATION_WARMUP_SKIP:
			if (hasAnimationListener(model)) skip=true;
			break;
		case ANIMATION_WARMUP_FAST:
			fast=true;
			break;
		}

		if (skip) model.warmUpTime=0;
		return fast;
	}

	/**
	 * Beendet die Simulation
	 * @param successful	War der Abschluss erfolgreich?
	 * @see #abortRunTest()
	 * @see #stepInt(boolean, boolean)
	 */
	private void finalizeSimulation(final boolean successful) {
		if (surfaceAnimator==null) return;
		if (simulator==null) return;

		surfaceAnimator.setRecordSystem(null,false);
		simulationSuccessful=successful;
		simulatorLock.acquireUninterruptibly();
		try {
			if (timer!=null) {timer.cancel(); timer=null;}
			if (simulator!=null) simulator.finalizeRun();
			simulator=null;
		} finally {
			simulatorLock.release();
		}
		if (encoder!=null) {
			encoder.done();
			if (encoder instanceof MJPEGSystem) new AnimationRecordWaitDialog(this,(MJPEGSystem)encoder);
			encoder=null;
		}
		surfaceAnimator=null;
		if (animationDone!=null) SwingUtilities.invokeLater(animationDone);
	}

	/**
	 * Gibt an, ob die Simulation erfolgreich beendet wurde.
	 * @return	Gibt <code>true</code> zur�ck, wenn die Simulation erfolgreich beendet wurde
	 */
	public boolean isSimulationSuccessful() {
		return simulationSuccessful;
	}

	/**
	 * Zeitpunkt der letzten Statuszeilen-Aktualisierung
	 * @see #updateStatus(long)
	 */
	private long lastStatusUpdate;

	/**
	 * Anzahl an bislang simulierten Kundenank�nften beim letzten Aufruf von {@link #updateStatus(long)}
	 * @see #updateStatus(long)
	 */
	private long lastStatusCurrent;

	/**
	 * Letzter Zeichenkettenwert f�r die bislang simulierten Kundenank�nfte
	 * @see #lastStatusCurrent
	 * @see #updateStatus(long)
	 */
	private String lastStatusCurrentString;

	/**
	 * Gesamtanzahl an zu simulierenden Kundenank�nften beim letzten Aufruf von {@link #updateStatus(long)}
	 * @see #updateStatus(long)
	 */
	private long lastStatusSum;

	/**
	 * Letzter Zeichenkettenwert f�r die Gesamtanzahl an zu simulierenden Kundenank�nften
	 * @see #updateStatus(long)
	 */
	private String lastStatusSumString;

	/**
	 * Aktualisiert die Anzeige in der Statuszeile
	 * @param currentTime	Aktuelle Simulationszeit
	 */
	private void updateStatus(final long currentTime) {
		final long time=System.currentTimeMillis();
		if (time-lastStatusUpdate<20) return;
		lastStatusUpdate=time;

		long current=0;
		long sum=0;
		if (simulator!=null) {
			current=simulator.getCurrentClients();
			sum=simulator.getCountClients();
		}

		if (lastStatusCurrent!=current || lastStatusCurrentString==null) lastStatusCurrentString=NumberTools.formatLong(current);
		if (sum>=0) {
			if (lastStatusSum!=sum || lastStatusSumString==null) lastStatusSumString=NumberTools.formatLong(sum);
		}

		final String recordStatus;
		if (encoder!=null) {
			recordStatus=String.format(infoMJPEG,NumberTools.formatLong(encoder.getFrameCount()),NumberTools.formatLong(encoder.getBytesCount()/1024));
		} else {
			recordStatus="";

		}
		final String currentTimeString=TimeTools.formatLongTime(currentTime/1000);
		if (sum<=0) {
			statusBar.setText(String.format(infoNoSum,currentTimeString,lastStatusCurrentString,recordStatus));
		} else {
			statusBar.setText(String.format(infoSum,currentTimeString,lastStatusCurrentString,lastStatusSumString,NumberTools.formatPercent(((double)current)/sum,0),recordStatus));
			progressBar.setValue((int)(current/1000));
		}
	}

	/**
	 * F�hrt die Animation und die Verz�gerungen aus.
	 * @param simData	Simulationsdatenobjekt
	 * @param timeStepDelay	Verz�gerungswert (0..25)
	 */
	private void delaySystem(final SimulationData simData, int timeStepDelay) {
		if (lastTimeStep>0) {
			double seconds=(simData.currentTime-lastTimeStep)/1000.0;

			double d=(delaySystem>0)?(0.04/delaySystem):0.2;
			d=FastMath.max(d,0.1);

			int steps;
			long delayMS;
			if (delay>100) {
				delayMS=100;
				steps=(int)Math.round(seconds*10);
				if (steps<1) {
					steps=1;
					delayMS=Math.round(seconds*1000);
				}
			} else {
				if (timeStepDelay>10) timeStepDelay=(int)FastMath.pow(timeStepDelay,1.2);
				delayMS=FastMath.round(d*timeStepDelay*seconds);
				steps=(int)FastMath.round(delayMS/50.0);
				if (steps<1) steps=1;
				if (steps>40) steps=40;
				delayMS=FastMath.min(FastMath.round(FastMath.sqrt(100.0*delayMS/steps)/100),500);
				if (timeStepDelay>10) delayMS=FastMath.max(delay,timeStepDelay/2);
				if (delayMS>0) delayMS=FastMath.max(delayMS,10);
			}

			final long save_currentTime=simData.currentTime;
			for (int i=1;i<=steps;i++) {
				try {
					final long stepTime=lastTimeStep+i*(save_currentTime-lastTimeStep)/steps;
					simData.currentTime=stepTime;
					updateStatus(stepTime);
					surfaceAnimator.updateSurfaceAnimationDisplayElements(simData,true,false);
					Thread.sleep(delayMS);
					if (speedChanged) break;
				} catch (InterruptedException e) {Thread.currentThread().interrupt(); return;}
			}
			simData.currentTime=save_currentTime;
		}
		speedChanged=false;
		lastTimeStep=simData.currentTime;
		updateStatus(simData.currentTime);
	}

	@Override
	public boolean updateViewer(SimulationData simData) {
		if (subViewer!=null) subViewer.updateViewer(simData);
		return updateViewer(simData,null,false);
	}

	/**
	 * Letzter Zeitpunkt (in System-Millisekunden) an dem {@link #updateViewer(SimulationData, RunDataClient, boolean)}
	 * aufgerufen wurde.
	 * @see #updateViewer(SimulationData, RunDataClient, boolean)
	 */
	private long lastUpdateStep=0;

	@Override
	public boolean updateViewer(SimulationData simData, RunDataClient client, boolean moveByTransport) {
		if (abortRun) return false;
		if (continueAsSimulation) return false;
		if (surfaceAnimator==null) return true;

		surfaceAnimator.setSlowMode(running && delayInt>0 && (hasAnalogElements || hasConveyorElements) && setup.useSlowModeAnimation);

		if (subViewer!=null) subViewer.updateViewer(simData,client,moveByTransport);

		surfacePanel.setAnimationSimulationData(simData);

		long currentTime=System.currentTimeMillis();
		if (currentTime<=lastUpdateStep+5 && delayInt==0 && encoder==null) {
			surfaceAnimator.updateSurfaceAnimationDisplayElements(simData,false,true);
			return true;
		}
		lastUpdateStep=currentTime;

		mutex.acquireUninterruptibly();
		try {
			if (simData==null) simData=this.simData;
			if (simData==null) return true;
			if (simData.runData.isWarmUp && fastWarmUp) return true;
			this.simData=simData;
			if (logger==null || !logger.isActive()) {
				delaySystem(simData,delayInt/4); /* Verz�gerungen von einem Ereignis zum n�chsten ausschalten im Einzelschrittmodus. */
			} else {
				updateStatus(simData.currentTime); /* Aber Statuszeile muss aktualisiert werden. (Passiert sonst in delaySystem.) */
			}
			if (!moveByTransport) surfaceAnimator.process(simData,client,FastMath.min(20,delayInt/4));
			surfacePanel.repaint(); /* Wichtig, sonst wird im Einzelschrittmodus der letzte Schritt nicht korrekt dargestellt (und Zahlenwerte an den Stationen stimmen nicht!) */
		} finally {mutex.release();}

		if (startPaused && fastWarmUp) {
			startPaused=false;
			playPause();
			surfacePanel.repaint();
		}

		return true;
	}

	@Override
	public boolean updateViewer(SimulationData simData, RunDataTransporter transporter) {
		if (abortRun) return false;
		if (continueAsSimulation) return false;

		surfaceAnimator.setSlowMode(running && delayInt>0);

		if (subViewer!=null) subViewer.updateViewer(simData,transporter);

		surfacePanel.setAnimationSimulationData(simData);

		long currentTime=System.currentTimeMillis();
		if (currentTime<=lastUpdateStep+5 && delayInt==0 && encoder==null) {
			surfaceAnimator.updateSurfaceAnimationDisplayElements(simData,false,true);
			return true;
		}
		lastUpdateStep=currentTime;

		mutex.acquireUninterruptibly();
		try {
			if (simData==null) simData=this.simData;
			if (simData==null) return true;
			if (simData.runData.isWarmUp && fastWarmUp) return true;
			this.simData=simData;
			if (logger==null || !logger.isActive()) {
				delaySystem(simData,delayInt/4); /* Verz�gerungen von einem Ereignis zum n�chsten ausschalten im Einzelschrittmodus. */
			} else {
				updateStatus(simData.currentTime); /* Aber Statuszeile muss aktualisiert werden. (Passiert sonst in delaySystem.) */
			}
			surfaceAnimator.process(simData,transporter,FastMath.min(20,delayInt/4));
		} finally {
			mutex.release();
		}
		return true;
	}

	/**
	 * Teil dem Panel mit, dass es geschlossen werden soll.<br>
	 * Dadurch wird ggf. der Abbruch der Simulation ausgel�st.
	 */
	public void closeRequest() {
		abortRun=true;
		if (!running) abortRunTest();
	}

	/**
	 * Bricht die Simulation ab, wenn {@link #abortRun} gesetzt ist.
	 * Reagiert au�erdem, wenn sich der Simulator beendet hat.
	 * @return	Liefert <code>true</code>, wenn die Simulation noch l�uft
	 */
	private boolean abortRunTest() {
		if (abortRun) {
			if (simulator!=null) {
				UsageStatistics.getInstance().addSimulationClients(simulator.getCurrentClients());
				simulator.cancel();
			}
		}

		if (abortRun || simulator==null || !simulator.isRunning()) {
			mutex.acquireUninterruptibly();
			try {
				finalizeSimulation(!abortRun);
			} finally {mutex.release();}
			return false;
		}
		return true;
	}

	/**
	 * Pr�ft in regelm��igen Abst�nden ob die Simulation abgebrochen wurde.
	 * @see AnimationPanel#abortRunTest()
	 */
	private class UpdateInfoTask extends TimerTask {
		@Override
		public void run() {
			if (abortRunTest())	{
				simulatorLock.acquireUninterruptibly();
				try {
					if (timer!=null)
						timer.schedule(new UpdateInfoTask(),100);
				} finally {
					simulatorLock.release();
				}
			}
		}
	}

	/**
	 * Wird aufgerufen, wenn die Animationsgeschwindigkeit ge�ndert wurde.
	 * @see #delay
	 * @see #speedChanged
	 */
	private void animationDelayChanged() {
		speedChanged=true;
		if (delay/10!=setup.animationDelay) {
			setup.animationDelay=delay/10;
			setup.saveSetup();
		}
	}

	/**
	 * Aktualisiert die Zoomfaktoranzeige, wenn sich der Zoomfaktor ge�ndert hat.
	 * @see #labelZoom
	 */
	private void zoomChanged() {
		labelZoom.setText(FastMath.round(100*surfacePanel.getZoom())+"% ");
	}

	/**
	 * Liefert den aktuell eingestellten Zoomfaktor
	 * @return Aktueller Zoomfaktor
	 */
	public double getZoom() {
		return surfacePanel.getZoom();
	}

	/**
	 * Zeigt das Kontextmen� zur Auswahl des Zoomfaktors an.
	 * @param parent	�bergeordnetes Element zur Ausrichtung des Popupmen�s.
	 * @see #labelZoom
	 */
	private void showZoomContextMenu(final Component parent) {
		final JPopupMenu popup=new JPopupMenu();

		final int value=Math.max(1,Math.min(20,(int)Math.round(surfacePanel.getZoom()*5)));

		final JSlider slider=new JSlider(SwingConstants.VERTICAL,1,20,value);
		slider.setMinorTickSpacing(1);
		slider.setPaintTicks(true);
		final Dictionary<Integer,JComponent> labels=new Hashtable<>();
		labels.put(1,new JLabel("20%"));
		labels.put(2,new JLabel("40%"));
		labels.put(3,new JLabel("60%"));
		labels.put(4,new JLabel("80%"));
		labels.put(5,new JLabel("100%"));
		labels.put(6,new JLabel("120%"));
		labels.put(10,new JLabel("200%"));
		labels.put(15,new JLabel("300%"));
		labels.put(20,new JLabel("400%"));
		slider.setLabelTable(labels);
		slider.setPaintLabels(true);
		slider.setValue(value);
		slider.addChangeListener(e->{surfacePanel.setZoom(slider.getValue()/5.0); zoomChanged();});
		slider.setPreferredSize(new Dimension(slider.getPreferredSize().width,350));

		popup.add(slider);

		popup.show(parent,0,-350);
	}

	/**
	 * Zeigt den Modelleigenschaften-Dialog (im Nur-Lese-Modus) an.
	 * @see ModelPropertiesDialog
	 */
	private void showModelPropertiesDialog() {
		if (model==null) return;
		final ModelPropertiesDialog dialog=new ModelPropertiesDialog(this,model,true,null);
		dialog.setVisible(true);
	}

	/**
	 * Zeigt das Popupmen� zur Einstellung der Simulationsgeschwindigkeit an.
	 * @see #buttonSpeed
	 */
	private void animationSpeedPopup() {
		final JPopupMenu popup=new JPopupMenu();

		final JSlider slider=new JSlider(SwingConstants.VERTICAL,0,11,6);
		slider.setMinorTickSpacing(1);
		slider.setPaintTicks(true);
		final Dictionary<Integer,JComponent> labels=new Hashtable<>();
		labels.put(11,new JLabel(Language.tr("Animation.Toolbar.Speed.Maximal")));
		labels.put(10, new JLabel(Language.tr("Animation.Toolbar.Speed.Fast")));
		labels.put(7,new JLabel(Language.tr("Animation.Toolbar.Speed.Normal")));
		labels.put(1,new JLabel(Language.tr("Animation.Toolbar.Speed.Slow")));
		labels.put(0,new JLabel(Language.tr("Animation.Toolbar.Speed.RealTime")));
		slider.setLabelTable(labels);
		slider.setPaintLabels(true);
		slider.setValue(FastMath.min(11,FastMath.max(0,11-delay/10)));
		slider.addChangeListener(e->{
			delay=(11-slider.getValue())*10;
			delayInt=delay;
			animationDelayChanged();
		});

		popup.add(slider);

		popup.show(buttonSpeed,0,buttonSpeed.getHeight());
	}

	/**
	 * Zeigt das Popupmen� zur Konfiguration der Animation an.
	 * @see #buttonTools
	 */
	private void animationToolsPopup() {
		final JPopupMenu popup=new JPopupMenu();

		JMenuItem submenu;
		ButtonGroup buttonGroup;

		if (!SetupBase.memoryOnly) {
			popup.add(submenu=new JMenu(Language.tr("Main.Menu.AnimationStartMode")));
			submenu.add(menuStartModeRun=new JRadioButtonMenuItem(Language.tr("Main.Menu.AnimationStartMode.Run")));
			menuStartModeRun.addActionListener(new ToolBarListener());
			submenu.add(menuStartModePause=new JRadioButtonMenuItem(Language.tr("Main.Menu.AnimationStartMode.Pause")));
			menuStartModePause.addActionListener(new ToolBarListener());
			buttonGroup=new ButtonGroup();
			buttonGroup.add(menuStartModeRun);
			buttonGroup.add(menuStartModePause);
			menuStartModeRun.setSelected(!setup.animationStartPaused);
			menuStartModePause.setSelected(setup.animationStartPaused);
		}

		popup.add(submenu=new JMenu(Language.tr("Main.Menu.AnalogValues")));
		submenu.add(menuAnalogValuesFast=new JRadioButtonMenuItem(Language.tr("Main.Menu.AnalogValues.Fast")));
		menuAnalogValuesFast.addActionListener(new ToolBarListener());
		submenu.add(menuAnalogValuesExact=new JRadioButtonMenuItem(Language.tr("Main.Menu.AnalogValues.Exact")));
		menuAnalogValuesExact.addActionListener(new ToolBarListener());
		buttonGroup=new ButtonGroup();
		buttonGroup.add(menuAnalogValuesFast);
		buttonGroup.add(menuAnalogValuesExact);
		menuAnalogValuesFast.setSelected(!setup.useSlowModeAnimation);
		menuAnalogValuesExact.setSelected(setup.useSlowModeAnimation);

		if (!SetupBase.memoryOnly) {
			popup.add(submenu=new JMenu(Language.tr("Main.Menu.AnimationScreenshotMode")));
			submenu.add(menuScreenshotModeHome=new JRadioButtonMenuItem(Language.tr("Main.Menu.AnimationScreenshotMode.Home")+" ("+FileSystemView.getFileSystemView().getHomeDirectory()+")"));
			menuScreenshotModeHome.addActionListener(new ToolBarListener());
			String custom="";
			if (setup.imagePathAnimation!=null && !setup.imagePathAnimation.trim().isEmpty()) custom=" ("+setup.imagePathAnimation.trim()+")";
			submenu.add(menuScreenshotModeCustom=new JRadioButtonMenuItem(Language.tr("Main.Menu.AnimationScreenshotMode.Custom")+custom));
			menuScreenshotModeCustom.addActionListener(new ToolBarListener());
			buttonGroup=new ButtonGroup();
			buttonGroup.add(menuScreenshotModeHome);
			buttonGroup.add(menuScreenshotModeCustom);
			menuScreenshotModeHome.setSelected(setup.imagePathAnimation==null || setup.imagePathAnimation.trim().isEmpty());
			menuScreenshotModeCustom.setSelected(setup.imagePathAnimation!=null && !setup.imagePathAnimation.trim().isEmpty());
		}

		popup.add(menuShowLog=new JCheckBoxMenuItem(Language.tr("SettingsDialog.Tabs.Simulation.ShowSingleStepLogData")));
		menuShowLog.addActionListener(new ToolBarListener());
		menuShowLog.setSelected(setup.showSingleStepLogData);

		popup.show(buttonTools,0,buttonTools.getHeight());
	}

	/**
	 * Befehl: Animation anhalten oder fortsetzen
	 * @see #buttonPlayPause
	 */
	private void playPause() {
		simulatorLock.acquireUninterruptibly();
		try {
			if (running) {
				/* Pause */
				running=false;
				buttonStep.setEnabled(true);
				buttonPlayPause.setText(Language.tr("Animation.Toolbar.Play"));
				buttonPlayPause.setToolTipText(Language.tr("Animation.Toolbar.Play.Info")+" ("+keyStrokeToString(KeyStroke.getKeyStroke(KeyEvent.VK_F6,0))+")");
				buttonPlayPause.setIcon(Images.ANIMATION_PLAY.getIcon());
				if (simulator!=null) simulator.pauseExecution();

				if (timer!=null) {timer.cancel(); timer=null;}
			} else {
				/* Play */
				if (surfaceAnimator!=null) surfaceAnimator.setFullRecording(false);
				if (logger!=null) {
					logger.setActive(false);
					if (simulator!=null && logger.getNextLogger()==null) simulator.pauseLogging();
				}
				logArea.setVisible(false);
				delayInt=delay;
				running=true;
				buttonStep.setEnabled(false);
				buttonPlayPause.setText(Language.tr("Animation.Toolbar.Pause"));
				buttonPlayPause.setToolTipText(Language.tr("Animation.Toolbar.Pause.Info")+" ("+keyStrokeToString(KeyStroke.getKeyStroke(KeyEvent.VK_F6,0))+")");
				buttonPlayPause.setIcon(Images.ANIMATION_PAUSE.getIcon());
				if (simulator!=null) simulator.resumeExecution();

				timer=new Timer("AnimationCancelCheck",false);
				timer.schedule(new UpdateInfoTask(),100);
			}
		} finally {
			simulatorLock.release();
		}
	}

	/**
	 * Wurden im letzten Zeitschritt Logging-Daten erg�nzt?
	 * @see #multiSingleCoreSteps(boolean)
	 * @see #stepInt(boolean, boolean)
	 * @see #loggerCallback(CallbackLoggerData)
	 */
	private volatile boolean stepLogChanged;

	/**
	 * Wird von {@link #step(boolean)} verwendet,
	 * um sicher zu stellen, dass der vorherige Update-Thread
	 * abgeschlossen ist, bevor der n�chste startet.
	 * @see #step(boolean)
	 */
	private Semaphore stepLock=new Semaphore(1);

	/**
	 * F�hrt einen Animationsschritt im Single-Core-Modus aus.
	 * @param fullRecording	Modus zur vollst�ndigen Erfassung der Animationsdaten
	 * @see #step(boolean)
	 */
	private void multiSingleCoreSteps(final boolean fullRecording) {
		stepInt(false,fullRecording);
		SwingUtilities.invokeLater(()->{
			if (!stepLogChanged && simulator!=null) multiSingleCoreSteps(fullRecording);
		});
	}

	/**
	 * F�hrt einen Animationsschritt aus.
	 * @param fullRecording	Modus zur vollst�ndigen Erfassung der Animationsdaten.
	 * @see ModelSurfaceAnimatorBase#getAnimationStepInfo(long, simulator.runmodel.RunModel, List, List)
	 */
	public void step(final boolean fullRecording) {
		final boolean multiCore=setup.useMultiCoreAnimation;

		if (!multiCore) {
			multiSingleCoreSteps(fullRecording);
		} else {
			if (!stepLock.tryAcquire()) return;
			new Thread(()->{
				try {stepInt(true,fullRecording);} finally {stepLock.release();}
			},"AnimationStepper").start();
		}
	}

	/**
	 * F�hrt einen Animationsschritt aus.
	 * @param multiCore	Weiteren CPU-Kern f�r die Animation verwenden (<code>true</code>)?
	 * @param fullRecording	Modus zur vollst�ndigen Erfassung der Animationsdaten
	 * @see #step(boolean)
	 * @see #multiSingleCoreSteps(boolean)
	 */
	private void stepInt(final boolean multiCore, final boolean fullRecording) {
		if (logger!=null) {
			logger.setActive(true);
			simulatorLock.acquireUninterruptibly();
			try {
				if (simulator!=null && logger.getNextLogger()==null) simulator.continueLogging();
			} finally {
				simulatorLock.release();
			}
		}
		if (surfaceAnimator==null) return;
		surfaceAnimator.setFullRecording(fullRecording);
		surfaceAnimator.setSlowMode(false);
		delayInt=100;
		stepLogChanged=false;

		boolean finalize=false;
		while (!stepLogChanged && simulator!=null) { /* So viele Schritte ausf�hren, bis es einen neuen Log-Eintrag gibt (um nicht f�r folgenlose Recheck-Events zu stoppen). */
			simulatorLock.acquireUninterruptibly();
			try {
				if (simulator!=null) simulator.stepExecution(multiCore);
				if (simulator==null || !simulator.isRunning()) {finalize=true; break;}
			} finally {
				simulatorLock.release();
			}
			if (!multiCore) break;
		}
		if (finalize) finalizeSimulation(true);
	}

	/**
	 * Befehl: Simulation ohne Animationsausgabe zu Ende f�hren
	 * @see #continueAsSimulation
	 */
	private void finishAsSimulation() {
		continueAsSimulation=true;
		if (!running) playPause();

		surfaceAnimator.setRecordSystem(null,false);
		if (timer!=null) timer.cancel();
		simulationSuccessful=false;
		if (encoder!=null) {
			encoder.done();
			if (encoder instanceof MJPEGSystem) new AnimationRecordWaitDialog(this,(MJPEGSystem)encoder);
			encoder=null;
		}

		if (animationDone!=null) SwingUtilities.invokeLater(animationDone);
		if (sendToSimulation!=null) SwingUtilities.invokeLater(sendToSimulation);
	}

	/**
	 * Ermittelt den in einem Verzeichnis n�chsten verf�gbaren Dateinamen f�r einen Screenshot
	 * @param path	Verzeichnis in dem der Screenshot abgelegt werden soll
	 * @return	Verf�gbarer Dateiname
	 * @see #saveScreenshot()
	 */
	private File getNextScreenshotFile(final String path) {
		final File folder;
		if (path==null || path.trim().isEmpty()) {
			folder=FileSystemView.getFileSystemView().getHomeDirectory();
		} else {
			folder=new File(path);
		}
		if (!folder.isDirectory()) return null;
		int nr=0;
		File file=null;
		while (nr==0 || (file!=null && file.exists())) {
			nr++;
			file=new File(folder,"Animation-"+nr+".png");
		}
		return file;
	}

	/**
	 * Befehl: Screenshot des aktuellen Modellzustands aufnehmen
	 * @see #buttonScreenshot
	 */
	private void saveScreenshot() {
		final File file=getNextScreenshotFile(setup.imagePathAnimation);
		if (file==null) return;
		surfacePanel.saveImageToFile(file,"png",setup.imageSize,setup.imageSize);
	}

	/**
	 * Erstellt einen Screenshot im aktuellen Animationsstatus und liefert diesen als Bild-Objekt zur�ck
	 * @return	Screenshot im aktuellen Animationsstatus
	 */
	public BufferedImage getScreenshot() {
		return surfacePanel.getImageMaxSize(-1,-1);
	}

	/**
	 * Maximale Gr��e f�r einen Logging-Eintrag
	 * @see #loggerCallback(CallbackLoggerData)
	 */
	private static final int MAX_LOG_VIEWER_SIZE=4_000;

	/**
	 * Erfasst Logging-Daten f�r die Ausgabe im unteren Fensterbereich
	 * @param data	Logging-Daten
	 */
	private void loggerCallback(final CallbackLoggerData data) {
		stepLogChanged=true;

		if (!setup.showSingleStepLogData) return;

		if (logTextHistory==null) logTextHistory=new ArrayList<>();
		if (logTextHistoryPlain==null) logTextHistoryPlain=new ArrayList<>();
		if (!logArea.isVisible()) {
			logTextHistory.clear();
			logTextHistoryPlain.clear();
		}

		logArea.setVisible(true);

		boolean newMessage=false;
		if (logTimeStamp!=data.timeStamp || logText==null) {
			logText=new StringBuilder();
			logTextPlain=new StringBuilder();
			logTimeStamp=data.timeStamp;
			newMessage=true;
		} else {
			if (logTextPlain.length()<MAX_LOG_VIEWER_SIZE) {
				logText.append("<br>");
				logTextPlain.append("\n");
			}
		}
		final String colorCode;
		if (data.color==null || data.color.equals(Color.BLACK)) {
			colorCode="FFFFFF";
		} else {
			colorCode=Integer.toHexString(data.color.getRed())+Integer.toHexString(data.color.getGreen())+Integer.toHexString(data.color.getBlue());
		}
		if (logTextPlain.length()<MAX_LOG_VIEWER_SIZE) {
			if (data.id>=0) {
				logText.append(data.time+": <b><span style=\"background-color: #"+colorCode+";\">&nbsp; "+data.event+" (id="+data.id+")"+" &nbsp;</span></b> "+data.info);
			} else {
				logText.append(data.time+": <b><span style=\"background-color: #"+colorCode+";\">&nbsp; "+data.event+" &nbsp;</span></b> "+data.info);
			}
			logTextPlain.append(data.time+": "+data.event+" - "+data.info);
		}
		final String message="<html><body>"+logText.toString()+"</body></html>";
		final String messagePlain=logTextPlain.toString();
		logLabel.setText(message);

		if (newMessage || logTextHistory.isEmpty()) {
			logTextHistory.add(message);
			logTextHistoryPlain.add(messagePlain);
			while (logTextHistory.size()>100) logTextHistory.remove(0);
			while (logTextHistoryPlain.size()>100) logTextHistoryPlain.remove(0);
		} else {
			logTextHistory.set(logTextHistory.size()-1,message);
			logTextHistoryPlain.set(logTextHistoryPlain.size()-1,messagePlain);
		}
		logTextDisplayIndex=logTextHistory.size()-1;

		logPrevious.setEnabled(logTextHistory.size()>1);
		logNext.setEnabled(false);
		logCurrent.setEnabled(false);
		logJS.setEnabled(!logger.getJSData().isEmpty());
	}

	/**
	 * Zeigt neuere oder �ltere Logging-Nachrichten an
	 * @param move	Verschiebungsrichtung (-1: vorherige Meldung, 0: neuste Meldung, 1: n�chste Meldung)
	 * @see #logPrevious
	 * @see #logNext
	 * @see #logCurrent
	 */
	private void displayLogMessage(final int move) {
		switch (move) {
		case -1: /* Vorherige Meldung */
			if (logTextDisplayIndex>0) logTextDisplayIndex--;
			break;
		case 0: /* Aktuelle bzw. neuste Meldung */
			logTextDisplayIndex=logTextHistory.size()-1;
			break;
		case 1: /* N�chste Meldung */
			if (logTextDisplayIndex<logTextHistory.size()-1) logTextDisplayIndex++;
			break;
		}

		logLabel.setText(logTextHistory.get(logTextDisplayIndex));

		logPrevious.setEnabled(logTextDisplayIndex>0);
		logNext.setEnabled(logTextDisplayIndex<logTextHistory.size()-1);
		logCurrent.setEnabled(logTextDisplayIndex<logTextHistory.size()-1);
	}

	/**
	 * Befehl: Logging-Ausgaben in die Zwischenablage kopieren
	 * @see #logCopy
	 */
	private void copyLogMessage() {
		final Clipboard clipboard=Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(new StringSelection(logTextHistoryPlain.get(logTextDisplayIndex)),null);
	}

	/**
	 * Befehl: "Animationsstart" - "Animation sofort starten"/"Im Pause-Modus starten"
	 * @param paused	Animation im Pausemodus starten?
	 * @see #menuStartModePause
	 * @see #menuStartModeRun
	 */
	private void commandAnimationStartMode(final boolean paused) {
		if (setup.animationStartPaused==paused) return;
		setup.animationStartPaused=paused;
		setup.saveSetup();
	}

	/**
	 * Befehl: "Analoge Werte in Animation" - "Schnelle Animation"/"�nderungen exakt anzeigen (langsam)"
	 * @param useSlowModeAnimation	Animation langsamer daf�r analoge Werte besser darstellen?
	 * @see #menuAnalogValuesFast
	 * @see #menuAnalogValuesExact
	 */
	private void commandAnalogValuesSlow(final boolean useSlowModeAnimation) {
		if (setup.useSlowModeAnimation==useSlowModeAnimation) return;
		setup.useSlowModeAnimation=useSlowModeAnimation;
		setup.saveSetup();
	}

	/**
	 * Befehl: "Verzeichnis zum Speichern von Bildern" - "Im Nutzerverzeichnis"
	 * @see #menuScreenshotModeHome
	 */
	private void commandScreenshotModeHome() {
		setup.imagePathAnimation="";
		setup.saveSetup();
		updateScreenshotButtonHint();
	}

	/**
	 * Befehl: "Verzeichnis zum Speichern von Bildern" - "Im ausgew�hlten Verzeichnis"
	 * @see #menuScreenshotModeCustom
	 */
	private void commandScreenshotModeCustom() {
		final JFileChooser fc=new JFileChooser();
		CommonVariables.initialDirectoryToJFileChooser(fc);
		if (setup.imagePathAnimation!=null && !setup.imagePathAnimation.trim().isEmpty() && new File(setup.imagePathAnimation).isDirectory()) {
			fc.setCurrentDirectory(new File(setup.imagePathAnimation));
		}
		fc.setDialogTitle(Language.tr("Batch.Output.Folder.Button.Hint"));
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (fc.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return;
		CommonVariables.initialDirectoryFromJFileChooser(fc);
		final File file=fc.getSelectedFile();
		setup.imagePathAnimation=file.toString();
		setup.saveSetup();
		updateScreenshotButtonHint();
	}

	/**
	 * Aktualisiert den Tooltip-Text f�r {@link #buttonScreenshot}
	 * gem�� dem gew�hlten Ausgabeordner f�r Screenshots.
	 * @see #buttonScreenshot
	 * @see #commandScreenshotModeHome()
	 * @see #commandScreenshotModeCustom()
	 */
	private void updateScreenshotButtonHint() {
		String folder=FileSystemView.getFileSystemView().getHomeDirectory().toString();
		if (setup.imagePathAnimation!=null && !setup.imagePathAnimation.trim().isEmpty()) folder=setup.imagePathAnimation.trim();
		buttonScreenshot.setToolTipText(Language.tr("Animation.Toolbar.Image.Info")+" ("+Language.tr("Animation.Toolbar.Image.Info.Folder")+": "+folder+")");
	}

	/**
	 * Befehl: "Logging-Daten im Einzelschrittmodus anzeigen" (an/aus umschalten)
	 * @see #menuShowLog
	 */
	private void toggleShowSingleStepLogData() {
		setup.showSingleStepLogData=!setup.showSingleStepLogData;
		setup.saveSetup();

		if (setup.showSingleStepLogData) {
			logger=parentLogger;
		} else {
			if (logger!=null) {
				logger.setActive(false);
				logger=null;
			}
			if (logArea.isVisible()) logArea.setVisible(false);
		}
	}

	/**
	 * Stellt ein Unter-Animator-Element ein, welches ebenfalls bei Animationsschritten benachrichtigt werden soll
	 * @param subViewer	Unter-Animator-Element (kann auch <code>null</code> sein, wenn kein zus�tzliches Element benachrichtigt werden soll)
	 */
	public void setSubViewer(final RunModelAnimationViewer subViewer) {
		this.subViewer=subViewer;
	}

	/**
	 * Liefert den momentan gew�hlten Delay-Wert f�r die �bertragung an Unter-Animator-Elemente
	 * @return	Aktueller Delay-Wert
	 */
	public int getDelayIntern() {
		return delayInt;
	}

	/**
	 * Beim letzten Aufruf von {@link #calcExpression()} zuletzt eingegebener Rechenausdruck.
	 * @see #calcExpression()
	 * @see ExpressionCalculatorDialog
	 */
	private String lastCaluclationExpression=null;

	/**
	 * Beim letzten Aufruf von {@link #calcExpression()} zuletzt eingegebener Javascript-Code.
	 * @see #calcExpression()
	 * @see ExpressionCalculatorDialog
	 */
	private String lastCaluclationJavaScript=null;

	/**
	 * Beim letzten Aufruf von {@link #calcExpression()} zuletzt eingegebener Java-Code.
	 * @see #calcExpression()
	 * @see ExpressionCalculatorDialog
	 */
	private String lastCaluclationJava=null;

	/**
	 * Beim letzten Aufruf von {@link #calcExpression()} zuletzt aktiver Tab.
	 * @see #calcExpression()
	 * @see ExpressionCalculatorDialog
	 */
	private int lastCaluclationTab=0;

	/**
	 * Befehl: Ausdruck berechnen
	 * @see #logExpression
	 * @see ExpressionCalculatorDialog
	 */
	private void calcExpression() {
		final ExpressionCalculatorDialog dialog=new ExpressionCalculatorDialog(
				this,
				simulator.getEditModel(),
				s->calculateExpression(s),
				s->runJavaScript(s),
				s->runJava(s),
				lastCaluclationTab,
				lastCaluclationExpression,
				lastCaluclationJavaScript,
				lastCaluclationJava
				);
		dialog.setVisible(true);
		lastCaluclationTab=dialog.getLastMode();
		lastCaluclationExpression=dialog.getLastExpression();
		lastCaluclationJavaScript=dialog.getLastJavaScript();
		lastCaluclationJava=dialog.getLastJava();
	}

	/**
	 * Befehl: Ergebnisse der Javascript-Skriptausf�hrung anzeigen
	 * @see #logJS
	 * @see AnimationJSInfoDialog
	 */
	private void showJSResults() {
		new AnimationJSInfoDialog(this,logger.getJSData());
	}

	/**
	 * Befehl: N�chste geplante Ereignisse anzeigen
	 * @see #logEvents
	 * @see NextEventsViewerDialog
	 */
	private void showEventslist() {
		new NextEventsViewerDialog(this,simData.eventManager.getAllEvents(),simData);
	}

	/**
	 * Liefert eine in ein JSON-Array umwandelbare Zuordnung mit Daten �ber die statischen Icons
	 * (Key: "staticImages"), die bewerten Icons (Key: "movingImages") und die aktuellen
	 * Logging-Ausgaben (Key: "logs").
	 * @return	Zuordnung mit Daten zum aktuellen Animationsschritt
	 */
	public Map<String,Object> getAnimationStepInfo() {
		final List<Map<String,String>> staticElementsList=new ArrayList<>();
		final List<Map<String,Object>> movingElementsList=new ArrayList<>();

		final Map<String,Object> map=new HashMap<>();
		final Map<String,Object> staticElements=new HashMap<>();
		final Map<String,Object> movingElements=new HashMap<>();
		String time="0";

		if (simData!=null) {
			if (surfaceAnimator!=null && simulator!=null) {
				surfaceAnimator.getAnimationStepInfo(simData.currentTime,simulator.getRunModel(),staticElementsList,movingElementsList);

				final Set<String> movingIDs=new HashSet<>();
				for (int i=0;i<movingElementsList.size();i++) {
					final Map<String,Object> movingMap=movingElementsList.get(i);
					final Object obj=movingMap.get("0");
					if (obj!=null) {
						@SuppressWarnings("unchecked")
						final String id=((Map<String,String>)obj).get("id");
						if (id!=null) movingIDs.add(id);
					}
					movingElements.put(""+(i+1),movingMap);
				}

				int nr=0;
				for (int i=0;i<staticElementsList.size();i++) {
					final Map<String,String> staticMap=staticElementsList.get(i);
					if (!movingIDs.contains(staticMap.get("id"))) {
						nr++;
						staticElements.put(""+nr,staticMap);
					}
				}

				map.put("staticImages",staticElements);
				map.put("movingImages",movingElements);
			}
			time=NumberTools.formatSystemNumber(simData.currentTime,3);
		}

		map.put("time",time);
		map.put("staticImages",staticElements);
		map.put("movingImages",movingElements);
		map.put("logs",logText.toString());
		return map;
	}

	/**
	 * Versucht den als Zeichenkette �bergebenen Ausdruck im Kontext der Simulationsdaten zu berechnen
	 * @param expression	Zu berechnender Ausdruck
	 * @return	Ergebnis oder im Fehlerfall <code>null</code>
	 */
	public Double calculateExpression(final String expression) {
		return surfaceAnimator.calculateExpression(expression);
	}

	/**
	 * Versucht das als Zeichenkette �bergebene Javascript im Kontext der Simulationsdaten auszuf�hren
	 * @param script	Auszuf�hrendes Javascript
	 * @return	R�ckgabewert (Text oder Fehlermeldung oder leere Zeichenkette)
	 */
	public String runJavaScript(final String script) {
		return surfaceAnimator.runJavaScript(script);
	}

	/**
	 * Versucht den als Zeichenkette �bergebenen Java-Code im Kontext der Simulationsdaten auszuf�hren
	 * @param script	Auszuf�hrender Java-Code
	 * @return	R�ckgabewert (Text oder Fehlermeldung oder leere Zeichenkette)
	 */
	public String runJava(final String script) {
		return surfaceAnimator.runJava(script);
	}

	/**
	 * Listener f�r Klicks auf die verschiedenen Symbolleisten-Schaltfl�chen
	 * @see AnimationPanel#toolbarListener
	 */
	private class ToolBarListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			final Object source=e.getSource();
			if (source==buttonZoomOut) {surfacePanel.zoomOut(); zoomChanged(); return;}
			if (source==buttonZoomIn) {surfacePanel.zoomIn(); zoomChanged(); return;}
			if (source==buttonZoomDefault) {surfacePanel.zoomDefault(); zoomChanged(); return;}
			if (source==buttonFindModel) {surfacePanel.centerModel(); return;}
			if (source==buttonAbort) {closeRequest(); buttonAbort.setEnabled(false); return;}
			if (source==buttonScreenshot) {saveScreenshot(); return;}
			if (source==buttonSimulation) {finishAsSimulation(); return;}
			if (source==buttonTools) {animationToolsPopup(); return;}
			if (source==buttonPlayPause) {playPause(); return;}
			if (source==buttonStep) {step(false);}
			if (source==buttonSpeed) {animationSpeedPopup(); return;}
			if (source==menuStartModeRun) {commandAnimationStartMode(false); return;}
			if (source==menuAnalogValuesFast) {commandAnalogValuesSlow(false); return;}
			if (source==menuAnalogValuesExact) {commandAnalogValuesSlow(true); return;}
			if (source==menuStartModePause) {commandAnimationStartMode(true); return;}
			if (source==menuScreenshotModeHome) {commandScreenshotModeHome(); return;}
			if (source==menuScreenshotModeCustom) {commandScreenshotModeCustom(); return;}
			if (source==menuShowLog) {toggleShowSingleStepLogData(); return;}
			if (source==buttonProperties) {showModelPropertiesDialog(); return;}
			if (source==logPrevious) {displayLogMessage(-1); return;}
			if (source==logNext) {displayLogMessage(1); return;}
			if (source==logCurrent) {displayLogMessage(0); return;}
			if (source==logCopy) {copyLogMessage(); return;}
			if (source==logExpression) {calcExpression(); return;}
			if (source==logJS) {showJSResults(); return;}
			if (source==logEvents) {showEventslist(); return;}
		}
	}
}