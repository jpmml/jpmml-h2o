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
import hex.genmodel.MojoModel;
import hex.genmodel.MojoReaderBackend;
import hex.genmodel.algos.xgboost.XGBoostMojoModel;

public class MojoModelUtil {

	private MojoModelUtil(){
	}

	static
	public MojoModel readFrom(MojoReaderBackend mojoReaderBackend) throws IOException {
		byte[] boosterBytes = null;

		if(mojoReaderBackend.exists("boosterBytes")){
			boosterBytes = mojoReaderBackend.getBinaryFile("boosterBytes");
		}

		MojoModel mojoModel = ModelMojoReader.readFrom(mojoReaderBackend);

		if(mojoModel instanceof XGBoostMojoModel){
			XGBoostMojoModel xgboostMojoModel = (XGBoostMojoModel)mojoModel;

			String[] columns = xgboostMojoModel.getNames();
			String[][] domains = xgboostMojoModel.getDomainValues();
			String responseColumn = xgboostMojoModel.getResponseName();
			boolean supervised = xgboostMojoModel.isSupervised();

			mojoModel = new XGBoostRawMojoModel(columns, domains, responseColumn, boosterBytes);
			mojoModel._supervised = xgboostMojoModel.isSupervised();
		}

		return mojoModel;
	}
}