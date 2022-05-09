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
import simulator.simparser.coresymbols.CalcSymbolStationDataQuantil;
import statistics.StatisticsDataPerformanceIndicator;
import statistics.StatisticsPerformanceIndicator;

/**
 * Im Falle von zwei Parametern:<br>
 * (a) Liefert das Quantil zur Wahrscheinlichkeit p (1. Parameter) der Bedienzeiten der Kunden, deren Name an Quelle bzw. Namenszuweisung id (2. Parameter) auftritt (in Sekunden).<br>
 * (b) Liefert das Quantil zur Wahrscheinlichkeit p (1. Parameter) der Bedienzeiten der Kunden an Station id (2. Parameter) (in Sekunden).<br>
 * Im Falle von einem Parameter:<br>
 * Liefert das Quantil zur Wahrscheinlichkeit p (1. Parameter) der Bedienzeiten �ber alle Kunden (in Sekunden).
 * @author Alexander Herzog
 */
public class CalcSymbolStationDataProcess_quantil extends CalcSymbolStationDataQuantil {
	/**
	 * Namen f�r das Symbol
	 * @see #getNames()
	 */
	private static final String[] names=new String[] {"Bedienzeit_quantil","ProcessTime_quantil","ProcessingTime_quantil","ServiceTime_quantil"};

	/**
	 * Konstruktor der Klasse
	 */
	public CalcSymbolStationDataProcess_quantil() {
		/*
		 * Wird nur ben�tigt, um einen JavaDoc-Kommentar f�r diesen (impliziten) Konstruktor
		 * setzen zu k�nnen, damit der JavaDoc-Compiler keine Warnung mehr ausgibt.
		 */
	}

	@Override
	public String[] getNames() {
		return names;
	}

	@Override
	protected double calc(final double p, final RunElementData data) {
		if (data.statisticProcess==null) return 0;
		return data.statisticProcess.getQuantil(p);
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
	protected double calcAll(final double p) {
		return getSimData().statistics.clientsAllProcessingTimes.getQuantil(p);
	}

	@Override
	protected double calcSingleClient(final double p, final String name) {
		StatisticsPerformanceIndicator indicator=getSimData().statistics.clientsProcessingTimes.get(name);
		if (indicator==null) return 0.0;
		return ((StatisticsDataPerformanceIndicator)indicator).getQuantil(p);
	}
}
