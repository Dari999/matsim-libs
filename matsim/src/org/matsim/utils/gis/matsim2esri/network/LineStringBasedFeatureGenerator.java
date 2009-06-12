/* *********************************************************************** *
 * project: org.matsim.*
 * LineStringBasedFeatureGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.utils.gis.matsim2esri.network;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.*;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.*;

import org.matsim.core.api.network.Link;
import org.matsim.core.utils.geometry.geotools.MGC;

public class LineStringBasedFeatureGenerator implements FeatureGenerator{


	private final WidthCalculator widthCalculator;
	private FeatureType featureType;
	private final CoordinateReferenceSystem crs;
	private final GeometryFactory geofac;


	public LineStringBasedFeatureGenerator(final WidthCalculator widthCalculator, final CoordinateReferenceSystem crs) {
		this.widthCalculator = widthCalculator;
		this.crs = crs;
		this.geofac = new GeometryFactory();
		initFeatureType();
	}


	private void initFeatureType() {

		AttributeType [] attribs = new AttributeType[10];
		attribs[0] = DefaultAttributeTypeFactory.newAttributeType("LineString",LineString.class, true, null, null, this.crs);
		attribs[1] = AttributeTypeFactory.newAttributeType("ID", String.class);
		attribs[2] = AttributeTypeFactory.newAttributeType("fromID", String.class);
		attribs[3] = AttributeTypeFactory.newAttributeType("toID", String.class);
		attribs[4] = AttributeTypeFactory.newAttributeType("length", Double.class);
		attribs[5] = AttributeTypeFactory.newAttributeType("freespeed", Double.class);
		attribs[6] = AttributeTypeFactory.newAttributeType("capacity", Double.class);
		attribs[7] = AttributeTypeFactory.newAttributeType("lanes", Double.class);
		attribs[8] = AttributeTypeFactory.newAttributeType("visWidth", Double.class);		
		attribs[9] = AttributeTypeFactory.newAttributeType("type", String.class);		

		try {
			this.featureType = FeatureTypeBuilder.newFeatureType(attribs, "link");
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}

	}


	public Feature getFeature(final Link link) {
		double width = this.widthCalculator.getWidth(link);
		LineString ls = this.geofac.createLineString(new Coordinate[] {MGC.coord2Coordinate(link.getFromNode().getCoord()),
				MGC.coord2Coordinate(link.getToNode().getCoord())});

		Object [] attribs = new Object[10];
		attribs[0] = ls;
		attribs[1] = link.getId().toString();
		attribs[2] = link.getFromNode().getId().toString();
		attribs[3] = link.getToNode().getId().toString();
		attribs[4] = link.getLength();
		attribs[5] = link.getFreespeed(org.matsim.core.utils.misc.Time.UNDEFINED_TIME);
		attribs[6] = link.getCapacity(org.matsim.core.utils.misc.Time.UNDEFINED_TIME);
		attribs[7] = link.getNumberOfLanes(org.matsim.core.utils.misc.Time.UNDEFINED_TIME);
		attribs[8] = width;
		attribs[9] = link.getType();

		try {
			return this.featureType.create(attribs);
		} catch (IllegalAttributeException e) {
			throw new RuntimeException(e);
		}

	}

}
