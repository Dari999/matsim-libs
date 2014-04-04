/* *********************************************************************** *
 * project: org.matsim.*
 * DgIdConverter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.ids;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;


/**
 * The KS2010 model uses different and more ids than MATSim. 
 * This class implements the symbolic conversion functions (String -> String). 
 * Intern, a IdPool is used that holds a mapping from each String id to an integer. 
 * The IdPool is required to prevent integer overflows on the cplex side. 
 * 
 * @author dgrether
 *
 */
public class DgIdConverter {

	private static final Logger log = Logger.getLogger(DgIdConverter.class);
	
	private DgIdPool idPool;
	
	public DgIdConverter(DgIdPool idpool){
		this.idPool = idpool;
	}
	
	/**
	 * creates an id for the crossing node of the extended crossing corresponding to
	 * the from node of the link. in the extended ks-model network the street 
	 * corresponding to the matsim link will start at this crossing node.
	 * 
	 * @param linkId the id of the link in the matsim network
	 * @return the id for the crossing node of the links from crossing
	 */
	public  Id convertLinkId2FromCrossingNodeId(Id linkId){
		String idString = linkId.toString() + "11";
		return idPool.createId(idString);
	}
	
	/**
	 * creates an id for the crossing node of the extended crossing corresponding to
	 * the to node of the link. in the extended ks-model network the street 
	 * corresponding to the matsim link will end at this crossing node.
	 * 
	 * @param linkId the id of the link in the matsim network
	 * @return the id for the crossing node of the links to crossing
	 */
	public  Id convertLinkId2ToCrossingNodeId(Id linkId){
		String idString = linkId.toString() + "99";
		return idPool.createId(idString);
	}
	
	/**
	 * converts back. see convertLinkId2ToCrossingNodeId(...)
	 * 
	 * @param toCrossingNodeId the id of the crossing node in the ks-model network
	 * @return the id of the matsim link corresponding to the street ending in this crossing node
	 */
	public Id convertToCrossingNodeId2LinkId(Id toCrossingNodeId){
		String sid = toCrossingNodeId.toString();
		if (sid.endsWith("99")){
			Id id = new IdImpl(sid.substring(0, sid.length() - 2));
			return id;
		}
		throw new IllegalStateException("Can not convert " + sid + " to link id");
	}
	
	/**
	 * converts back. see convertLinkId2FromCrossingNodeId(...)
	 * 
	 * @param fromCrossingNodeId the id of the crossing node in the ks-model network
	 * @return the id of the matsim link corresponding to the street starting in this crossing node
	 */
	public Id convertFromCrossingNodeId2LinkId(Id fromCrossingNodeId){
		String sid = fromCrossingNodeId.toString();
		if (sid.endsWith("11")){
			Id id = new IdImpl(sid.substring(0, sid.length() - 2));
			return id;
		}
		throw new IllegalStateException("Can not convert " + sid + " to link id");
	}
	
	/**
	 * creates a light id for a link to link relationship
	 * 
	 * @param fromLinkId
	 * @param fromLaneId
	 * @param toLinkId
	 * @return the light id
	 */
	public  Id convertFromLinkIdToLinkId2LightId(Id fromLinkId, Id fromLaneId, Id toLinkId){
		Id id =  null;
		if (fromLaneId == null){
			id = new IdImpl(fromLinkId.toString()  + "55" + toLinkId.toString());
		}
		else {
			id = new IdImpl(fromLinkId.toString() + "66" + fromLaneId.toString() + "55" + toLinkId.toString());
		}
		String idString = id.toString();
		return idPool.createId(idString);
	}
	
	/**
	 * creates an id for the crossing representing the matsim node in the ks-model
	 * 
	 * @param nodeId the matsim node id
	 * @return the corresponding crossing id in the ks-model
	 */
	public  Id convertNodeId2CrossingId(Id nodeId){
		String idString = nodeId.toString() + "77";
		return idPool.createId(idString);
	}
	
	/**
	 * converts back. see convertNodeId2CrossingId(...)
	 * 
	 * @param crossingId the crossing id in the ks-model
	 * @return the corresponding node id in the matsim network
	 */
	public Id convertCrossingId2NodeId(Id crossingId){
		String sid = crossingId.toString();
		if (sid.endsWith("77")){
			Id  id = new IdImpl(sid.substring(0, sid.length() - 2));
			return id;
		}
		throw new IllegalStateException("Can not convert " + sid + " to node id");
	}
	
	/**
	 * converts a matsim node ID of a node outside the signals bounding box 
	 * to the single crossing node ID existing for the not expanded crossing in the ks-model.
	 * (the signals bounding box determines the region of spatial expansion: all nodes within this area will be expanded.)
	 * 
	 * @param nodeId the id of the matsim node
	 * @return the crossing node id in the ks-model representing the single crossing node 
	 * of the not expanded crossing corresponding to the matsim node
	 */
	public Id convertNodeId2NotExpandedCrossingNodeId(Id nodeId){
		String idString = nodeId.toString();
		return idPool.createId(idString);
	}
	
	/**
	 * converts back. see convertNodeId2NotExpandedCrossingNodeId(...)
	 * 
	 * @param crossingId the crossing node id in the ks-model
	 * @return the id of the matsim node corresponding to the not expanded crossing 
	 * in the ks-model containing this crossing node
	 */
	public Id convertNotExpandedCrossingNodeId2NodeId(Id crossingNodeId){
		String sid = crossingNodeId.toString();
		return new IdImpl(sid);
	}
	
	/**
	 * creates a street id for the ks-model corresponding to the link id of the matsim network
	 * 
	 * @param linkId the link id in the matsim network
	 * @return the street id for the ks-model
	 */
	public Id convertLinkId2StreetId(Id linkId){
		String idString = linkId.toString() + "88";
		return idPool.createId(idString);
	}

	/**
	 * converts back. see convertLinkId2StreetId(...)
	 * 
	 * @param streetId the street id in the ks-model
	 * @return the corresponding link id in the matsim network
	 */
	public Id convertStreetId2LinkId(Id streetId){
		String sid = streetId.toString();
		if (sid.endsWith("88")){
			Id  id = new IdImpl(sid.substring(0, sid.length() - 2));
			return id;
		}
		throw new IllegalStateException("Can not convert " + sid + " to link id");
	}

	
	public Id createFromZone2ToZoneId(Id from, Id to){
		String idString = from + "22" + to;
		return idPool.createId(idString);
	}

	public Id createFromLink2ToLinkId(Id from, Id to){
		String idString = from + "33" + to;
		return idPool.createId(idString);
	}
	
	public Id createFrom2ToId(Id from, Id to){
		String idString = from + "44" + to;
		return idPool.createId(idString);
	}

	public Id getSymbolicId(Integer crossingId) {
		String idString = idPool.getStringId(crossingId);
		log.debug("Matched " + Integer.toString(crossingId) + " -> " + idString);
		return new IdImpl(idString);
	}

	
	
}
