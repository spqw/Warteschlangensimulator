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
package simulator.simparser.symbols;

import simulator.coreelements.RunElementData;
import simulator.simparser.coresymbols.CalcSymbolStationData;
import statistics.StatisticsDataPerformanceIndicator;

/**
 * Im Falle von einem Parameter:<br>
 * (a) Liefert den Median der Bedienzeiten der Kunden, deren Name an Quelle bzw. Namenszuweisung id (1. Parameter) auftritt (in Sekunden).<br>
 * (b) Liefert den Median der Bedienzeiten der Kunden an Station id (1. Parameter) (in Sekunden).<br>
 * Im Falle von keinem Parameter:<br>
 * Liefert den Median der Bedienzeiten �ber alle Kunden (in Sekunden).
 * @author Alexander Herzog
 */
public class CalcSymbolStationDataProcess_median extends CalcSymbolStationData {
	/**
	 * Namen f�r das Symbol
	 * @see #getNames()
	 */
	private static final String[] names=new String[] {"Bedienzeit_median","ProcessTime_median","ProcessingTime_median","ServiceTime_median"};

	@Override
	public String[] getNames() {
		return names;
	}

	/**
	 * Konstruktor der Klasse
	 */
	public CalcSymbolStationDataProcess_median() {
		/*
		 * Wird nur ben�tigt, um einen JavaDoc-Kommentar f�r diesen (impliziten) Konstruktor
		 * setzen zu k�nnen, damit der JavaDoc-Compiler keine Warnung mehr ausgibt.
		 */
	}

	@Override
	protected double calc(final RunElementData data) {
		if (data.statisticProcess==null) return 0;
		return data.statisticProcess.getMedian();
	}

	@Override
	protected boolean hasAllData() {
		return true;
	}

	@Override
	protected boolean hasSingleClientData() {
		return true;
	}

	@Override
	protected double calcAll() {
		return getSimData().statistics.clientsAllProcessingTimes.getMedian();
	}

	@Override
	protected double calcSingleClient(final String name) {
		final StatisticsDataPerformanceIndicator indicator=getClientDataIndicator(name,getSimData().statistics.clientsProcessingTimes);
		return (indicator==null)?0.0:indicator.getMedian();
	}

	@Override
	protected boolean hasStationAndClientData() {
		return true;
	}

	@Override
	protected double calcStationClient(final RunElementData data, final int clientTypeIndex) {
		if (data.statisticProcessByClientType==null) return 0;
		return data.statisticProcessByClientType[clientTypeIndex].getMedian();
	}
}
