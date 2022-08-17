/**
 * Copyright 2022 Alexander Herzog
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

import java.util.List;
import java.util.stream.IntStream;

import language.Language;
import simulator.builder.RunModelCreatorStatus;
import simulator.coreelements.RunElement;
import simulator.editmodel.EditModel;
import simulator.events.StationLeaveEvent;
import simulator.runmodel.RunDataClient;
import simulator.runmodel.RunModel;
import simulator.runmodel.SimulationData;
import ui.modeleditor.coreelements.ModelElement;
import ui.modeleditor.elements.ModelElementSub;
import ui.modeleditor.elements.ModelElementTeleportDestination;
import ui.modeleditor.elements.ModelElementTeleportSourceMulti;

/**
 * �quivalent zu {@link ModelElementTeleportSourceMulti}
 * @author Alexander Herzog
 * @see ModelElementTeleportSourceMulti
 */
public class RunElementTeleportSourceMulti extends RunElement {
	/** Namen der Teleport-Zielstationen */
	private String[] destinationStrings;
	/** IDs der Teleport-Zielstationen */
	private int[] destinationIDs;
	/** Teleport-Zielstationen (�bersetzt aus {@link #destinationIDs}) */
	private RunElement[] destinations;

	/**
	 * Konstruktor der Klasse
	 * @param element	Zugeh�riges Editor-Element
	 */
	public RunElementTeleportSourceMulti(final ModelElementTeleportSourceMulti element) {
		super(element,buildName(element,Language.tr("Simulation.Element.TeleportSourceMulti.Name")));
	}

	/**
	 * Sucht das Teleport-Ziel-Element mit dem angegebenen Namen
	 * @param editModel	Editormodell das alle Elemente enth�lt
	 * @param name	Name des Teleport-Ziel-Elements
	 * @return	Teleport-Ziel-Element Objekt oder <code>null</code>, wenn kein Teleport-Ziel mit dem angegebenen Namen gefunden wurde
	 */
	public static ModelElementTeleportDestination getDestination(final EditModel editModel, final String name) {
		if (name==null || name.trim().isEmpty()) return null;

		for (ModelElement e1 : editModel.surface.getElements()) {
			if (e1 instanceof ModelElementTeleportDestination && e1.getName().equals(name)) return (ModelElementTeleportDestination)e1;
			if (e1 instanceof ModelElementSub) {
				for (ModelElement e2 : ((ModelElementSub)e1).getSubSurface().getElements()) {
					if (e2 instanceof ModelElementTeleportDestination && e2.getName().equals(name)) return (ModelElementTeleportDestination)e2;
				}
			}
		}
		return null;
	}

	/**
	 * Sucht das Teleport-Ziel-Element mit dem angegebenen Namen und liefert seine ID
	 * @param editModel	Editormodell das alle Elemente enth�lt
	 * @param name	Name des Teleport-Ziel-Elements
	 * @return	ID des Teleport-Ziel-Element Objekts oder -1, wenn kein Teleport-Ziel mit dem angegebenen Namen gefunden wurde
	 */
	public static int getDestinationID(final EditModel editModel, final String name) {
		final ModelElementTeleportDestination destination=getDestination(editModel,name);
		if (destination==null) return -1;
		return destination.getId();
	}

	@Override
	public Object build(EditModel editModel, RunModel runModel, ModelElement element, ModelElementSub parent, boolean testOnly) {
		if (!(element instanceof ModelElementTeleportSourceMulti)) return null;

		final ModelElementTeleportSourceMulti sourceElement=(ModelElementTeleportSourceMulti)element;
		final RunElementTeleportSourceMulti source=new RunElementTeleportSourceMulti(sourceElement);

		source.destinationStrings=sourceElement.getDestinations().toArray(new String[0]);
		if (source.destinationStrings.length==0) return String.format(Language.tr("Simulation.Creator.NoTeleportDestination"),element.getId());
		source.destinationIDs=new int[source.destinationStrings.length];
		for (int i=0;i<source.destinationStrings.length;i++) {
			final int id=getDestinationID(editModel,source.destinationStrings[i]);
			if (id<0) return String.format(Language.tr("Simulation.Creator.InvalidTeleportDestination"),element.getId(),source.destinationStrings[i]);
			source.destinationIDs[i]=id;
		}

		return source;
	}

	@Override
	public RunModelCreatorStatus test(ModelElement element) {
		if (!(element instanceof ModelElementTeleportSourceMulti)) return null;

		final ModelElementTeleportSourceMulti sourceElement=(ModelElementTeleportSourceMulti)element;

		final List<String> destinationStrings=sourceElement.getDestinations();
		if (destinationStrings.size()==0) return new RunModelCreatorStatus(String.format(Language.tr("Simulation.Creator.NoTeleportDestination"),element.getId()),RunModelCreatorStatus.Status.TELEPORT_INVALID_DESTINATION);
		for (String destination: destinationStrings) {
			final int destinationID=getDestinationID(element.getModel(),destination);
			if (destinationID<0) return new RunModelCreatorStatus(String.format(Language.tr("Simulation.Creator.InvalidTeleportDestination"),element.getId(),destination),RunModelCreatorStatus.Status.TELEPORT_INVALID_DESTINATION);
		}

		return RunModelCreatorStatus.ok;
	}

	@Override
	public void prepareRun(final RunModel runModel) {
		destinations=IntStream.of(destinationIDs).mapToObj(id->runModel.elements.get(id)).toArray(RunElement[]::new);
	}

	@Override
	public RunElementTeleportSourceMultiData getData(final SimulationData simData) {
		RunElementTeleportSourceMultiData data;
		data=(RunElementTeleportSourceMultiData)(simData.runData.getStationData(this));
		if (data==null) {
			data=new RunElementTeleportSourceMultiData(this,destinations.length);
			simData.runData.setStationData(this,data);
		}
		return data;
	}


	@Override
	public void processArrival(SimulationData simData, RunDataClient client) {
		StationLeaveEvent.addLeaveEvent(simData,client,this,0);
	}

	@Override
	public void processLeave(SimulationData simData, RunDataClient client) {
		final RunElementTeleportSourceMultiData data=getData(simData);

		data.newClientsList[0]=client;

		/* Clone erstellen */
		for (int i=1;i<destinations.length;i++) data.newClientsList[i]=simData.runData.clients.getClone(client,simData);

		/* Logging */
		if (simData.loggingActive) {
			final StringBuilder sb=new StringBuilder();
			for (int i=1;i<destinationIDs.length;i++) {sb.append(", id="); sb.append(data.newClientsList[i].hashCode());}
			log(simData,Language.tr("Simulation.Log.TeleportMulti"),String.format(Language.tr("Simulation.Log.TeleportMulti.Info"),client.hashCode(),sb.toString(),name));
		}

		/* Kunden zu den n�chsten Stationen leiten */
		StationLeaveEvent.multiSendToStation(simData,data.newClientsList,this,destinations);
	}
}
