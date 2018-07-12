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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import hex.genmodel.MojoModel;
import org.dmg.pmml.MissingValueTreatmentMethod;
import org.jpmml.converter.BinaryFeature;
import org.jpmml.converter.CategoricalFeature;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.Label;
import org.jpmml.converter.MissingValueDecorator;
import org.jpmml.converter.PMMLEncoder;
import org.jpmml.converter.Schema;
import org.jpmml.converter.ValueUtil;

abstract
public class GlmMojoModelBaseConverter<M extends MojoModel> extends Converter<M> {

	public GlmMojoModelBaseConverter(M model){
		super(model);
	}

	@Override
	public Schema encodeSchema(H2OEncoder encoder){
		M model = getModel();

		boolean meanImputation = getMeanImputation(model);
		boolean useAllFactorLevels = getUseAllFactorLevels(model);

		Schema schema = super.encodeSchema(encoder);

		Label label = schema.getLabel();
		List<? extends Feature> features = schema.getFeatures();

		if(meanImputation){
			int cats = getCats(model);
			int[] catModes = getCatModes(model);

			int nums = getNums(model);
			double[] numMeans = getNumMeans(model);

			if(features.size() != (cats + nums) || (cats != catModes.length) || (nums != numMeans.length)){
				throw new IllegalArgumentException();
			}

			for(int i = 0; i < cats; i++){
				CategoricalFeature categoricalFeature = (CategoricalFeature)features.get(i);

				List<String> values = categoricalFeature.getValues();

				MissingValueDecorator missingValueDecorator = new MissingValueDecorator()
					.setMissingValueReplacement(values.get(catModes[i]))
					.setMissingValueTreatment(MissingValueTreatmentMethod.AS_MODE);

				encoder.addDecorator(categoricalFeature.getName(), missingValueDecorator);
			} // End for

			for(int i = 0; i < nums; i++){
				ContinuousFeature continuousFeature = (ContinuousFeature)features.get(cats + i);

				MissingValueDecorator missingValueDecorator = new MissingValueDecorator()
					.setMissingValueReplacement(ValueUtil.formatValue(numMeans[i]))
					.setMissingValueTreatment(MissingValueTreatmentMethod.AS_MEAN);

				encoder.addDecorator(continuousFeature.getName(), missingValueDecorator);
			}
		}

		Function<Feature, Stream<Feature>> function = new Function<Feature, Stream<Feature>>(){

			@Override
			public Stream<Feature> apply(Feature feature){
				PMMLEncoder encoder = feature.getEncoder();

				if(feature instanceof CategoricalFeature){
					CategoricalFeature categoricalFeature = (CategoricalFeature)feature;

					List<String> values = categoricalFeature.getValues();
					if(!useAllFactorLevels){
						values = values.subList(1, values.size());
					}

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
	public int getCats(MojoModel model){
		return (int)getFieldValue(GlmMojoModelBaseConverter.FIELD_CATS, model);
	}

	static
	public int[] getCatModes(MojoModel model){
		return (int[])getFieldValue(GlmMojoModelBaseConverter.FIELD_CATMODES, model);
	}

	static
	public String getFamily(MojoModel model){
		return (String)getFieldValue(GlmMojoModelBaseConverter.FIELD_FAMILY, model);
	}

	static
	public boolean getMeanImputation(MojoModel model){
		return (boolean)getFieldValue(GlmMojoModelBaseConverter.FIELD_MEANIMPUTATION, model);
	}

	static
	public int getNums(MojoModel model){
		return (int)getFieldValue(GlmMojoModelBaseConverter.FIELD_NUMS, model);
	}

	static
	public double[] getNumMeans(MojoModel model){
		return (double[])getFieldValue(GlmMojoModelBaseConverter.FIELD_NUMMEANS, model);
	}

	static
	public boolean getUseAllFactorLevels(MojoModel model){
		return (boolean)getFieldValue(GlmMojoModelBaseConverter.FIELD_USEALLFACTORLEVELS, model);
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
	private static final Field FIELD_CATS;
	private static final Field FIELD_CATMODES;
	private static final Field FIELD_FAMILY;
	private static final Field FIELD_MEANIMPUTATION;
	private static final Field FIELD_NUMS;
	private static final Field FIELD_NUMMEANS;
	private static final Field FIELD_USEALLFACTORLEVELS;

	static {

		try {
			FIELD_BETA = CLASS_GLMMOJOMODELBASE.getDeclaredField("_beta");
			FIELD_CATS = CLASS_GLMMOJOMODELBASE.getDeclaredField("_cats");
			FIELD_CATMODES = CLASS_GLMMOJOMODELBASE.getDeclaredField("_catModes");
			FIELD_FAMILY = CLASS_GLMMOJOMODELBASE.getDeclaredField("_family");
			FIELD_MEANIMPUTATION = CLASS_GLMMOJOMODELBASE.getDeclaredField("_meanImputation");
			FIELD_NUMS = CLASS_GLMMOJOMODELBASE.getDeclaredField("_nums");
			FIELD_NUMMEANS = CLASS_GLMMOJOMODELBASE.getDeclaredField("_numMeans");
			FIELD_USEALLFACTORLEVELS = CLASS_GLMMOJOMODELBASE.getDeclaredField("_useAllFactorLevels");
		} catch(ReflectiveOperationException roe){
			throw new RuntimeException(roe);
		}
	}
}