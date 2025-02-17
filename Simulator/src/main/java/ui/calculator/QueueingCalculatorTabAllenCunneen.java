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
package ui.calculator;

import java.io.Serializable;

import javax.swing.JCheckBox;

import language.Language;
import mathtools.NumberTools;
import simulator.editmodel.EditModel;
import ui.modeleditor.elements.ModelElementAnimationTextValue;
import ui.modeleditor.elements.ModelElementDispose;
import ui.modeleditor.elements.ModelElementProcess;
import ui.modeleditor.elements.ModelElementSource;

/**
 * Panel zur Berechnung von Kenngr��en in einem Warteschlangensystem
 * basierend auf der Allen-Cunneen-Formel
 * @author Alexander Herzog
 * @see QueueingCalculatorTabBase
 * @see QueueingCalculatorDialog
 */
public class QueueingCalculatorTabAllenCunneen extends QueueingCalculatorTabBase {
	/**
	 * Serialisierungs-ID der Klasse
	 * @see Serializable
	 */
	private static final long serialVersionUID = 6885193560880513420L;

	/** lambda (Ankunftsrate) */
	private final QueueingCalculatorInputPanel lambdaInput;
	/** bI (Ankunfts-Batch-Gr��e) */
	private final QueueingCalculatorInputPanel bIInput;
	/** mu (Bedienrate) */
	private final QueueingCalculatorInputPanel muInput;
	/** c (Anzahl an Bedienern) */
	private final QueueingCalculatorInputPanel cInput;
	/** bS (Bedien-Batch-Gr��e) */
	private final QueueingCalculatorInputPanel bSInput;
	/** cvI (Variationskoeffizient der Zwischenankunftszeiten) */
	private final QueueingCalculatorInputPanel cvIInput;
	/** cvS (Variationskoeffizient der Bedienzeiten) */
	private final QueueingCalculatorInputPanel cvSInput;
	/** PUp (Verf�gbarkeit) */
	private final QueueingCalculatorInputPanel PUpInput;
	/** EDt (Mittlere Ausfallzeit) */
	private final QueueingCalculatorInputPanel EDtInput;
	/** cvDt (Variationskoeffizient der Ausfallzeiten) */
	private final QueueingCalculatorInputPanel cvDtInput;

	/** KLB-Korrektur verwenden? */
	private final JCheckBox useKLBCorrection;
	/** Hanschke-Korrektur verwenden? */
	private final JCheckBox useHanschkeCorrection;

	/**
	 * Aktuell eingestellter Wert f�r lambda
	 * @see #calc()
	 */
	private double lambda;

	/**
	 * Aktuell eingestellter Wert f�r bI
	 * @see #calc()
	 */
	private int bI;

	/**
	 * Aktuell eingestellter Wert f�r mu
	 * @see #calc()
	 */
	private double mu;

	/**
	 * Aktuell eingestellter Wert f�r c
	 * @see #calc()
	 */
	private int c;

	/**
	 * Aktuell eingestellter Wert f�r bS
	 * @see #calc()
	 */
	private int bS;

	/**
	 * Aktuell eingestellter Wert f�r CV[I]
	 * @see #calc()
	 */
	private double cvI;

	/**
	 * Aktuell eingestellter Wert f�r CV[S]
	 * @see #calc()
	 */
	private double cvS;

