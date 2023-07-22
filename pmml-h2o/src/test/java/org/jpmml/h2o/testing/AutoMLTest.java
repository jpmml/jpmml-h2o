/*
 * Copyright (c) 2023 Villu Ruusmann
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

import org.jpmml.converter.testing.Datasets;
import org.jpmml.evaluator.testing.PMMLEquivalence;
import org.junit.Test;

public class AutoMLTest extends H2OEncoderBatchTest implements Datasets, H2OFields {

	public AutoMLTest(){
		super(new PMMLEquivalence(1e-13, 1e-13));
	}

	@Test
	public void evaluateAudit() throws Exception {
		evaluate("AutoML", AUDIT, excludeFields(AUDIT_DEFAULTCALIBRATION_ADJUSTED));
	}

	@Test
	public void evaluateAuto() throws Exception {
		evaluate("AutoML", AUTO);
	}

	@Test
	public void evaluateIris() throws Exception {
		evaluate("AutoML", IRIS);
	}
}