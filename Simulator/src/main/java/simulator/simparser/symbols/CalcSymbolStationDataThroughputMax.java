/**
 * Copyright 2021 Alexander Herzog
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
import simulator.elements.RunElementThroughputData;
import simulator.runmodel.SimulationData;
import simulator.simparser.coresymbols.CalcSymbolStationData;

/**
 * Liefert den maximalen Durchsatz an einer Station.
 * @author Alexander Herzog
 * @see RunElementData#maxThroughput
 */
public class CalcSymbolStationDataThroughputMax extends CalcSymbolStationData {
	/**
	 * Namen f�r das Symbol
	 * @see #getNames()
	 */
	private static final String[] names=new String[]{"DurchsatzMax","ThroughputMax"};

	/**
	 * Konstruktor der Klasse
	 */
	public CalcSymbolStationDataThroughputMax() {
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
		if (data instanceof RunElementThroughputData) return ((RunElementThroughputData)data).getValue(true);

		final SimulationData simData=getSimData();
		if (simData==null) return 0.0;
		if (simData.runData.isWarmUp) return 0.0;

		if (data.maxThroughputIntervalLength<=0) return 0.0;
		return 1000.0*data.maxThroughput/data.maxThroughputIntervalLength;
	}
}
