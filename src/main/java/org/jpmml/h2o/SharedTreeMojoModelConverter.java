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
import java.util.concurrent.atomic.AtomicInteger;

import hex.genmodel.algos.tree.NaSplitDir;
import hex.genmodel.algos.tree.SharedTreeMojoModel;
import hex.genmodel.utils.ByteBufferWrapper;
import hex.genmodel.utils.GenmodelBitSet;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.SimplePredicate;
import org.dmg.pmml.True;
import org.dmg.pmml.tree.BranchNode;
import org.dmg.pmml.tree.LeafNode;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.converter.CategoricalFeature;
import org.jpmml.converter.CategoryManager;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.ContinuousLabel;
import org.jpmml.converter.Feature;
import org.jpmml.converter.Label;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.PredicateManager;
import org.jpmml.converter.Schema;

abstract
public class SharedTreeMojoModelConverter<M extends SharedTreeMojoModel> extends Converter<M> {

	public SharedTreeMojoModelConverter(M model){
		super(model);
	}

	static
	public TreeModel encodeTreeModel(byte[] compressedTree, PredicateManager predicateManager, Schema schema){
		Label label = new ContinuousLabel(null, DataType.DOUBLE);

		AtomicInteger idSequence = new AtomicInteger(1);

		ByteBufferWrapper buffer = new ByteBufferWrapper(compressedTree);

		Node root = encodeNode(new True(), idSequence, compressedTree, buffer, predicateManager, new CategoryManager(), schema);

		TreeModel treeModel = new TreeModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(label), root)
			.setMissingValueStrategy(TreeModel.MissingValueStrategy.DEFAULT_CHILD);

		return treeModel;
	}

	static
	public Node encodeNode(Predicate predicate, AtomicInteger idSequence, byte[] compressedTree, ByteBufferWrapper byteBuffer, PredicateManager predicateManager, CategoryManager categoryManager, Schema schema){
		Integer id = nextId(idSequence);

		int nodeType = byteBuffer.get1U();

		int lmask = (nodeType & 51);
		int lmask2 = (nodeType & 0xC0) >> 2;

		int equal = (nodeType & 12);

		int colId = byteBuffer.get2();
		if(colId == 65535){
			throw new IllegalArgumentException();
		}

		int naSplitDir = byteBuffer.get1U();

		boolean naVsRest = (naSplitDir == NaSplitDir.NAvsREST.value());
		boolean leftward = (naSplitDir == NaSplitDir.NALeft.value()) || (naSplitDir == NaSplitDir.Left.value());

		Feature feature = schema.getFeature(colId);

		CategoryManager leftCategoryManager = categoryManager;
		CategoryManager rightCategoryManager = categoryManager;

		Predicate leftPredicate;
		Predicate rightPredicate;

		if(naVsRest){
			leftPredicate = predicateManager.createSimplePredicate(feature, SimplePredicate.Operator.IS_NOT_MISSING, null);
			rightPredicate = predicateManager.createSimplePredicate(feature, SimplePredicate.Operator.IS_MISSING, null);
		} else

		{
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

				FieldName name = categoricalFeature.getName();
				List<?> values = categoricalFeature.getValues();

				java.util.function.Predicate<Object> valueFilter = categoryManager.getValueFilter(name);

				List<Object> leftValues = new ArrayList<>();
				List<Object> rightValues = new ArrayList<>();

				for(int i = 0; i < values.size(); i++){
					Object value = values.get(i);

					if(!valueFilter.test(value)){
						continue;
					} // End if

					if(!bitSet.contains(i)){
						leftValues.add(value);
					} else

					{
						rightValues.add(value);
					}
				}

				leftCategoryManager = leftCategoryManager.fork(name, leftValues);
				rightCategoryManager = rightCategoryManager.fork(name, rightValues);

				leftPredicate = predicateManager.createSimpleSetPredicate(categoricalFeature, leftValues);
				rightPredicate = predicateManager.createSimpleSetPredicate(categoricalFeature, rightValues);
			} else

			{
				ContinuousFeature continuousFeature = feature.toContinuousFeature();

				Double splitVal = (double)byteBuffer.get4f();

				leftPredicate = predicateManager.createSimplePredicate(continuousFeature, SimplePredicate.Operator.LESS_THAN, splitVal);
				rightPredicate = predicateManager.createSimplePredicate(continuousFeature, SimplePredicate.Operator.GREATER_OR_EQUAL, splitVal);
			}
		}

		Node leftChild;

		ByteBufferWrapper leftByteBuffer = new ByteBufferWrapper(compressedTree);
		leftByteBuffer.skip(byteBuffer.position());

		if(lmask <= 3){
			leftByteBuffer.skip(lmask + 1);
		} // End if

		if((lmask & 16) != 0){
			double score = leftByteBuffer.get4f();

			leftChild = new LeafNode()
				.setId(nextId(idSequence))
				.setScore(score)
				.setPredicate(leftPredicate);
		} else

		{
			leftChild = encodeNode(leftPredicate, idSequence, compressedTree, leftByteBuffer, predicateManager, leftCategoryManager, schema);
		}

		Node rightChild;

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

			rightChild = new LeafNode()
				.setId(nextId(idSequence))
				.setScore(score)
				.setPredicate(rightPredicate);
		} else

		{
			rightChild = encodeNode(rightPredicate, idSequence, compressedTree, rightByteBuffer, predicateManager, rightCategoryManager, schema);
		}

		Node result = new BranchNode()
			.setId(id)
			.setDefaultChild(leftward ? leftChild.getId() : rightChild.getId())
			.setPredicate(predicate)
			.addNodes(leftChild, rightChild);

		return result;
	}

	static
	public byte[][] getCompressedTrees(SharedTreeMojoModel model){
		return (byte[][])getFieldValue(FIELD_COMPRESSEDTREES, model);
	}

	static
	public int getNTreeGroups(SharedTreeMojoModel model){
		return (int)getFieldValue(FIELD_NTREEGROUPS, model);
	}

	static
	public int getNTreesPerGroup(SharedTreeMojoModel model){
		return (int)getFieldValue(FIELD_NTREESPERGROUP, model);
	}

	static
	private Integer nextId(AtomicInteger id){
		return Integer.valueOf(id.getAndIncrement());
	}

	private static final Field FIELD_COMPRESSEDTREES;
	private static final Field FIELD_NTREEGROUPS;
	private static final Field FIELD_NTREESPERGROUP;

	static {

		try {
			FIELD_COMPRESSEDTREES = SharedTreeMojoModel.class.getDeclaredField("_compressed_trees");
			FIELD_NTREEGROUPS = SharedTreeMojoModel.class.getDeclaredField("_ntree_groups");
			FIELD_NTREESPERGROUP = SharedTreeMojoModel.class.getDeclaredField("_ntrees_per_group");
		} catch(ReflectiveOperationException roe){
			throw new RuntimeException(roe);
		}
	}
}