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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import hex.genmodel.MojoModel;
import org.jpmml.converter.BinaryFeature;
import org.jpmml.converter.CategoricalFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.Label;
import org.jpmml.converter.PMMLEncoder;
import org.jpmml.converter.Schema;

abstract
public class GlmMojoModelBaseConverter<M extends MojoModel> extends Converter<M> {

	public GlmMojoModelBaseConverter(M model){
		super(model);
	}

	@Override
	public Schema encodeSchema(H2OEncoder encoder){
		Schema schema = super.encodeSchema(encoder);

		Label label = schema.getLabel();
		List<Feature> features = new ArrayList<>(schema.getFeatures());

		Function<Feature, Stream<Feature>> function = new Function<Feature, Stream<Feature>>(){

			@Override
			public Stream<Feature> apply(Feature feature){
				PMMLEncoder encoder = feature.getEncoder();

				if(feature instanceof CategoricalFeature){
					CategoricalFeature categoricalFeature = (CategoricalFeature)feature;

					List<String> values = categoricalFeature.getValues();

					return values.stream()
						.map(value -> new BinaryFeature(encoder, categoricalFeature.getName(), categoricalFeature.getDataType(), value));
				}

				return Stream.of(feature);
			}
		};

		features = features.stream()
			.flatMap(function)
			.collect(Collectors.toList());

		return new Schema(label, features);
	}

	static
	public double[] getBeta(MojoModel model){
		return (double[])getFieldValue(GlmMojoModelBaseConverter.FIELD_BETA, model);
	}

	static
	public String getFamily(MojoModel model){
		return (String)getFieldValue(GlmMojoModelBaseConverter.FIELD_FAMILY, model);
	}

	private static final Class<?> CLASS_GLMMOJOMODELBASE;

	static {

		try {
			CLASS_GLMMOJOMODELBASE = Class.forName("hex.genmodel.algos.glm.GlmMojoModelBase");
		} catch(ReflectiveOperationException roe){
			throw new RuntimeException(roe);
		}
	}

	private static final Field FIELD_BETA;
	private static final Field FIELD_FAMILY;

	static {

		try {
			FIELD_BETA = CLASS_GLMMOJOMODELBASE.getDeclaredField("_beta");
			FIELD_FAMILY = CLASS_GLMMOJOMODELBASE.getDeclaredField("_family");
		} catch(ReflectiveOperationException roe){
			throw new RuntimeException(roe);
		}
	}
}