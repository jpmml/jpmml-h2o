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

import java.lang.reflect.Field;
import java.util.List;

import com.google.common.primitives.Doubles;
import hex.genmodel.algos.glm.GlmMojoModel;
import org.dmg.pmml.regression.RegressionModel;
import org.jpmml.converter.Feature;
import org.jpmml.converter.Schema;
import org.jpmml.converter.SchemaUtil;
import org.jpmml.converter.regression.RegressionModelUtil;

public class GlmMojoModelConverter extends GlmMojoModelBaseConverter<GlmMojoModel> {

	public GlmMojoModelConverter(GlmMojoModel model){
		super(model);
	}

	@Override
	public RegressionModel encodeModel(Schema schema){
		GlmMojoModel model = getModel();

		List<? extends Feature> features = schema.getFeatures();

		List<Double> beta = Doubles.asList(getBeta(model));

		SchemaUtil.checkSize(beta.size() - 1, features);

		List<Double> coefficients = beta.subList(0, beta.size() - 1);
		Double intercept = beta.get(beta.size() - 1);

		RegressionModel.NormalizationMethod normalizationMethod;

		String link = getLink(model);
		switch(link){
			case "identity":
				normalizationMethod = RegressionModel.NormalizationMethod.NONE;
				break;
			case "logit":
				normalizationMethod = RegressionModel.NormalizationMethod.LOGIT;
				break;
			default:
				throw new H2OException("Link function \'" + link + "\' is not supported");
		}

		RegressionModel regressionModel;

		String family = getFamily(model);
		switch(family){
			case "binomial":
				regressionModel = RegressionModelUtil.createBinaryLogisticClassification(features, coefficients, intercept, normalizationMethod, true, schema);
				break;
			case "gaussian":
				regressionModel = RegressionModelUtil.createRegression(features, coefficients, intercept, normalizationMethod, schema);
				break;
			default:
				throw new H2OException("Distribution family \'" + family + "\' is not supported");
		}

		return regressionModel;
	}

	static
	public String getLink(GlmMojoModel model){
		return (String)getFieldValue(GlmMojoModelConverter.FIELD_LINK, model);
	}

	private static final Field FIELD_LINK;

	static {

		try {
			FIELD_LINK = GlmMojoModel.class.getDeclaredField("_link");
		} catch(ReflectiveOperationException roe){
			throw new RuntimeException(roe);
		}
	}
}