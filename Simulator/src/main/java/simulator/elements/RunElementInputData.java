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

import simulator.coreelements.RunElement;
import simulator.coreelements.RunElementData;

/**
 * Laufzeitdaten eines <code>RunElementInput</code>-Laufzeit-Objekts
 * @author Alexander Herzog
 * @see RunElementInput
 * @see RunElementData
 */
public class RunElementInputData extends RunElementData {
	/**
	 * Aktuelle Position in der Liste der Eingabewerte
	 */
	public int position;

	/**
	 * Konstruktor der Klasse <code>RunElementInputData</code>
	 * @param station	Zu dem Datenobjekt zugehöriges <code>RunElementInput</code>-Element
	 */
	public RunElementInputData(final RunElement station) {
		super(station);
		position=0;
	}
}
