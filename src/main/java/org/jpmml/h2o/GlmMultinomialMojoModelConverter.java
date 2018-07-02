/*
 * Copyright (c) 2018 Villu Ruusmann
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

import java.util.ArrayList;
import java.util.List;

import com.google.common.primitives.Doubles;
import hex.genmodel.algos.glm.GlmMultinomialMojoModel;
import org.dmg.pmml.DataType;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.regression.RegressionModel;
import org.dmg.pmml.regression.RegressionTable;
import org.jpmml.converter.CategoricalLabel;
import org.jpmml.converter.Feature;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.Schema;
import org.jpmml.converter.regression.RegressionModelUtil;

public class GlmMultinomialMojoModelConverter extends GlmMojoModelBaseConverter<GlmMultinomialMojoModel> {

	public GlmMultinomialMojoModelConverter(GlmMultinomialMojoModel model){
		super(model);
	}

	@Override
	public RegressionModel encodeModel(Schema schema){
		GlmMultinomialMojoModel model = getModel();

		CategoricalLabel categoricalLabel = (CategoricalLabel)schema.getLabel();
		List<? extends Feature> features = schema.getFeatures();

		List<Double> beta = Doubles.asList(getBeta(model));

		if(beta.size() != categoricalLabel.size() * (features.size() + 1)){
			throw new IllegalArgumentException();
		}

		List<RegressionTable> regressionTables = new ArrayList<>();

		int offset = 0;

		for(int i = 0; i < categoricalLabel.size(); i++){
			List<Double> coefficients = beta.subList(offset, offset + features.size());
			Double intercept = beta.get(offset + features.size());

			RegressionTable regressionTable = RegressionModelUtil.createRegressionTable(features, coefficients, intercept)
				.setTargetCategory(categoricalLabel.getValue(i));

			regressionTables.add(regressionTable);

			offset += (features.size() + 1);
		}

		RegressionModel regressionModel = new RegressionModel(MiningFunction.CLASSIFICATION, ModelUtil.createMiningSchema(categoricalLabel), regressionTables)
			.setNormalizationMethod(RegressionModel.NormalizationMethod.SOFTMAX)
			.setOutput(ModelUtil.createProbabilityOutput(DataType.DOUBLE, categoricalLabel));

		return regressionModel;
	}
}