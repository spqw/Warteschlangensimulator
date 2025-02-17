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

import language.Language;
import parser.MathCalcError;
import simulator.builder.RunModelCreatorStatus;
import simulator.coreelements.RunElementPassThrough;
import simulator.editmodel.EditModel;
import simulator.events.StationLeaveEvent;
import simulator.events.SystemChangeEvent;
import simulator.runmodel.RunDataClient;
import simulator.runmodel.RunModel;
import simulator.runmodel.SimulationData;
import simulator.simparser.ExpressionCalc;
import simulator.simparser.ExpressionMultiEval;
import ui.modeleditor.coreelements.ModelElement;
import ui.modeleditor.elements.ModelElementHold;
import ui.modeleditor.elements.ModelElementSub;

/**
 * �quivalent zu <code>ModelElementHold</code>
 * @author Alexander Herzog
 * @see ModelElementHold
 */
public class RunElementHold extends RunElementPassThrough implements StateChangeListener, PickUpQueue {
	/** Bedingung, die f�r eine Weitergabe der Kunden erf�llt sein muss */
	private String condition;
	/** Priorit�ts-Rechenausdr�cke */
	private String[] priority;
	/** Individuelle kundenbasierende Pr�fung? */
	private boolean useClientBasedCheck;
	/** Regelm��ige Pr�fung der Bedingung? */
	private boolean useTimedChecks;

	/**
	 * Konstruktor der Klasse
	 * @param element	Zugeh�riges Editor-Element
	 */
	public RunElementHold(final ModelElementHold element) {
		super(element,buildName(element,Language.tr("Simulation.Element.Hold.Name")));
	}

	@Override
	public Object build(final EditModel editModel, final RunModel runModel, final ModelElement element, final ModelElementSub parent, final boolean testOnly) {
		if (!(element instanceof ModelElementHold)) return null;
		final ModelElementHold holdElement=(ModelElementHold)element;
		final RunElementHold hold=new RunElementHold(holdElement);

		/* Auslaufende Kante */
		final String edgeError=hold.buildEdgeOut(holdElement);
		if (edgeError!=null) return edgeError;

		/* Bedingung */
		final String condition=holdElement.getCondition();
		if (condition==null || condition.trim().isEmpty()) {
			hold.condition=null;
		} else {
			final int error=ExpressionMultiEval.check(condition,runModel.variableNames);
			if (error>=0) return String.format(Language.tr("Simulation.Creator.HoldCondition"),condition,element.getId(),error+1);
			hold.condition=condition;
		}

		/* Priorit�ten */
		hold.priority=new String[runModel.clientTypes.length];
		for (int i=0;i<hold.priority.length;i++) {
			String priorityString=holdElement.getPriority(runModel.clientTypes[i]);
			if (priorityString==null || priorityString.trim().isEmpty()) priorityString=ModelElementHold.DEFAULT_CLIENT_PRIORITY;
			if (priorityString.equalsIgnoreCase(ModelElementHold.DEFAULT_CLIENT_PRIORITY)) {
				hold.priority[i]=null; /* Default Priorit�t als null vermerken */
			} else {
				final ExpressionCalc calc=new ExpressionCalc(runModel.variableNames);
				final int error=calc.parse(priorityString);
				if (error>=0) return String.format(Language.tr("Simulation.Creator.HoldClientPriority"),element.getId(),runModel.clientTypes[i],priorityString,error+1);

				hold.priority[i]=priorityString;
			}
		}

		/* Individuelle kundenbasierende Pr�fung */
		hold.useClientBasedCheck=holdElement.isClientBasedCheck();

		/* Zeitabh�ngige Checks */
		hold.useTimedChecks=holdElement.isUseTimedChecks();

		return hold;
	}

	@Override
	public RunModelCreatorStatus test(final ModelElement element) {
		if (!(element instanceof ModelElementHold)) return null;
		final ModelElementHold holdElement=(ModelElementHold)element;

		/* Auslaufende Kante */
		final RunModelCreatorStatus edgeError=testEdgeOut(holdElement);
		if (edgeError!=null) return edgeError;

		return RunModelCreatorStatus.ok;
	}

	@Override
	public RunElementHoldData getData(final SimulationData simData) {
		RunElementHoldData data;
		data=(RunElementHoldData)(simData.runData.getStationData(this));
		if (data==null) {
			data=new RunElementHoldData(this,condition,priority,simData.runModel.variableNames);
			simData.runData.setStationData(this,data);
		}
		return data;
	}

	@Override
	public void processArrival(final SimulationData simData, final RunDataClient client) {
		final RunElementHoldData data=getData(simData);

		data.queueLockedForPickUp=true;
		try {
			/* Kunden in Warteschlange einreihen */
			data.waitingClients.add(client);
			client.lastWaitingStart=simData.currentTime;

			/* Kunden an Station in Statistik */
			simData.runData.logClientEntersStationQueue(simData,this,data,client);

			/* System �ber Status-�nderung benachrichtigen */
			simData.runData.fireStateChangeNotify(simData);

			/* Interesse an zeitabh�ngigen Pr�fungen anmelden */
			if (useTimedChecks) simData.runData.requestTimedChecks(simData,this);
		} finally {
			data.queueLockedForPickUp=false;
		}
	}

