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

import hex.genmodel.algos.gbm.GbmMojoModel;
import hex.genmodel.utils.DistributionFamily;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.Model;
import org.dmg.pmml.OpType;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.mining.Segmentation;
import org.dmg.pmml.mining.Segmentation.MultipleModelMethod;
import org.dmg.pmml.regression.RegressionModel;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.converter.CMatrixUtil;
import org.jpmml.converter.CategoricalLabel;
import org.jpmml.converter.ContinuousLabel;
import org.jpmml.converter.FieldNameUtil;
import org.jpmml.converter.Label;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.Schema;
import org.jpmml.converter.mining.MiningModelUtil;

public class GbmMojoModelConverter extends SharedTreeMojoModelConverter<GbmMojoModel> {

	public GbmMojoModelConverter(GbmMojoModel model){
		super(model);
	}

	@Override
	public MiningModel encodeModel(Schema schema){
		GbmMojoModel model = getModel();

		int ntreeGroups = getNTreeGroups(model);
		int ntreesPerGroup = getNTreesPerGroup(model);

		Label label = schema.getLabel();

		List<TreeModel> treeModels = encodeTreeModels(schema);

		if((DistributionFamily.gaussian).equals(model._family)){
			ContinuousLabel continuousLabel = (ContinuousLabel)label;

			MiningModel miningModel = new MiningModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(continuousLabel))
				.setSegmentation(MiningModelUtil.createSegmentation(MultipleModelMethod.SUM, treeModels))
				.setTargets(ModelUtil.createRescaleTargets(null, model._init_f, continuousLabel));

			return miningModel;
		} else

		if((DistributionFamily.poisson).equals(model._family) || (DistributionFamily.gamma).equals(model._family) || (DistributionFamily.tweedie).equals(model._family)){
			ContinuousLabel continuousLabel = new ContinuousLabel(null, DataType.DOUBLE);

			MiningModel miningModel = new MiningModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(continuousLabel))
				.setSegmentation(MiningModelUtil.createSegmentation(MultipleModelMethod.SUM, treeModels))
				.setTargets(ModelUtil.createRescaleTargets(null, model._init_f, continuousLabel))
				.setOutput(ModelUtil.createPredictedOutput(FieldName.create("gbmValue"), OpType.CONTINUOUS, DataType.DOUBLE));

			return MiningModelUtil.createRegression(miningModel, RegressionModel.NormalizationMethod.EXP, schema);
		} else

		if((DistributionFamily.bernoulli).equals(model._family)){
			ContinuousLabel continuousLabel = new ContinuousLabel(null, DataType.DOUBLE);

			MiningModel miningModel = new MiningModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(continuousLabel))
				.setSegmentation(MiningModelUtil.createSegmentation(Segmentation.MultipleModelMethod.SUM, treeModels))
				.setTargets(ModelUtil.createRescaleTargets(null, model._init_f, continuousLabel))
				.setOutput(ModelUtil.createPredictedOutput(FieldName.create("gbmValue"), OpType.CONTINUOUS, DataType.DOUBLE));

			return MiningModelUtil.createBinaryLogisticClassification(miningModel, 1d, 0d, RegressionModel.NormalizationMethod.LOGIT, true, schema);
		} else

		if((DistributionFamily.multinomial).equals(model._family)){
			CategoricalLabel categoricalLabel = (CategoricalLabel)label;

			List<Model> models = new ArrayList<>();

			for(int i = 0; i < categoricalLabel.size(); i++){
				MiningModel miningModel = new MiningModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(null))
					.setSegmentation(MiningModelUtil.createSegmentation(Segmentation.MultipleModelMethod.SUM, CMatrixUtil.getRow(treeModels, ntreesPerGroup, ntreeGroups, i)))
					.setOutput(ModelUtil.createPredictedOutput(FieldNameUtil.create("gbmValue", categoricalLabel.getValue(i)), OpType.CONTINUOUS, DataType.DOUBLE));

				models.add(miningModel);
			}

			return MiningModelUtil.createClassification(models, RegressionModel.NormalizationMethod.SOFTMAX, true, schema);
		} else

		{
			throw new IllegalArgumentException("Distribution family " + model._family + " is not supported");
		}
	}
}