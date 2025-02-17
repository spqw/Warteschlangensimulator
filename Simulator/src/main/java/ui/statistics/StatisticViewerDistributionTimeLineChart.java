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

import java.awt.Color;
import java.net.URL;
import java.util.Map;
import java.util.stream.Stream;

import language.Language;
import mathtools.distribution.DataDistributionImpl;
import simulator.statistics.Statistics;
import statistics.StatisticsDataCollector;
import statistics.StatisticsDataPerformanceIndicator;
import statistics.StatisticsDataPerformanceIndicatorWithNegativeValues;
import statistics.StatisticsLongRunPerformanceIndicator;
import statistics.StatisticsMultiPerformanceIndicator;
import statistics.StatisticsPerformanceIndicator;
import statistics.StatisticsTimePerformanceIndicator;
import systemtools.statistics.StatisticViewerLineChart;
import ui.help.Help;

/**
 * Dieser Viewer gibt die Verteilung der Zwischenankunfts-, Warte- und Bedienzeiten in Form eines Liniendiagramms zu den Simulationsergebnissen aus.
 * @see StatisticViewerLineChart
 * @author Alexander Herzog
 */
public class StatisticViewerDistributionTimeLineChart extends StatisticViewerLineChart {
	/** Statistikobjekt, aus dem die anzuzeigenden Daten entnommen werden sollen */
	private final Statistics statistics;
	/** Gibt an, welche Daten genau ausgegeben werden sollen */
	private final Mode mode;
	/** Wird als Modus {@link Mode#MODE_VALUE_RECORDING} verwendet, so kann hier der Name der Datenaufzeichnung-Station, deren Daten ausgegeben werden sollen, angegeben werden. */
	private final String data;

	/** Farben f�r die Diagrammlinien */
	private static final Color[] COLORS=new Color[]{Color.RED,Color.BLUE,Color.GREEN,Color.BLACK};

	/** Maximalanzahl an anzuzeigenden Datenreihen */
	private static final int MAX_SERIES=100;

