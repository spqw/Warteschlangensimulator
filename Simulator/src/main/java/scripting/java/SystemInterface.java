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
package scripting.java;

import java.util.Map;

/**
 * Teil-Interface, damit Nutzer-Java-Codes auf simulationsabh�ngige Daten zugreifen kann.
 * @author Alexander Herzog
 * @see SimulationInterface
 */
public interface SystemInterface {
	/**
	 * Berechnet einen Ausdruck im Kontext der Simulation (aber nicht im Kontext eines bestimmten Kunden).
	 * @param expression	Zu berechnender Ausdruck.
	 * @return	Liefert im Erfolgsfall ein {@link Double}-Objekt. Im Fehlerfall eine Fehlermeldung.
	 */
	Object calc(final String expression);

	/**
	 * Liefert die aktuelle Zeit in der Simulation als Sekundenwert
	 * @return	Zeit in der Simulation (in Sekunden)
	 */
	double getTime();

	/**
	 * Gibt an, ob sich die Simulation noch in der Einschwingphase befindet.
	 * @return	Gibt <code>true</code> zur�ck, wenn sich das System noch in der Einschwingphase befindet
	 */
	boolean isWarmUp();

	/**
	 * Liefert die Anzahl an Kunden an einer Station.
	 * @param id	ID der Station
	 * @return	Anzahl an Kunden an der Station
	 */
	int getWIP(final int id);

	/**
	 * Liefert die Anzahl an Kunden an einer Station.
	 * @param stationName Name der Station (identisch zu den Namen, die in $("...") Befehlen in Rechenausdr�cken verwendet werden k�nnen)
	 * @return Anzahl an Kunden an der Station
	 */
	int getWIP(final String stationName);

	/**
	 * Liefert die Anzahl an Kunden in der Warteschlange an einer Station
	 * @param id	ID der Station
	 * @return	Anzahl an Kunden in der Warteschlange an der Station
	 */
	int getNQ(final int id);

	/**
	 * Liefert die Anzahl an Kunden in der Warteschlange an einer Station.
	 * @param stationName Name der Station (identisch zu den Namen, die in $("...") Befehlen in Rechenausdr�cken verwendet werden k�nnen)
	 * @return Anzahl an Kunden in der Warteschlange an der Station
	 */
	int getNQ(final String stationName);

	/**
	 * Liefert die Anzahl an Kunden in Bedienung an einer Station
	 * @param id ID der Station
	 * @return Anzahl an Kunden in Bedienung an der Station
	 */
	int getNS(final int id);

	/**
	 * Liefert die Anzahl an Kunden in Bedienung an einer Station.
	 * @param stationName Name der Station (identisch zu den Namen, die in $("...") Befehlen in Rechenausdr�cken verwendet werden k�nnen)
	 * @return Anzahl an Kunden in Bedienung an der Station
	 */
	int getNS(final String stationName);

	/**
	 * Liefert die Anzahl an Kunden an allen Stationen zusammen
	 * @return	Anzahl an Kunden an allen Stationen zusammen
	 */
	int getWIP();

	/**
	 * Liefert die Anzahl an Kunden in der Warteschlange an allen Stationen zusammen
	 * @return	Anzahl an Kunden in der Warteschlange an allen Stationen zusammen
	 */
	int getNQ();

	/**
	 * Liefert die Anzahl an Kunden in Bedienung an allen Stationen zusammen
	 * @return	Liefert die Anzahl an Kunden in Bedienung an allen Stationen zusammen
	 */
	int getNS();

	/**
	 * Setzt den Wert einer Simulator-Variable. Die Variable muss bereits existieren, sonst erfolgt keine Zuweisung.
	 * @param varName	Name der Variable
	 * @param varValue	Neuer Wert (Integer, Double oder String, der dann zun�chst interpretiert wird)
	 */
	void set(final String varName, final Object varValue);

	/**
	 * Stellt den aktuellen Wert an einem "Analoger Wert"- oder "Tank"-Element ein.
	 * @param elementID	ID des Elements an dem der Wert eingestellt werden soll
	 * @param value	Neuer Wert (Zahl oder berechenbarer Ausdruck)
	 */
	void setAnalogValue(final Object elementID, final Object value);

	/**
	 * Stellt die aktuelle �nderungsrate an einem "Analoger Wert"-Element ein.
	 * @param elementID	ID des Elements an dem die �nderungsrate eingestellt werden soll
	 * @param value	Neuer Wert (Zahl oder berechenbarer Ausdruck)
	 */
	void setAnalogRate(final Object elementID, final Object value);

	/**
	 * Stellt den maximalen Durchfluss an einem Ventil eines Tanks ein
	 * @param elementID	ID des Tanks an dem der maximale Durchfluss an einem Ventil eingestellt werden soll
	 * @param valveNr	1-basierte Nummer des Ventils
	 * @param value	Neuer Wert (Zahl oder berechenbarer Ausdruck)
	 */
	void setAnalogValveMaxFlow(final Object elementID, final Object valveNr, final Object value);

