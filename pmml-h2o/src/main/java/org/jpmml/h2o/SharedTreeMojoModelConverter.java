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
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.google.common.collect.Iterables;
import hex.genmodel.algos.tree.NaSplitDir;
import hex.genmodel.algos.tree.SharedTreeMojoModel;
import hex.genmodel.utils.ByteBufferWrapper;
import hex.genmodel.utils.GenmodelBitSet;
import org.dmg.pmml.DataType;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.Model;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.SimplePredicate;
import org.dmg.pmml.True;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.tree.CountingBranchNode;
import org.dmg.pmml.tree.CountingLeafNode;
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
import org.jpmml.converter.ValueUtil;

abstract
public class SharedTreeMojoModelConverter<M extends SharedTreeMojoModel> extends Converter<M> {

	public SharedTreeMojoModelConverter(M model){
		super(model);
	}

	public List<TreeModel> encodeTreeModels(Schema schema){
		SharedTreeMojoModel model = getModel();

		if(model._mojo_version < 1.2d){
			throw new IllegalArgumentException("Version " + model._mojo_version + " is not supported");
		}

		byte[][] compressedTrees = getCompressedTrees(model);
		byte[][] compressedTreesAux = getCompressedTreesAux(model);

		PredicateManager predicateManager = new PredicateManager();

		List<TreeModel> result = new ArrayList<>();

		for(int i = 0, max = Math.max(compressedTrees.length, compressedTreesAux.length); i < max; i++){
			byte[] compressedTree = compressedTrees[i];
			byte[] compressedTreeAux = compressedTreesAux[i];

			TreeModel treeModel = encodeTreeModel(compressedTree, compressedTreeAux, predicateManager, schema);

			result.add(treeModel);
		}

		return result;
	}

	public TreeModel encodeTreeModel(byte[] compressedTree, byte[] compressedTreeAux, PredicateManager predicateManager, Schema schema){
		Label label = new ContinuousLabel(DataType.DOUBLE);

		ByteBufferWrapper byteBuffer = new ByteBufferWrapper(compressedTree);

		Map<Integer, SharedTreeMojoModel.AuxInfo> auxInfos = SharedTreeMojoModel.readAuxInfos(compressedTreeAux);

		Node root = encodeNode(byteBuffer, 0, True.INSTANCE, compressedTree, auxInfos, new CategoryManager(), predicateManager, schema);

		TreeModel treeModel = new TreeModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(label), root)
			.setMissingValueStrategy(TreeModel.MissingValueStrategy.DEFAULT_CHILD);

