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
package simulator.simparser.coresymbols;

import mathtools.distribution.DataDistributionImpl;
import parser.MathCalcError;
import simulator.coreelements.RunElement;
import simulator.coreelements.RunElementData;
import simulator.elements.RunElementAssign;
import simulator.elements.RunElementSource;
import simulator.runmodel.SimulationData;

/**
 * Basisklasse f�r Funktionen, die Histogramme f�r jeweils eine Station/einen Kundentyp ausgeben.
 * @author Alexander Herzog
 * @see SimulationData
 * @see CalcSymbolSimData
 */
public abstract class CalcSymbolStationDataHistogram extends CalcSymbolSimData {
	/**
	 * Liefert die Verteilung auf deren Basis das Histogramm f�r eine Station erstellt werden soll.
	 * @param data	Stationsdatenobjekt der Station f�r die das Histogramm erstellt werden soll
	 * @return	Verteilung auf deren Basis das Histogramm f�r eine Station erstellt werden soll
	 */
	protected abstract DataDistributionImpl getDistribution(final RunElementData data);

	/**
	 * Konstruktor der Klasse
	 */
	public CalcSymbolStationDataHistogram() {
		/*
		 * Wird nur ben�tigt, um einen JavaDoc-Kommentar f�r diesen (impliziten) Konstruktor
		 * setzen zu k�nnen, damit der JavaDoc-Compiler keine Warnung mehr ausgibt.
		 */
	}

	/**
	 * K�nnen Histogramme f�r einzelne Kundentypen erstellt werden?
	 * @return	Wird hier <code>true</code> zur�ckgegeben, so m�ssen die Methoden {@link #getDistributionForClientType(String)} und {@link #getDistributionSumForClientType(String)} implementiert werden.
	 */
	protected boolean hasSingleClientData() {
		return false;
	}

	/**
	 * Liefert die Verteilung auf deren Basis das Histogramm f�r einen Kundentyp erstellt werden soll.
	 * @param name	Name des Kundentyps
	 * @return	Verteilung auf deren Basis das Histogramm f�r einen Kundentyp erstellt werden soll
	 * @see #hasSingleClientData()
	 */
	protected DataDistributionImpl getDistributionForClientType(final String name) {
		return null;
	}

	/**
	 * Liefert die Summe �ber die Verteilung auf deren Basis das Histogramm f�r eine Station erstellt werden soll.
	 * @param data	Stationsdatenobjekt der Station f�r die das Histogramm erstellt werden soll
	 * @return	Summe der Verteilungswerte
	 */
	protected abstract double getDistributionSum(final RunElementData data);

	/**
	 * Liefert die Summe �ber die Verteilung auf deren Basis das Histogramm f�r einen Kundentyp erstellt werden soll.
	 * @param name	Name des Kundentyps
	 * @return	Summe der Verteilungswerte
	 * @see #hasSingleClientData()
	 */
	protected double getDistributionSumForClientType(final String name) {
		return 0.0;
	}

	/**
	 * ID der Station, f�r die zuletzt Daten abgefragt wurden
	 * @see #getDistributionByID(double)
	 * @see #getDistributionSumByID(double)
	 */
	private double lastDistributionId=-1;

	/**
	 * Kundentypnamen zu der zuletzt abgefragten ID
	 * @see #lastDistributionClientTypeName
	 * @see #getDistributionByID(double)
	 * @see #getDistributionSumByID(double)
	 */
	private String lastDistributionClientTypeName;

	/**
	 * Stationsdaten zu der zuletzt abgefragten ID
	 * @see #lastDistributionClientTypeName
	 * @see #getDistributionByID(double)
	 * @see #getDistributionSumByID(double)
	 */
	private RunElementData lastDistributionRunElement=null;

	/**
	 * Liefert die Verteilung auf Basis einer Stations-ID (entweder um eine Stations-Kenngr��e auszulesen oder um indirekt �ber eine Quelle einen Kundentyp zu identifizieren)
	 * @param id	Stations-ID
	 * @return	Verteilung auf deren Basis das Histogramm erstellt werden soll
	 */
	protected DataDistributionImpl getDistributionByID(final double id) {
		if (lastDistributionId==id) {
			if (lastDistributionRunElement!=null) return getDistribution(lastDistributionRunElement);
			if (lastDistributionClientTypeName!=null) return getDistributionForClientType(lastDistributionClientTypeName);
		}

		if (hasSingleClientData()) {
			final RunElement element=getRunElementForID(id);
			if (element==null) return null;
			String name=null;
			if (element instanceof RunElementSource) name=((RunElementSource)element).clientTypeName;
			if (element instanceof RunElementAssign) name=((RunElementAssign)element).clientTypeName;
			if (name!=null) {
				lastDistributionId=id;
				lastDistributionClientTypeName=name;
				return getDistributionForClientType(name);
			}
			/* name==null: Evtl. nicht pro Kundentyp sondern pro Station */
		}

		final RunElementData data=getRunElementDataForID(id);
		if (data==null) return null;
		lastDistributionId=id;
		lastDistributionRunElement=data;
		return getDistribution(data);
	}