	/**
	 * Liefert die Anzahl an vorhandenen Bedienern �ber alle Bedienergruppen.
	 * @return	Anzahl an vorhandenen Bedienern �ber alle Bedienergruppen
	 */
	int getAllResourceCount();

	/**
	 * Liefert die Anzahl an vorhandenen Bedienern in einer bestimmten Bedienergruppe.
	 * @param resourceId	1-basierende ID der Bedienergruppe
	 * @return	Anzahl an vorhandenen Bedienern
	 */
	int getResourceCount(final int resourceId);

	/**
	 * Stellt die Anzahl an vorhandenen Bedienern in einer bestimmten Bedienergruppe ein.
	 * @param resourceId	1-basierende ID der Bedienergruppe
	 * @param count	Anzahl an vorhandenen Bedienern
	 * @return	Liefert <code>true</code> zur�ck, wenn die Anzahl ver�ndert werden konnte.
	 */
	boolean setResourceCount(final int resourceId, final int count);

	/**
	 * Gibt an, wie viele Bediener eines bestimmten Typs zu einem Zeitpunkt in Ausfallzeit sind
	 * @param resourceId	1-basierender Index der Bedienergruppe
	 * @return	Anzahl an Bedienern
	 */
	int getResourceDown(final int resourceId);

	/**
	 * Gibt an, wie viele Bediener zu einem Zeitpunkt insgesamt in Ausfallzeit sind
	 * @return	Anzahl an Bedienern
	 */
	int getAllResourceDown();

	/**
	 * Liefert den Namen des Kundentypen, der als letztes an der Bedienstation bedient wurde.
	 * @param id	ID der Bedienstation
	 * @return	Name des Kundentypen oder ein leerer String, wenn die Bedienstation noch keinen Kunden bedient hat, Batch-Bedienungen durchf�hrt oder die ID nicht zu einer Bedienstation geh�rt
	 */
	String getLastClientTypeName(final int id);

	/**
	 * L�st ein Signal aus.
	 * @param signalName	Name des Signal
	 */
	void signal(final String signalName);

	/**
	 * Registriert ein Ereignis zur sp�teren Ausf�hrung des Skripts an einer Station.
	 * @param stationId	ID der Skript- oder der Skript-Bedingung-Station, an der die Verarbeitung ausgel�st werden soll
	 * @param time	Zeitpunkt der Skriptausf�hrung
	 * @return	Liefert <code>true</code>, wenn ein entsprechendes Ereignis in die Ereignisliste aufgenommen werden konnte
	 */
	boolean triggerScriptExecution(final int stationId, final double time);

	/**
	 * Ruft eine Methode in einer Klassendatei, die im Plugins-Ordner liegt, auf.
	 * @param className	Name der Klassendatei (ohne Dateinamenserweiterung) bzw. Name der Klasse
	 * @param functionName	Name der aufzurufenden Methode innerhalb der Klasse
	 * @param data	Zus�tzliche Daten, die als Parameter an die Methode �bergeben werden
	 * @return	R�ckgabewert der Methode
	 */
	Object runPlugin(final String className, final String functionName, final Object data);

	/**
	 * Erfasst eine Meldung in der Logging-Ausgabe.
	 * @param obj	Zu erfassende Meldung
	 */
	void log(final Object obj);

	/**
	 * Liefert die Liste der Kunden an einer Verz�gerung-Station.
	 * @param id	ID der Verz�gerung-Station
	 * @return	Liste der Kunden an der Station oder <code>null</code>, wenn keine Kundenliste ermittelt werden konnte
	 */
	ClientsInterface getDelayStationData(final int id);

	/**
	 * Liefert die Liste der Kunden in der Warteschlange an einer Bedienstation.
	 * @param id	ID der Bedienstation
	 * @return	Liste der Kunden an der Station oder <code>null</code>, wenn keine Kundenliste ermittelt werden konnte
	 */
	ClientsInterface getProcessStationQueueData(final int id);

	/**
	 * Liefert das Stations-lokales Datenobjekt f�r Skript-Daten.
	 * @return	Stations-lokales Datenobjekt f�r Skript-Daten
	 */
	Map<String,Object> getMapLocal();

	/**
	 * Liefert das globale Datenobjekt f�r �ber alle Stationen hinweg gemeinsam genutzte Skript-Daten.
	 * @return	Globales Datenobjekt f�r �ber alle Stationen hinweg gemeinsam genutzte Skript-Daten
	 */
	Map<String,Object> getMapGlobal();

	/**
	 * Beendet die Simulation sofort.
	 * @param message	Optionale Nachricht, die als Fehlermeldung ausgegeben wird (kann <code>null</code> sein, dann wird die Simulation ohne Fehler beendet).
	 */
	void terminateSimulation(final String message);

	/**
	 * Pausiert die aktuelle Animation.
	 * (Setzt voraus, dass das Modell im Animationsmodus l�uft
	 * und die Animation momentan nicht sowieso schon im
	 * Einzelschritt-Modus l�uft.)
	 */
	void pauseAnimation();
}