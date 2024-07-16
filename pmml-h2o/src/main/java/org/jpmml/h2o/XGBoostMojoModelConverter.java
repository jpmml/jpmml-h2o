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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dmg.pmml.mining.MiningModel;
import org.jpmml.converter.BinaryFeature;
import org.jpmml.converter.CategoricalFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.Label;
import org.jpmml.converter.MissingValueFeature;
import org.jpmml.converter.ModelEncoder;
import org.jpmml.converter.Schema;
import org.jpmml.xgboost.HasXGBoostOptions;
import org.jpmml.xgboost.Learner;
import org.jpmml.xgboost.XGBoostUtil;

public class XGBoostMojoModelConverter extends Converter<XGBoostMojoModel> {

	public XGBoostMojoModelConverter(XGBoostMojoModel model){
		super(model);
	}

	@Override
	public Schema toMojoModelSchema(Schema schema){
		ModelEncoder encoder = schema.getEncoder();
		Label label = schema.getLabel();
		List<? extends Feature> features = schema.getFeatures();

		Function<Feature, Stream<Feature>> function = new Function<Feature, Stream<Feature>>(){

			@Override
			public Stream<Feature> apply(Feature feature){

				if(feature instanceof CategoricalFeature){
					CategoricalFeature categoricalFeature = (CategoricalFeature)feature;

					List<?> values = categoricalFeature.getValues();

					Stream<Feature> binaryFeaturesStream = values.stream()
						.map(value -> new BinaryFeature(encoder, categoricalFeature, value));

					Stream<Feature> missingValueFeatureStream = Stream.of(new MissingValueFeature(encoder, categoricalFeature));

					return Stream.concat(binaryFeaturesStream, missingValueFeatureStream);
				}

				return Stream.of(feature);
			}
		};

		features = features.stream()
			.flatMap(function)
			.collect(Collectors.toList());

		return new Schema(encoder, label, features);
	}

	@Override
	public MiningModel encodeModel(Schema schema){
		XGBoostMojoModel model = getModel();

		byte[] boosterBytes = model.getBoosterBytes();

		Learner learner;

		try(InputStream is = new ByteArrayInputStream(boosterBytes)){
			learner = XGBoostUtil.loadLearner(is);
		} catch(IOException ioe){
			throw new IllegalArgumentException(ioe);
		}

		Map<String, Object> options = new LinkedHashMap<>();
		options.put(HasXGBoostOptions.OPTION_COMPACT, Boolean.TRUE);
		options.put(HasXGBoostOptions.OPTION_NUMERIC, Boolean.TRUE);

		Schema xgbSchema = learner.toXGBoostSchema(schema);

		return learner.encodeModel(options, xgbSchema);
	}
}