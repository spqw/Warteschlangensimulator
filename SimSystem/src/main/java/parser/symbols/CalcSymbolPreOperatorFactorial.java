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

import mathtools.Functions;
import parser.MathCalcError;
import parser.coresymbols.CalcSymbolPreOperator;

/**
 * Fakult�tsfunktion
 * @author Alexander Herzog
 * @see CalcSymbolPostOperatorFactorial
 */
public final class CalcSymbolPreOperatorFactorial extends CalcSymbolPreOperator {
	/**
	 * Namen f�r das Symbol
	 * @see #getNames()
	 */
	private static final String[] names=new String[]{"factorial","fact","fakult�t"};

	@Override
	public String[] getNames() {
		return names;
	}

	@Override
	protected double calc(double[] parameters) throws MathCalcError {
		if (parameters.length!=1) throw error();
		double signum=Math.signum(parameters[0]);
		if (signum==0.0) signum=1;
		return Math.round(signum*Functions.getFactorial((int)Math.round(Math.abs(parameters[0]))));
	}

	@Override
	protected double calcOrDefault(final double[] parameters, final double fallbackValue) {
		if (parameters.length!=1) return fallbackValue;
		double signum=Math.signum(parameters[0]);
		if (signum==0.0) signum=1;
		return Math.round(signum*Functions.getFactorial((int)Math.round(Math.abs(parameters[0]))));
	}
}
