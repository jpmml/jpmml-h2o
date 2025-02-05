/*
 * Copyright (c) 2024 Villu Ruusmann
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
import org.junit.jupiter.api.Test;

public class ExtendedIsolationForestMojoModelConverterTest extends H2OEncoderBatchTest implements Datasets {

	@Test
	public void evaluateHousing() throws Exception {
		evaluate("ExtendedIsolationForest", HOUSING, excludeFields("meanPathLength"));
	}
}