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

import hex.genmodel.MojoModel;
import hex.genmodel.algos.drf.DrfMojoModel;
import hex.genmodel.algos.ensemble.StackedEnsembleMojoModel;
import hex.genmodel.algos.gbm.GbmMojoModel;
import hex.genmodel.algos.glm.GlmMojoModel;
import hex.genmodel.algos.glm.GlmMultinomialMojoModel;
import hex.genmodel.algos.isofor.IsolationForestMojoModel;

public class ConverterFactory {

	protected ConverterFactory(){
	}

	public Converter<? extends MojoModel> newConverter(MojoModel model){

		if(model instanceof DrfMojoModel){
			return new DrfMojoModelConverter((DrfMojoModel)model);
		} else

		if(model instanceof GbmMojoModel){
			return new GbmMojoModelConverter((GbmMojoModel)model);
		} else

		if(model instanceof GlmMojoModel){
			return new GlmMojoModelConverter((GlmMojoModel)model);
		} else

		if(model instanceof GlmMultinomialMojoModel){
			return new GlmMultinomialMojoModelConverter((GlmMultinomialMojoModel)model);
		} else

		if(model instanceof IsolationForestMojoModel){
			return new IsolationForestMojoModelConverter((IsolationForestMojoModel)model);
		} else

		if(model instanceof StackedEnsembleMojoModel){
			return new StackedEnsembleMojoModelConverter((StackedEnsembleMojoModel)model);
		} else

		if(model instanceof XGBoostMojoModel){
			return new XGBoostMojoModelConverter((XGBoostMojoModel)model);
		}

		throw new IllegalArgumentException("No converter for MOJO model " + model);
	}

	static
	public ConverterFactory newConverterFactory(){
		return new ConverterFactory();
	}
}