		return treeModel;
	}

	public Node encodeNode(ByteBufferWrapper byteBuffer, Integer id, Predicate predicate, byte[] compressedTree, Map<Integer, SharedTreeMojoModel.AuxInfo> auxInfos, CategoryManager categoryManager, PredicateManager predicateManager, Schema schema){
		SharedTreeMojoModel.AuxInfo auxInfo = auxInfos.get(id);
		if(auxInfo == null){
			throw new IllegalArgumentException();
		}

		int nodeType = byteBuffer.get1U();

		int lmask = (nodeType & 51);
		int lmask2 = (nodeType & 0xC0) >> 2;

		int equal = (nodeType & 12);

		int colId = byteBuffer.get2();
		if(colId == 65535){
			double score = byteBuffer.get4f();

			Node result = new CountingLeafNode(score, predicate)
				.setId(id);

			return result;
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
					throw new IllegalArgumentException("Node type " + equal + " is not supported");
				}

				String name = categoricalFeature.getName();
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

				leftPredicate = predicateManager.createPredicate(categoricalFeature, leftValues);
				rightPredicate = predicateManager.createPredicate(categoricalFeature, rightValues);
			} else

			{
				ContinuousFeature continuousFeature = feature.toContinuousFeature();

				Double splitVal = (double)byteBuffer.get4f();

				leftPredicate = predicateManager.createSimplePredicate(continuousFeature, SimplePredicate.Operator.LESS_THAN, splitVal);
				rightPredicate = predicateManager.createSimplePredicate(continuousFeature, SimplePredicate.Operator.GREATER_OR_EQUAL, splitVal);
			}
		}

		Node leftChild;

		Integer leftId = auxInfo.nidL;

		ByteBufferWrapper leftByteBuffer = new ByteBufferWrapper(compressedTree);
		leftByteBuffer.skip(byteBuffer.position());

		if(lmask <= 3){
			leftByteBuffer.skip(lmask + 1);
		} // End if

		if((lmask & 16) != 0){
			double score = leftByteBuffer.get4f();

			leftChild = new CountingLeafNode(score, leftPredicate)
				.setId(leftId);
		} else

		{
			leftChild = encodeNode(leftByteBuffer, leftId, leftPredicate, compressedTree, auxInfos, leftCategoryManager, predicateManager, schema);
		}

		Node rightChild;

		Integer rightId = auxInfo.nidR;

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
				throw new IllegalArgumentException("Node type " + lmask + " is not supported");
		}

		if((lmask2 & 16) != 0){
			double score = rightByteBuffer.get4f();

			rightChild = new CountingLeafNode(score, rightPredicate)
				.setId(rightId);
		} else

		{
			rightChild = encodeNode(rightByteBuffer, rightId, rightPredicate, compressedTree, auxInfos, rightCategoryManager, predicateManager, schema);
		}

		ensureScore(leftChild, auxInfo.predL);
		ensureScore(rightChild, auxInfo.predR);

		ensureRecordCount(leftChild, auxInfo.weightL);
		ensureRecordCount(rightChild, auxInfo.weightR);

		Node result = new CountingBranchNode(null, predicate)
			.setId(id)
			.setDefaultChild(leftward ? leftChild.getId() : rightChild.getId())
			.addNodes(leftChild, rightChild);

		if(id == 0){
			float weight = (auxInfo.weightL + auxInfo.weightR);

			ensureScore(result, (auxInfo.predL * auxInfo.weightL + auxInfo.predR * auxInfo.weightR) / weight);
			ensureRecordCount(result, weight);
		}

		return result;
	}

	protected void ensureScore(Node node, double score){

		if(node.hasScore()){

			if(!Objects.equals(node.getScore(), score)){
				throw new IllegalArgumentException();
			}
		} else

		{
			node.setScore(score);
		}
	}

	protected void ensureRecordCount(Node node, double recordCount){

		if(node.getRecordCount() != null){
			throw new IllegalArgumentException();
		}

		node.setRecordCount(ValueUtil.narrow(recordCount));
	}

	static
	public Model encodeTreeEnsemble(List<TreeModel> treeModels, Function<List<TreeModel>, MiningModel> ensembleFunction){

		if(treeModels.size() == 1){
			return Iterables.getOnlyElement(treeModels);
		}

		return ensembleFunction.apply(treeModels);
	}

	static
	public byte[][] getCompressedTrees(SharedTreeMojoModel model){
		return (byte[][])getFieldValue(FIELD_COMPRESSEDTREES, model);
	}

	static
	public byte[][] getCompressedTreesAux(SharedTreeMojoModel model){
		return (byte[][])getFieldValue(FIELD_COMPRESSEDTREESAUX, model);
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
	private static final Field FIELD_COMPRESSEDTREESAUX;
	private static final Field FIELD_NTREEGROUPS;
	private static final Field FIELD_NTREESPERGROUP;

	static {

		try {
			FIELD_COMPRESSEDTREES = SharedTreeMojoModel.class.getDeclaredField("_compressed_trees");
			FIELD_COMPRESSEDTREESAUX = SharedTreeMojoModel.class.getDeclaredField("_compressed_trees_aux");
			FIELD_NTREEGROUPS = SharedTreeMojoModel.class.getDeclaredField("_ntree_groups");
			FIELD_NTREESPERGROUP = SharedTreeMojoModel.class.getDeclaredField("_ntrees_per_group");
		} catch(ReflectiveOperationException roe){
			throw new RuntimeException(roe);
		}
	}
}