	/**
	 * Liefert die Summe �ber die Verteilung auf Basis einer Stations-ID (entweder um eine Stations-Kenngr��e auszulesen oder um indirekt �ber eine Quelle einen Kundentyp zu identifizieren)
	 * @param id	Stations-ID
	 * @return	Summe der Verteilungswerte
	 */
	protected double getDistributionSumByID(final double id) {
		if (lastDistributionId==id) {
			if (lastDistributionRunElement!=null) return getDistributionSum(lastDistributionRunElement);
			if (lastDistributionClientTypeName!=null) return getDistributionSumForClientType(lastDistributionClientTypeName);
		}

		if (hasSingleClientData()) {
			final RunElement element=getRunElementForID(id);
			if (element==null) return 0.0;
			String name=null;
			if (element instanceof RunElementSource) name=((RunElementSource)element).clientTypeName;
			if (element instanceof RunElementAssign) name=((RunElementAssign)element).clientTypeName;
			if (name!=null) {
				lastDistributionId=id;
				lastDistributionClientTypeName=name;
				return getDistributionSumForClientType(name);
			}
			/* name==null: Evtl. nicht pro Kundentyp sondern pro Station */
		}

		final RunElementData data=getRunElementDataForID(id);
		if (data==null) return 0;
		lastDistributionId=id;
		lastDistributionRunElement=data;
		return getDistributionSum(data);
	}

	/**
	 * Summe �ber die Werte beim letzten Aufruf von {@link #calc(double[])} oder
	 * {@link #calcOrDefault(double[], double)}.<br>
	 * Bestimmt, ob {@link #lastResult} wieder verwendet werden kann oder ob der Wert
	 * neu berechnet werden muss.
	 * @see #calc(double[])
	 * @see #calcOrDefault(double[], double)
	 * @see #lastResult
	 */
	private double lastSum;

	/**
	 * Startindex beim letzten Aufruf von {@link #calc(double[])} oder
	 * {@link #calcOrDefault(double[], double)}.<br>
	 * Bestimmt, ob {@link #lastResult} wieder verwendet werden kann oder ob der Wert
	 * neu berechnet werden muss.
	 * @see #calc(double[])
	 * @see #calcOrDefault(double[], double)
	 * @see #lastResult
	 */
	private double lastParam1;

	/**
	 * Endindex beim letzten Aufruf von {@link #calc(double[])} oder
	 * {@link #calcOrDefault(double[], double)}.<br>
	 * Bestimmt, ob {@link #lastResult} wieder verwendet werden kann oder ob der Wert
	 * neu berechnet werden muss.
	 * @see #calc(double[])
	 * @see #calcOrDefault(double[], double)
	 * @see #lastResult
	 */
	private double lastParam2;

	/**
	 * Ergebniswert beim letzten Aufruf von {@link #calc(double[])} oder
	 * {@link #calcOrDefault(double[], double)}.<br>
	 * @see #calc(double[])
	 * @see #calcOrDefault(double[], double)
	 */
	private double lastResult;

	@Override
	protected double calc(final double[] parameters) throws MathCalcError {
		if (parameters.length<2 || parameters.length>3) throw error();

		final double sum=getDistributionSumByID(parameters[0]);
		if (sum<1) return 0.0;

		if (lastSum==sum) {
			if (parameters.length==3 && lastParam1==parameters[1] && lastParam2==parameters[2]) return lastResult;
			if (parameters.length==2 && lastParam1==parameters[1]) return lastResult;
		}

		final DataDistributionImpl dist=getDistributionByID(parameters[0]);
		if (dist==null) return 0.0;
		final double[] densityData=dist.densityData;

		final int densityDataLength=densityData.length;
		final double scale=densityDataLength/dist.upperBound;

		if (parameters.length==2) {
			final int index=(int)(parameters[1]*scale+0.5);
			if (index<0 || index>=densityDataLength) return 0.0;

			lastSum=sum;
			lastParam1=parameters[1];
			lastResult=dist.densityData[index]/sum;
		} else {
			final int index1=(int)(parameters[1]*scale+0.5);
			int index2=(int)(parameters[2]*scale+0.5);
			if (index1<0 || index1>=densityDataLength) return 0.0;
			if (index2>=densityDataLength) index2=densityDataLength-1;
			if (index2<index1) return 0.0;

			double part=0;
			for (int i=index1+1;i<=index2;i++) part+=densityData[i];

			lastSum=sum;
			lastParam1=parameters[1];
			lastParam2=parameters[2];
			lastResult=part/sum;
		}

		return lastResult;
	}

	@Override
	protected double calcOrDefault(final double[] parameters, final double fallbackValue) {
		if (parameters.length<2 || parameters.length>3) return fallbackValue;

		final double sum=getDistributionSumByID(parameters[0]);
		if (sum<1) return 0.0;

		if (lastSum==sum) {
			if (parameters.length==3 && lastParam1==parameters[1] && lastParam2==parameters[2]) return lastResult;
			if (parameters.length==2 && lastParam1==parameters[1]) return lastResult;
		}

		final DataDistributionImpl dist=getDistributionByID(parameters[0]);
		if (dist==null) return 0.0;
		final double[] densityData=dist.densityData;

		final int densityDataLength=densityData.length;
		final double scale=densityDataLength/dist.upperBound;

		if (parameters.length==2) {
			final int index=(int)(parameters[1]*scale+0.5);
			if (index<0 || index>=densityDataLength) return 0.0;

			lastSum=sum;
			lastParam1=parameters[1];
			lastResult=dist.densityData[index]/sum;
		} else {
			final int index1=(int)(parameters[1]*scale+0.5);
			int index2=(int)(parameters[2]*scale+0.5);
			if (index1<0 || index1>=densityDataLength) return 0.0;
			if (index2>=densityDataLength) index2=densityDataLength-1;
			if (index2<index1) return 0.0;

			double part=0;
			for (int i=index1+1;i<=index2;i++) part+=densityData[i];

			lastSum=sum;
			lastParam1=parameters[1];
			lastParam2=parameters[2];
			lastResult=part/sum;
		}

		return lastResult;
	}
}
