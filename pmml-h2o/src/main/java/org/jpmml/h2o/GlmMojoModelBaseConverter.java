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
import org.dmg.pmml.MissingValueTreatmentMethod;
import org.jpmml.converter.BinaryFeature;
import org.jpmml.converter.CategoricalFeature;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.Label;
import org.jpmml.converter.ModelEncoder;
import org.jpmml.converter.Schema;
import org.jpmml.converter.SchemaUtil;

abstract
public class GlmMojoModelBaseConverter<M extends MojoModel> extends Converter<M> {

	public GlmMojoModelBaseConverter(M model){
		super(model);
	}

	@Override
	public Schema toMojoModelSchema(Schema schema){
		M model = getModel();

		int cats = getCats(model);
		int[] catOffsets = getCatOffsets(model);
		int nums = getNums(model);

		boolean meanImputation = getMeanImputation(model);
		boolean useAllFactorLevels = getUseAllFactorLevels(model);

		ModelEncoder encoder = schema.getEncoder();
		Label label = schema.getLabel();
		List<? extends Feature> features = schema.getFeatures();

		List<? extends Feature> categoricalFeatures = features.stream()
			.filter(feature -> (feature instanceof CategoricalFeature))
			.collect(Collectors.toList());

		SchemaUtil.checkSize(cats, categoricalFeatures);

		for(int i = 0; i < cats; i++){
			CategoricalFeature categoricalFeature = (CategoricalFeature)categoricalFeatures.get(i);

			SchemaUtil.checkCardinality((catOffsets[i + 1] - catOffsets[i]) + (useAllFactorLevels ? 0 : 1), categoricalFeature);
		}

		List<? extends Feature> continuousFeatures = features.stream()
			.filter(feature -> !(feature instanceof CategoricalFeature))
			.map(feature -> feature.toContinuousFeature())
			.collect(Collectors.toList());

		SchemaUtil.checkSize(nums, continuousFeatures);

		List<Feature> reorderedFeatures = new ArrayList<>();
		reorderedFeatures.addAll(categoricalFeatures);
		reorderedFeatures.addAll(continuousFeatures);

		features = reorderedFeatures;

		if(meanImputation){
			int[] catModes = getCatModes(model);
			double[] numMeans = getNumMeans(model);

			if(catModes.length != cats){
				throw new IllegalArgumentException("Expected " + cats + " mode values, got " + catModes.length + " mode values");
			} // End if

			if(numMeans.length != nums){
				throw new IllegalArgumentException("Expected " + nums + " mean values, got " + numMeans.length + " mean values");
			}

			for(int i = 0; i < cats; i++){
				CategoricalFeature categoricalFeature = (CategoricalFeature)categoricalFeatures.get(i);

				List<?> values = categoricalFeature.getValues();

				ImputerUtil.encodeFeature(categoricalFeature, values.get(catModes[i]), MissingValueTreatmentMethod.AS_MODE);
			} // End for

			for(int i = 0; i < nums; i++){
				ContinuousFeature continuousFeature = (ContinuousFeature)continuousFeatures.get(i);

				ImputerUtil.encodeFeature(continuousFeature, numMeans[i], MissingValueTreatmentMethod.AS_MEAN);
			}
		}

		Function<Feature, Stream<Feature>> function = new Function<Feature, Stream<Feature>>(){

			@Override
			public Stream<Feature> apply(Feature feature){

				if(feature instanceof CategoricalFeature){
					CategoricalFeature categoricalFeature = (CategoricalFeature)feature;

					List<?> values = categoricalFeature.getValues();
					if(!useAllFactorLevels){
						values = values.subList(1, values.size());
					}

					return values.stream()
						.map(value -> new BinaryFeature(encoder, categoricalFeature, value));
				}

				return Stream.of(feature);
			}
		};

		features = features.stream()
			.flatMap(function)
			.collect(Collectors.toList());

		return new Schema(encoder, label, features);
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
	public int[] getCatOffsets(MojoModel model){
		return (int[])getFieldValue(GlmMojoModelBaseConverter.FIELD_CATOFFSETS, model);
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
	private static final Field FIELD_CATOFFSETS;
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
			FIELD_CATOFFSETS = CLASS_GLMMOJOMODELBASE.getDeclaredField("_catOffsets");
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