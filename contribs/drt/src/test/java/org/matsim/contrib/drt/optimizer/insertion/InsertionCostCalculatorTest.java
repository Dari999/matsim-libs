/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.optimizer.insertion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator.INFEASIBLE_SOLUTION_COST;
import static org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleImpl;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.testcases.fakes.FakeLink;

import com.google.common.collect.ImmutableList;

/**
 * @author Michal Maciejewski (michalm)
 */
public class InsertionCostCalculatorTest {
	private final Link fromLink = link("from");
	private final Link toLink = link("to");
	private final DrtRequest drtRequest = DrtRequest.newBuilder().fromLink(fromLink).toLink(toLink).build();

	@Test
	public void testCalculate() {
		VehicleEntry entry = entry(new double[] { 20, 50 });
		var insertion = new InsertionWithDetourData<>(insertion(entry, 0, 1), null, null, null, null);

		//feasible solution
		assertCalculate(insertion, new DetourTimeInfo(0, 0, 11, 22), 11 + 22);

		//feasible solution - longest possible pickup and dropoff time losses
		assertCalculate(insertion, new DetourTimeInfo(0, 0, 20, 30), 20 + 30);

		//infeasible solution - time constraints at stop 0
		assertCalculate(insertion, new DetourTimeInfo(0, 0, 21, 29), INFEASIBLE_SOLUTION_COST);

		//infeasible solution - vehicle time constraints
		assertCalculate(insertion, new DetourTimeInfo(0, 0, 20, 31), INFEASIBLE_SOLUTION_COST);
	}

	private <D> void assertCalculate(InsertionWithDetourData<D> insertion, DetourTimeInfo detourTimeInfo,
			double expectedCost) {
		@SuppressWarnings("unchecked")
		var detourTimeCalculator = (InsertionDetourTimeCalculator<D>)mock(InsertionDetourTimeCalculator.class);
		var insertionCostCalculator = new DefaultInsertionCostCalculator<>(
				new CostCalculationStrategy.RejectSoftConstraintViolations(), detourTimeCalculator);
		when(detourTimeCalculator.calculateDetourTimeInfo(insertion)).thenReturn(detourTimeInfo);
		assertThat(insertionCostCalculator.calculate(drtRequest, insertion)).isEqualTo(expectedCost);
	}

	private VehicleEntry entry(double[] slackTimes) {
		return new VehicleEntry(null, null, null, slackTimes);
	}

	private Link link(String id) {
		return new FakeLink(Id.createLinkId(id));
	}

	private InsertionGenerator.Insertion insertion(VehicleEntry entry, int pickupIdx, int dropoffIdx) {
		return new InsertionGenerator.Insertion(entry,
				new InsertionGenerator.InsertionPoint(pickupIdx, null, null, null),
				new InsertionGenerator.InsertionPoint(dropoffIdx, null, null, null));
	}
}
