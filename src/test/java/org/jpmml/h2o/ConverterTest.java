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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import com.google.common.io.ByteStreams;
import hex.genmodel.MojoModel;
import hex.genmodel.TmpMojoReaderBackend;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.ArchiveBatch;
import org.jpmml.evaluator.IntegrationTest;
import org.jpmml.evaluator.IntegrationTestBatch;
import org.jpmml.evaluator.PMMLEquivalence;

public class ConverterTest extends IntegrationTest {

	public ConverterTest(){
		super(new PMMLEquivalence(1e-14, 1e-14));
	}

	public ConverterTest(Equivalence<Object> equivalence){
		super(equivalence);
	}

	@Override
	protected ArchiveBatch createBatch(String name, String dataset, Predicate<FieldName> predicate){
		ArchiveBatch result = new IntegrationTestBatch(name, dataset, predicate){

			@Override
			public IntegrationTest getIntegrationTest(){
				return ConverterTest.this;
			}

			@Override
			public PMML getPMML() throws Exception {
				MojoModel mojoModel;

				try(InputStream is = open("/mojo/" + getName() + getDataset() + ".zip")){
					File tmpZipFile = File.createTempFile(getName() + getDataset(), ".zip");

					try(OutputStream os = new FileOutputStream(tmpZipFile)){
						ByteStreams.copy(is, os);
					}

					TmpMojoReaderBackend mojoReaderBackend = new TmpMojoReaderBackend(tmpZipFile);

					try {
						mojoModel = MojoModelUtil.readFrom(mojoReaderBackend);
					} finally {
						mojoReaderBackend.close();
					}
				}

				ConverterFactory converterFactory = ConverterFactory.newConverterFactory();

				Converter<?> converter = converterFactory.newConverter(mojoModel);

				PMML pmml = converter.encodePMML();

				ensureValidity(pmml);

				return pmml;
			}
		};

		return result;
	}
}