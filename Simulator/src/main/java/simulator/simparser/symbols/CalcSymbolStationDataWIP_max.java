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
import statistics.StatisticsTimePerformanceIndicator;

/**
 * Im Falle von einem Parameter:<br>
 * (a) Liefert die maximale Anzahl an Kunden, deren Name an Quelle bzw. Namenszuweisung id (1. Parameter) auftritt.<br>
 * (b) Liefert die maximale Anzahl an Kunden an Station id (1. Parameter).<br>
 * Im Falle von keinem Parameter:<br>
 * Liefert die maximale Anzahl an Kunden im System.
 * @author Alexander Herzog
 */
public class CalcSymbolStationDataWIP_max extends CalcSymbolStationData {
	/**
	 * Namen f�r das Symbol
	 * @see #getNames()
	 */
	private static final String[] names=new String[]{"WIP_max","Station_max","N_max","WIP_Maximum","Station_Maximum","N_Maximum"};

	/**
	 * Konstruktor der Klasse
	 */
	public CalcSymbolStationDataWIP_max() {
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
	protected boolean hasAllData() {
		return true;
	}

	@Override
	protected boolean hasSingleClientData() {
		return true;
	}

	@Override
	protected double calcAll() {
		return getSimData().statistics.clientsInSystem.getTimeMax();
	}

	@Override
	protected double calcSingleClient(final String name) {
		final StatisticsTimePerformanceIndicator indicator=getClientTimeIndicator(name,getSimData().statistics.clientsInSystemByClient);
		return (indicator==null)?0.0:indicator.getTimeMax();
	}

	@Override
	protected double calc(final RunElementData data) {
		if (data.statisticClientsAtStation==null) return 0;
		return data.statisticClientsAtStation.getTimeMax();
	}
}