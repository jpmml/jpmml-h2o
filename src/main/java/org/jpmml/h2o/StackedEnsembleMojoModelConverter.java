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
import org.dmg.pmml.Output;
import org.dmg.pmml.OutputField;
import org.jpmml.converter.CategoricalLabel;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.ContinuousLabel;
import org.jpmml.converter.Feature;
import org.jpmml.converter.Label;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.Schema;
import org.jpmml.converter.mining.MiningModelUtil;

public class StackedEnsembleMojoModelConverter extends Converter<StackedEnsembleMojoModel> {

	public StackedEnsembleMojoModelConverter(StackedEnsembleMojoModel model){
		super(model);
	}

	@Override
	public Model encodeModel(Schema schema){
		StackedEnsembleMojoModel model = getModel();

		ConverterFactory converterFactory = ConverterFactory.newConverterFactory();

		Label label = schema.getLabel();
		List<Feature> features = new ArrayList<>();

		List<Model> models = new ArrayList<>();

		Schema segmentSchema = schema.toAnonymousSchema();

		H2OEncoder encoder = new H2OEncoder();

		MojoModel[] baseModels = getBaseModels(model);
		for(int i = 0; i < baseModels.length; i++){
			MojoModel baseModel = baseModels[i];

			// XXX
			if(!(baseModel instanceof SharedTreeMojoModel)){
				throw new IllegalArgumentException();
			}

			Converter<?> converter = converterFactory.newConverter(baseModel);

			Schema baseModelSchema = converter.toMojoModelSchema(segmentSchema);

			Model segmentModel = converter.encodeModel(baseModelSchema);

			List<OutputField> outputFields = new ArrayList<>();

			if(model._nclasses == 1){
				ContinuousLabel continuousLabel = (ContinuousLabel)label;

				OutputField predictedField = ModelUtil.createPredictedField(FieldName.create("stack(" + i + ")"), DataType.DOUBLE, OpType.CONTINUOUS)
					.setFinalResult(false);

				outputFields.add(predictedField);
			} else

			if(model._nclasses >= 2){
				CategoricalLabel categoricalLabel = (CategoricalLabel)label;

				List<String> values = categoricalLabel.getValues();

				if(model._nclasses == 2){
					values = values.subList(1, 2);
				}

				for(String value : values){
					OutputField probabilityField = ModelUtil.createProbabilityField(FieldName.create("stack(" + i +", " + value + ")"), DataType.DOUBLE, value)
						.setFinalResult(false);

					outputFields.add(probabilityField);
				}
			} else

			{
				throw new IllegalArgumentException();
			}

			Output segmentOutput = ModelUtil.ensureOutput(segmentModel);

			for(OutputField outputField : outputFields){
				segmentOutput.addOutputFields(outputField);

				features.add(new ContinuousFeature(encoder, outputField));

				// XXX
				encoder.createDataField(outputField.getName(), null);
			}

			models.add(segmentModel);
		}

		MojoModel metaLearner = getMetaLearner(model);
		if(metaLearner != null){
			Converter<?> converter = converterFactory.newConverter(metaLearner);

			Schema metaLearnerSchema = converter.toMojoModelSchema(new Schema(label, features));

			Model segmentModel = converter.encodeModel(metaLearnerSchema);

			models.add(segmentModel);
		}

		return MiningModelUtil.createModelChain(models, schema);
	}

	static
	public MojoModel[] getBaseModels(StackedEnsembleMojoModel model){
		return (MojoModel[])getFieldValue(StackedEnsembleMojoModelConverter.FIELD_BASEMODELS, model);
	}

	public MojoModel getMetaLearner(StackedEnsembleMojoModel model){
		return (MojoModel)getFieldValue(StackedEnsembleMojoModelConverter.FIELD_METALEARNER, model);
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
}