/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.surprice.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.analysis.Bins;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class TravelTimeCalculator implements AgentDepartureEventHandler, AgentArrivalEventHandler, 
	ActivityStartEventHandler {

	private final Map<Id, Double> agentDepartures = new HashMap<Id, Double>();
	private final Map<Id, Double> agentArrivals = new HashMap<Id, Double>();
	private final Map<Id, String> agentArrivalsMode = new HashMap<Id, String>();
	private final ArrayList<Double> travelTimes = new ArrayList<Double>();
	private double sumTripDurations = 0;
	private double sumTripDurationsIncomeWeighted = 0;
	private int sumTrips = 0;
	private TreeMap<Integer, ArrayList<Double>> carTimesPerIncome = new TreeMap<Integer, ArrayList<Double>>();
	private TreeMap<Integer, ArrayList<Double>> ptTimesPerIncome = new TreeMap<Integer, ArrayList<Double>>();
	private TreeMap<Integer, ArrayList<Double>> ttTimesPerIncome = new TreeMap<Integer, ArrayList<Double>>();
	private Bins ttBins;
	private ObjectAttributes incomes;
	private TreeMap<Id, Double> ttPerAgent = new TreeMap<Id, Double>(); 
	
	public TravelTimeCalculator(Bins ttBins, ObjectAttributes incomes) {
		this.ttBins = ttBins;
		this.incomes = incomes;
	}
	
	@Override
	public void handleEvent(final AgentDepartureEvent event) {
		this.agentDepartures.put(event.getPersonId(), event.getTime());
		
		if (this.ttPerAgent.get(event.getPersonId()) == null) {
			this.ttPerAgent.put(event.getPersonId(), 0.0);
		}		
	}

	@Override
	public void handleEvent(final AgentArrivalEvent event) {
		this.agentArrivals.put(event.getPersonId(), event.getTime());
		this.agentArrivalsMode.put(event.getPersonId(), event.getLegMode());
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		Double depTime = this.agentDepartures.remove(event.getPersonId());
		Double arrTime = this.agentArrivals.remove(event.getPersonId());
		String mode = this.agentArrivalsMode.remove(event.getPersonId());
		if (depTime != null) {
			double travTime = arrTime - depTime;
			this.sumTripDurations += travTime;
			this.sumTrips++;
			
			this.travelTimes.add(travTime);
			
			double income = (Double)this.incomes.getAttribute(event.getPersonId().toString(), "income");
			this.ttBins.addVal(income, travTime);
			
			this.sumTripDurationsIncomeWeighted += travTime * income;
			
			if (mode.equals("car")) {
				if (this.carTimesPerIncome.get((int)income) == null) {
					this.carTimesPerIncome.put((int)income, new ArrayList<Double>());
				}
				this.carTimesPerIncome.get((int)income).add(travTime);
			}
			else if (mode.equals("pt")) {
				if (this.ptTimesPerIncome.get((int)income) == null) {
					this.ptTimesPerIncome.put((int)income, new ArrayList<Double>());
				}
				this.ptTimesPerIncome.get((int)income).add(travTime);
			}
			this.ttTimesPerIncome.get((int)income).add(travTime);
			double val = this.ttPerAgent.get(event.getPersonId());
			this.ttPerAgent.put(event.getPersonId(), val + travTime);
		}
	}
	
	@Override
	public void reset(final int iteration) {
		this.agentArrivals.clear();
		this.agentDepartures.clear();
		this.travelTimes.clear();
		this.sumTripDurations = 0;
		this.sumTrips = 0;	
		this.ttBins.clear();
		this.carTimesPerIncome.clear();
		this.ptTimesPerIncome.clear();
		this.ttPerAgent.clear();
	}
	
	public ArrayList<Double> getTravelTimes() {
		return this.travelTimes;
	}

	public double getAverageTripDuration() {
		return (this.sumTripDurations / this.sumTrips);
	}

	public double getSumTripDurationsIncomeWeighted() {
		return this.sumTripDurationsIncomeWeighted / (this.getAverageTripDuration() * this.sumTrips);
	}

	public TreeMap<Integer, ArrayList<Double>> getCarPerIncome() {
		return carTimesPerIncome;
	}

	public TreeMap<Integer, ArrayList<Double>> getPTPerIncome() {
		return ptTimesPerIncome;
	}

	public TreeMap<Id, Double> getTTPerAgent() {
		return ttPerAgent;
	}
	public TreeMap<Integer, ArrayList<Double>> getTTPerIncome() {
		return this.ttTimesPerIncome;
	}
}