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

import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.jpmml.converter.testing.ModelEncoderBatchTest;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.testing.PMMLEquivalence;

public class H2OTest extends ModelEncoderBatchTest {

	public H2OTest(){
		this(new PMMLEquivalence(1e-14, 1e-14));
	}

	public H2OTest(Equivalence<Object> equivalence){
		super(equivalence);
	}

	@Override
	public H2OTestBatch createBatch(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		H2OTestBatch result = new H2OTestBatch(algorithm, dataset, columnFilter, equivalence){

			@Override
			public H2OTest getArchiveBatchTest(){
				return H2OTest.this;
			}
		};

		return result;
	}
}