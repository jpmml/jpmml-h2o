/*
 * Copyright (c) 2026 Villu Ruusmann
 *
 * This file is part of JPMML-H2O
 *
 * JPMML-H2O is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-H2O is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-H2O.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jpmml.h2o;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.dmg.pmml.regression.RegressionModel;
import org.jpmml.converter.Feature;
import org.jpmml.converter.Schema;
import org.jpmml.converter.regression.RegressionModelUtil;

public class SigmoidCalibration extends Calibration {

	private double[] calibGlmBeta = null;


	public SigmoidCalibration(double[] calibGlmBeta){
		setCalibGlmBeta(calibGlmBeta);
	}

	@Override
	public String getName(){
		return "sigmoidCalibration";
	}

	@Override
	public RegressionModel encodeModel(List<Feature> probabilityFeatures, Schema schema){
		double[] calibGlmBeta = getCalibGlmBeta();

		if(calibGlmBeta.length != 2){
			throw new IllegalArgumentException();
		}

		Feature feature = probabilityFeatures.get(0);

		return RegressionModelUtil.createBinaryLogisticClassification(Collections.singletonList(feature), Collections.singletonList(calibGlmBeta[0]), calibGlmBeta[1], RegressionModel.NormalizationMethod.LOGIT, false, schema);
	}

	public double[] getCalibGlmBeta(){
		return this.calibGlmBeta;
	}

	private void setCalibGlmBeta(double[] calibGlmBeta){
		this.calibGlmBeta = Objects.requireNonNull(calibGlmBeta);
	}
}
