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
import org.dmg.pmml.Field;
import org.dmg.pmml.MissingValueTreatmentMethod;
import org.jpmml.converter.DerivedOutputField;
import org.jpmml.converter.Feature;
import org.jpmml.converter.MissingValueDecorator;
import org.jpmml.converter.ModelEncoder;

public class ImputerUtil {

	private ImputerUtil(){
	}

	static
	public Feature encodeFeature(Feature feature, Object replacementValue, MissingValueTreatmentMethod missingValueTreatmentMethod){
		ModelEncoder encoder = (ModelEncoder)feature.getEncoder();

		Field<?> field = feature.getField();

		if((field instanceof DataField) || (field instanceof DerivedOutputField)){
			encoder.addDecorator(field.getName(), new MissingValueDecorator(missingValueTreatmentMethod, replacementValue));

			return feature;
		} else

		{
			throw new IllegalArgumentException();
		}
	}
}