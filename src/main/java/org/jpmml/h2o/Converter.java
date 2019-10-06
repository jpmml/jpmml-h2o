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
import java.util.Objects;

import hex.genmodel.MojoModel;
import org.dmg.pmml.DataField;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Model;
import org.dmg.pmml.PMML;
import org.jpmml.converter.Schema;

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

			DataField dataField = encoder.createDataField(FieldName.create(name), categories);

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

	public PMML encodePMML(){
		H2OEncoder encoder = new H2OEncoder();

		Schema schema = encodeSchema(encoder);

		schema = toMojoModelSchema(schema);

		Model model = encodeModel(schema);

		return encoder.encodePMML(model);
	}

	public M getModel(){
		return this.model;
	}

	private void setModel(M model){
		this.model = Objects.requireNonNull(model);
	}

	static
	protected Class<?> getDeclaredClass(Class<?> clazz, String name) throws ReflectiveOperationException {
		String subclassName = clazz.getName() + "$" + name;

		Class<?>[] declaredClazzes = clazz.getDeclaredClasses();
		for(Class<?> declaredClazz : declaredClazzes){

			if((subclassName).equals(declaredClazz.getName())){
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
			if(!field.isAccessible()){
				field.setAccessible(true);
			}

			return field.get(object);
		} catch(ReflectiveOperationException roe){
			throw new RuntimeException(roe);
		}
	}
}