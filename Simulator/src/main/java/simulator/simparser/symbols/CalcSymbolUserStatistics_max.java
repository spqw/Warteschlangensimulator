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

import simulator.simparser.coresymbols.CalcSymbolUserStatistics;
import statistics.StatisticsDataPerformanceIndicatorWithNegativeValues;

/**
 * Liefert den Maximalwert des Statistikeintrags <code>nr</code> (2. Parameter) (1-basierend) an Statistik-Station id (1. Parameter).
 * @author Alexander Herzog
 */
public class CalcSymbolUserStatistics_max extends CalcSymbolUserStatistics {
	/**
	 * Namen f�r das Symbol
	 * @see #getNames()
	 */
	private static final String[] names=new String[] {"Statistik_max","Statistics_max","Statistik_Maximum","Statistics_Maximum"};

	@Override
	public String[] getNames() {
		return names;
	}

	@Override
	protected double processIndicator(StatisticsDataPerformanceIndicatorWithNegativeValues indicator) {
		return indicator.getMax();
	}
}
