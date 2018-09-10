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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OpType;
import org.jpmml.converter.CategoricalFeature;
import org.jpmml.converter.CategoricalLabel;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.ContinuousLabel;
import org.jpmml.converter.Feature;
import org.jpmml.converter.Label;
import org.jpmml.converter.ModelEncoder;
import org.jpmml.converter.Schema;

public class H2OEncoder extends ModelEncoder {

	private Label label = null;

	private List<Feature> features = new ArrayList<>();


	public DataField createDataField(FieldName name, String[] categories){

		if(categories != null){
			return createDataField(name, OpType.CATEGORICAL, DataType.STRING, Arrays.asList(categories));
		} else

		{
			return createDataField(name, OpType.CONTINUOUS, DataType.DOUBLE);
		}
	}

	public Schema createSchema(){
		return new Schema(getLabel(), getFeatures());
	}

	public void setLabel(DataField dataField){
		OpType opType = dataField.getOpType();

		switch(opType){
			case CONTINUOUS:
				setLabel(new ContinuousLabel(dataField));
				break;
			case CATEGORICAL:
				setLabel(new CategoricalLabel(dataField));
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	public void addFeature(DataField dataField){
		OpType opType = dataField.getOpType();

		switch(opType){
			case CONTINUOUS:
				addFeature(new ContinuousFeature(this, dataField));
				break;
			case CATEGORICAL:
				addFeature(new CategoricalFeature(this, dataField));
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	public Label getLabel(){
		return this.label;
	}

	public void setLabel(Label label){
		this.label = label;
	}

	public List<Feature> getFeatures(){
		return this.features;
	}

	public void addFeature(Feature feature){
		this.features.add(feature);
	}
}