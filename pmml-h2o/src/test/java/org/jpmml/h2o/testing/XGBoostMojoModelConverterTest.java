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
package org.jpmml.h2o.testing;

import org.jpmml.evaluator.testing.PMMLEquivalence;
import org.junit.jupiter.api.Test;

public class XGBoostMojoModelConverterTest extends H2OEncoderBatchTest implements H2OAlgorithms, H2ODatasets, H2OFields {

	public XGBoostMojoModelConverterTest(){
		super(new PMMLEquivalence(1e-6, 1e-6));
	}

	@Test
	public void evaluateAudit() throws Exception {
		evaluate(XGBOOST, AUDIT, new PMMLEquivalence(6e-6, 6e-6));
	}

	@Test
	public void evaluateAuditNA() throws Exception {
		evaluate(XGBOOST, AUDIT_NA, new PMMLEquivalence(3e-6, 3e-6));
	}

	@Test
	public void evaluateAuto() throws Exception {
		evaluate(XGBOOST, AUTO);
	}

	@Test
	public void evaluateAutoNA() throws Exception {
		evaluate(XGBOOST, AUTO_NA);
	}

	@Test
	public void evaluateIris() throws Exception {
		evaluate(XGBOOST, IRIS);
	}
}