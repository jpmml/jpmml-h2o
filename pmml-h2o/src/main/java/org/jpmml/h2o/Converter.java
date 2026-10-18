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
import java.util.List;
import java.util.Map;
import java.util.Objects;

import hex.ModelCategory;
import hex.genmodel.MojoModel;
import hex.genmodel.descriptor.ModelDescriptor;
import org.dmg.pmml.DataField;
import org.dmg.pmml.Decision;
import org.dmg.pmml.Decisions;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.Model;
import org.dmg.pmml.Output;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.PMML;
import org.dmg.pmml.PMMLFunctions;
import org.dmg.pmml.ResultFeature;
import org.jpmml.converter.CategoricalLabel;
import org.jpmml.converter.ExpressionUtil;
import org.jpmml.converter.FieldNameUtil;
import org.jpmml.converter.FieldNames;
import org.jpmml.converter.Schema;
import org.jpmml.converter.mining.MiningModelUtil;

abstract
public class Converter<M extends MojoModel> {

	private M model = null;


	public Converter(M model){
		setModel(model);
	}

	abstract
	public Model encodeModel(Schema schema);

	public Schema encodeSchema(H2OEncoder encoder){
		M model = getModel();

		String[] names = model.getNames();
		int responseIdx = -1;

		if(model.isSupervised()){
			responseIdx = model.getResponseIdx();
		}

		for(int i = 0; i < names.length; i++){
			String name = names[i];
			String[] categories = model.getDomainValues(name);

			DataField dataField = encoder.createDataField(name, categories);

			if(i == responseIdx){
				encoder.setLabel(dataField);
			} else

			{
				encoder.addFeature(dataField);
			}
		}

		return encoder.createSchema();
	}

	public Schema toMojoModelSchema(Schema schema){
		return schema;
	}

	public PMML encodePMML(Map<String, ?> options){
		M model = getModel();

		ModelDescriptor modelDescriptor = model._modelDescriptor;

		H2OEncoder encoder = new H2OEncoder();

		Schema schema = encodeSchema(encoder);

		schema = toMojoModelSchema(schema);

		Model pmmlModel = encodeModel(options, schema);

		ModelCategory modelCategory = model.getModelCategory();
		if(modelCategory == ModelCategory.Binomial){
			Number defaultThreshold = getDefaultThreshold();

			if(defaultThreshold != null){
				encodeDefaultThreshold(pmmlModel, defaultThreshold, schema);
			}
		}

		if(modelDescriptor != null){
			String algorithmName = pmmlModel.getAlgorithmName();

			if(algorithmName == null){
				String algoFullName = modelDescriptor.algoFullName();

				pmmlModel.setAlgorithmName(algoFullName);
			}
		}

		return encoder.encodePMML(pmmlModel);
	}

	public Model encodeModel(Map<String, ?> options, Schema schema){
		schema = configureSchema(options, schema);

		Model model = encodeModel(schema);

		model = configureModel(options, model);

		return model;
	}

	public Schema configureSchema(Map<String, ?> options, Schema schema){
		return schema;
	}

	public Model configureModel(Map<String, ?> options, Model model){
		return model;
	}

	public Number getDefaultThreshold(){
		M model = getModel();

		return model._defaultThreshold;
	}

	public M getModel(){
		return this.model;
	}

	private void setModel(M model){
		this.model = Objects.requireNonNull(model);
	}

	static
	public void encodeDefaultThreshold(Model pmmlModel, Number defaultThreshold, Schema schema){
		CategoricalLabel categoricalLabel = schema.requireCategoricalLabel()
			.expectCardinality(2);

		Model finalModel = MiningModelUtil.getFinalModel(pmmlModel);

		Output output = finalModel.getOutput();
		if(output == null){
			throw new IllegalArgumentException();
		}

		String name = FieldNameUtil.create(FieldNames.PROBABILITY, categoricalLabel.getValue(1));

		Expression expression = ExpressionUtil.createApply(PMMLFunctions.IF,
			ExpressionUtil.createApply(PMMLFunctions.GREATEROREQUAL, new FieldRef(name), ExpressionUtil.createConstant(defaultThreshold)),
			ExpressionUtil.createConstant(categoricalLabel.getDataType(), categoricalLabel.getValue(1)),
			ExpressionUtil.createConstant(categoricalLabel.getDataType(), categoricalLabel.getValue(0))
		);

		Decisions decisions = new Decisions();

		List<?> values = categoricalLabel.getValues();
		for(Object value : values){
			decisions.addDecisions(new Decision(value));
		}

		OutputField defaultThresholdOutputField = new OutputField(FieldNameUtil.create("defaultThreshold", categoricalLabel), categoricalLabel.getOpType(), categoricalLabel.getDataType())
			.setResultFeature(ResultFeature.DECISION)
			.setDecisions(decisions)
			.setExpression(expression);

		output.addOutputFields(defaultThresholdOutputField);
	}

	static
	protected Class<?> getDeclaredClass(Class<?> clazz, String name) throws ReflectiveOperationException {
		String subclassName = clazz.getName() + "$" + name;

		Class<?>[] declaredClazzes = clazz.getDeclaredClasses();
		for(Class<?> declaredClazz : declaredClazzes){

			if(Objects.equals(subclassName, declaredClazz.getName())){
				return declaredClazz;
			}
		}

		throw new ClassNotFoundException(subclassName);
	}

	static
	protected <M extends MojoModel> Object getFieldValue(Field field, M model){
		return getFieldValue(field, (Object)model);
	}

	static
	protected Object getFieldValue(Field field, Object object){

		try {
			if(!field.canAccess(object)){
				field.setAccessible(true);
			}

			return field.get(object);
		} catch(ReflectiveOperationException roe){
			throw new RuntimeException(roe);
		}
	}
}