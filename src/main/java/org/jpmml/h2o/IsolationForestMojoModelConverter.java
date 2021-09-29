/*
 * Copyright (c) 2019 Villu Ruusmann
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

import hex.genmodel.algos.isofor.IsolationForestMojoModel;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMMLFunctions;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.mining.Segmentation;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.PMMLUtil;
import org.jpmml.converter.Schema;
import org.jpmml.converter.Transformation;
import org.jpmml.converter.mining.MiningModelUtil;
import org.jpmml.converter.transformations.AbstractTransformation;

public class IsolationForestMojoModelConverter extends SharedTreeMojoModelConverter<IsolationForestMojoModel> {

	public IsolationForestMojoModelConverter(IsolationForestMojoModel model){
		super(model);
	}

	@Override
	public MiningModel encodeModel(Schema schema){
		IsolationForestMojoModel model = getModel();

		int minPathLength = getMinPathLength(model);
		int maxPathLength = getMaxPathLength(model);

		if(minPathLength >= maxPathLength){
			throw new IllegalArgumentException();
		}

		List<TreeModel> treeModels = encodeTreeModels(schema);

		Transformation anomalyScore = new AbstractTransformation(){

			@Override
			public FieldName getName(FieldName name){
				return FieldName.create("anomalyScore");
			}

			@Override
			public boolean isFinalResult(){
				return true;
			}

			@Override
			public Expression createExpression(FieldRef fieldRef){
				return PMMLUtil.createApply(PMMLFunctions.DIVIDE, PMMLUtil.createApply(PMMLFunctions.SUBTRACT, PMMLUtil.createConstant(maxPathLength / (double)treeModels.size()), fieldRef), PMMLUtil.createConstant((maxPathLength - minPathLength) / (double)treeModels.size()));
			}
		};

		MiningModel miningModel = new MiningModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(null))
			.setSegmentation(MiningModelUtil.createSegmentation(Segmentation.MultipleModelMethod.AVERAGE, treeModels))
			.setOutput(ModelUtil.createPredictedOutput(FieldName.create("meanPathLength"), OpType.CONTINUOUS, DataType.DOUBLE, anomalyScore));

		return miningModel;
	}

	static
	public int getMaxPathLength(IsolationForestMojoModel model){
		return (int)getFieldValue(FIELD_MAX_PATH_LENGTH, model);
	}

	static
	public int getMinPathLength(IsolationForestMojoModel model){
		return (int)getFieldValue(FIELD_MIN_PATH_LENGTH, model);
	}

	private static final Field FIELD_MAX_PATH_LENGTH;
	private static final Field FIELD_MIN_PATH_LENGTH;

	static {

		try {
			FIELD_MAX_PATH_LENGTH = IsolationForestMojoModel.class.getDeclaredField("_max_path_length");
			FIELD_MIN_PATH_LENGTH = IsolationForestMojoModel.class.getDeclaredField("_min_path_length");
		} catch(ReflectiveOperationException roe){
			throw new RuntimeException(roe);
		}
	}
}