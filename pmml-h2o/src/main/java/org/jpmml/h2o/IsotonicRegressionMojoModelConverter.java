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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import hex.genmodel.algos.isotonic.IsotonicCalibrator;
import hex.genmodel.algos.isotonic.IsotonicRegressionMojoModel;
import org.dmg.pmml.DataType;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.Expression;
import org.dmg.pmml.LinearNorm;
import org.dmg.pmml.MapValues;
import org.dmg.pmml.NormContinuous;
import org.dmg.pmml.OpType;
import org.dmg.pmml.OutlierTreatmentMethod;
import org.dmg.pmml.PMMLFunctions;
import org.dmg.pmml.regression.RegressionModel;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.ExpressionUtil;
import org.jpmml.converter.Feature;
import org.jpmml.converter.FieldNameUtil;
import org.jpmml.converter.ModelEncoder;
import org.jpmml.converter.Schema;
import org.jpmml.converter.SchemaUtil;
import org.jpmml.converter.regression.RegressionModelUtil;

public class IsotonicRegressionMojoModelConverter extends Converter<IsotonicRegressionMojoModel> {

	public IsotonicRegressionMojoModelConverter(IsotonicRegressionMojoModel model){
		super(model);
	}

	@Override
	public RegressionModel encodeModel(Schema schema){
		IsotonicRegressionMojoModel model = getModel();

		IsotonicCalibrator isotonicCalibrator = getIsotonicCalibrator(model);

		ModelEncoder encoder = schema.getEncoder();
		List<? extends Feature> features = schema.getFeatures();

		Feature feature = SchemaUtil.getOnlyFeature(features);

		Feature isotonicFeature = encodeIsotonicCalibrator(isotonicCalibrator, feature, encoder);

		return RegressionModelUtil.createRegression(Collections.singletonList(isotonicFeature), Collections.singletonList(1d), null, RegressionModel.NormalizationMethod.NONE, schema);
	}

	static
	public Feature encodeIsotonicCalibrator(IsotonicCalibrator isotonicCalibrator, Feature feature, ModelEncoder encoder){
		double minX = isotonicCalibrator._min_x;
		double maxX = isotonicCalibrator._max_x;
		double[] thresholdsX = isotonicCalibrator._thresholds_x;
		double[] thresholdsY = isotonicCalibrator._thresholds_y;

		if(thresholdsX.length != thresholdsY.length || thresholdsX.length < 2){
			throw new IllegalArgumentException();
		} // End if

		if(minX != thresholdsX[0] || maxX != thresholdsX[thresholdsX.length - 1]){
			throw new IllegalArgumentException();
		}

		ContinuousFeature continuousFeature = feature.toContinuousFeature();

		Expression expression;

		Expression tiesExpression = encodeIsotonicTies(isotonicCalibrator, continuousFeature);
		Expression curveExpression = encodeIsotonicCurve(isotonicCalibrator, continuousFeature);

		if(tiesExpression != null){
			DerivedField lookupDerivedField = encoder.createDerivedField(FieldNameUtil.create("isotonicRegressionLookup", continuousFeature), OpType.CONTINUOUS, DataType.DOUBLE, tiesExpression);

			ContinuousFeature lookupFeature = new ContinuousFeature(encoder, lookupDerivedField);

			expression = ExpressionUtil.createApply(PMMLFunctions.IF,
				ExpressionUtil.createApply(PMMLFunctions.ISNOTMISSING, lookupFeature.ref()),
				lookupFeature.ref(),
				curveExpression
			);
		} else

		{
			expression = curveExpression;
		}

		DerivedField derivedField = encoder.createDerivedField(FieldNameUtil.create("isotonicRegression", continuousFeature), OpType.CONTINUOUS, DataType.DOUBLE, expression);

		return new ContinuousFeature(encoder, derivedField);
	}

	static
	private MapValues encodeIsotonicTies(IsotonicCalibrator isotonicCalibrator, ContinuousFeature continuousFeature){
		double[] thresholdsX = isotonicCalibrator._thresholds_x;
		double[] thresholdsY = isotonicCalibrator._thresholds_y;

		List<Double> inputValues = new ArrayList<>();
		List<Double> outputValues = new ArrayList<>();

		for(int begin = 0, end; begin < thresholdsX.length; begin = end + 1){
			end = getRunEnd(thresholdsX, begin);

			if(end > begin){
				double x = thresholdsX[begin];

				inputValues.add(x);
				outputValues.add(thresholdsY[Arrays.binarySearch(thresholdsX, x)]);
			}
		}

		if(inputValues.isEmpty()){
			return null;
		}

		MapValues mapValues = ExpressionUtil.createMapValues(continuousFeature.getName(), inputValues, outputValues)
			.setDataType(DataType.DOUBLE);

		return mapValues;
	}

	static
	private NormContinuous encodeIsotonicCurve(IsotonicCalibrator isotonicCalibrator, ContinuousFeature continuousFeature){
		double[] thresholdsX = isotonicCalibrator._thresholds_x;
		double[] thresholdsY = isotonicCalibrator._thresholds_y;

		NormContinuous normContinuous = new NormContinuous(continuousFeature.getName(), null)
			.setOutlierTreatment(OutlierTreatmentMethod.AS_EXTREME_VALUES);

		for(int begin = 0, end; begin < thresholdsX.length; begin = end + 1){
			end = getRunEnd(thresholdsX, begin);

			double x = thresholdsX[begin];

			if(end == begin){
				normContinuous.addLinearNorms(new LinearNorm(x, thresholdsY[begin]));
			} else

			{
				double y = thresholdsY[Arrays.binarySearch(thresholdsX, x)];

				normContinuous.addLinearNorms(
					new LinearNorm(x, (begin == 0) ? y : thresholdsY[begin]),
					new LinearNorm(x, (end == thresholdsX.length - 1) ? y : thresholdsY[end])
				);
			}
		}

		return normContinuous;
	}

	static
	private int getRunEnd(double[] values, int begin){
		int end = begin;

		while(end + 1 < values.length && values[end + 1] == values[begin]){
			end++;
		}

		return end;
	}

	static
	public IsotonicCalibrator getIsotonicCalibrator(IsotonicRegressionMojoModel model){
		return (IsotonicCalibrator)getFieldValue(IsotonicRegressionMojoModelConverter.FIELD_ISOTONIC_CALIBRATOR, model);
	}

	private static final Field FIELD_ISOTONIC_CALIBRATOR;

	static {

		try {
			FIELD_ISOTONIC_CALIBRATOR = IsotonicRegressionMojoModel.class.getDeclaredField("_isotonic_calibrator");
		} catch(ReflectiveOperationException roe){
			throw new RuntimeException(roe);
		}
	}
}