	/**
	 * Konstruktor der Klasse
	 */
	public QueueingCalculatorTabAllenCunneen() {
		super("Allen-Cunneen",Language.tr("LoadCalculator.Tab.AllenCunneen"),Language.tr("LoadCalculator.TUCOnlineCalculator"),"https://a-herzog.github.io/QueueCalc");

		/* Ankunftsrate (lambda) */
		lambdaInput=getPanel(Language.tr("LoadCalculator.ArrivalRate"),true);
		lambdaInput.addDefault("&lambda; ("+unitSecondsInv+")",QueueingCalculatorInputPanel.NumberMode.POSITIVE_DOUBLE,3.5/60,infoRate);
		lambdaInput.addOption("&lambda; ("+unitMinutesInv+")",60,false,infoRate);
		lambdaInput.addOption("&lambda; ("+unitHoursInv+")",3600,false,infoRate);
		lambdaInput.addOption("1/&lambda; ("+unitSeconds+")",1,true,infoInterarrivalTime);
		lambdaInput.addOption("1/&lambda; ("+unitMinutes+")",60,true,infoInterarrivalTime);
		lambdaInput.addOption("1/&lambda; ("+unitHours+")",3600,true,infoInterarrivalTime);
		lambdaInput.setVisibleOptionIndex(1);
		add(lambdaInput.get());

		/* Ankunftsbatchgr��e (b(I)) */
		bIInput=getPanel(Language.tr("LoadCalculator.ArrivalBatchSize"),false);
		bIInput.addDefault("b(I)=",QueueingCalculatorInputPanel.NumberMode.POSITIVE_LONG,1,null);
		add(bIInput.get());

		/* Bedienrate (mu) */
		muInput=getPanel(Language.tr("LoadCalculator.AverageHoldingAndPostProcessingTime"),true);
		muInput.addDefault("&mu; ("+unitSecondsInv+")",QueueingCalculatorInputPanel.NumberMode.POSITIVE_DOUBLE,1.0/60/3,infoRate);
		muInput.addOption("&mu; ("+unitMinutesInv+")",60,false,infoRate);
		muInput.addOption("&mu; ("+unitHoursInv+")",3600,false,infoRate);
		muInput.addOption("1/&mu; ("+unitSeconds+")",1,true,infoTime);
		muInput.addOption("1/&mu; ("+unitMinutes+")",60,true,infoTime);
		muInput.addOption("1/&mu; ("+unitHours+")",3600,true,infoTime);
		muInput.setVisibleOptionIndex(4);
		add(muInput.get());

		/* Anzahl Bediener (c) */
		cInput=getPanel(Language.tr("LoadCalculator.Agents"),false);
		cInput.addDefault("c=",QueueingCalculatorInputPanel.NumberMode.POSITIVE_LONG,13,null);
		add(cInput.get());

		/* Ankunftsbatchgr��e (b(S)) */
		bSInput=getPanel(Language.tr("LoadCalculator.BatchSize"),false);
		bSInput.addDefault("b(S)=",QueueingCalculatorInputPanel.NumberMode.POSITIVE_LONG,1,null);
		add(bSInput.get());

		/* Variationskoeffizient der Zwischenankunftszeiten (CV[I]) */
		cvIInput=getPanel(Language.tr("LoadCalculator.ArrivalRateCV"),false);
		cvIInput.addDefault("CV[I]=",QueueingCalculatorInputPanel.NumberMode.NOT_NEGATIVE_DOUBLE,1,null);
		add(cvIInput.get());

		/* Variationskoeffizient der Bedienzeiten (CV[S]) */
		cvSInput=getPanel(Language.tr("LoadCalculator.WorkingRateCV"),false);
		cvSInput.addDefault("CV[S]=",QueueingCalculatorInputPanel.NumberMode.NOT_NEGATIVE_DOUBLE,1,null);
		add(cvSInput.get());

		/* Verf�gbarkeit und Ausfallzeiten */
		PUpInput=getPanel(Language.tr("LoadCalculator.Uptime"),false);
		PUpInput.addDefault("P(Up)=",QueueingCalculatorInputPanel.NumberMode.PROBABILITY,1,null);
		add(PUpInput.get());
		EDtInput=getPanel(Language.tr("LoadCalculator.AverageDownTime"),false);
		EDtInput.addDefault("E[Dt]=",QueueingCalculatorInputPanel.NumberMode.NOT_NEGATIVE_DOUBLE,1,null);
		add(EDtInput.get());
		cvDtInput=getPanel(Language.tr("LoadCalculator.DownTimeCV"),false);
		cvDtInput.addDefault("CV[Dt]=",QueueingCalculatorInputPanel.NumberMode.NOT_NEGATIVE_DOUBLE,1,null);
		add(cvDtInput.get());

		/* Korrekturfaktoren */
		addSection("Korrekturfaktoren");
		useKLBCorrection=addCheckBox(Language.tr("LoadCalculator.OptionKLB"),Language.tr("LoadCalculator.OptionKLB.Paper"),"http://content.ikr.uni-stuttgart.de/en/Content/Publications/Archive/Kraemer_ITC8_40421.pdf");
		useKLBCorrection.setToolTipText("<html><body>"+Language.tr("LoadCalculator.OptionKLB.Info")+"</body></html>");
		useHanschkeCorrection=addCheckBox(Language.tr("LoadCalculator.OptionHanschke"),Language.tr("LoadCalculator.OptionHanschke.Paper"),"https://www.sciencedirect.com/science/article/abs/pii/S0167637705000441");
		useHanschkeCorrection.setToolTipText("<html><body>"+Language.tr("LoadCalculator.OptionHanschke.Info")+"</body></html>");
	}