	/**
	 * W�hlt die von {@link StatisticViewerDistributionTimeLineChart} auszugebende Information aus.
	 * @author Alexander Herzog
	 * @see StatisticViewerDistributionTimeLineChart#StatisticViewerDistributionTimeLineChart(Statistics, Mode)
	 */
	public enum Mode {
		/** Verteilungsdiagramm der Zwischenankunftszeiten der Kunden an den "Quelle"-Stationen */
		MODE_INTERARRIVAL_CLIENTS,
		/** Verteilungsdiagramm der Zwischenankunftszeiten der Kunden an den einzelnen Stationen */
		MODE_INTERARRIVAL_STATION,
		/** Verteilungsdiagramm der Zwischenankunftszeiten der Kunden an den einzelnen Stationen auf Batch-Basis */
		MODE_INTERARRIVAL_STATION_BATCH,
		/** Verteilungsdiagramm der Zwischenankunftszeiten der Kunden an den einzelnen Stationen nach Kundentypen weiter ausdifferenziert */
		MODE_INTERARRIVAL_STATION_CLIENTS,
		/** Verteilungsdiagramm der Zwischenankunftszeiten der Kunden an den einzelnen Stationen nach Warteschlangenl�nge weiter ausdifferenziert */
		MODE_INTERARRIVAL_STATION_STATES,
		/** Verteilungsdiagramm der Zwischenabgangszeiten der Kunden aus dem System */
		MODE_INTERLEAVE_CLIENTS,
		/** Verteilungsdiagramm der Zwischenabgangszeiten der Kunden bei den einzelnen Stationen */
		MODE_INTERLEAVE_STATION,
		/** Verteilungsdiagramm der Zwischenabgangszeiten der Kunden bei den einzelnen Stationen auf Batch-Basis */
		MODE_INTERLEAVE_STATION_BATCH,
		/** Verteilungsdiagramm der Zwischenabgangszeiten der Kunden bei den einzelnen Stationen nach Kundentypen weiter ausdifferenziert */
		MODE_INTERLEAVE_STATION_CLIENTS,
		/** Verteilungsdiagramm der Wartezeiten der Kunden */
		MODE_WAITING_CLIENTS,
		/** Verteilungsdiagramm der Transportzeiten der Kunden */
		MODE_TRANSFER_CLIENTS,
		/** Verteilungsdiagramm der Bedienzeiten der Kunden */
		MODE_PROCESSING_CLIENTS,
		/** Verteilungsdiagramm der Verweilzeiten der Kunden */
		MODE_RESIDENCE_CLIENTS,
		/** Verteilungsdiagramm der an den Stationen aufgetretenen Wartezeiten */
		MODE_WAITING_STATION,
		/** Verteilungsdiagramm der an den Stationen aufgetretenen Transportzeiten */
		MODE_TRANSFER_STATION,
		/** Verteilungsdiagramm der an den Stationen aufgetretenen Bedienzeiten */
		MODE_PROCESSING_STATION,
		/** Verteilungsdiagramm der an den Stationen aufgetretenen Verweilzeiten */
		MODE_RESIDENCE_STATION,
		/** Verteilungsdiagramm der an den Stationen aufgetretenen R�stzeiten */
		MODE_SETUP_STATION,
		/** Verteilungsdiagramm der an den Stationen aufgetretenen Wartezeiten (Einzelzeiten der Kunden summiert) */
		MODE_WAITING_STATION_TOTAL,
		/** Verteilungsdiagramm der an den Stationen aufgetretenen Transportzeiten (Einzelzeiten der Kunden summiert) */
		MODE_TRANSFER_STATION_TOTAL,
		/** Verteilungsdiagramm der an den Stationen aufgetretenen Bedienzeiten (Einzelzeiten der Kunden summiert) */
		MODE_PROCESSING_STATION_TOTAL,
		/** Verteilungsdiagramm der an den Stationen aufgetretenen Verweilzeiten (Einzelzeiten der Kunden summiert) */
		MODE_RESIDENCE_STATION_TOTAL,
		/** Verteilungsdiagramm der an den Stationen aufgetretenen Wartezeiten (zus�tzlich ausdifferenziert nach Kundentypen) */
		MODE_WAITING_STATION_CLIENT,
		/** Verteilungsdiagramm der an den Stationen aufgetretenen Transportzeiten (zus�tzlich ausdifferenziert nach Kundentypen) */
		MODE_TRANSFER_STATION_CLIENT,
		/** Verteilungsdiagramm der an den Stationen aufgetretenen Bedienzeiten (zus�tzlich ausdifferenziert nach Kundentypen) */
		MODE_PROCESSING_STATION_CLIENT,
		/** Verteilungsdiagramm der an den Stationen aufgetretenen Verweilzeiten (zus�tzlich ausdifferenziert nach Kundentypen) */
		MODE_RESIDENCE_STATION_CLIENT,
		/** Verteilungsdiagramm der Anzahl an Kunden an den Stationen */
		MODE_NUMBER_STATION,
		/** Verteilungsdiagramm der Anzahl an Kunden an den Stationen nach Kundentypen */
		MODE_NUMBER_STATION_CLIENT_TYPES,
		/** Verteilungsdiagramm der Anzahl an Kunden im System nach Kundentypen */
		MODE_NUMBER_CLIENT,
		/** Verteilungsdiagramm der Anzahl an Kunden an den Stationswarteschlangen */
		MODE_QUEUE,
		/** Verteilungsdiagramm der Anzahl an Kunden an den Stationswarteschlangen nach Kundentypen */
		MODE_QUEUE_CLIENT_TYPE,
		/** Verteilungsdiagramm der Anzahl an Kunden an den Stationen in Bedienung */
		MODE_PROCESS,
		/** Verteilungsdiagramm der Anzahl an Kunden an den Stationen in Bedienung nach Kundentypen */
		MODE_PROCESS_CLIENT_TYPE,
		/** Verteilungsdiagramm der Werte der Laufzeitstatistik */
		MODE_ADDITIONAL_STATISTICS,
		/** Verteilungsdiagramm mit den Werten der Kundendatenfelder */
		MODE_CLIENT_DATA_DISTRIBUTION,
		/** Verteilungsdiagramm mit den Werten der Kundendatenfelder (aufgeschl�sselt nach Kundentypen) */
		MODE_CLIENT_DATA_DISTRIBUTION_BY_CLIENT_TYPES,
		/** Verteilungsdiagramme der an den Datenaufzeichnung-Stationen erfassten Werten */
		MODE_VALUE_RECORDING,
	}