	@Override
	public boolean interestedInChangeNotifiesAtTheMoment(final SimulationData simData) {
		final RunElementHoldData data=getData(simData);
		return data.waitingClients.size()>0;
	}

	/**
	 * Gibt einen einzelnen Kunden frei.
	 * @param simData	Simulationsdatenobjekt
	 * @param data	Thread-lokales Datenobjekt zu der Station
	 * @param client	Kunde
	 * @param clientIndex	Index des Kunden in der Liste der Kunden an der Station
	 */
	private void releaseClient(final SimulationData simData, final RunElementHoldData data, final RunDataClient client, final int clientIndex) {
		/* Kunde aus Warteschlange entfernen und weiterleiten */
		data.waitingClients.remove(clientIndex);
		StationLeaveEvent.addLeaveEvent(simData,client,this,0);
		StationLeaveEvent.unannounceClient(simData,client,getNext());
		data.lastRelease=simData.currentTime;

		/* Wartezeit in Statistik */
		final long waitingTime=simData.currentTime-client.lastWaitingStart;
		simData.runData.logStationProcess(simData,this,client,waitingTime,0,0,waitingTime);
		client.addStationTime(id,waitingTime,0,0,waitingTime);

		/* Kunden an Station in Statistik */
		simData.runData.logClientLeavesStationQueue(simData,this,data,client);

		/* Logging */
		if (simData.loggingActive) log(simData,Language.tr("Simulation.Log.Hold"),String.format(Language.tr("Simulation.Log.Hold.Info"),client.logInfo(simData),name));
	}

	/**
	 * Pr�ft, ob einer der Kunden freigegeben werden kann.<br>
	 * Dabei wird die Priorit�t nicht gepr�ft und es ist auch bereits bekannt, dass es Kunden in der Liste gibt.
	 * @param simData	Simulationsdatenobjekt
	 * @return	Gibt <code>true</code> zur�ck, wenn ein Kunde freigegeben werden konnte
	 * @param data	Thread-lokales Datenobjekt zu der Station
	 * @see #systemStateChangeNotify(SimulationData)
	 */
	private boolean releaseTestFIFO(final SimulationData simData, final RunElementHoldData data) {
		final int size=data.waitingClients.size();
		final double[] variableValues=simData.runData.variableValues;
		for (int index=0;index<size;index++) {
			final RunDataClient client=data.waitingClients.get(index);

			/* Ist die Bedingung erf�llt? */
			final boolean conditionIsTrue;
			if (useClientBasedCheck) {
				simData.runData.setClientVariableValues(client.waitingTime+(simData.currentTime-client.lastWaitingStart),client.transferTime,client.processTime); /* Auch die bisherige Wartezeit an der aktuellen Station schon mitz�hlen. */
				conditionIsTrue=(data.condition==null || data.condition.eval(variableValues,simData,client));
			} else {
				simData.runData.setClientVariableValues(null);
				conditionIsTrue=(data.condition==null || data.condition.eval(variableValues,simData,null));
			}
			if (!conditionIsTrue) {
				if (useClientBasedCheck) continue; else break;
			}

			/* Kunde freigeben */
			releaseClient(simData,data,client,index);

			/* Warten weitere Kunden? - Wenn ja in einer ms ein weiterer Check, ob die Bedingung noch erf�llt ist. */
			/* -> wird bereits durch "return true;" vom Aufrufer erledigt. */
			return true;
		}

		return false;
	}

	/** Umrechnungsfaktor von Millisekunden auf Sekunden, um die Division w�hrend der Simulation zu vermeiden */
	private static final double toSecFactor=1.0/1000.0;

	/**
	 * Berechnet den Score-Wert eines Kunden.
	 * @param simData	Simulationsdatenobjekt
	 * @param holdData	Thread-lokales Datenobjekt zu der Station
	 * @param client	Kunde
	 * @return	Score-Wert des Kunden
	 */
	private double getClientScore(final SimulationData simData, final RunElementHoldData holdData, final RunDataClient client) {
		final ExpressionCalc calc=holdData.priority[client.type];
		if (calc==null) { /* = Text war "w", siehe RunElementProcessData()  */
			return (((double)simData.currentTime)-client.lastWaitingStart)*toSecFactor;
		} else {
			simData.runData.setClientVariableValues(simData.currentTime-client.lastWaitingStart,client.transferTime,client.processTime);
			try {
				return calc.calc(simData.runData.variableValues,simData,client);
			} catch (MathCalcError e) {
				simData.calculationErrorStation(calc,this);
				return 0;
			}
		}
	}

