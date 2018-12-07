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

import java.io.IOException;

import hex.genmodel.ModelMojoReader;

public class XGBoostMojoReader extends ModelMojoReader<XGBoostMojoModel> {

	@Override
	public String getModelName(){
		return "XGBoost";
	}

	@Override
	protected void readModelData() throws IOException {
	}

	@Override
	protected XGBoostMojoModel makeModel(String[] columns, String[][] domains, String responseColumn){
		byte[] boosterBytes;

		try {
			boosterBytes = readblob("boosterBytes");
		} catch(IOException ioe){
			throw new IllegalStateException(ioe);
		}

		return new XGBoostMojoModel(columns, domains, responseColumn, boosterBytes);
	}

	@Override
	public String mojoVersion(){
		return "1.00";
	}
}