	/**
	 * Konstruktor der Klasse
	 * @param statistics	Statistikobjekt, aus dem die anzuzeigenden Daten entnommen werden sollen
	 * @param mode	Gibt an, welche Daten genau ausgegeben werden sollen
	 * @see Mode
	 */
	public StatisticViewerDistributionTimeLineChart(final Statistics statistics, final Mode mode) {
		super();
		this.statistics=statistics;
		this.mode=mode;
		this.data=null;
	}

	/**
	 * Konstruktor der Klasse
	 * @param statistics	Statistikobjekt, aus dem die anzuzeigenden Daten entnommen werden sollen
	 * @param mode	Gibt an, welche Daten genau ausgegeben werden sollen
	 * @param data	Wird als Modus {@link Mode#MODE_VALUE_RECORDING} verwendet, so kann hier der Name der Datenaufzeichnung-Station, deren Daten ausgegeben werden sollen, angegeben werden.
	 * @see Mode
	 */
	public StatisticViewerDistributionTimeLineChart(final Statistics statistics, final Mode mode, final String data) {
		super();
		this.statistics=statistics;
		this.mode=mode;
		this.data=data;
	}

	/**
	 * Zeigt im Fu�bereich der Hilfeseite eine "Erkl�rung einblenden"-Schaltfl�che, die,
	 * wenn sie angeklickt wird, eine html-Hilfeseite anzeigt.
	 * @param topic	Hilfe-Thema (wird als Datei in den "description_*"-Ordern gesucht)
	 */
	private void addDescription(final String topic) {
		final URL url=StatisticViewerDistributionTimeLineChart.class.getResource("description_"+Language.getCurrentLanguage()+"/"+topic+".html");
		addDescription(url,helpTopic->Help.topic(getViewer(false),helpTopic));
	}

	/**
	 * Erzeugt ein Linien-Diagramm
	 * @param title	Titel
	 * @param indicator	Darzustellende Verteilungen
	 * @param xLabel	Beschriftung der x-Achse
	 * @param colorMap	Farben f�r die Linien
	 */
	private void requestDiagrammTimeDistribution(final String title, StatisticsMultiPerformanceIndicator indicator, final String xLabel, final Map<String,Color> colorMap) {
		initLineChart(title);
		setupChartTimePercent(title,xLabel,Language.tr("Statistics.Part"));

		final String[] names=indicator.getNames();
		final StatisticsDataPerformanceIndicator[] indicators=indicator.getAll(StatisticsDataPerformanceIndicator.class);

		for (int i=0;i<Math.min(names.length,MAX_SERIES);i++) {
			Color color=null;
			if (colorMap!=null) color=colorMap.get(names[i]);
			if (color==null) color=COLORS[i%COLORS.length];

			final DataDistributionImpl dist=indicators[i].getNormalizedDistribution();
			if (dist!=null) addSeriesTruncated(title+" - "+names[i],color,dist,1800);
		}

		smartZoom(1);
	}

	/**
	 * Erzeugt ein Linien-Diagramm; nimmt keine leeren Datens�tze auf.
	 * @param title	Titel
	 * @param indicator	Darzustellende Verteilungen
	 * @param xLabel	Beschriftung der x-Achse
	 * @param colorMap	Farben f�r die Linien
	 */
	private void requestDiagrammTimeDistributionNoEmpty(final String title, StatisticsMultiPerformanceIndicator indicator, final String xLabel, final Map<String,Color> colorMap) {
		initLineChart(title);
		setupChartTimePercent(title,xLabel,Language.tr("Statistics.Part"));

		final String[] names=indicator.getNames();
		final StatisticsDataPerformanceIndicator[] indicators=indicator.getAll(StatisticsDataPerformanceIndicator.class);

		for (int i=0;i<Math.min(names.length,MAX_SERIES);i++) {
			Color color=null;
			if (colorMap!=null) color=colorMap.get(names[i]);
			if (color==null) color=COLORS[i%COLORS.length];
			if (indicators[i].getCount()==0) continue;

			final DataDistributionImpl dist=indicators[i].getNormalizedDistribution();
			if (dist!=null) addSeriesTruncated(title+" - "+names[i],color,dist,1800);
		}

		smartZoom(1);
	}