	/**
	 * Gibt den Kunden mit der h�chsten Priorit�t frei.<br>
	 * Dabei wird die Bedingung nicht gepr�ft und es ist auch bereits bekannt, dass es Kunden in der Liste gibt.
	 * @param simData	Simulationsdatenobjekt
	 * @param data	Thread-lokales Datenobjekt zu der Station
	 */
	private void releaseClientWithHighestPriority(final SimulationData simData, final RunElementHoldData data) {
		final int size=data.waitingClients.size();

		int bestIndex=0;
		RunDataClient bestClient=data.waitingClients.get(0);
		double bestPriority=getClientScore(simData,data,bestClient);

		for (int index=1;index<size;index++) {
			final RunDataClient client=data.waitingClients.get(index);
			final double priority=getClientScore(simData,data,client);
			if (priority>bestPriority) {
				bestIndex=index;
				bestClient=client;
				bestPriority=priority;
			}
		}

		releaseClient(simData,data,bestClient,bestIndex);
	}

	/**
	 * Pr�ft, ob einer der Kunden freigegeben werden kann.<br>
	 * Die Bedingung und die Priorit�t werden f�r jeden Kunden individuell gepr�ft. Es ist bereits bekannt, dass es Kunden in der Liste gibt.
	 * @param simData	Simulationsdatenobjekt
	 * @return	Gibt <code>true</code> zur�ck, wenn ein Kunde freigegeben werden konnte
	 * @param data	Thread-lokales Datenobjekt zu der Station
	 */
	private boolean releaseTestPriorityIndividual(final SimulationData simData, final RunElementHoldData data) {
		final int size=data.waitingClients.size();
		final double[] variableValues=simData.runData.variableValues;

		int bestIndex=-1;
		double bestPriority=0;
		RunDataClient bestClient=null;

		for (int index=0;index<size;index++) {
			final RunDataClient client=data.waitingClients.get(index);
			final double priority=getClientScore(simData,data,client);
			if (priority>bestPriority) {
				simData.runData.setClientVariableValues(client.waitingTime+(simData.currentTime-client.lastWaitingStart),client.transferTime,client.processTime); /* Auch die bisherige Wartezeit an der aktuellen Station schon mitz�hlen. */
				final boolean conditionIsTrue=(data.condition==null || data.condition.eval(variableValues,simData,client));
				if (conditionIsTrue) {
					bestIndex=index;
					bestClient=client;
					bestPriority=priority;
				}
			}
		}

		if (bestClient==null) {
			return false;
		} else {
			releaseClient(simData,data,bestClient,bestIndex);
			return true;
		}
	}

	/**
	 * Pr�ft, ob einer der Kunden freigegeben werden kann.<br>
	 * Es wird der freigebbare Kunde mit der h�chsten Priorit�t freigegeben. Es ist bereits bekannt, dass es Kunden in der Liste gibt.
	 * @param simData	Simulationsdatenobjekt
	 * @return	Gibt <code>true</code> zur�ck, wenn ein Kunde freigegeben werden konnte
	 * @param data	Thread-lokales Datenobjekt zu der Station
	 * @see #systemStateChangeNotify(SimulationData)
	 */
	private boolean releaseTestPriority(final SimulationData simData, final RunElementHoldData data) {
		if (useClientBasedCheck) {
			return releaseTestPriorityIndividual(simData,data);
		} else {
			final double[] variableValues=simData.runData.variableValues;
			simData.runData.setClientVariableValues(null);
			final boolean conditionIsTrue=(data.condition==null || data.condition.eval(variableValues,simData,null));
			if (conditionIsTrue) {
				releaseClientWithHighestPriority(simData,data);
				return true;
			} else {
				return false;
			}
		}
	}

	@Override
	public boolean systemStateChangeNotify(final SimulationData simData) {
		final RunElementHoldData data=getData(simData);

		/* Warten �berhaupt Kunden? */
		if (data.waitingClients.size()==0) return false;

		data.queueLockedForPickUp=true;
		try {
			if (data.lastRelease<simData.currentTime) {
				if (data.allPriorityFIFO) {
					return releaseTestFIFO(simData,data);
				} else {
					return releaseTestPriority(simData,data);
				}
			} else {
				SystemChangeEvent.triggerEvent(simData,1);
				return false;
			}

		} finally {
			data.queueLockedForPickUp=false;
		}
	}

	@Override
	public RunDataClient getClient(final SimulationData simData) {
		final RunElementHoldData data=getData(simData);
		if (data.queueLockedForPickUp) return null;
		if (data.waitingClients.size()==0) return null;

		final RunDataClient client=data.waitingClients.remove(0);
		final long waitingTime=simData.currentTime-client.lastWaitingStart;
		/* Nein, da Kunde an der Station ja nicht bedient wurde: simData.runData.logStationProcess(simData,this,waitingTime,0,0); */
		client.addStationTime(id,waitingTime,0,0,waitingTime);
		simData.runData.logClientLeavesStationQueue(simData,this,data,client);

		return client;
	}
}
