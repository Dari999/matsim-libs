/*******************************************************************************
 * Copyright (c) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Stefan Schroeder - initial API and implementation
 ******************************************************************************/
package org.matsim.contrib.freight.jsprit;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.algorithm.state.StateId;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.constraint.ConstraintManager;
import com.graphhopper.jsprit.io.algorithm.AlgorithmConfig;
import com.graphhopper.jsprit.io.algorithm.AlgorithmConfigXmlReader;
import com.graphhopper.jsprit.io.algorithm.VehicleRoutingAlgorithms;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.TourElement;

import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem.FleetSize;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingActivityCosts;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.job.Service;
import com.graphhopper.jsprit.core.problem.job.Service.Builder;
import com.graphhopper.jsprit.core.problem.job.Shipment;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.DeliverShipment;
import com.graphhopper.jsprit.core.problem.solution.route.activity.PickupService;
import com.graphhopper.jsprit.core.problem.solution.route.activity.PickupShipment;
import com.graphhopper.jsprit.core.problem.solution.route.activity.ServiceActivity;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivities;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity.JobActivity;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.util.Coordinate;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * A factory that creates matsim-object from jsprit
 * (https://github.com/jsprit/jsprit) and vice versa.
 *
 * @author sschroeder
 *         <p>
 *         Extend the functionality for the use of shipments
 * @author kturner
 */
public class MatsimJspritFactory {

	private static final  Logger log = Logger.getLogger(MatsimJspritFactory.class);

	// How to deal with a multi-depot VRP? Which depotLink should be used? kmt
	// jul/18

	/**
	 * Creates (MATSim) CarrierShipment from a (jsprit) service
	 *
	 * @param service   to be transformed to Shipment
	 * @param depotLink as from-link for Shipment
	 * @return CarrierShipment
	 * @see CarrierShipment, Service
	 */
	static CarrierShipment createCarrierShipmentFromService(Service service, String depotLink) {
		CarrierShipment carrierShipment = CarrierShipment.Builder
				.newInstance(Id.create(service.getId(), CarrierShipment.class), Id.create(depotLink, Link.class),
						Id.create(service.getLocation().getId(), Link.class), service.getSize().get(0))
				.setDeliveryServiceTime(service.getServiceDuration())
				.setDeliveryTimeWindow(
						TimeWindow.newInstance(service.getTimeWindow().getStart(), service.getTimeWindow().getEnd()))
				.build();
		for (String skill : service.getRequiredSkills().values()) {
			FreightUtils.addSkill(carrierShipment, skill);
		}
		return carrierShipment;
	}

	/**
	 * Creates (MATSim) {@link CarrierShipment} from a (jsprit) {@link Shipment}
	 *
	 * @param shipment to be transformed to MATSim
	 * @return CarrierShipment
	 * @see CarrierShipment, Shipment
	 */
	static CarrierShipment createCarrierShipment(Shipment shipment) {
		CarrierShipment carrierShipment = CarrierShipment.Builder
				.newInstance(Id.create(shipment.getId(), CarrierShipment.class),
						Id.createLinkId(shipment.getPickupLocation().getId()),
						Id.createLinkId(shipment.getDeliveryLocation().getId()), shipment.getSize().get(0))
				.setDeliveryServiceTime(shipment.getDeliveryServiceTime())
				.setDeliveryTimeWindow(TimeWindow.newInstance(shipment.getDeliveryTimeWindow().getStart(),
						shipment.getDeliveryTimeWindow().getEnd()))
				.setPickupServiceTime(shipment.getPickupServiceTime())
				.setPickupTimeWindow(TimeWindow.newInstance(shipment.getPickupTimeWindow().getStart(),
						shipment.getPickupTimeWindow().getEnd()))
				.build();
		FreightUtils.setSkills(carrierShipment, shipment.getRequiredSkills().values());
		return carrierShipment;
	}

	/**
	 * Creates (jsprit) {@link Shipment} from a (MATSim) {@link CarrierShipment}
	 *
	 * @param carrierShipment to be transformed to jsprit
	 * @return Shipment
	 * @see CarrierShipment, Shipment
	 */
	static Shipment createShipment(CarrierShipment carrierShipment) {
		Shipment.Builder shipmentBuilder = Shipment.Builder.newInstance(carrierShipment.getId().toString())
				.setDeliveryLocation(Location.newInstance(carrierShipment.getTo().toString()))
				.setDeliveryServiceTime(carrierShipment.getDeliveryServiceTime())
				.setDeliveryTimeWindow(com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow
						.newInstance(carrierShipment.getDeliveryTimeWindow().getStart(),
								carrierShipment.getDeliveryTimeWindow().getEnd()))
				.setPickupServiceTime(carrierShipment.getPickupServiceTime())
				.setPickupLocation(Location.newInstance(carrierShipment.getFrom().toString()))
				.setPickupTimeWindow(com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow.newInstance(
						carrierShipment.getPickupTimeWindow().getStart(),
						carrierShipment.getPickupTimeWindow().getEnd()))
				.addSizeDimension(0, carrierShipment.getSize());
		for (String skill : FreightUtils.getSkills(carrierShipment)) {
			shipmentBuilder.addRequiredSkill(skill);
		}
		return shipmentBuilder.build();
	}

	static Shipment createShipment(CarrierShipment carrierShipment, Coord fromCoord, Coord toCoord) {
		Location.Builder fromLocationBuilder = Location.Builder.newInstance();
		fromLocationBuilder.setId(carrierShipment.getFrom().toString());
		if (fromCoord != null) {
			fromLocationBuilder.setCoordinate(Coordinate.newInstance(fromCoord.getX(), fromCoord.getY()));
		}
		Location fromLocation = fromLocationBuilder.build();

		Location.Builder toLocationBuilder = Location.Builder.newInstance();
		toLocationBuilder.setId(carrierShipment.getTo().toString());
		if (toCoord != null) {
			toLocationBuilder.setCoordinate(Coordinate.newInstance(toCoord.getX(), toCoord.getY()));
		}
		Location toLocation = toLocationBuilder.build();

		Shipment.Builder shipmentBuilder = Shipment.Builder.newInstance(carrierShipment.getId().toString())
				.setDeliveryLocation(toLocation).setDeliveryServiceTime(carrierShipment.getDeliveryServiceTime())
				.setDeliveryTimeWindow(com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow
						.newInstance(carrierShipment.getDeliveryTimeWindow().getStart(),
								carrierShipment.getDeliveryTimeWindow().getEnd()))
				.setPickupServiceTime(carrierShipment.getPickupServiceTime()).setPickupLocation(fromLocation)
				.setPickupTimeWindow(com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow.newInstance(
						carrierShipment.getPickupTimeWindow().getStart(),
						carrierShipment.getPickupTimeWindow().getEnd()))
				.addSizeDimension(0, carrierShipment.getSize());
		for (String skill : FreightUtils.getSkills(carrierShipment)) {
			shipmentBuilder.addRequiredSkill(skill);
		}
		return shipmentBuilder.build();
	}

	static Service createService(CarrierService carrierService, Coord locationCoord) {
		Location.Builder locationBuilder = Location.Builder.newInstance();
		locationBuilder.setId(carrierService.getLocationLinkId().toString());
		if (locationCoord != null) {
			locationBuilder.setCoordinate(Coordinate.newInstance(locationCoord.getX(), locationCoord.getY()));
		}
		Location location = locationBuilder.build();
		Builder serviceBuilder = Service.Builder.newInstance(carrierService.getId().toString());
		serviceBuilder.addSizeDimension(0, carrierService.getCapacityDemand());
		serviceBuilder.setLocation(location).setServiceTime(carrierService.getServiceDuration())
				.setTimeWindow(com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow.newInstance(
						carrierService.getServiceStartTimeWindow().getStart(),
						carrierService.getServiceStartTimeWindow().getEnd()));
		for (String skill : FreightUtils.getSkills(carrierService)) {
			serviceBuilder.addRequiredSkill(skill);
		}
		return serviceBuilder.build();
	}

	static CarrierService createCarrierService(Service service) {
		CarrierService.Builder serviceBuilder = CarrierService.Builder.newInstance(
				Id.create(service.getId(), CarrierService.class), Id.create(service.getLocation().getId(), Link.class));
		serviceBuilder.setCapacityDemand(service.getSize().get(0));
		serviceBuilder.setServiceDuration(service.getServiceDuration());
		serviceBuilder.setServiceStartTimeWindow(
				TimeWindow.newInstance(service.getTimeWindow().getStart(), service.getTimeWindow().getEnd()));
		CarrierService carrierService = serviceBuilder.build();
		FreightUtils.setSkills(carrierService, service.getRequiredSkills().values());
		return carrierService;
	}

	/**
	 * Creates jsprit-vehicle from a matsim-carrier-vehicle.
	 *
	 * @param carrierVehicle to be transformed to jsprit
	 * @param locationCoord  to use as start location
	 * @return jsprit vehicle
	 * @see Vehicle, CarrierVehicle
	 */
	static Vehicle createVehicle(CarrierVehicle carrierVehicle, Coord locationCoord) {
		Location.Builder vehicleLocationBuilder = Location.Builder.newInstance();
		vehicleLocationBuilder.setId(carrierVehicle.getLocation().toString());
		if (locationCoord != null) {
			vehicleLocationBuilder.setCoordinate(Coordinate.newInstance(locationCoord.getX(), locationCoord.getY()));
		}
		Location vehicleLocation = vehicleLocationBuilder.build();
		com.graphhopper.jsprit.core.problem.vehicle.VehicleType vehicleType = createVehicleType(
				carrierVehicle.getType());
		VehicleImpl.Builder vehicleBuilder = VehicleImpl.Builder.newInstance(carrierVehicle.getId().toString());
		vehicleBuilder.setEarliestStart(carrierVehicle.getEarliestStartTime())
				.setLatestArrival(carrierVehicle.getLatestEndTime()).setStartLocation(vehicleLocation)
				.setType(vehicleType);
		for (String skill : FreightUtils.getSkills(carrierVehicle.getType())) {
			vehicleBuilder.addSkill(skill);
		}

		VehicleImpl vehicle = vehicleBuilder.build();

		if (carrierVehicle.getEarliestStartTime() != vehicle.getEarliestDeparture())
			throw new AssertionError("carrierVeh must have the same earliestDep as vrpVeh");
		if (carrierVehicle.getLatestEndTime() != vehicle.getLatestArrival())
			throw new AssertionError("carrierVeh must have the same latestArr as vrpVeh");
		if (!carrierVehicle.getLocation().toString().equals(vehicle.getStartLocation().getId()))
			throw new AssertionError("locations must be equal");
		return vehicle;
	}

	/**
	 * Creates {@link CarrierVehicle} from a {basics.route.Vehicle}
	 *
	 * @param vehicle to be transformed to CarrierVehicle
	 * @return carrierVehicle
	 * @see CarrierVehicle, Vehicle
	 */
	static CarrierVehicle createCarrierVehicle(Vehicle vehicle) {
		String vehicleId = vehicle.getId();
		CarrierVehicle.Builder vehicleBuilder = CarrierVehicle.Builder.newInstance(
				Id.create(vehicleId, org.matsim.vehicles.Vehicle.class),
				Id.create(vehicle.getStartLocation().getId(), Link.class));
		VehicleType carrierVehicleType = createCarrierVehicleType(vehicle.getType());
		vehicleBuilder.setType(carrierVehicleType);
		vehicleBuilder.setEarliestStart(vehicle.getEarliestDeparture());
		vehicleBuilder.setLatestEnd(vehicle.getLatestArrival());
		CarrierVehicle carrierVehicle = vehicleBuilder.build();

		for (String skill : vehicle.getSkills().values()) {
			FreightUtils.addSkill(carrierVehicle.getType(), skill);
		}

		if (vehicle.getEarliestDeparture() != carrierVehicle.getEarliestStartTime())
			throw new AssertionError("vehicles must have the same earliestStartTime");
		if (vehicle.getLatestArrival() != carrierVehicle.getLatestEndTime())
			throw new AssertionError("vehicles must have the same latestEndTime");
		if (!vehicle.getStartLocation().getId().equals(carrierVehicle.getLocation().toString()))
			throw new AssertionError("locs must be the same");
		return carrierVehicle;
	}

	/**
	 * Creates {@link VehicleType} from
	 * {@link com.graphhopper.jsprit.core.problem.vehicle.VehicleType}.
	 *
	 * <p>
	 * No description and engineInformation can be set here. Do it by calling
	 * setEngineInformation(engineInfo) from the returned object.
	 *
	 * @param jspritVehType to be transformed
	 * @return CarrierVehicleType
	 */
	static VehicleType createCarrierVehicleType(com.graphhopper.jsprit.core.problem.vehicle.VehicleType jspritVehType) {
		VehicleType vehicleType = VehicleUtils.getFactory()
				.createVehicleType(Id.create(jspritVehType.getTypeId(), VehicleType.class));
		vehicleType.getCapacity().setWeightInTons(jspritVehType.getCapacityDimensions().get(0));
		vehicleType.getCostInformation().setCostsPerMeter(jspritVehType.getVehicleCostParams().perDistanceUnit);
		vehicleType.getCostInformation().setCostsPerSecond(jspritVehType.getVehicleCostParams().perTransportTimeUnit);
		vehicleType.getCostInformation().setFixedCost(jspritVehType.getVehicleCostParams().fix);
		VehicleUtils.setCostsPerSecondInService(vehicleType.getCostInformation(), jspritVehType.getVehicleCostParams().perServiceTimeUnit);
		VehicleUtils.setCostsPerSecondWaiting(vehicleType.getCostInformation(), jspritVehType.getVehicleCostParams().perWaitingTimeUnit);
		vehicleType.setMaximumVelocity(jspritVehType.getMaxVelocity());
		return vehicleType;
	}

	/**
	 * Creates {@link com.graphhopper.jsprit.core.problem.vehicle.VehicleType} from
	 * {@link VehicleType}.
	 */
	static com.graphhopper.jsprit.core.problem.vehicle.VehicleType createVehicleType(VehicleType carrierVehicleType) {
		if (carrierVehicleType == null)
			throw new IllegalStateException("carrierVehicleType is null");
		VehicleTypeImpl.Builder typeBuilder = VehicleTypeImpl.Builder
				.newInstance(carrierVehicleType.getId().toString());
		if (carrierVehicleType.getCapacity().getVolumeInCubicMeters() < Double.MAX_VALUE) {
			throw new RuntimeException(
					"restrictions can currently only be set for \"other\".  not a big problem, but needs to be implemented.  "
							+ "kai/kai, sep'19");
		}
		if (carrierVehicleType.getCapacity().getWeightInTons() < Double.MAX_VALUE) {
			throw new RuntimeException(
					"restrictions can currently only be set for \"other\".  not a big problem, but needs to be implemented.  "
							+ "kai/kai, sep'19");
		}
		final double vehicleCapacity = carrierVehicleType.getCapacity().getOther();
		final int vehicleCapacityInt = (int) vehicleCapacity;
		if (vehicleCapacity - vehicleCapacityInt > 0) {
			log.warn("vehicle capacity truncated to int: before=" + vehicleCapacity + "; after=" + vehicleCapacityInt);
			// yyyyyy this implies that we would have fewer problems if we set vehicle
			// capacity in kg instead of in tons in our data model. kai, aug'19
		}
		typeBuilder.addCapacityDimension(0, vehicleCapacityInt);
		typeBuilder.setCostPerDistance(carrierVehicleType.getCostInformation().getCostsPerMeter());

		typeBuilder.setCostPerTransportTime(carrierVehicleType.getCostInformation().getCostsPerSecond());
		if (VehicleUtils.getCostsPerSecondInService(carrierVehicleType.getCostInformation()) != null) {
			typeBuilder.setCostPerServiceTime(VehicleUtils.getCostsPerSecondInService(carrierVehicleType.getCostInformation()));
		} else {
			log.info("Costs per service time is not set in VehicleType attributes. Will use the value of costsPerMeter instead. VehicleTypeId: " + carrierVehicleType.getId());
			typeBuilder.setCostPerServiceTime(carrierVehicleType.getCostInformation().getCostsPerSecond());
		}
		if (VehicleUtils.getCostsPerSecondWaiting(carrierVehicleType.getCostInformation()) != null) {
			typeBuilder.setCostPerWaitingTime(VehicleUtils.getCostsPerSecondWaiting(carrierVehicleType.getCostInformation()));
		} else {
			log.info("Costs per waiting time is not set in VehicleType attributes. Will use the value of costsPerMeter instead. VehicleTypeId: " + carrierVehicleType.getId());
			typeBuilder.setCostPerWaitingTime(carrierVehicleType.getCostInformation().getCostsPerSecond());
		}
		typeBuilder.setFixedCost(carrierVehicleType.getCostInformation().getFixedCosts());
		typeBuilder.setMaxVelocity(carrierVehicleType.getMaximumVelocity());
		return typeBuilder.build();
	}

	/**
	 * Creates {@link ScheduledTour} from {@link VehicleRoute}.
	 *
	 * @param route to be transformed
	 * @return ScheduledTour
	 * @throws IllegalStateException if tourActivity is NOT {@link ServiceActivity}.
	 */
	public static ScheduledTour createTour(VehicleRoute route) {
		if (route.getDepartureTime() != route.getStart().getEndTime())
			throw new AssertionError("at this point route.getDepartureTime and route.getStart().getEndTime() must be equal");

		TourActivities tour = route.getTourActivities();
		CarrierVehicle carrierVehicle = createCarrierVehicle(route.getVehicle());
		double depTime = route.getStart().getEndTime();

		Tour.Builder tourBuilder = Tour.Builder.newInstance();
		tourBuilder.scheduleStart(Id.create(route.getStart().getLocation().getId(), Link.class));
		for (TourActivity act : tour.getActivities()) {
			if (act instanceof ServiceActivity || act instanceof PickupService) {
				log.debug("Found ServiceActivity or PickupService : " + act.getName() + " at location "
						+ act.getLocation().getId() + " : " + act.getLocation().getCoordinate());
				Service job = (Service) ((JobActivity) act).getJob();
				CarrierService carrierService = createCarrierService(job);
				tourBuilder.addLeg(new Leg());
				tourBuilder.scheduleService(carrierService);
			} else if (act instanceof DeliverShipment) {
				log.debug("Found DeliveryShipment: " + act.getName() + " at location " + act.getLocation().getId()
						+ " : " + act.getLocation().getCoordinate());
				Shipment job = (Shipment) ((JobActivity) act).getJob();
				CarrierShipment carrierShipment = createCarrierShipment(job);
				tourBuilder.addLeg(new Leg());
				tourBuilder.scheduleDelivery(carrierShipment);
			} else if (act instanceof PickupShipment) {
				log.debug("Found PickupShipment: " + act.getName() + " at location " + act.getLocation().getId() + " : "
						+ act.getLocation().getCoordinate());
				Shipment job = (Shipment) ((JobActivity) act).getJob();
				CarrierShipment carrierShipment = createCarrierShipment(job);
				tourBuilder.addLeg(new Leg());
				tourBuilder.schedulePickup(carrierShipment);
			} else
				throw new IllegalStateException("unknown tourActivity occurred. this cannot be");
		}
		tourBuilder.addLeg(new Leg());
		tourBuilder.scheduleEnd(Id.create(route.getEnd().getLocation().getId(), Link.class));
		org.matsim.contrib.freight.carrier.Tour vehicleTour = tourBuilder.build();
		ScheduledTour sTour = ScheduledTour.newInstance(vehicleTour, carrierVehicle, depTime);
    
		if (route.getDepartureTime() != sTour.getDeparture())
			throw new AssertionError("departureTime of both route and scheduledTour must be equal");

		return sTour;
	}

	/**
	 * Creates {@link VehicleRoute} from {@link ScheduledTour}.
	 *
	 * <p>
	 * The {@link Network} is required to retrieve coordinates.
	 *
	 * @param scheduledTour         to be transformed
	 * @param vehicleRoutingProblem the routing problem
	 * @return VehicleRoute
	 */
	public static VehicleRoute createRoute(ScheduledTour scheduledTour, VehicleRoutingProblem vehicleRoutingProblem) {
		CarrierVehicle carrierVehicle = scheduledTour.getVehicle();
		double depTime = scheduledTour.getDeparture();
		Tour tour = scheduledTour.getTour();
		Id<org.matsim.vehicles.Vehicle> vehicleId = carrierVehicle.getId();
		Vehicle jspritVehicle = getVehicle(vehicleId.toString(), vehicleRoutingProblem);
		if (jspritVehicle == null)
			throw new IllegalStateException("jsprit-vehicle to id=" + vehicleId.toString() + " is missing");

		VehicleRoute.Builder routeBuilder = VehicleRoute.Builder.newInstance(jspritVehicle);
		routeBuilder.setJobActivityFactory(vehicleRoutingProblem.getJobActivityFactory());
		routeBuilder.setDepartureTime(depTime);

		for (TourElement e : tour.getTourElements()) {
			if (e instanceof org.matsim.contrib.freight.carrier.Tour.TourActivity) {
				if (e instanceof org.matsim.contrib.freight.carrier.Tour.ServiceActivity) {
					CarrierService carrierService = ((org.matsim.contrib.freight.carrier.Tour.ServiceActivity) e)
							.getService();
					Service service = (Service) vehicleRoutingProblem.getJobs().get(carrierService.getId().toString());
					if (service == null)
						throw new IllegalStateException("service to id=" + carrierService.getId() + " is missing");
					routeBuilder.addService(service);
				}
			}
		}
		VehicleRoute route = routeBuilder.build();
		log.debug("jsprit route: " + route);
		log.debug("start-location: " + route.getStart().getLocation() + " endTime: " + route.getDepartureTime() + "("
				+ route.getStart().getEndTime() + ")");
		for (TourActivity act : route.getActivities()) {
			log.debug("act: " + act);
		}
		log.debug("end: " + route.getEnd());
		if (route.getDepartureTime() != scheduledTour.getDeparture())
			throw new AssertionError("departureTimes of both routes must be equal");
		return route;
	}

	private static Vehicle getVehicle(String id, VehicleRoutingProblem vehicleRoutingProblem) {
		for (Vehicle v : vehicleRoutingProblem.getVehicles()) {
			if (v.getId().equals(id))
				return v;
		}
		return null;
	}

	/**
	 * Creates an immutable {@link VehicleRoutingProblem} from {@link Carrier}.
	 *
	 * <p>
	 * For creation it takes only the information needed to setup the problem (not
	 * the solution, i.e. predefined plans are ignored).
	 * <p>
	 * The network is required to retrieve coordinates of locations.
	 * <p>
	 * Note that currently only services ({@link Service}) and Shipments
	 * ({@link Shipment}) are supported.
	 * <p>
	 * Pickups and deliveries can be defined as shipments with only one location
	 * (toLocation for delivery and fromLocation for pickup). Implementation follows
	 */
	public static VehicleRoutingProblem createRoutingProblem(Carrier carrier, Network network,
			VehicleRoutingTransportCosts transportCosts, VehicleRoutingActivityCosts activityCosts) {
		VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
		boolean serviceInVrp = false;
		boolean shipmentInVrp = false;

		FleetSize fleetSize;
		CarrierCapabilities carrierCapabilities = carrier.getCarrierCapabilities();
		if (carrierCapabilities.getFleetSize()
				.equals(org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize.INFINITE)) {
			fleetSize = FleetSize.INFINITE;
			vrpBuilder.setFleetSize(fleetSize);
		} else {
			fleetSize = FleetSize.FINITE;
			vrpBuilder.setFleetSize(fleetSize);
		}
		for (CarrierVehicle v : carrierCapabilities.getCarrierVehicles().values()) {
			Coord coordinate = null;
			if (network != null) {
				Link link = network.getLinks().get(v.getLocation());
				if (link == null)
					throw new IllegalStateException("vehicle.locationId cannot be found in network [vehicleId="
							+ v.getId() + "][locationId=" + v.getLocation() + "]");
				coordinate = link.getCoord();
			} else
				log.warn("cannot find linkId " + v.getId());
			Vehicle veh = createVehicle(v, coordinate);

			if (veh.getEarliestDeparture() != v.getEarliestStartTime())
				throw new AssertionError("earliestDeparture of both vehicles must be equal");
			if (veh.getLatestArrival() != v.getLatestEndTime())
				throw new AssertionError("latestArrTime of both vehicles must be equal");

			vrpBuilder.addVehicle(veh);
		}

		for (CarrierService service : carrier.getServices().values()) {
			if (shipmentInVrp) {
				throw new UnsupportedOperationException(
						"VRP with mixed Services and Shipments may lead to invalid solutions because of vehicle capacity handling are different");
			}
			Coord coordinate = null;
			if (network != null) {
				Link link = network.getLinks().get(service.getLocationLinkId());
				if (link != null) {
					coordinate = link.getCoord();
				} else
					log.warn("cannot find linkId " + service.getLocationLinkId());
			}
			serviceInVrp = true;
			vrpBuilder.addJob(createService(service, coordinate));
		}

		for (CarrierShipment carrierShipment : carrier.getShipments().values()) {
			if (serviceInVrp) {
				throw new UnsupportedOperationException(
						"VRP with mixed Services and Shipments may lead to invalid solutions because of vehicle capacity handling are different");
			}
			Coord fromCoordinate = null;
			Coord toCoordinate = null;
			if (network != null) {
				Link fromLink = network.getLinks().get(carrierShipment.getFrom());
				Link toLink = network.getLinks().get(carrierShipment.getTo());

				if (fromLink != null && toLink != null) { // Shipment to be delivered from specified location to
															// specified location
					fromCoordinate = fromLink.getCoord();
					toCoordinate = toLink.getCoord();
					vrpBuilder.addJob(createShipment(carrierShipment, fromCoordinate, toCoordinate));
				} else
					throw new IllegalStateException(
							"cannot create shipment since neither fromLinkId " + carrierShipment.getTo()
									+ " nor toLinkId " + carrierShipment.getTo() + " exists in network.");

			}
			shipmentInVrp = true;
			vrpBuilder.addJob(createShipment(carrierShipment, fromCoordinate, toCoordinate));
		}

		if (transportCosts != null)
			vrpBuilder.setRoutingCost(transportCosts);
		if (activityCosts != null)
			vrpBuilder.setActivityCosts(activityCosts);
		return vrpBuilder.build();
	}

	/**
	 * Creates {@link VehicleRoutingProblem.Builder} from {@link Carrier} for later
	 * building of the {@link VehicleRoutingProblem}. This is required if you need
	 * to add stuff to the problem later, e.g. because it cannot solely be retrieved
	 * from network and carrier such as {@link NetworkBasedTransportCosts}.
	 *
	 * <p>
	 * For creation it takes only the information needed to setup the problem (not
	 * the solution, i.e. predefined plans are ignored).
	 * <p>
	 * The network is required to retrieve coordinates of locations.
	 * <p>
	 * Note that currently only services ({@link Service}) and Shipments
	 * ({@link Shipment}) are supported.
	 * <p>
	 * Pickups and deliveries can be defined as shipments with only one location
	 * (toLocation for delivery and fromLocation for pickup). Implementation follows
	 */
	public static VehicleRoutingProblem.Builder createRoutingProblemBuilder(Carrier carrier, Network network) {
		VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
		boolean serviceInVrp = false;
		boolean shipmentInVrp = false;

		FleetSize fleetSize;
		CarrierCapabilities carrierCapabilities = carrier.getCarrierCapabilities();
		if (carrierCapabilities.getFleetSize()
				.equals(org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize.INFINITE)) {
			fleetSize = FleetSize.INFINITE;
			vrpBuilder.setFleetSize(fleetSize);
		} else {
			fleetSize = FleetSize.FINITE;
			vrpBuilder.setFleetSize(fleetSize);
		}
		for (CarrierVehicle v : carrierCapabilities.getCarrierVehicles().values()) {
			Coord coordinate = null;
			if (network != null) {
				Link link = network.getLinks().get(v.getLocation());
				if (link == null)
					throw new IllegalStateException("vehicle.locationId cannot be found in network [vehicleId="
							+ v.getId() + "][locationId=" + v.getLocation() + "]");
				coordinate = link.getCoord();
			} else
				log.warn("cannot find linkId " + v.getId());
			vrpBuilder.addVehicle(createVehicle(v, coordinate));
		}

		for (CarrierService service : carrier.getServices().values()) {
			log.debug("Handle CarrierService: " + service.toString());
			if (shipmentInVrp) {
				throw new UnsupportedOperationException(
						"VRP with mixed Services and Shipments may lead to invalid solutions because of vehicle capacity handling are different");
			}
			Coord coordinate = null;
			if (network != null) {
				Link link = network.getLinks().get(service.getLocationLinkId());
				if (link == null) {
					throw new IllegalStateException("cannot create service since linkId " + service.getLocationLinkId()
							+ " does not exists in network.");
				} else
					coordinate = link.getCoord();
			}
			serviceInVrp = true;
			vrpBuilder.addJob(createService(service, coordinate));
		}

		for (CarrierShipment carrierShipment : carrier.getShipments().values()) {
			log.debug("Handle CarrierShipment: " + carrierShipment.toString());
			if (serviceInVrp) {
				throw new UnsupportedOperationException(
						"VRP with mixed Services and Shipments may lead to invalid solutions because of vehicle capacity handling are different");
			}

			Coord fromCoordinate = null;
			Coord toCoordinate = null;
			if (network != null) {
				Link fromLink = network.getLinks().get(carrierShipment.getFrom());
				Link toLink = network.getLinks().get(carrierShipment.getTo());

				if (fromLink != null && toLink != null) { // Shipment to be delivered from specified location to
															// specified location
					log.debug("Shipment identified as Shipment: " + carrierShipment.getId().toString());
					fromCoordinate = fromLink.getCoord();
					toCoordinate = toLink.getCoord();
				} else
					throw new IllegalStateException("cannot create shipment " + carrierShipment.getId().toString()
							+ " since either fromLinkId " + carrierShipment.getFrom() + " or toLinkId "
							+ carrierShipment.getTo() + " exists in network.");

			}
			shipmentInVrp = true;
			vrpBuilder.addJob(createShipment(carrierShipment, fromCoordinate, toCoordinate));
		}

		return vrpBuilder;
	}

	/**
	 * Creates a {@link VehicleRoutingProblemSolution} from {@link CarrierPlan}.
	 *
	 * <p>
	 * To retrieve coordinates the {@link Network} is required. </br>
	 */
	public static VehicleRoutingProblemSolution createSolution(CarrierPlan plan,
			VehicleRoutingProblem vehicleRoutingProblem) {
		List<VehicleRoute> routes = new ArrayList<VehicleRoute>();
		for (ScheduledTour tour : plan.getScheduledTours()) {
			VehicleRoute route = createRoute(tour, vehicleRoutingProblem);
			routes.add(route);
		}
		double costs;
		if (plan.getScore() == null)
			costs = -9999.0;
		else
			costs = plan.getScore() * -1.0;
		return new VehicleRoutingProblemSolution(routes, costs);
	}

	/**
	 * Creates a {@link Carrier} from {@link VehicleRoutingProblem}. The carrier is
	 * initialized with the carrierId, i.e. <code>carrier.getId()</code> returns
	 * carrierId. </br>
	 */
	public static Carrier createCarrier(String carrierId, VehicleRoutingProblem vrp) {
		Id<Carrier> id = Id.create(carrierId, Carrier.class);
		Carrier carrier = CarrierUtils.createCarrier(id);
		CarrierCapabilities.Builder capabilityBuilder = CarrierCapabilities.Builder.newInstance();

		// fleet and vehicles
		if (vrp.getFleetSize().equals(FleetSize.FINITE)) {
			capabilityBuilder.setFleetSize(org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize.FINITE);
		} else
			capabilityBuilder.setFleetSize(org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize.INFINITE);
		for (com.graphhopper.jsprit.core.problem.vehicle.VehicleType type : vrp.getTypes()) {
			capabilityBuilder.addType(createCarrierVehicleType(type));
		}
		for (Vehicle vehicle : vrp.getVehicles()) {
			capabilityBuilder.addVehicle(createCarrierVehicle(vehicle));
		}
		carrier.setCarrierCapabilities(capabilityBuilder.build());

		return carrier;
	}

	/**
	 * Creates a {@link CarrierPlan} from {@link VehicleRoutingProblemSolution}.
	 *
	 * <p>
	 * Note that the costs of the solution are multiplied by -1 to represent the
	 * corresponding score of a carrierPlan.
	 * <p>
	 * The input parameter {@link Carrier} is just required to initialize the plan.
	 * </br>
	 */
	public static CarrierPlan createPlan(Carrier carrier, VehicleRoutingProblemSolution solution) {
		Collection<ScheduledTour> tours = new ArrayList<ScheduledTour>();
		for (VehicleRoute route : solution.getRoutes()) {
			ScheduledTour scheduledTour = createTour(route);
			tours.add(scheduledTour);
		}
		CarrierPlan carrierPlan = new CarrierPlan(carrier, tours);
		carrierPlan.setScore(solution.getCost() * (-1));
		return carrierPlan;
	}

	/**
	 * Creates or load the VehicleRoutingAlgorithm. If an algorithmFile is given the
	 * algorithm will be load. If there is no algorithm given a new algorithm will
	 * be created. In both situation the distanceConstraint will be used if he is
	 * set in the freightConfigGroup.
	 */
	public static VehicleRoutingAlgorithm loadOrCreateVehicleRoutingAlgorithm(Scenario scenario,
			FreightConfigGroup freightConfig, NetworkBasedTransportCosts netBasedCosts, VehicleRoutingProblem problem) {
		VehicleRoutingAlgorithm algorithm;
		final String vehicleRoutingAlgorithmFile = freightConfig.getVehicleRoutingAlgorithmFile();

		if (vehicleRoutingAlgorithmFile != null && !vehicleRoutingAlgorithmFile.equals("")) {
			log.info("Will read in VehicleRoutingAlgorithm from " + vehicleRoutingAlgorithmFile);
			URL vraURL;
			try {
				vraURL = IOUtils.extendUrl(scenario.getConfig().getContext(), vehicleRoutingAlgorithmFile);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			switch (freightConfig.getUseDistanceConstraintForTourPlanning()) {
			case noDistanceConstraint:
				algorithm = VehicleRoutingAlgorithms.readAndCreateAlgorithm(problem, vraURL);
				break;
			case basedOnEnergyConsumption:
				log.info("Use the distanceConstraint based on energy consumption.");
				StateManager stateManager = new StateManager(problem);

				StateId distanceStateId = stateManager.createStateId("distance");

				stateManager.addStateUpdater(new DistanceUpdater(distanceStateId, stateManager, netBasedCosts));

				ConstraintManager constraintManager = new ConstraintManager(problem, stateManager);
				constraintManager.addConstraint(
						new DistanceConstraint(
								FreightUtils.getCarrierVehicleTypes(scenario), netBasedCosts),
						ConstraintManager.Priority.CRITICAL);

				AlgorithmConfig algorithmConfig = new AlgorithmConfig();
				AlgorithmConfigXmlReader xmlReader = new AlgorithmConfigXmlReader(algorithmConfig);
				xmlReader.read(vraURL);
				algorithm = VehicleRoutingAlgorithms.readAndCreateAlgorithm(problem, algorithmConfig, 0, null,
						stateManager, constraintManager, true);
				break;
			default:
				throw new IllegalStateException(
						"Unexpected value: " + freightConfig.getUseDistanceConstraintForTourPlanning());
			}

		} else {
			log.info("Use a VehicleRoutingAlgorithm out of the box.");
			switch (freightConfig.getUseDistanceConstraintForTourPlanning()) {
			case noDistanceConstraint:
				algorithm = new SchrimpfFactory().createAlgorithm(problem);
				break;
			case basedOnEnergyConsumption:
				log.info("Use the distanceConstraint based on energy consumption.");
				StateManager stateManager = new StateManager(problem);

				StateId distanceStateId = stateManager.createStateId("distance");

				stateManager.addStateUpdater(new DistanceUpdater(distanceStateId, stateManager, netBasedCosts));

				ConstraintManager constraintManager = new ConstraintManager(problem, stateManager);
				constraintManager.addConstraint(
						new DistanceConstraint(
								FreightUtils.getCarrierVehicleTypes(scenario), netBasedCosts),
						ConstraintManager.Priority.CRITICAL);
				algorithm = Jsprit.Builder.newInstance(problem)
						.setStateAndConstraintManager(stateManager, constraintManager).buildAlgorithm();
				break;
			default:
				throw new IllegalStateException(
						"Unexpected value: " + freightConfig.getUseDistanceConstraintForTourPlanning());
			}
		}
		return algorithm;
	}
}