	/**
	 * Ist eine Verteilung auf 0 konzentriert?
	 * @param dist	Zu pr�fende Verteilung
	 * @return	Verteilung auf 0 konzentriert?
	 */
	private boolean isDistNull(final DataDistributionImpl dist) {
		if (dist==null) return true;
		if (dist.densityData==null || dist.densityData.length<2) return true;
		for (int i=1;i<dist.densityData.length;i++) if (dist.densityData[i]>0) return false;
		return true;
	}

	/**
	 * Erzeugt ein Zustands-Linien-Diagramm
	 * @param title	Titel
	 * @param indicator	Darzustellende Verteilungen
	 * @param system	Darzustellende Gesamt-Verteilungen (kann <code>null</code> sein)
	 * @param xLabel	Beschriftung der x-Achse
	 * @param colorMap	Farben f�r die Linien
	 */
	private void requestDiagrammStateDistribution(final String title, StatisticsMultiPerformanceIndicator indicator, final StatisticsTimePerformanceIndicator system, final String xLabel, final Map<String,Color> colorMap) {
		initLineChart(title);
		setupChartValuePercent(title,xLabel,Language.tr("Statistics.Part"));

		final String[] names=indicator.getNames();
		final StatisticsPerformanceIndicator[] indicators=indicator.getAll();

		DataDistributionImpl dist;

		if (system!=null) {
			dist=system.getNormalizedDistribution();
			addSeries(Language.tr("Statistics.System"),Color.BLACK,dist);
		}

		int colorIndex=0;
		for (int i=0;i<Math.min(names.length,MAX_SERIES);i++) {
			Color color=null;
			if (colorMap!=null) color=colorMap.get(names[i]);
			if (color==null) color=COLORS[colorIndex%COLORS.length];
			if (indicators[i] instanceof StatisticsTimePerformanceIndicator) {
				dist=((StatisticsTimePerformanceIndicator)indicators[i]).getNormalizedDistribution();
				if (isDistNull(dist)) continue;
				colorIndex++;
				addSeries(names[i],color,dist);
			}
			if (indicators[i] instanceof StatisticsDataPerformanceIndicator) {
				dist=((StatisticsDataPerformanceIndicator)indicators[i]).getNormalizedDistribution();
				if (dist!=null) {
					if (isDistNull(dist)) continue;
					colorIndex++;
					addSeries(names[i],color,dist);
				}
			}
			if (indicators[i] instanceof StatisticsDataPerformanceIndicatorWithNegativeValues) {
				dist=((StatisticsDataPerformanceIndicatorWithNegativeValues)indicators[i]).getNormalizedDistribution();
				if (dist!=null) {
					if (isDistNull(dist)) continue;
					colorIndex++;
					addSeries(names[i],color,dist);
				}
			}
		}

		smartZoom(1);
	}

