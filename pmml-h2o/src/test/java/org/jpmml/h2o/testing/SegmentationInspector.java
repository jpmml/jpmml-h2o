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

import java.util.List;

import org.dmg.pmml.VisitorAction;
import org.dmg.pmml.mining.Segment;
import org.dmg.pmml.mining.Segmentation;
import org.jpmml.model.InvalidElementException;
import org.jpmml.model.visitors.AbstractVisitor;

public class SegmentationInspector extends AbstractVisitor {

	@Override
	public VisitorAction visit(Segmentation segmentation){
		Segmentation.MultipleModelMethod multipleModelMethod = segmentation.getMultipleModelMethod();

		switch(multipleModelMethod){
			case MAJORITY_VOTE:
			case WEIGHTED_MAJORITY_VOTE:
			case AVERAGE:
			case WEIGHTED_AVERAGE:
			case MEDIAN:
			case WEIGHTED_MEDIAN:
			case SUM:
			case WEIGHTED_SUM:
				{
					List<Segment> segments = segmentation.getSegments();

					if(segments.size() <= 1){
						throw new InvalidElementException(segmentation);
					}
				}
				break;
			default:
				break;
		}

		return super.visit(segmentation);
	}
}