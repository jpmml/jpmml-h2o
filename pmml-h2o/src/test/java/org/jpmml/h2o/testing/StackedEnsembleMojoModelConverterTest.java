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

import org.jpmml.converter.testing.Datasets;
import org.jpmml.evaluator.testing.PMMLEquivalence;
import org.junit.jupiter.api.Test;

public class StackedEnsembleMojoModelConverterTest extends H2OEncoderBatchTest implements Datasets, H2OFields {

	@Test
	public void evaluateAuditNA() throws Exception {
		evaluate("StackedEnsemble", AUDIT_NA, excludeFields(AUDIT_DEFAULTCALIBRATION_ADJUSTED));
	}

	@Test
	public void evaluateAutoNA() throws Exception {
		evaluate("StackedEnsemble", AUTO_NA);
	}

	@Test
	public void evaluateIris() throws Exception {
		evaluate("StackedEnsemble", IRIS, new PMMLEquivalence(1e-13, 1e-13));
	}
}