	/**
	 * Generiert ein Verteilungsdiagramm der Werte der Laufzeitstatistik.
	 * @param title	Titel
	 * @param indicator	Datenreihen
	 */
	private void requestDiagrammSpecialDistribution(final String title, StatisticsMultiPerformanceIndicator indicator) {
		final String[] names=indicator.getNames();
		final StatisticsLongRunPerformanceIndicator[] indicators=indicator.getAll(StatisticsLongRunPerformanceIndicator.class);
		final DataDistributionImpl[] dist=Stream.of(indicators).map(in->in.getDistribution()).toArray(DataDistributionImpl[]::new);

		double maxLength=0;
		for (int i=0;i<Math.min(names.length,MAX_SERIES);i++) maxLength=Math.max(maxLength,dist[i].upperBound);


		double scale=1.0;
		String unit=Language.tr("Statistics.InSeconds");
		if (maxLength>=1800) {
			scale*=1.0/60.0;
			maxLength/=60.0;
			unit=Language.tr("Statistics.InMinutes");
		}
		if (maxLength>=1800) {
			scale*=1.0/60.0;
			maxLength/=60.0;
			unit=Language.tr("Statistics.InHours");
		}
		if (maxLength>=1440) {
			scale*=1.0/24.0;
			maxLength/=24.0;
			unit=Language.tr("Statistics.InDays");
		}


		initLineChart(title);
		setupChartTimeValue(title,Language.tr("Statistic.Viewer.Chart.Time"),unit,Language.tr("Statistic.Viewer.Chart.Value"));

		for (int i=0;i<Math.min(names.length,MAX_SERIES);i++) {
			final Color color=COLORS[i%COLORS.length];
			final DataDistributionImpl showDist;
			if (scale==1.0) {
				showDist=dist[i];
			} else {
				showDist=new DataDistributionImpl(dist[i].upperBound*scale,dist[i].densityData);
			}
			addSeries(names[i],color,showDist);
		}

		smartZoom(1);
	}

	/**
	 * Generiert ein Verteilungsdiagramm der an den Datenaufzeichnung-Stationen erfassten Werte.
	 * @param title	Titel
	 * @param name	Name der Datenreihe
	 * @param values	Werte
	 * @param count	Anzahl an Werten
	 */
	private void requestDiagrammDataCollection(final String title, final String name, final double[] values, final int count) {
		initLineChart(title);
		setupChartTimeValue(title,Language.tr("Statistic.Viewer.Chart.Time"),Language.tr("Statistic.Viewer.Chart.Value"));

		addSeries(name,Color.BLUE,values,count);

		smartZoom(1);
	}

