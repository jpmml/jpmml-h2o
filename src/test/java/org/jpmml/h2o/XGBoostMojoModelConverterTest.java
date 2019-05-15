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

import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.PMMLEquivalence;
import org.junit.Test;

public class XGBoostMojoModelConverterTest extends ConverterTest {

	public XGBoostMojoModelConverterTest(){
		super(new PMMLEquivalence(1e-6, 1e-6));
	}

	@Test
	public void evaluateAudit() throws Exception {
		FieldName[] targetFields = {FieldName.create("Adjusted")};

		evaluate("XGBoost", "Audit", excludeFields(targetFields), new PMMLEquivalence(6e-6, 6e-6));
	}

	@Test
	public void evaluateAuditNA() throws Exception {
		FieldName[] targetFields = {FieldName.create("Adjusted")};

		evaluate("XGBoost", "AuditNA", excludeFields(targetFields), new PMMLEquivalence(3e-6, 3e-6));
	}

	@Test
	public void evaluateAuto() throws Exception {
		evaluate("XGBoost", "Auto");
	}

	@Test
	public void evaluateAutoNA() throws Exception {
		evaluate("XGBoost", "AutoNA");
	}

	@Test
	public void evaluateIris() throws Exception {
		evaluate("XGBoost", "Iris");
	}
}