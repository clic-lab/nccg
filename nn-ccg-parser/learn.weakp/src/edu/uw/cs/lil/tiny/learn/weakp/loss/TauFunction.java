/*******************************************************************************
 * UW SPF - The University of Washington Semantic Parsing Framework
 * <p>
 * Copyright (C) 2013 Yoav Artzi
 * <p>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 ******************************************************************************/
package edu.uw.cs.lil.tiny.learn.weakp.loss;

import java.util.List;

import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.learn.weakp.loss.LossSensitivePerceptronCKY.RelativeLossFunction;
import edu.uw.cs.lil.tiny.parser.IDerivation;
import edu.uw.cs.utils.composites.Pair;

/**
 * Tau function as used in the Singh-Miller and Collins 2007. This weight
 * function gives higher weight to samples that are responsible for more
 * violations. Optimal parses are weighted uniformly.
 * 
 * @author Yoav Artzi
 * @param <MR>
 */
@Deprecated
public class TauFunction<MR> implements IUpdateWeightFunction<MR> {
	private final RelativeLossFunction						deltaLossFunction;
	private final double									margin;
	private final int										numViolatingOptimalParse;
	private final IHashVector								paramsVector;
	private final double									valueForOptimalParses;
	private final int[]										vCTotalValues;
	private final List<Pair<Double, ? extends IDerivation<MR>>>	violatingOptimalParses;
	
	public TauFunction(
			List<Pair<Double, ? extends IDerivation<MR>>> violatingOptimalParses,
			List<Pair<Double, ? extends IDerivation<MR>>> violatingNonOptimalParses,
			double margin, IHashVector paramsVector,
			RelativeLossFunction deltaLossFunction) {
		this.violatingOptimalParses = violatingOptimalParses;
		this.margin = margin;
		this.paramsVector = paramsVector;
		this.deltaLossFunction = deltaLossFunction;
		this.valueForOptimalParses = 1.0 / violatingOptimalParses.size();
		this.numViolatingOptimalParse = violatingOptimalParses.size();
		this.vCTotalValues = new int[numViolatingOptimalParse];
		
		// Calculate array of vCTotal values
		int cCounter = 0;
		for (final Pair<Double, ? extends IDerivation<MR>> c : violatingOptimalParses) {
			for (final Pair<Double, ? extends IDerivation<MR>> e : violatingNonOptimalParses) {
				vCTotalValues[cCounter] += vCe(c, e);
			}
			++cCounter;
		}
	}
	
	@Override
	public double evalNonOptimalParse(
			Pair<Double, ? extends IDerivation<MR>> nonOptimalParse) {
		double ret = 0;
		int cCounter = 0;
		for (final Pair<Double, ? extends IDerivation<MR>> c : violatingOptimalParses) {
			ret += (double) vCe(c, nonOptimalParse)
					/ (numViolatingOptimalParse * vCTotalValues[cCounter]);
			++cCounter;
		}
		return ret;
	}
	
	@Override
	public double evalOptimalParse(
			Pair<Double, ? extends IDerivation<MR>> optimalParse) {
		return valueForOptimalParses;
	}
	
	private int vCe(Pair<Double, ? extends IDerivation<MR>> c,
			Pair<Double, ? extends IDerivation<MR>> e) {
		return c.second().getAverageMaxFeatureVector()
				.addTimes(-1.0, e.second().getAverageMaxFeatureVector())
				.vectorMultiply(paramsVector) < margin
				* deltaLossFunction.loss(e.first()) ? 1 : 0;
	}
	
}
