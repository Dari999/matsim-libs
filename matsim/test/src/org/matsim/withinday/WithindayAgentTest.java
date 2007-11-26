/* *********************************************************************** *
 * project: org.matsim.*
 * WithindayAgentTest.java
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

package org.matsim.withinday;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import org.matsim.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.SimulationTimer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Route;
import org.matsim.withinday.WithindayAgent;
import org.matsim.withinday.coopers.CoopersAgentLogicFactory;
import org.matsim.withinday.trafficmanagement.EmptyControlInputImpl;
import org.matsim.withinday.trafficmanagement.VDSSign;
import org.matsim.withinday.trafficmanagement.feedbackcontroler.BangBangControler;

/**
 * @author dgrether
 */
public class WithindayAgentTest extends TestCase {

	private static final String networkFile = "./test/input/org/matsim/withinday/network.xml";

	private NetworkLayer network;

	private Route route1;

	private Route route2;

	private Route agentRoute;

	private Plan plan;

	private Leg leg;

	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.network = this.loadNetwork(networkFile);
		this.createRoutes();
	}

	private NetworkLayer loadNetwork(final String filename) {
		Gbl.reset();
		NetworkLayer network = new QueueNetworkLayer();
		Gbl.createConfig(null);
		MatsimNetworkReader parser = new MatsimNetworkReader(network);
		parser.readFile(filename);
		Gbl.createWorld().setNetworkLayer(network);
		return network;
	}

	private void createRoutes() {
		this.route1 = new Route();
		this.route2 = new Route();
		this.agentRoute = new Route();
		ArrayList<Node> list = new ArrayList<Node>();
		list.add(this.network.getNode("3"));
		list.add(this.network.getNode("31"));
		list.add(this.network.getNode("4"));
		this.route1.setRoute(list);
		list = new ArrayList<Node>();
		list.add(this.network.getNode("3"));
		list.add(this.network.getNode("32"));
		list.add(this.network.getNode("4"));
		this.route2.setRoute(list);
		list = new ArrayList<Node>();
		list.add(this.network.getNode("3"));
		list.add(this.network.getNode("32"));
		list.add(this.network.getNode("4"));
		this.agentRoute.setRoute(list);

	}

	private VDSSign createSign() {
		VDSSign sign = new VDSSign();
		sign.setSignLink(this.network.getLink("2"));
		sign.setDirectionLink(this.network.getLink("7"));
		sign.setBenefitControl(false);
		sign.setMessageHoldTime(1);
		sign.setControlEvents(1);
		sign.setDeadZoneSystemInput(0.0);
		sign.setDeadZoneSystemOutput(0.0);
		sign.setNominalSplitting(1);
		sign.setControler(new BangBangControler());
		sign.setCompliance(1.0);
		// create control input
		EmptyControlInputImpl controlInput = new EmptyControlInputImpl();
		controlInput.setNashTime(0);
		controlInput.setMainRoute(this.route1);
		controlInput.setAlternativeRoute(this.route2);
		// set control input
		sign.setControlInput(controlInput);
		sign.init();
		sign.open();
		sign.calculateOutput(SimulationTimer.getTime());
		return sign;
	}

	private WithindayAgent createAgent(final String homeLink, final String workLink) {
		Person p = new Person("1", "f", "23", "yes", "always", "yes");
		this.plan = new Plan(p);
		p.addPlan(this.plan);
		this.leg = null;
		try {
			this.plan.createAct("h", 0.0, 0.0, homeLink, "00:00", "00:00", "00:00", "false");
			this.leg = this.plan.createLeg(null, "car", "00:00", "00:00", "00:00");
			this.plan.createAct("work", 0.0, 0.0, workLink, "00:00", "00:00", "00:00", "false");
		} catch (Exception e) {
			e.printStackTrace();
		}
		p.setSelectedPlan(this.plan);
		this.leg.setRoute(this.agentRoute);
		//create the vehicle 
		WithindayAgentTestOccupiedVehicle v = new WithindayAgentTestOccupiedVehicle(
				this.leg, (QueueLink) this.network.getLink("2"), (QueueLink) this.network
						.getLink("7"), this.plan.getActsLegs());

	  //create the agentlogicfactory with 
		//...the vdssigns
		List<VDSSign> signs = new LinkedList<VDSSign>();
		signs.add(createSign());
		//... the scoring function
		CharyparNagelScoringConfigGroup scoringFunctionConfig = Gbl.getConfig().charyparNagelScoring();
		scoringFunctionConfig.addParam("activityType_1", "work");
		scoringFunctionConfig.addParam("activityPriority_1", "1");
		scoringFunctionConfig.addParam("activityTypicalDuration_1", "01:00");
		CoopersAgentLogicFactory factory = new CoopersAgentLogicFactory(
				this.network, scoringFunctionConfig, signs);
		//create the agent
		int sightDistance = 1;
		return new WithindayAgent(p, v, sightDistance, factory);
	}

	/**
	 * Test method for
	 * {@link org.matsim.withinday.WithindayAgent#replan()}.
	 */
	public void testReplan() {
		WithindayAgent agent = createAgent("2", "7");
		agent.replan();
		assertNotSame("The selected plan should be exchanged by a new one", this.plan, agent.getPerson().getSelectedPlan());
		//going into the details
	  // first testing the plan of the person
		Leg newPlansLeg = (Leg) agent.getPerson().getSelectedPlan().getActsLegs().get(1);
		ArrayList<Node> newPlansRoute = newPlansLeg.getRoute().getRoute();
		assertTrue("the agent's new route should have the same size as the old one", newPlansRoute.size() == this.agentRoute.getRoute().size());
		assertEquals("agent should be rerouted via node 31", this.network.getNode("31"), newPlansRoute.get(1));
		assertEquals("check the last node of rerouting!", this.network.getNode("4"), newPlansRoute.get(newPlansRoute.size()-1));
		//second testing the vehicle
		assertNotSame("The current leg should be exchanged by a new one", this.leg, agent.getVehicle().getCurrentLeg());
		ArrayList<Node> newLegsRoute = agent.getVehicle().getCurrentLeg().getRoute().getRoute();
		assertTrue("the agent's new route should have the same size as the old one", newLegsRoute.size() == this.agentRoute.getRoute().size());
		assertEquals("agent should be rerouted via node 31", this.network.getNode("31"), newLegsRoute.get(1));
		assertEquals("check the last node of rerouting!", this.network.getNode("4"), newLegsRoute.get(newLegsRoute.size()-1));
		
		//enlarge scenario
		ArrayList<Node> list = this.agentRoute.getRoute();
		list.add(0, this.network.getNode("2"));
		agent = createAgent("1", "7");
		agent.replan();
		assertNotSame("The selected plan should be exchanged by a new one", this.plan, agent.getPerson().getSelectedPlan());
		//going into the details
	  // first testing the plan of the person
		newPlansLeg = (Leg) agent.getPerson().getSelectedPlan().getActsLegs().get(1);
		newPlansRoute = newPlansLeg.getRoute().getRoute();
		assertTrue("the agent's new route should have the same size as the old one", newPlansRoute.size() == this.agentRoute.getRoute().size());
		assertEquals("agent should be rerouted via node 31", this.network.getNode("31"), newPlansRoute.get(2));
		assertEquals("check the last node of rerouting!", this.network.getNode("4"), newPlansRoute.get(newPlansRoute.size()-1));
		//second testing the vehicle
		assertNotSame("The current leg should be exchanged by a new one", this.leg, agent.getVehicle().getCurrentLeg());
		newLegsRoute = agent.getVehicle().getCurrentLeg().getRoute().getRoute();
		assertTrue("the agent's new route should have the same size as the old one", newLegsRoute.size() == this.agentRoute.getRoute().size());
		assertEquals("agent should be rerouted via node 31", this.network.getNode("31"), newLegsRoute.get(2));
		assertEquals("check the last node of rerouting!", this.network.getNode("4"), newLegsRoute.get(newLegsRoute.size()-1));
		
		//again enlarge scenario
		list.add(this.network.getNode("5"));
		agent = createAgent("1", "8");
		agent.replan();
		assertNotSame("The selected plan should be exchanged by a new one", this.plan, agent.getPerson().getSelectedPlan());
		//going into the details
	  // first testing the plan of the person
		newPlansLeg = (Leg) agent.getPerson().getSelectedPlan().getActsLegs().get(1);
		newPlansRoute = newPlansLeg.getRoute().getRoute();
		assertTrue("the agent's new route should have the same size as the old one", newPlansRoute.size() == this.agentRoute.getRoute().size());
		assertEquals("agent should be rerouted via node 31", this.network.getNode("31"), newPlansRoute.get(2));
		assertEquals("check the last node of rerouting!", this.network.getNode("5"), newPlansRoute.get(newPlansRoute.size()-1));
		//second testing the vehicle
		assertNotSame("The current leg should be exchanged by a new one", this.leg, agent.getVehicle().getCurrentLeg());
		newLegsRoute = agent.getVehicle().getCurrentLeg().getRoute().getRoute();
		assertTrue("the agent's new route should have the same size as the old one", newLegsRoute.size() == this.agentRoute.getRoute().size());
		assertEquals("agent should be rerouted via node 31", this.network.getNode("31"), newLegsRoute.get(2));
		assertEquals("check the last node of rerouting!", this.network.getNode("5"), newLegsRoute.get(newLegsRoute.size()-1));

	}

}
