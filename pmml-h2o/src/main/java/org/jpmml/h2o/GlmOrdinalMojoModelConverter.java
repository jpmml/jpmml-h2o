/*
 * Copyright (c) 2023 Villu Ruusmann
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.google.common.primitives.Doubles;
import hex.genmodel.algos.glm.GlmOrdinalMojoModel;
import org.dmg.pmml.DataType;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.OpType;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.mining.Segmentation;
import org.dmg.pmml.regression.RegressionModel;
import org.jpmml.converter.CategoricalLabel;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.ModelEncoder;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.OrdinalLabel;
import org.jpmml.converter.Schema;
import org.jpmml.converter.SchemaUtil;
import org.jpmml.converter.mining.MiningModelUtil;
import org.jpmml.converter.regression.RegressionModelUtil;

public class GlmOrdinalMojoModelConverter extends GlmMojoModelBaseConverter<GlmOrdinalMojoModel> {

	public GlmOrdinalMojoModelConverter(GlmOrdinalMojoModel model){
		super(model);
	}

	@Override
	public Schema encodeSchema(H2OEncoder encoder){
		Schema schema = super.encodeSchema(encoder);

		CategoricalLabel categoricalLabel = schema.requireCategoricalLabel();

		encoder.toOrdinal(categoricalLabel.getName(), categoricalLabel.getValues());

		return schema.toRelabeledSchema(categoricalLabel.toOrdinalLabel());
	}

	@Override
	public MiningModel encodeModel(Schema schema){
		GlmOrdinalMojoModel model = getModel();

		ModelEncoder encoder = schema.getEncoder();
		OrdinalLabel ordinalLabel = schema.requireOrdinalLabel();
		List<? extends Feature> features = schema.getFeatures();

		List<Double> beta = Doubles.asList(getBeta(model));

		SchemaUtil.checkSize(beta.size() - ordinalLabel.size(), ordinalLabel, features);

		List<? extends Number> sharedCoefficients = null;

		List<Number> thresholds = new ArrayList<>();

		int offset = 0;

		for(int i = 0; i < ordinalLabel.size(); i++){
			List<Double> coefficients = beta.subList(offset, offset + features.size());
			Double intercept = beta.get(offset + features.size());

			if(i < (ordinalLabel.size() - 1)){

				if(sharedCoefficients == null){
					sharedCoefficients = coefficients;
				} else

				{
					if(!Objects.equals(sharedCoefficients, coefficients)){
						throw new H2OException("Expected the same coefficient values, got " + sharedCoefficients + " and " + coefficients);
					}
				}

				thresholds.add(intercept);
			} else

			{
				if(sharedCoefficients == null){
					throw new IllegalArgumentException();
				} else

				{
					for(Number coefficient : coefficients){

						if(coefficient.doubleValue() != 0d){
							throw new H2OException("Expected a zero coefficient value, got " + coefficient);
						}
					}

					if(intercept.doubleValue() != 0d){
						throw new H2OException("Expected a zero intercept value, got " + intercept);
					}
				}
			}

			offset += (features.size() + 1);
		}

		Schema segmentSchema = schema.toAnonymousRegressorSchema(DataType.DOUBLE);

		RegressionModel firstRegressionModel = RegressionModelUtil.createRegression(features, sharedCoefficients, 0d, RegressionModel.NormalizationMethod.NONE, segmentSchema);

		OutputField linpredOutputField = ModelUtil.createPredictedField("linpred", OpType.CONTINUOUS, DataType.DOUBLE);

		DerivedField linpredField = encoder.createDerivedField(firstRegressionModel, linpredOutputField, true);

		Feature feature = new ContinuousFeature(encoder, linpredField);

		RegressionModel secondRegressionModel = RegressionModelUtil.createOrdinalClassification(feature, thresholds, RegressionModel.NormalizationMethod.LOGIT, true, schema);

		return MiningModelUtil.createModelChain(Arrays.asList(firstRegressionModel, secondRegressionModel), Segmentation.MissingPredictionTreatment.RETURN_MISSING);
	}
}