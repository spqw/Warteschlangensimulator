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
package parser.symbols;

import java.util.Arrays;

import mathtools.distribution.DataDistributionImpl;
import mathtools.distribution.tools.DistributionRandomNumber;
import parser.MathCalcError;
import parser.coresymbols.CalcSymbolPreOperator;

/**
 * Liefert eine Zufallszahl gem�� einer empirischen Verteilungsfunktion
 * @author Alexander Herzog
 */
public class CalcSymbolEmpiricalDistributionRandom extends CalcSymbolPreOperator {
	@Override
	public String[] getNames() {
		return new String[]{"EmpirischeZufallszahl","EmpiricalRandom"};
	}

	@Override
	protected double calc(double[] parameters) throws MathCalcError {
		if (parameters.length<2) throw error();
		final double upper=Math.max(0.00001,parameters[parameters.length-1]);

		final double[] data=Arrays.copyOf(parameters,parameters.length-1);
		final DataDistributionImpl dist=new DataDistributionImpl(upper,data);
		dist.normalizeDensity();
		return DistributionRandomNumber.randomNonNegative(dist);
	}

	@Override
	protected boolean isDeterministic() {
		return false;
	}
}
