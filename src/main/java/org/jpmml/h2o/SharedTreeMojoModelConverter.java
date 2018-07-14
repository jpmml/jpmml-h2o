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

import hex.genmodel.algos.tree.SharedTreeMojoModel;
import hex.genmodel.utils.ByteBufferWrapper;
import hex.genmodel.utils.GenmodelBitSet;
import org.dmg.pmml.DataType;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.SimplePredicate;
import org.dmg.pmml.SimpleSetPredicate;
import org.dmg.pmml.True;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.converter.CategoricalFeature;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.ContinuousLabel;
import org.jpmml.converter.Feature;
import org.jpmml.converter.Label;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.PMMLUtil;
import org.jpmml.converter.Schema;
import org.jpmml.converter.ValueUtil;

abstract
public class SharedTreeMojoModelConverter<M extends SharedTreeMojoModel> extends Converter<M> {

	public SharedTreeMojoModelConverter(M model){
		super(model);
	}

	static
	public TreeModel encodeTreeModel(byte[] compressedTree, Schema schema){
		Label label = new ContinuousLabel(null, DataType.DOUBLE);

		Node root = new Node()
			.setPredicate(new True());

		ByteBufferWrapper buffer = new ByteBufferWrapper(compressedTree);

		encodeNode(root, compressedTree, buffer, schema);

		return new TreeModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(label), root);
	}

	static
	public void encodeNode(Node node, byte[] compressedTree, ByteBufferWrapper byteBuffer, Schema schema){
		int nodeType = byteBuffer.get1U();

		int lmask = (nodeType & 51);
		int lmask2 = (nodeType & 0xC0) >> 2;

		int equal = (nodeType & 12);

		int colId = byteBuffer.get2();
		if(colId == 65535){
			throw new IllegalArgumentException();
		}

		int naSplitDir = byteBuffer.get1U();

		Feature feature = schema.getFeature(colId);

		Predicate leftPredicate;
		Predicate rightPredicate;

		if(equal != 0){
			CategoricalFeature categoricalFeature = (CategoricalFeature)feature;

			GenmodelBitSet bitSet = new GenmodelBitSet(0);

			if(equal == 8){
				bitSet.fill2(compressedTree, byteBuffer);
			} else

			if(equal == 12){
				bitSet.fill3(compressedTree, byteBuffer);
			} else

			{
				throw new IllegalArgumentException();
			}

			List<String> values = categoricalFeature.getValues();

			List<String> leftValues = new ArrayList<>();
			List<String> rightValues = new ArrayList<>();

			for(int i = 0; i < values.size(); i++){
				String value = values.get(i);

				if(!bitSet.contains(i)){
					leftValues.add(value);
				} else

				{
					rightValues.add(value);
				}
			}

			leftPredicate = new SimpleSetPredicate(categoricalFeature.getName(), SimpleSetPredicate.BooleanOperator.IS_IN, PMMLUtil.createStringArray(leftValues));
			rightPredicate = new SimpleSetPredicate(categoricalFeature.getName(), SimpleSetPredicate.BooleanOperator.IS_IN, PMMLUtil.createStringArray(rightValues));
		} else

		{
			ContinuousFeature continuousFeature = feature.toContinuousFeature();

			double splitVal = byteBuffer.get4f();

			String value = ValueUtil.formatValue(splitVal);

			leftPredicate = new SimplePredicate(continuousFeature.getName(), SimplePredicate.Operator.LESS_THAN)
				.setValue(value);

			rightPredicate = new SimplePredicate(continuousFeature.getName(), SimplePredicate.Operator.GREATER_OR_EQUAL)
				.setValue(value);
		}

		Node leftChild = new Node()
			.setPredicate(leftPredicate);

		ByteBufferWrapper leftByteBuffer = new ByteBufferWrapper(compressedTree);
		leftByteBuffer.skip(byteBuffer.position());

		if(lmask <= 3){
			leftByteBuffer.skip(lmask + 1);
		} // End if

		if((lmask & 16) != 0){
			double score = leftByteBuffer.get4f();

			leftChild.setScore(ValueUtil.formatValue(score));
		} else

		{
			encodeNode(leftChild, compressedTree, leftByteBuffer, schema);
		}

		Node rightChild = new Node()
			.setPredicate(rightPredicate);

		ByteBufferWrapper rightByteBuffer = new ByteBufferWrapper(compressedTree);
		rightByteBuffer.skip(byteBuffer.position());

		switch(lmask){
			case 0:
				rightByteBuffer.skip(rightByteBuffer.get1U());
				break;
			case 1:
				rightByteBuffer.skip(rightByteBuffer.get2());
				break;
			case 2:
				rightByteBuffer.skip(rightByteBuffer.get3());
				break;
			case 3:
				rightByteBuffer.skip(rightByteBuffer.get4());
				break;
			case 48:
				rightByteBuffer.skip(4);
				break;
			default:
				throw new IllegalArgumentException();
		}

		if((lmask2 & 16) != 0){
			double score = rightByteBuffer.get4f();

			rightChild.setScore(ValueUtil.formatValue(score));
		} else

		{
			encodeNode(rightChild, compressedTree, rightByteBuffer, schema);
		}

		node.addNodes(leftChild, rightChild);
	}

	static
	public byte[][] getCompressedTrees(SharedTreeMojoModel model){
		return (byte[][])getFieldValue(FIELD_COMPRESSEDTREES, model);
	}

	static
	public Number getMojoVersion(SharedTreeMojoModel model){
		return (Number)getFieldValue(FIELD_MOJOVERSION, model);
	}

	static
	public int getNTreeGroups(SharedTreeMojoModel model){
		return (int)getFieldValue(FIELD_NTREEGROUPS, model);
	}

	static
	public int getNTreesPerGroup(SharedTreeMojoModel model){
		return (int)getFieldValue(FIELD_NTREESPERGROUP, model);
	}

	private static final Field FIELD_COMPRESSEDTREES;
	private static final Field FIELD_MOJOVERSION;
	private static final Field FIELD_NTREEGROUPS;
	private static final Field FIELD_NTREESPERGROUP;

	static {

		try {
			FIELD_COMPRESSEDTREES = SharedTreeMojoModel.class.getDeclaredField("_compressed_trees");
			FIELD_MOJOVERSION = SharedTreeMojoModel.class.getDeclaredField("_mojo_version");
			FIELD_NTREEGROUPS = SharedTreeMojoModel.class.getDeclaredField("_ntree_groups");
			FIELD_NTREESPERGROUP = SharedTreeMojoModel.class.getDeclaredField("_ntrees_per_group");
		} catch(ReflectiveOperationException roe){
			throw new RuntimeException(roe);
		}
	}
}