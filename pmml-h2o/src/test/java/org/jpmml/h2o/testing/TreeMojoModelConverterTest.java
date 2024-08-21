/*
 * Copyright (c) 2022 Villu Ruusmann
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
import org.jpmml.converter.testing.Datasets;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.testing.PMMLEquivalence;
import org.jpmml.model.visitors.VisitorBattery;
import org.junit.Test;

public class TreeMojoModelConverterTest extends H2OEncoderBatchTest implements Datasets, H2OFields {

	public TreeMojoModelConverterTest(){
		super(new PMMLEquivalence(1e-13, 1e-13));
	}

	@Override
	public H2OEncoderBatch createBatch(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		H2OEncoderBatch result = new H2OEncoderBatch(algorithm, dataset, columnFilter, equivalence){

			@Override
			public TreeMojoModelConverterTest getArchiveBatchTest(){
				return TreeMojoModelConverterTest.this;
			}

			@Override
			public VisitorBattery getValidators(){
				VisitorBattery visitorBattery = super.getValidators();

				visitorBattery.add(SegmentationInspector.class);

				return visitorBattery;
			}
		};

		return result;
	}

	@Test
	public void evaluateAudit() throws Exception {
		evaluate("DecisionTree", AUDIT, excludeFields(AUDIT_ADJUSTED));
	}

	@Test
	public void evaluateAuditNA() throws Exception {
		evaluate("DecisionTree", AUDIT_NA, excludeFields(AUDIT_DEFAULTCALIBRATION_ADJUSTED));
	}

	@Test
	public void evaluateAuto() throws Exception {
		evaluate("DecisionTree", AUTO);
	}

	@Test
	public void evaluateAutoNA() throws Exception {
		evaluate("DecisionTree", AUTO_NA);
	}

	@Test
	public void evaluateIris() throws Exception {
		evaluate("DecisionTree", IRIS);
	}

	private final static String SPLIT_CAT = "SplitCat";

	@Test
	public void evaluateSplitCat() throws Exception {
		evaluate("DecisionTree", SPLIT_CAT);
	}

}