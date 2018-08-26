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

import java.io.Closeable;

import hex.genmodel.MojoModel;

public class XGBoostMojoModel extends MojoModel implements Closeable {

	private byte[] boosterBytes = null;


	public XGBoostMojoModel(String[] columns, String[][] domains, String responseColumn, byte[] boosterBytes){
		super(columns, domains, responseColumn);

		setBoosterBytes(boosterBytes);
	}

	@Override
	public double[] score0(double[] row, double[] preds){
		throw new UnsupportedOperationException();
	}

	@Override
	public void close(){
		setBoosterBytes(null);
	}

	public byte[] getBoosterBytes(){
		return this.boosterBytes;
	}

	private void setBoosterBytes(byte[] boosterBytes){
		this.boosterBytes = boosterBytes;
	}
}