	@Override
	protected void firstChartRequest() {
		Map<String,Color> colorMap;

		switch (mode) {
		case MODE_INTERARRIVAL_CLIENTS:
			colorMap=statistics.editModel.clientData.getStatisticColors(statistics.editModel.surface.getClientTypes());
			requestDiagrammTimeDistribution(Language.tr("Statistics.DistributionOfTheInterArrivalTimes"),statistics.clientsInterarrivalTime,Language.tr("Statistics.Distance"),colorMap);
			addDescription("PlotTimeDistribution");
			break;
		case MODE_INTERARRIVAL_STATION:
			requestDiagrammTimeDistribution(Language.tr("Statistics.DistributionOfTheInterArrivalTimes"),statistics.stationsInterarrivalTime,Language.tr("Statistics.Distance"),null);
			addDescription("PlotTimeDistribution");
			break;
		case MODE_INTERARRIVAL_STATION_BATCH:
			requestDiagrammTimeDistributionNoEmpty(Language.tr("Statistics.DistributionOfTheInterArrivalTimesBatch"),statistics.stationsInterarrivalTimeBatch,Language.tr("Statistics.Distance"),null);
			addDescription("PlotTimeDistribution");
			break;
		case MODE_INTERARRIVAL_STATION_CLIENTS:
			requestDiagrammTimeDistribution(Language.tr("Statistics.DistributionOfTheInterArrivalTimes"),statistics.stationsInterarrivalTimeByClientType,Language.tr("Statistics.Distance"),null);
			addDescription("PlotTimeDistribution");
			break;
		case MODE_INTERARRIVAL_STATION_STATES:
			requestDiagrammTimeDistribution(Language.tr("Statistics.DistributionOfTheInterArrivalTimes"),statistics.stationsInterarrivalTimeByState,Language.tr("Statistics.Distance"),null);
			addDescription("PlotTimeDistribution");
			break;
		case MODE_INTERLEAVE_CLIENTS:
			colorMap=statistics.editModel.clientData.getStatisticColors(statistics.editModel.surface.getClientTypes());
			requestDiagrammTimeDistribution(Language.tr("Statistics.DistributionOfTheInterLeaveTimes"),statistics.clientsInterleavingTime,Language.tr("Statistics.Distance"),colorMap);
			addDescription("PlotTimeDistribution");
			break;
		case MODE_INTERLEAVE_STATION:
			requestDiagrammTimeDistribution(Language.tr("Statistics.DistributionOfTheInterLeaveTimes"),statistics.stationsInterleavingTime,Language.tr("Statistics.Distance"),null);
			addDescription("PlotTimeDistribution");
			break;
		case MODE_INTERLEAVE_STATION_BATCH:
			requestDiagrammTimeDistribution(Language.tr("Statistics.DistributionOfTheInterLeaveTimesBatch"),statistics.stationsInterleavingTimeBatch,Language.tr("Statistics.Distance"),null);
			addDescription("PlotTimeDistribution");
			break;
		case MODE_INTERLEAVE_STATION_CLIENTS:
			requestDiagrammTimeDistribution(Language.tr("Statistics.DistributionOfTheInterLeaveTimes"),statistics.stationsInterleavingTimeByClientType,Language.tr("Statistics.Distance"),null);
			addDescription("PlotTimeDistribution");
			break;
		case MODE_WAITING_CLIENTS:
			colorMap=statistics.editModel.clientData.getStatisticColors(statistics.editModel.surface.getClientTypes());
			requestDiagrammTimeDistribution(Language.tr("Statistics.DistributionOfTheWaitingTimes"),statistics.clientsWaitingTimes,Language.tr("Statistics.WaitingTime"),colorMap);
			addDescription("PlotTimeDistribution");
			break;
		case MODE_TRANSFER_CLIENTS:
			colorMap=statistics.editModel.clientData.getStatisticColors(statistics.editModel.surface.getClientTypes());
			requestDiagrammTimeDistribution(Language.tr("Statistics.DistributionOfTheTransferTimes"),statistics.clientsTransferTimes,Language.tr("Statistics.TransferTime"),colorMap);
			addDescription("PlotTimeDistribution");
			break;
		case MODE_PROCESSING_CLIENTS:
			colorMap=statistics.editModel.clientData.getStatisticColors(statistics.editModel.surface.getClientTypes());
			requestDiagrammTimeDistribution(Language.tr("Statistics.DistributionOfTheProcessTimes"),statistics.clientsProcessingTimes,Language.tr("Statistics.ProcessTime"),colorMap);
			addDescription("PlotTimeDistribution");
			break;
		case MODE_RESIDENCE_CLIENTS:
			colorMap=statistics.editModel.clientData.getStatisticColors(statistics.editModel.surface.getClientTypes());
			requestDiagrammTimeDistribution(Language.tr("Statistics.DistributionOfTheResidenceTimes"),statistics.clientsResidenceTimes,Language.tr("Statistics.ResidenceTime"),colorMap);
			addDescription("PlotTimeDistribution");
			break;
		case MODE_WAITING_STATION:
			requestDiagrammTimeDistribution(Language.tr("Statistics.DistributionOfTheWaitingTimes"),statistics.stationsWaitingTimes,Language.tr("Statistics.WaitingTime"),null);
			addDescription("PlotTimeDistribution");
			break;
		case MODE_TRANSFER_STATION:
			requestDiagrammTimeDistribution(Language.tr("Statistics.DistributionOfTheTransferTimes"),statistics.stationsTransferTimes,Language.tr("Statistics.TransferTime"),null);
			addDescription("PlotTimeDistribution");
			break;
		case MODE_PROCESSING_STATION:
			requestDiagrammTimeDistribution(Language.tr("Statistics.DistributionOfTheProcessTimes"),statistics.stationsProcessingTimes,Language.tr("Statistics.ProcessTime"),null);
			addDescription("PlotTimeDistribution");
			break;
		case MODE_RESIDENCE_STATION:
			requestDiagrammTimeDistribution(Language.tr("Statistics.DistributionOfTheResidenceTimes"),statistics.stationsResidenceTimes,Language.tr("Statistics.ResidenceTime"),null);
			addDescription("PlotTimeDistribution");
			break;
		case MODE_SETUP_STATION:
			requestDiagrammTimeDistribution(Language.tr("Statistics.DistributionOfTheSetupTimes"),statistics.stationsSetupTimes,Language.tr("Statistics.SetupTime"),null);
			addDescription("PlotTimeDistribution");
			break;
		case MODE_WAITING_STATION_TOTAL:
			requestDiagrammTimeDistribution(Language.tr("Statistics.DistributionOfTheWaitingTimesTotal"),statistics.stationsTotalWaitingTimes,Language.tr("Statistics.WaitingTime"),null);
			addDescription("PlotTimeDistribution");
			break;
		case MODE_TRANSFER_STATION_TOTAL:
			requestDiagrammTimeDistribution(Language.tr("Statistics.DistributionOfTheTransferTimesTotal"),statistics.stationsTotalTransferTimes,Language.tr("Statistics.TransferTime"),null);
			addDescription("PlotTimeDistribution");
			break;
		case MODE_PROCESSING_STATION_TOTAL:
			requestDiagrammTimeDistribution(Language.tr("Statistics.DistributionOfTheProcessTimesTotal"),statistics.stationsTotalProcessingTimes,Language.tr("Statistics.ProcessTime"),null);
			addDescription("PlotTimeDistribution");
			break;
		case MODE_RESIDENCE_STATION_TOTAL:
			requestDiagrammTimeDistribution(Language.tr("Statistics.DistributionOfTheResidenceTimesTotal"),statistics.stationsTotalResidenceTimes,Language.tr("Statistics.ResidenceTime"),null);
			addDescription("PlotTimeDistribution");
			break;
		case MODE_WAITING_STATION_CLIENT:
			colorMap=statistics.editModel.clientData.getStatisticColors(statistics.editModel.surface.getClientTypes());
			requestDiagrammTimeDistribution(Language.tr("Statistics.DistributionOfTheWaitingTimes"),statistics.stationsWaitingTimesByClientType,Language.tr("Statistics.WaitingTime"),colorMap);
			addDescription("PlotTimeDistribution");
			break;
		case MODE_TRANSFER_STATION_CLIENT:
			colorMap=statistics.editModel.clientData.getStatisticColors(statistics.editModel.surface.getClientTypes());
			requestDiagrammTimeDistribution(Language.tr("Statistics.DistributionOfTheTransferTimes"),statistics.stationsTransferTimesByClientType,Language.tr("Statistics.TransferTime"),colorMap);
			addDescription("PlotTimeDistribution");
			break;
		case MODE_PROCESSING_STATION_CLIENT:
			colorMap=statistics.editModel.clientData.getStatisticColors(statistics.editModel.surface.getClientTypes());
			requestDiagrammTimeDistribution(Language.tr("Statistics.DistributionOfTheProcessTimes"),statistics.stationsProcessingTimesByClientType,Language.tr("Statistics.ProcessTime"),colorMap);
			addDescription("PlotTimeDistribution");
			break;
		case MODE_RESIDENCE_STATION_CLIENT:
			colorMap=statistics.editModel.clientData.getStatisticColors(statistics.editModel.surface.getClientTypes());
			requestDiagrammTimeDistribution(Language.tr("Statistics.DistributionOfTheResidenceTimes"),statistics.stationsResidenceTimesByClientType,Language.tr("Statistics.ResidenceTime"),colorMap);
			addDescription("PlotTimeDistribution");
			break;
		case MODE_NUMBER_STATION:
			requestDiagrammStateDistribution(Language.tr("Statistics.DistributionOfNumberOfClientsAtStations")+" ("+Language.tr("Statistics.total")+")",statistics.clientsAtStationByStation,statistics.clientsInSystem,Language.tr("Statistics.ClientsAtStation"),null);
			addDescription("PlotCountDistribution");
			break;
		case MODE_NUMBER_STATION_CLIENT_TYPES:
			colorMap=statistics.editModel.clientData.getStatisticColors(statistics.editModel.surface.getClientTypes());
			requestDiagrammStateDistribution(Language.tr("Statistics.DistributionOfNumberOfClientsAtStationsByClientTypes")+" ("+Language.tr("Statistics.total")+")",statistics.clientsAtStationByStationAndClient,null,Language.tr("Statistics.ClientsAtStation"),colorMap);
			addDescription("PlotCountDistribution");
			break;
		case MODE_NUMBER_CLIENT:
			colorMap=statistics.editModel.clientData.getStatisticColors(statistics.editModel.surface.getClientTypes());
			requestDiagrammStateDistribution(Language.tr("Statistics.DistributionOfNumberOfClientsByType")+" ("+Language.tr("Statistics.total")+")",statistics.clientsInSystemByClient,statistics.clientsInSystem,Language.tr("Statistics.ClientsByType"),colorMap);
			addDescription("PlotCountDistribution");
			break;
		case MODE_QUEUE:
			requestDiagrammStateDistribution(Language.tr("Statistics.DistributionOfNumberOfClientsAtStationQueues"),statistics.clientsAtStationQueueByStation,statistics.clientsInSystemQueues,Language.tr("Statistics.ClientsInQueue"),null);
			addDescription("PlotCountDistribution");
			break;
		case MODE_QUEUE_CLIENT_TYPE:
			colorMap=statistics.editModel.clientData.getStatisticColors(statistics.editModel.surface.getClientTypes());
			requestDiagrammStateDistribution(Language.tr("Statistics.DistributionOfNumberOfClientsAtStationQueuesByClientTypes"),statistics.clientsAtStationQueueByStationAndClient,null,Language.tr("Statistics.ClientsInQueue"),colorMap);
			addDescription("PlotCountDistribution");
			break;
		case MODE_PROCESS:
			requestDiagrammStateDistribution(Language.tr("Statistics.DistributionOfNumberOfClientsAtStationProcess"),statistics.clientsAtStationProcessByStation,statistics.clientsInSystemProcess,Language.tr("Statistics.ClientsInProcess"),null);
			addDescription("PlotCountDistribution");
			break;
		case MODE_PROCESS_CLIENT_TYPE:
			colorMap=statistics.editModel.clientData.getStatisticColors(statistics.editModel.surface.getClientTypes());
			requestDiagrammStateDistribution(Language.tr("Statistics.DistributionOfNumberOfClientsAtStationProcessByClientTypes"),statistics.clientsAtStationProcessByStationAndClient,null,Language.tr("Statistics.ClientsInProcess"),colorMap);
			addDescription("PlotCountDistribution");
			break;
		case MODE_ADDITIONAL_STATISTICS:
			requestDiagrammSpecialDistribution(Language.tr("Statistics.AdditionalStatistics"),statistics.longRunStatistics);
			addDescription("PlotAdditionalStatistics");
			break;
		case MODE_CLIENT_DATA_DISTRIBUTION:
			requestDiagrammStateDistribution(Language.tr("Statistics.ClientData.Distribution")+" ("+Language.tr("Statistics.total")+")",statistics.clientData,null,Language.tr("Statistics.Value"),null);
			plot.getDomainAxis().setLabel(Language.tr("Statistics.Value"));
			addDescription("PlotClientDataDistribution");
			break;
		case MODE_CLIENT_DATA_DISTRIBUTION_BY_CLIENT_TYPES:
			requestDiagrammStateDistribution(Language.tr("Statistics.ClientData.Distribution")+" ("+Language.tr("Statistics.total")+")",statistics.clientDataByClientTypes,null,Language.tr("Statistics.Value"),null);
			plot.getDomainAxis().setLabel(Language.tr("Statistics.Value"));
			addDescription("PlotClientDataDistribution");
			break;
		case MODE_VALUE_RECORDING:
			final StatisticsDataCollector collector=((StatisticsDataCollector)statistics.valueRecording.get(data+"-1"));
			requestDiagrammDataCollection(data,data,collector.getValuesReadOnly(),collector.getCount());
			plot.getDomainAxis().setLabel(Language.tr("Statistics.RecordedValue"));
			addDescription("PlotX");
			break;
		}

		initTooltips();
	}
}