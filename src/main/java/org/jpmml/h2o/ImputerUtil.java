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

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.Expression;
import org.dmg.pmml.Field;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.MissingValueTreatmentMethod;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.FeatureUtil;
import org.jpmml.converter.MissingValueDecorator;
import org.jpmml.converter.ModelEncoder;
import org.jpmml.converter.ObjectFeature;
import org.jpmml.converter.PMMLUtil;
import org.jpmml.converter.StringFeature;
import org.jpmml.converter.ValueUtil;

public class ImputerUtil {

	private ImputerUtil(){
	}

	static
	public Feature encodeFeature(Feature feature, Object replacementValue, MissingValueTreatmentMethod missingValueTreatmentMethod){
		ModelEncoder encoder = (ModelEncoder)feature.getEncoder();

		Field<?> field = encoder.getField(feature.getName());

		if(field instanceof DataField){
			MissingValueDecorator missingValueDecorator = new MissingValueDecorator()
				.setMissingValueReplacement(ValueUtil.formatValue(replacementValue))
				.setMissingValueTreatment(missingValueTreatmentMethod);

			encoder.addDecorator(feature.getName(), missingValueDecorator);

			return feature;
		} else

		if(field instanceof DerivedField){
			FieldRef fieldRef = feature.ref();

			Expression expression = PMMLUtil.createApply("if", PMMLUtil.createApply("isMissing", fieldRef), PMMLUtil.createConstant(replacementValue, feature.getDataType()), fieldRef);

			DerivedField derivedField = encoder.createDerivedField(FeatureUtil.createName("imputer", feature), field.getOpType(), field.getDataType(), expression);

			DataType dataType = derivedField.getDataType();
			switch(dataType){
				case INTEGER:
				case FLOAT:
				case DOUBLE:
					return new ContinuousFeature(encoder, derivedField);
				case STRING:
					return new StringFeature(encoder, derivedField);
				default:
					return new ObjectFeature(encoder, derivedField.getName(), derivedField.getDataType());
			}
		} else

		{
			throw new IllegalArgumentException();
		}
	}
}