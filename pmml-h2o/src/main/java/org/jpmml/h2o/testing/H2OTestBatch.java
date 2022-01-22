/*
 * Copyright (c) 2020 Villu Ruusmann
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

import java.io.InputStream;
import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import hex.genmodel.MojoModel;
import org.dmg.pmml.PMML;
import org.jpmml.converter.testing.ModelEncoderBatch;
import org.jpmml.evaluator.ResultField;
import org.jpmml.h2o.Converter;
import org.jpmml.h2o.ConverterFactory;
import org.jpmml.h2o.MojoModelUtil;

abstract
public class H2OTestBatch extends ModelEncoderBatch {

	public H2OTestBatch(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		super(algorithm, dataset, columnFilter, equivalence);
	}

	@Override
	abstract
	public H2OTest getArchiveBatchTest();

	public String getMojoPath(){
		return "/mojo/" + (getAlgorithm() + getDataset()) + ".zip";
	}

	@Override
	public PMML getPMML() throws Exception {
		MojoModel mojoModel;

		try(InputStream is = open(getMojoPath())){
			mojoModel = MojoModelUtil.readFrom(is);
		}

		ConverterFactory converterFactory = ConverterFactory.newConverterFactory();

		Converter<?> converter = converterFactory.newConverter(mojoModel);

		PMML pmml = converter.encodePMML();

		validatePMML(pmml);

		return pmml;
	}
}