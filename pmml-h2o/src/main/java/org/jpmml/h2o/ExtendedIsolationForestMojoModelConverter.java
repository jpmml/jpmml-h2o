/*
 * Copyright (c) 2024 Villu Ruusmann
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;
import hex.genmodel.algos.isoforextended.ExtendedIsolationForestMojoModel;
import hex.genmodel.utils.ByteBufferWrapper;
import org.dmg.pmml.Apply;
import org.dmg.pmml.DataType;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMMLFunctions;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.SimplePredicate;
import org.dmg.pmml.True;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.mining.Segmentation;
import org.dmg.pmml.tree.BranchNode;
import org.dmg.pmml.tree.CountingLeafNode;
import org.dmg.pmml.tree.LeafNode;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.ContinuousLabel;
import org.jpmml.converter.ExpressionUtil;
import org.jpmml.converter.Feature;
import org.jpmml.converter.FieldNameUtil;
import org.jpmml.converter.Label;
import org.jpmml.converter.ModelEncoder;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.Schema;
import org.jpmml.converter.Transformation;
import org.jpmml.converter.ValueUtil;
import org.jpmml.converter.mining.MiningModelUtil;
import org.jpmml.converter.transformations.AbstractTransformation;

public class ExtendedIsolationForestMojoModelConverter extends Converter<ExtendedIsolationForestMojoModel> {

	public ExtendedIsolationForestMojoModelConverter(ExtendedIsolationForestMojoModel model){
		super(model);
	}

	@Override
	public MiningModel encodeModel(Schema schema){
		ExtendedIsolationForestMojoModel model = getModel();

		long sampleSize = getSampleSize(model);

		List<TreeModel> treeModels = encodeTreeModels(schema);

		Transformation anomalyScore = new AbstractTransformation(){

			@Override
			public String getName(String name){
				return "anomalyScore";
			}

			@Override
			public boolean isFinalResult(){
				return true;
			}

			@Override
			public Expression createExpression(FieldRef fieldRef){
				return ExpressionUtil.createApply(PMMLFunctions.POW, ExpressionUtil.createConstant(2d), ExpressionUtil.createApply(PMMLFunctions.DIVIDE, fieldRef, ExpressionUtil.createConstant(-1d * ExtendedIsolationForestMojoModel.averagePathLengthOfUnsuccessfulSearch(sampleSize))));
			}
		};

		MiningModel miningModel = new MiningModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(null))
			.setSegmentation(MiningModelUtil.createSegmentation(Segmentation.MultipleModelMethod.AVERAGE, Segmentation.MissingPredictionTreatment.RETURN_MISSING, treeModels))
			.setOutput(ModelUtil.createPredictedOutput("meanPathLength", OpType.CONTINUOUS, DataType.DOUBLE, anomalyScore));

		return miningModel;
	}

	private List<TreeModel> encodeTreeModels(Schema schema){
		ExtendedIsolationForestMojoModel model = getModel();

		byte[][] compressedTrees = getCompressedTrees(model);

		List<TreeModel> result = new ArrayList<>();

		for(int i = 0, max = compressedTrees.length; i < max; i++){
			byte[] compressedTree = compressedTrees[i];

			TreeModel treeModel = encodeTreeModel(i, compressedTree, schema);

			result.add(treeModel);
		}

		return result;
	}

	private TreeModel encodeTreeModel(int index, byte[] compressedTree, Schema schema){
		ByteBufferWrapper byteBuffer = new ByteBufferWrapper(compressedTree);

		Map<Integer, Node> nodeMap = new HashMap<>();
		Map<Integer, Feature> featureMap = new HashMap<>();
		Map<Integer, Integer> countMap = new HashMap<>();

		int sizeOfBranchingArrays = byteBuffer.get4();

		double[] n = new double[sizeOfBranchingArrays];
		double[] p = new double[sizeOfBranchingArrays];

		while(byteBuffer.hasRemaining()){
			int nodeNumber = byteBuffer.get4();
			int nodeType = byteBuffer.get1U();

			if(nodeNumber == 0){

				if(!nodeMap.isEmpty()){
					break;
				}
			}

			Node node;

			switch(nodeType){
				case 'N':
					node = new BranchNode();

					Feature feature = loadFeature(FieldNameUtil.create("split", index, nodeNumber), byteBuffer, n, p, schema);
					featureMap.put(nodeNumber, feature);

					break;
				case 'L':
					node = new CountingLeafNode();

					int numRows = loadSampleSize(byteBuffer);
					countMap.put(nodeNumber, numRows);

					break;
				default:
					throw new IllegalArgumentException();
			}

			nodeMap.put(nodeNumber, node);
		}

		Label label = new ContinuousLabel(DataType.DOUBLE);

		Node root = encodeNode(0, True.INSTANCE, 0, nodeMap, featureMap, countMap);

		TreeModel treeModel = new TreeModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(label), root);

		return treeModel;
	}

	static
	private Node encodeNode(int index, Predicate predicate, int height, Map<Integer, Node> nodeMap, Map<Integer, Feature> featureMap, Map<Integer, Integer> countMap){
		Node result = nodeMap.get(index);

		result
			.setId(index)
			.setPredicate(predicate);

		if(result instanceof BranchNode){
			Feature feature = featureMap.get(index);

			String name = feature.getName();

			Predicate leftPredicate = new SimplePredicate(name, SimplePredicate.Operator.LESS_OR_EQUAL, 0);
			Predicate rightPredicate = new SimplePredicate(name, SimplePredicate.Operator.GREATER_THAN, 0);

			Node leftChild = encodeNode(2 * index + 1, leftPredicate, height + 1, nodeMap, featureMap, countMap);
			Node rightChild = encodeNode(2 * index + 2, rightPredicate, height + 1, nodeMap, featureMap, countMap);

			result.addNodes(leftChild, rightChild);
		} else

		if(result instanceof LeafNode){
			Integer numRows = countMap.get(index);

			result.setScore(height + ExtendedIsolationForestMojoModel.averagePathLengthOfUnsuccessfulSearch(numRows));
		} else

		{
			throw new IllegalArgumentException();
		}

		return result;
	}

	static
	private Feature loadFeature(String name, ByteBufferWrapper byteBuffer, double[] n, double[] p, Schema schema){
		ModelEncoder encoder = (ModelEncoder)schema.getEncoder();

		for(int i = 0; i < n.length; i++){
			n[i] = byteBuffer.get8d();
		}

		for(int i = 0; i < p.length; i++){
			p[i] = byteBuffer.get8d();
		}

		List<Expression> expressions = new ArrayList<>();

		for(int i = 0; i < n.length; i++){
			Feature feature = schema.getFeature(i);

			if(ValueUtil.isZero(n[i])){
				continue;
			}

			ContinuousFeature continuousFeature = feature.toContinuousFeature();

			Expression expression = continuousFeature.ref();

			if(!ValueUtil.isOne(p[i])){
				expression = ExpressionUtil.createApply(PMMLFunctions.SUBTRACT, expression, ExpressionUtil.createConstant(p[i]));
			}

			expression = ExpressionUtil.createApply(PMMLFunctions.MULTIPLY, expression, ExpressionUtil.createConstant(n[i]));

			expressions.add(expression);
		}

		Expression expression;

		if(expressions.size() == 1){
			expression = Iterables.getOnlyElement(expressions);
		} else

		if(expressions.size() >= 2){
			Apply apply = ExpressionUtil.createApply(PMMLFunctions.SUM);

			(apply.getExpressions()).addAll(expressions);

			expression  =apply;
		} else

		{
			throw new IllegalArgumentException();
		}

		DerivedField derivedField = encoder.createDerivedField(name, OpType.CONTINUOUS, DataType.DOUBLE, expression);

		return new ContinuousFeature(encoder, derivedField);
	}

	static
	private int loadSampleSize(ByteBufferWrapper byteBuffer){
		return byteBuffer.get4();
	}

	static
	public byte[][] getCompressedTrees(ExtendedIsolationForestMojoModel model){
		return (byte[][])getFieldValue(FIELD_COMPRESSEDTREES, model);
	}

	static
	public long getSampleSize(ExtendedIsolationForestMojoModel model){
		return (long)getFieldValue(FIELD_SAMPLE_SIZE, model);
	}

	private static final Field FIELD_COMPRESSEDTREES;
	private static final Field FIELD_SAMPLE_SIZE;

	static {

		try {
			FIELD_COMPRESSEDTREES = ExtendedIsolationForestMojoModel.class.getDeclaredField("_compressedTrees");
			FIELD_SAMPLE_SIZE = ExtendedIsolationForestMojoModel.class.getDeclaredField("_sample_size");
		} catch(ReflectiveOperationException roe){
			throw new RuntimeException(roe);
		}
	}
}