	@Override
	public void calc() {
		if (!lambdaInput.isValueOk()) {setError(); return;}
		if (!bIInput.isValueOk()) {setError(); return;}
		if (!muInput.isValueOk()) {setError(); return;}
		if (!cInput.isValueOk()) {setError(); return;}
		if (!bSInput.isValueOk()) {setError(); return;}
		if (!cvIInput.isValueOk()) {setError(); return;}
		if (!cvSInput.isValueOk()) {setError(); return;}

		if (!PUpInput.isValueOk()) {setError(); return;}
		final double PUp=PUpInput.getDouble();
		final double EDt;
		final double cvDt;
		if (PUp==1.0) {
			EDt=1;
			cvDt=1;
		} else {
			if (!EDtInput.isValueOk()) {setError(); return;}
			if (!cvDtInput.isValueOk()) {setError(); return;}
			EDt=EDtInput.getDouble();
			cvDt=cvDtInput.getDouble();
		}

		lambda=lambdaInput.getDouble();
		bI=(int)bIInput.getLong();
		mu=muInput.getDouble();
		c=(int)cInput.getLong();
		bS=(int)bSInput.getLong();
		cvI=cvIInput.getDouble();
		cvS=cvSInput.getDouble();

		/*
		 * Rechnungen sind mit
		 * Hanschke: "Approximations for the mean queue length of the GIX/G(b,b)/c queue" abgeglichen.
		 */

		/* Umrechnung von Arrival-Batches auf einzelne Kunden */
		lambda=lambda*bI;

		/* Ber�cksichtigung der verringerten Uptime */
		mu=mu*PUp;

		final double a=lambda/mu/bS;
		final double rho=lambda/mu/(bS*c);

		final double scvI=cvI*cvI;
		final double scvS=cvS*cvS;

		/*
		PC1=(c*rho)^c/(c!(1-rho));
		PC=PC1/(PC1+sum(k=0...c-1; (c*rho)^k/k!))
		E[NQ]=rho/(1-rho)*PC*(bI*SCV[I]+bS*SCV[S])/2+(bI-1)/2+(bS-1)/2
		E[N]=E[NQ]+bS*c*rho
		 */

		final double PC1=powerFactorial(c*rho,c)/(1-rho);
		double PC=0; for(int i=0;i<=c-1;i++) PC+=powerFactorial(c*rho,i);
		PC=PC1/(PC1+PC);

		final double scvScompl=scvS+PUp*(1-PUp)*EDt*(1+cvDt*cvDt)*mu;

		double KLB=1; /* W. Kraemer, M. Langenbach-Belz, Approximate formulae for the delay in the queueing system G/G/1. in: Proceedings of the Eighth International Teletraffic Congress, Melbourne, 1976, pp. 235.1�235.8. */
		if (useKLBCorrection.isSelected()) {
			final double scvIstar=bI/((double)bS)*scvI;
			if (scvIstar<=1) {
				KLB=Math.exp(-2/3*(1-rho)/PC*Math.pow(1-scvIstar,2)/(scvIstar+scvScompl));
			} else {
				KLB=Math.exp(-(1-rho)*(scvIstar-1)/(scvIstar+4*scvScompl));
			}
		}

		double H=0; /* Th. Hanschke, Approximations for the mean queue length of the GIX/G(b,b)/c queue. in: Operations Research Letters 34 (2006) 205 � 213. */
		if (useHanschkeCorrection.isSelected()) {
			H=Math.max(bI-bS*c,0)*rho/2;
		}

		final double ENQ=rho/(1-rho)*PC*(bI*scvI+bS*scvScompl)/2*KLB+(((double)bI)-1)/2+(((double)bS)-1)/2+H;
		final double EN=ENQ+((double)bS)*(c)*rho;
		final double EW=ENQ/lambda;
		final double EV=EW+1/mu;

		final StringBuilder result=new StringBuilder();

		result.append(Language.tr("LoadCalculator.OfferedWorkLoad")+" a="+NumberTools.formatNumber(a,2)+"<br>");
		if (rho>=1) {
			result.append(Language.tr("LoadCalculator.WorkLoad")+" (rho) &rho;="+NumberTools.formatPercent(rho,2)+" ("+Language.tr("LoadCalculator.AllenCunneenInvalidWorkLoad")+")");
		} else {
			result.append(Language.tr("LoadCalculator.WorkLoad")+" (rho) &rho;="+NumberTools.formatPercent(rho,2)+"<br>");
			result.append(Language.tr("LoadCalculator.AverageQueueLength")+" E[N<sub>Q</sub>]="+NumberTools.formatNumber(ENQ,2)+"<br>");
			result.append(Language.tr("LoadCalculator.AverageNumberOfClientsInTheSystem")+" E[N]="+NumberTools.formatNumber(EN,2)+"<br>");
			result.append(Language.tr("LoadCalculator.AverageWaitingTime")+" E[W]="+NumberTools.formatNumber(EW,2)+" ("+Language.tr("LoadCalculator.Units.InSeconds")+")<br>");
			result.append(Language.tr("LoadCalculator.AverageResidenceTime")+" E[V]="+NumberTools.formatNumber(EV,2)+" ("+Language.tr("LoadCalculator.Units.InSeconds")+")<br>");
			result.append(Language.tr("LoadCalculator.FlowFactor")+" E[V]/E[S]="+NumberTools.formatNumber(EV*mu,2)+"<br>");
			if (useKLBCorrection.isSelected()) {
				result.append(Language.tr("LoadCalculator.ResultFactorKLB")+"="+NumberTools.formatNumber(KLB,2)+"<br>");
			}
			if (useHanschkeCorrection.isSelected()) {
				result.append(Language.tr("LoadCalculator.ResultFactorHanschke")+"="+NumberTools.formatNumber(H,2)+"<br>");
			}
		}

		setResult(result.toString());
	}

