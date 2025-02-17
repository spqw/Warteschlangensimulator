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
package simulator.elements;

import java.util.Arrays;

import simulator.coreelements.RunElement;
import simulator.coreelements.RunElementData;
import simulator.simparser.ExpressionCalc;

/**
 * Laufzeitdaten eines <code>RunElementSourceExtern</code>-Laufzeit-Objekts
 * @author Alexander Herzog
 * @see RunElementSourceExtern
 * @see RunElementData
 */
public class RunElementSourceExternData extends RunElementData {
	/**
	 * N�chster zu verwendender Eintrag aus der jeweiligen Kundentypliste
	 */
	public int[] nextIndex;

	/**
	 * Objekt zur Berechnung von Ausdr�cken wenn m�glich wiederverwenden
	 */
	public ExpressionCalc calc;

	/**
	 * Konstruktor der Klasse <code>RunElementSourceData</code>
	 * @param station	Station zu diesem Datenelement
	 * @param typesCount	Gr��e des {@link #nextIndex} Arrays, d.h. Anzahl der verschiedenen Kundentypen an dieser Station
	 */
	public RunElementSourceExternData(final RunElement station, final int typesCount) {
		super(station);
		nextIndex=new int[typesCount];
		Arrays.fill(nextIndex,0);
	}
}