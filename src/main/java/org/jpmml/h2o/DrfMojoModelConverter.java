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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import hex.genmodel.algos.drf.DrfMojoModel;
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
import org.jpmml.converter.Feature;
import org.jpmml.converter.Label;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.Schema;
import org.jpmml.converter.mining.MiningModelUtil;

public class DrfMojoModelConverter extends SharedTreeMojoModelConverter<DrfMojoModel> {

	public DrfMojoModelConverter(DrfMojoModel model){
		super(model);
	}

	@Override
	public Model encodeModel(Schema schema){
		DrfMojoModel model = getModel();

		boolean binomialDoubleTrees = getBinomialDoubleTrees(model);
		byte[][] compressedTrees = getCompressedTrees(model);
		Number mojoVersion = getMojoVersion(model);
		int ntreeGroups = getNTreeGroups(model);
		int ntreesPerGroup = getNTreesPerGroup(model);

		if(mojoVersion.doubleValue() != 1.2D){
			throw new IllegalArgumentException();
		}

		Label label = schema.getLabel();
		List<? extends Feature> features = schema.getFeatures();

		List<TreeModel> treeModels = Stream.of(compressedTrees)
			.map(compressedTree -> encodeTreeModel(compressedTree, schema))
			.collect(Collectors.toList());

		if(model._nclasses == 1){
			ContinuousLabel continuousLabel = (ContinuousLabel)label;

			MiningModel miningModel = new MiningModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(continuousLabel))
				.setSegmentation(MiningModelUtil.createSegmentation(Segmentation.MultipleModelMethod.AVERAGE, treeModels));

			return miningModel;
		} else

		if(model._nclasses == 2 && !binomialDoubleTrees){
			MiningModel miningModel = new MiningModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(null))
				.setSegmentation(MiningModelUtil.createSegmentation(MultipleModelMethod.AVERAGE, treeModels))
				.setOutput(ModelUtil.createPredictedOutput(FieldName.create("drfValue"), OpType.CONTINUOUS, DataType.DOUBLE));

			return MiningModelUtil.createBinaryLogisticClassification(miningModel, -1d, 1d, RegressionModel.NormalizationMethod.NONE, true, schema);
		} else

		{
			CategoricalLabel categoricalLabel = (CategoricalLabel)label;

			List<Model> models = new ArrayList<>();

			for(int i = 0; i < categoricalLabel.size(); i++){
				MiningModel miningModel = new MiningModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(null))
					.setSegmentation(MiningModelUtil.createSegmentation(Segmentation.MultipleModelMethod.SUM, CMatrixUtil.getRow(treeModels, ntreesPerGroup, ntreeGroups, i)))
					.setOutput(ModelUtil.createPredictedOutput(FieldName.create("drfValue(" + categoricalLabel.getValue(i) + ")"), OpType.CONTINUOUS, DataType.DOUBLE));

				models.add(miningModel);
			}

			return MiningModelUtil.createClassification(models, RegressionModel.NormalizationMethod.SIMPLEMAX, true, schema);
		}
	}

	static
	public boolean getBinomialDoubleTrees(DrfMojoModel model){
		return (boolean)getFieldValue(FIELD_BOOLEANDOUBLETREES, model);
	}

	private static final Field FIELD_BOOLEANDOUBLETREES;

	static {

		try {
			FIELD_BOOLEANDOUBLETREES = DrfMojoModel.class.getDeclaredField("_binomial_double_trees");
		} catch(ReflectiveOperationException roe){
			throw new RuntimeException(roe);
		}
	}
}