package playground.andreas.bvgAna.level1;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import junit.framework.Assert;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;

public class VehId2PersonEnterLeaveVehicleMapTest {

	TreeMap<Id, ArrayList<PersonEntersVehicleEvent>> enter = new TreeMap<Id, ArrayList<PersonEntersVehicleEvent>>();
	TreeMap<Id, ArrayList<PersonLeavesVehicleEvent>> leave = new TreeMap<Id, ArrayList<PersonLeavesVehicleEvent>>();
	
	@Test
	public void testVehId2PersonEnterLeaveVehicleMap() {
		
		Id[] ida= new Id[15];
		Set<Id> idSet = new TreeSet<Id>();
	    for (int ii=0; ii<15; ii++){
	    	ida[ii] = new IdImpl(ii); 
	        idSet.add(ida[ii]);
	    }
	    
//	    assign Ids to routes, vehicles and agents to be used in Test
	    
	    Id vehId1 = ida[1];
	    Id vehId2 = ida[2];
	    Id persId1 = ida[4];
	    Id persId2 = ida[5];
	    Id persId3 = ida[6];
	    Id persId4 = ida[7];

//	    create events
	    
	    EventsFactory ef = new EventsFactory();
	    
	    PersonEntersVehicleEvent event1 = ef.createPersonEntersVehicleEvent(2., persId1, vehId1);
	    PersonEntersVehicleEvent event2 = ef.createPersonEntersVehicleEvent(2.1, persId2, vehId1);  
	    PersonLeavesVehicleEvent event3 = ef.createPersonLeavesVehicleEvent(2.2, persId3, vehId2);
	    PersonLeavesVehicleEvent event4 = ef.createPersonLeavesVehicleEvent(2.3, persId4, vehId2);
	    
//	    create instance of class to be tested
	    
	    VehId2PersonEnterLeaveVehicleMap test = new VehId2PersonEnterLeaveVehicleMap();
	    
//	    handle events
	    
	    test.handleEvent(event1);
	    test.handleEvent(event2);
	    test.handleEvent(event3);
	    test.handleEvent(event4);
	    
//	    add events to local TreeMaps for comparison
	    
	    enter.put(event1.getVehicleId(), new ArrayList<PersonEntersVehicleEvent>());
	    enter.get(event1.getVehicleId()).add(event1);
	    enter.get(event2.getVehicleId()).add(event2);
	    
	    leave.put(event3.getVehicleId(), new ArrayList<PersonLeavesVehicleEvent>());
	    leave.get(event3.getVehicleId()).add(event3);
	    leave.get(event4.getVehicleId()).add(event4);
	    
//	    test
	    	    
//	    Assert.assertEquals(enter.get(vehId1).get(0), test.getVehId2PersonEnterEventMap().get(vehId1).get(0));
	    Assert.assertEquals(event1.getTime(), test.getVehId2PersonEnterEventMap().get(vehId1).get(0).getTime(), 0.);
	    
//	    Assert.assertEquals(enter.get(vehId1).get(1), test.getVehId2PersonEnterEventMap().get(vehId1).get(1));
	    Assert.assertEquals(event2.getTime(), test.getVehId2PersonEnterEventMap().get(vehId1).get(1).getTime(), 0.);

	    
//	    Assert.assertEquals(leave.get(vehId2).get(0), test.getVehId2PersonLeaveEventMap().get(vehId2).get(0));
	    Assert.assertEquals(event3.getTime(), test.getVehId2PersonLeaveEventMap().get(vehId2).get(0).getTime(), 0.);

//	    Assert.assertEquals(leave.get(vehId2).get(1), test.getVehId2PersonLeaveEventMap().get(vehId2).get(1));
	    Assert.assertEquals(event4.getTime(), test.getVehId2PersonLeaveEventMap().get(vehId2).get(1).getTime(), 0.);

	    
		
	}

}
