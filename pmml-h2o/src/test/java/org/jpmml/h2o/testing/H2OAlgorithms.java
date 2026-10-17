/*
 * Copyright (c) 2026 Villu Ruusmann
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
package org.jpmml.h2o.testing;

public interface H2OAlgorithms {

	String AUTOML = "AutoML";
	String DECISION_TREE = "DecisionTree";
	String EXTENDED_ISOLATION_FOREST = "ExtendedIsolationForest";
	String GBM = "GBM";
	String GBM_GAMMA = GBM + "Gamma";
	String GBM_POISSON = GBM + "Poisson";
	String GBM_TWEEDIE = GBM + "Tweedie";
	String GLM = "GLM";
	String GLM_ORDINAL = GLM + "Ordinal";
	String ISOLATION_FOREST = "IsolationForest";
	String RANDOM_FOREST = "RandomForest";
	String STACKED_ENSEMBLE = "StackedEnsemble";
	String XGBOOST = "XGBoost";
}
