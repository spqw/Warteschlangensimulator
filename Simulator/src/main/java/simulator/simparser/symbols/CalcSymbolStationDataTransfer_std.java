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
import statistics.StatisticsPerformanceIndicator;

/**
 * Im Falle von einem Parameter:<br>
 * (a) Liefert die Standardabweichung der Transferzeiten der Kunden, deren Name an Quelle bzw. Namenszuweisung id (1. Parameter) auftritt (bezogen auf Sekunden).<br>
 * (b) Liefert die Standardabweichung der Transferzeiten der Kunden an Station id (1. Parameter) (bezogen auf Sekunden).<br>
 * Im Falle von keinem Parameter:<br>
 * Liefert die Standardabweichung der Transferzeiten �ber alle Kunden (bezogen auf Sekunden).
 * @author Alexander Herzog
 */
public class CalcSymbolStationDataTransfer_std extends CalcSymbolStationData {
	/**
	 * Namen f�r das Symbol
	 * @see #getNames()
	 */
	private static final String[] names=new String[]{
			"Transferzeit_std","Transferzeit_sd","Transferzeit_Standardabweichung",
			"TransferTime_std","TransferTime_sd","TransferTime_Standardabweichung",
	};

	/**
	 * Konstruktor der Klasse
	 */
	public CalcSymbolStationDataTransfer_std() {
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
	protected double calc(final RunElementData data) {
		if (data.statisticTransfer==null) return 0;
		return data.statisticTransfer.getSD();
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
		return getSimData().statistics.clientsAllTransferTimes.getSD();
	}

	@Override
	protected double calcSingleClient(final String name) {
		StatisticsPerformanceIndicator indicator=getSimData().statistics.clientsTransferTimes.get(name);
		if (indicator==null) return 0.0;
		return ((StatisticsDataPerformanceIndicator)indicator).getSD();
	}
}
