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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;

import com.google.common.io.ByteStreams;
import hex.genmodel.ModelMojoReader;
import hex.genmodel.MojoModel;
import hex.genmodel.TmpMojoReaderBackend;

public class MojoModelUtil {

	private MojoModelUtil(){
	}

	static
	public MojoModel readFrom(InputStream is) throws IOException {
		File tmpZipFile = File.createTempFile("mojo", ".zip");

		try(OutputStream os = new FileOutputStream(tmpZipFile)){
			ByteStreams.copy(is, os);
		}

		return readFrom(tmpZipFile, true);
	}

	static
	public MojoModel readFrom(File tmpZipFile, boolean deleterAfterRead) throws IOException {
		TmpMojoReaderBackend mojoReaderBackend = new TmpMojoReaderBackend(tmpZipFile){

			@Override
			public void close() throws IOException {

				if(!deleterAfterRead){

					try {
						Field field = TmpMojoReaderBackend.class.getDeclaredField("_tempZipFile");

						if(!field.isAccessible()){
							field.setAccessible(true);
						}

						field.set(this, null);
					} catch(ReflectiveOperationException roe){
						throw new RuntimeException(roe);
					}
				}

				super.close();
			}
		};

		try {
			return ModelMojoReader.readFrom(mojoReaderBackend);
		} finally {
			mojoReaderBackend.close();
		}
	}
}