	@Override
	protected String getHelpPageName() {
		return "allenCunneen";
	}

	@Override
	public EditModel buildModel() {
		final EditModel model=super.buildModel();

		final double meanInterArrivalTime=1/lambda;
		final ModelElementSource source=addSource(model,meanInterArrivalTime,bI,cvI,50,100);

		final double meanServiceTime=1/mu;
		final ModelElementProcess process=addProcess(model,meanServiceTime,cvS,bS,c,Language.tr("Editor.Operator.Plural"),250,100);

		final ModelElementDispose dispose=addExit(model,450,100);

		addEdge(model,source,process);
		addEdge(model,process,dispose);

		addText(model,"E[I]="+NumberTools.formatNumber(meanInterArrivalTime)+" "+Language.tr("LoadCalculator.Units.Seconds"),false,50,200);
		addText(model,"E[S]="+NumberTools.formatNumber(meanServiceTime)+" "+Language.tr("LoadCalculator.Units.Seconds"),false,50,220);
		addText(model,"c="+c,false,50,240);
		addText(model,"&rho;="+NumberTools.formatPercent(meanServiceTime*bS/meanInterArrivalTime/bI/c),false,50,260);
		addText(model,"CV[I]="+NumberTools.formatNumber(cvI),false,50,280);
		addText(model,"CV[S]="+NumberTools.formatNumber(cvS),false,50,300);

		addExpression(model,Language.tr("LoadCalculator.ModelBuilder.SimRho"),"Resource_avg()/Resource_count()",50,340).setMode(ModelElementAnimationTextValue.ModeExpression.MODE_EXPRESSION_PERCENT);
		addExpression(model,Language.tr("LoadCalculator.ModelBuilder.SimEW"),"waitingTime_avg()",50,380);
		addExpression(model,Language.tr("LoadCalculator.ModelBuilder.SimEV"),"residenceTime_avg()",50,420);
		addExpression(model,Language.tr("LoadCalculator.ModelBuilder.SimENQ"),"NQ_avg()",50,460);
		addExpression(model,Language.tr("LoadCalculator.ModelBuilder.SimEN"),"WIP_avg()",50,500);

		return model;
	}
}
