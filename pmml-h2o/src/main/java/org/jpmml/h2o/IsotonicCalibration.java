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

import hex.genmodel.algos.isotonic.IsotonicCalibrator;
import org.dmg.pmml.regression.RegressionModel;
import org.jpmml.converter.Feature;
import org.jpmml.converter.ModelEncoder;
import org.jpmml.converter.Schema;
import org.jpmml.converter.regression.RegressionModelUtil;

public class IsotonicCalibration extends Calibration {

	private IsotonicCalibrator isotonicCalibrator = null;


	public IsotonicCalibration(IsotonicCalibrator isotonicCalibrator){
		setIsotonicCalibrator(isotonicCalibrator);
	}

	@Override
	public String getName(){
		return "isotonicCalibration";
	}

	@Override
	public RegressionModel encodeModel(List<Feature> probabilityFeatures, Schema schema){
		IsotonicCalibrator isotonicCalibrator = getIsotonicCalibrator();

		ModelEncoder encoder = schema.getEncoder();

		Feature feature = probabilityFeatures.get(1);

		Feature isotonicFeature = IsotonicRegressionMojoModelConverter.encodeIsotonicCalibrator(isotonicCalibrator, feature, encoder);

		return RegressionModelUtil.createBinaryLogisticClassification(Collections.singletonList(isotonicFeature), Collections.singletonList(1d), null, RegressionModel.NormalizationMethod.NONE, false, schema);
	}

	public IsotonicCalibrator getIsotonicCalibrator(){
		return this.isotonicCalibrator;
	}

	private void setIsotonicCalibrator(IsotonicCalibrator isotonicCalibrator){
		this.isotonicCalibrator = Objects.requireNonNull(isotonicCalibrator);
	}
}
