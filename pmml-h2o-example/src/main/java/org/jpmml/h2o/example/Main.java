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
package org.jpmml.h2o.example;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import com.beust.jcommander.DefaultUsageFormatter;
import com.beust.jcommander.IUsageFormatter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import hex.genmodel.MojoModel;
import org.dmg.pmml.PMML;
import org.jpmml.h2o.Converter;
import org.jpmml.h2o.ConverterFactory;
import org.jpmml.h2o.MojoModelUtil;
import org.jpmml.model.JAXBSerializer;
import org.jpmml.model.metro.MetroJAXBSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

	@Parameter (
		names = {"--mojo-input"},
		description = "MOJO input file",
		required = true,
		order = 1
	)
	private File input = null;

	@Parameter (
		names = {"--pmml-output"},
		description = "PMML output file",
		required = true,
		order = 2
	)
	private File output = null;

	@Parameter (
		names = "--help",
		description = "Show the list of configuration options and exit",
		help = true,
		order = Integer.MAX_VALUE
	)
	private boolean help = false;


	static
	public void main(String... args) throws Exception {
		Main main = new Main();

		JCommander commander = new JCommander(main);
		commander.setProgramName(Main.class.getName());

		IUsageFormatter usageFormatter = new DefaultUsageFormatter(commander);

		try {
			commander.parse(args);
		} catch(ParameterException pe){
			StringBuilder sb = new StringBuilder();

			sb.append(pe.toString());
			sb.append("\n");

			usageFormatter.usage(sb);

			System.err.println(sb.toString());

			System.exit(-1);
		}

		if(main.help){
			StringBuilder sb = new StringBuilder();

			usageFormatter.usage(sb);

			System.out.println(sb.toString());

			System.exit(0);
		}

		main.run();
	}

	public void run() throws Exception {
		MojoModel mojoModel;

		try {
			logger.info("Loading MOJO..");

			long begin = System.currentTimeMillis();
			mojoModel = MojoModelUtil.readFrom(this.input, false);
			long end = System.currentTimeMillis();

			logger.info("Loaded MOJO in {} ms.", (end - begin));
		} catch(Exception e){
			logger.error("Failed to load MOJO", e);

			throw e;
		}

		PMML pmml;

		try {
			logger.info("Converting MOJO to PMML..");

			ConverterFactory converterFactory = ConverterFactory.newConverterFactory();

			Converter<?> converter = converterFactory.newConverter(mojoModel);

			long begin = System.currentTimeMillis();
			pmml = converter.encodePMML();
			long end = System.currentTimeMillis();

			logger.info("Converted MOJO to PMML in {} ms.", (end - begin));
		} catch(Exception e){
			logger.error("Failed to convert MOJO to PMML", e);

			throw e;
		}

		try(OutputStream os = new FileOutputStream(this.output)){
			logger.info("Marshalling PMML..");

			JAXBSerializer jaxbSerializer = new MetroJAXBSerializer();

			long begin = System.currentTimeMillis();
			jaxbSerializer.serializePretty(pmml, os);
			long end = System.currentTimeMillis();

			logger.info("Marshalled PMML in {} ms.", (end - begin));
		} catch(Exception e){
			logger.error("Failed to marshal PMML", e);

			throw e;
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(Main.class);
}