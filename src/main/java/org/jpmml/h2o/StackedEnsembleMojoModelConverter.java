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

import hex.genmodel.MojoModel;
import hex.genmodel.algos.ensemble.StackedEnsembleMojoModel;
import hex.genmodel.algos.tree.SharedTreeMojoModel;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Model;
import org.dmg.pmml.OpType;
import org.dmg.pmml.OutputField;
import org.jpmml.converter.CategoricalLabel;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.ContinuousLabel;
import org.jpmml.converter.DerivedOutputField;
import org.jpmml.converter.Feature;
import org.jpmml.converter.FieldNameUtil;
import org.jpmml.converter.Label;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.Schema;
import org.jpmml.converter.SchemaUtil;

public class StackedEnsembleMojoModelConverter extends Converter<StackedEnsembleMojoModel> {

	public StackedEnsembleMojoModelConverter(StackedEnsembleMojoModel model){
		super(model);
	}

	@Override
	public Schema encodeSchema(H2OEncoder encoder){
		StackedEnsembleMojoModel model = getModel();

		ConverterFactory converterFactory = ConverterFactory.newConverterFactory();

		Schema schema = super.encodeSchema(encoder);

		Label label = schema.getLabel();
		List<Feature> features = new ArrayList<>();

		Schema segmentSchema = schema.toAnonymousSchema();

		Object[] baseModels = getBaseModels(model);
		for(int i = 0; i < baseModels.length; i++){
			Object baseModel = baseModels[i];

			MojoModel mojoModel = getMojoModel(baseModel);
			int[] mapping = getMapping(baseModel);

			if(!(mojoModel instanceof SharedTreeMojoModel)){
				throw new IllegalArgumentException("Stacking of models other than decision tree models is not supported");
			} // End if

			if(mapping != null && !isSequential(mapping)){
				throw new IllegalArgumentException("Feature re-indexing is not supported");
			}

			Converter<?> converter = converterFactory.newConverter(mojoModel);

			Schema baseModelSchema = converter.toMojoModelSchema(segmentSchema);

			Model segmentModel = converter.encodeModel(baseModelSchema);

			if(model._nclasses == 1){
				ContinuousLabel continuousLabel = (ContinuousLabel)label;

				OutputField predictedOutputField = ModelUtil.createPredictedField(FieldNameUtil.create("stack", i), OpType.CONTINUOUS, DataType.DOUBLE)
					.setFinalResult(false);

				DerivedOutputField predictedField = encoder.createDerivedField(segmentModel, predictedOutputField, false);

				features.add(new ContinuousFeature(encoder, predictedField));
			} else

			{
				CategoricalLabel categoricalLabel = (CategoricalLabel)label;

				SchemaUtil.checkSize(model._nclasses, categoricalLabel);

				List<?> values = categoricalLabel.getValues();

				if(model._nclasses == 2){
					values = values.subList(1, 2);
				}

				for(Object value : values){
					OutputField probabilityOutputField = ModelUtil.createProbabilityField(FieldName.create("stack(" + i +", " + value + ")"), DataType.DOUBLE, value)
						.setFinalResult(false);

					DerivedOutputField probabilityField = encoder.createDerivedField(segmentModel, probabilityOutputField, false);

					features.add(new ContinuousFeature(encoder, probabilityField));
				}
			}

			encoder.addTransformer(segmentModel);
		}

		return new Schema(encoder, label, features);
	}

	@Override
	public Model encodeModel(Schema schema){
		StackedEnsembleMojoModel model = getModel();

		ConverterFactory converterFactory = ConverterFactory.newConverterFactory();

		MojoModel metaLearner = getMetaLearner(model);
		if(metaLearner == null){
			throw new IllegalArgumentException();
		}

		Converter<?> converter = converterFactory.newConverter(metaLearner);

		Schema metaLearnerSchema = converter.toMojoModelSchema(schema);

		return converter.encodeModel(metaLearnerSchema);
	}

	static
	public Object[] getBaseModels(StackedEnsembleMojoModel model){
		return (Object[])getFieldValue(StackedEnsembleMojoModelConverter.FIELD_BASEMODELS, model);
	}

	static
	public MojoModel getMetaLearner(StackedEnsembleMojoModel model){
		return (MojoModel)getFieldValue(StackedEnsembleMojoModelConverter.FIELD_METALEARNER, model);
	}

	static
	public MojoModel getMojoModel(Object baseModel){
		return (MojoModel)getFieldValue(StackedEnsembleMojoModelConverter.FIELD_MOJOMODEL, baseModel);
	}

	static
	public int[] getMapping(Object baseModel){
		return (int[])getFieldValue(StackedEnsembleMojoModelConverter.FIELD_MAPPING, baseModel);
	}

	static
	private boolean isSequential(int[] values){

		for(int i = 0; i < values.length; i++){

			if(values[i] != i){
				return false;
			}
		}

		return true;
	}

	private static final Field FIELD_BASEMODELS;
	private static final Field FIELD_METALEARNER;

	static {

		try {
			FIELD_BASEMODELS = StackedEnsembleMojoModel.class.getDeclaredField("_baseModels");
			FIELD_METALEARNER = StackedEnsembleMojoModel.class.getDeclaredField("_metaLearner");
		} catch(ReflectiveOperationException roe){
			throw new RuntimeException(roe);
		}
	}

	private static final Class<?> CLASS_STACKEDENSEMBLESUBMODEL;

	static {

		try {
			CLASS_STACKEDENSEMBLESUBMODEL = getDeclaredClass(StackedEnsembleMojoModel.class, "StackedEnsembleMojoSubModel");
		} catch(ReflectiveOperationException roe){
			throw new RuntimeException(roe);
		}
	}

	private static final Field FIELD_MOJOMODEL;
	private static final Field FIELD_MAPPING;

	static {

		try {
			FIELD_MOJOMODEL = CLASS_STACKEDENSEMBLESUBMODEL.getDeclaredField("_mojoModel");
			FIELD_MAPPING = CLASS_STACKEDENSEMBLESUBMODEL.getDeclaredField("_mapping");
		} catch(ReflectiveOperationException roe){
			throw new RuntimeException(roe);
		}
	}
}