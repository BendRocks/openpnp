package org.openpnp.machine.reference.driver;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.events.FeederSelectedEvent;
import org.openpnp.gui.FeedersPanel;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.machine.reference.ReferencePasteDispenser;
import org.openpnp.machine.reference.driver.GcodeDriver.Axis;
import org.openpnp.machine.reference.driver.wizards.AbstractSerialPortDriverConfigurationWizard;
import org.openpnp.machine.reference.driver.wizards.TVMSettings;
import org.openpnp.machine.reference.feeder.ReferenceAutoFeeder;
import org.openpnp.machine.reference.feeder.ReferenceSlotAutoFeeder;
import org.openpnp.machine.reference.feeder.ReferenceSlotAutoFeeder.Bank;
import org.openpnp.machine.reference.feeder.ReferenceSlotAutoFeeder.Feeder;
import org.openpnp.machine.reference.feeder.TVM920SlotAutoFeeder;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.PropertySheetHolder.PropertySheet;
import org.openpnp.util.IdentifiableList;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

public class TVM920Driver implements ReferenceDriver {

	@Attribute(required = false)
	private double feedRateMmPerMinute = 5000;

	private TVM920Control hw;

	private HashMap<Head, Location> headLocations = new HashMap<>();
	
	// In the line above, we track the head locations. We can learn the location
	// of each nozzle by offseting from the head locations. However, we don't know
	// the heigh of each nozzle. So, that is stored here.
	private double[] nozzleHeights = new double[4]; 
	
	public TVM920Driver() { 
		try {
			hw = new TVM920Control();
			hw.Init();			
		} catch (Exception ex) {

		}
	}

	private void log(String s) {
		Logger.debug(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")) + " " + s);
	}

	@Override
	public void close() throws IOException {
		hw.StopHeartbeat();
	}


	// Look up head location from hashmap
	protected Location getHeadLocation(Head head) {
		Location l = headLocations.get(head);
		if (l == null) {
			l = new Location(LengthUnit.Millimeters, 0, 0, 0, 0);
			setHeadLocation(head, l);
		}
		return l;
	}

	// Save head location 
	protected void setHeadLocation(Head head, Location l) {
		log(String.format("setHeadLocation(%s, %s)", head, l));
		headLocations.put(head, l);
	}

	@Override
	public void home(ReferenceHead head) throws Exception {
		log(String.format("home(%s)", head));
		// Find home location
		hw.findHome();

		// Update home location. Pull this from hardware
		setHeadLocation(head, getHeadLocation(head).derive(hw.getXPosMM(), hw.getYPosMM(), 0.0, 0.0));
	}

	// 0 = F01, 1 = F2....32 = R01, 33 = R02...
	public void feederOpen(int feederNumber) {
		log(String.format("feederOpen(%d)", feederNumber));
		hw.feederOpen(feederNumber);
	}

	public void feedersCloseAll() {
		log(String.format("feedersCloseAll()"));
		hw.feedersCloseAll();
	}
	
	@Override
	public void moveTo(ReferenceHeadMountable hm, Location location, double speed) throws Exception {
		
		log(String.format("moveTo(HM:%s, Loc:%s, Speed:%.3f)", hm.getName(), location, speed));
		checkEnabled();
		
		Instant stopwatch = Instant.now();

		// Subtract the offsets from the incoming Location. This converts the
		// offset coordinates to driver / absolute coordinates.
		location = location.subtract(hm.getHeadOffsets());

		// Convert the Location to millimeters, since that's the unit that
		// this driver works in natively.
		location = location.convertToUnits(LengthUnit.Millimeters);
		
		// Get the current location of the Head that we'll move. This is 
		// pulled from the hashmap 
		Location hl = getHeadLocation(hm.getHead());
		
		int nozzleIndex = -1;
		// If a nozzle is involved, we need the head index
		if (hm.getName().contains("NZ"))
		{ 
			nozzleIndex = Character.getNumericValue(hm.getName().charAt(2));
		}
		
		double x = location.getX();
		double y = location.getY();
		double z = location.getZ();
		double theta = location.getRotation();
		
		// Do we need to do a z move?
		if ( (Double.isNaN(z) == false) && (nozzleIndex != -1) ){
			nozzleHeights[nozzleIndex] = z;
			hw.moveZAbs(nozzleIndex, z, speed);  
		}
		
		hw.moveXYThetaAbs(x, y, nozzleIndex, theta, speed);
		
		log("   MoveTo() complete. Elapsed: " + Duration.between(stopwatch, Instant.now()));
		
        hl = hl.derive(	Double.isNaN(location.getX()) ? null : location.getX(),
                		Double.isNaN(location.getY()) ? null : location.getY(),
                		Double.isNaN(location.getZ()) ? null : location.getZ(),
                		Double.isNaN(location.getRotation()) ? null : location.getRotation());

		setHeadLocation(hm.getHead(), hl);
	}

	@Override
	public Location getLocation(ReferenceHeadMountable hm) {
		int nozzleIndex = -1;
		double z = 0;
		
		if (hm.getName().contains("NZ"))
		{ 
			nozzleIndex = Character.getNumericValue(hm.getName().charAt(2));
			z = nozzleHeights[nozzleIndex];
		}
				
		Location loc = getHeadLocation(hm.getHead()).add(hm.getHeadOffsets());
		loc = loc.derive(null, null, z, null);
					
		return loc;
	}

	@Override
	public void pick(ReferenceNozzle nozzle) throws Exception {
		log(String.format("pick()", nozzle));

		int nozzleIndex = Character.getNumericValue(nozzle.getName().charAt(2));
		hw.pickOpen(nozzleIndex);
	}

	@Override
	public void place(ReferenceNozzle nozzle) throws Exception {
		log(String.format("place()", nozzle));

		int nozzleIndex = Character.getNumericValue(nozzle.getName().charAt(2));
		hw.pickClose(nozzleIndex);
	}

	@Override
	public void actuate(ReferenceActuator actuator, boolean on) throws Exception {
		log( String.format("actuate(%s, %s)", actuator, on));
		if (actuator.getName().equals("UpCamLights"))
		{
				hw.upLightOn(on);
		}
		else if (actuator.getName().equals("DownCamLights"))
		{
				hw.downLightOn(on);
		}
	}

	@Override
	public void actuate(ReferenceActuator actuator, double value) throws Exception {
		String s = "TVM920Driverl:actuate(ReferenceActuator actuator, double value) called. This shouldn't happen on the TVM920.";
		log(s);
		throw new IllegalArgumentException(s);
	}

	@Override
	public void setEnabled(boolean enabled) throws Exception {
		log(String.format("setEnabled(%s)", enabled));
		if (enabled) {
			hw.startHeartbeat();
			Logger.debug("Heartbeat started");
		} else {
			hw.StopHeartbeat();
			Logger.debug("Heartbeat stopped");
		}
	}

	private boolean checkEnabled() {
		if (hw != null)
			return hw.checkEnabled();
		else
			return false;
	}

	@Override
	public void dispense(ReferencePasteDispenser dispenser, Location startLocation, Location endLocation,
			long dispenseTimeMilliseconds) throws Exception {
		String s = "TVM920Driverl:dispense() called. This shouldn't happen on the TVM920.";
		log(s);
		throw new IllegalArgumentException(s);
	}
	
	public Location getHomeLocation(){
		return hw.getHomeLocation();
	}
	
	// Sets current XY location as home. BUGBUG: Should make sure
	// home location is no more than +/-10 mm from HW location to ensure
	// user doesn't set home to location that would render bounds checking
	// useless
	public void setAsHomeLocation()
	{
		hw.setAsHomeLocation();
	}
	

	public void createTVM920Nozzles() {
		ReferenceMachine rm = (ReferenceMachine) Configuration.get().getMachine();
		ReferenceHead rh = (ReferenceHead) rm.getHeads().get(0);
		
		int numNozzles = 4;

		try {
			
			// Remove all existing nozzles
			while (rh.getNozzles().size() > 0)
			{
				Nozzle n = rh.getNozzles().get(0);
				
				try
				{
					rh.removeNozzle(n);
				}
				catch (Exception e)
				{
					
				}
			}
			
			
			// Add 4 new nozzles with required TVM characteristics. 
			for (int i = 0; i < numNozzles; i++) {
				ReferenceNozzle rn = new ReferenceNozzle();
				ReferenceNozzleTip nt = new ReferenceNozzleTip();

				rn.setPickDwellMilliseconds(100);
				rn.setPlaceDwellMilliseconds(100);
				rn.setLimitRotation(false);

				rn.setName("NZ" + Integer.toString(i));
				nt.setName("NT" + Integer.toString(i));

				switch (i) {
				case 0:
					rn.setHeadOffsets(new Location(LengthUnit.Millimeters, -38.8, -59.8, 0, 0));
					break;

				case 1:
					rn.setHeadOffsets(new Location(LengthUnit.Millimeters, -38.8 + 25.6, -59.8, 0, 0));
					break;

				case 2:
					rn.setHeadOffsets(new Location(LengthUnit.Millimeters, -38.8 + 50.2, -59.8, 0, 0));
					break;

				case 3:
					rn.setHeadOffsets(new Location(LengthUnit.Millimeters, -38.8 + 75.5, -59.8, 0, 0));
					break;
				}
				
				rh.addNozzle(rn);
				rn.addNozzleTip(nt);
				//rn.setNozzleTip(nt);
			}
			
			//while (rh.getNozzles().size() > numNozzles)
			//{
			//	Nozzle n = rh.getNozzles().get(0);
			//	rh.removeNozzle(n);
			//}

		} catch (Exception e) {
			log("Exception setting up tips and nozzles: " + e.toString());
		}
	}

	//
	// Removes all feeders from machine and creates new front/rear feeders. This should rarely be called after initial config
	//
	public void createTVM920Feeders() {
		try {
			ReferenceMachine rm = (ReferenceMachine) Configuration.get().getMachine();
			ReferenceHead rh = (ReferenceHead) rm.getHeads().get(0);

			// Remove all feeders from machine
			while (rm.getFeeders().size() > 0) {
				org.openpnp.spi.Feeder f = rm.getFeeders().get(0);
				log("Removed feeder");
				rm.removeFeeder(f);
			}

			// Remove all banks except one
			while (TVM920SlotAutoFeeder.getBanks().size() > 1) {
				TVM920SlotAutoFeeder.getBanks().remove(1);
			}

			TVM920SlotAutoFeeder.getBanks().get(0).setName("FRONT");

			TVM920SlotAutoFeeder.getBanks().get(0).getFeeders().clear();

			for (int i = 1; i < 5; i++) {
				TVM920SlotAutoFeeder frontFeeder = new TVM920SlotAutoFeeder();
				frontFeeder.setName("F" + String.format("%02d", i)); 
				frontFeeder.setEnabled(true);
				frontFeeder.setBank(TVM920SlotAutoFeeder.getBanks().get(0));
				frontFeeder.setLocation(new Location(LengthUnit.Millimeters, (i-1) * 16.5 + 11.8, 14.8, -10, 0));
				rm.addFeeder(frontFeeder);

				TVM920SlotAutoFeeder rearFeeder = new TVM920SlotAutoFeeder();
				rearFeeder.setName("R" + String.format("%02d", i)); // Do not change this "R" prefix without looking at code in TVM920SlotAutoFeeder
				rearFeeder.setEnabled(true);
				rearFeeder.setBank(TVM920SlotAutoFeeder.getBanks().get(0));
				rearFeeder.setLocation(new Location(LengthUnit.Millimeters, 440 + i * -18, 0, -12, 0));
				rm.addFeeder(rearFeeder);
			}
			
			MainFrame.get().getFeedersTab().refresh();

		} catch (Exception ex) {
			log("Exception in createTVM920Feeders() " + ex.getMessage());
		}

	}
	
	public void createTVM920Actuators(){
		try{
			ReferenceMachine rm = (ReferenceMachine) Configuration.get().getMachine();
			
			while (rm.getActuators().isEmpty() == false){
				ReferenceActuator ra = (ReferenceActuator)rm.getActuators().get(0);
				rm.removeActuator(ra);
			}
			
			ReferenceActuator ra = new ReferenceActuator();
			ra.setName("UpCamLights"); // Do not change this name without changing name in Actuator code above
			rm.addActuator(ra);
			
			ra = new ReferenceActuator();
			ra.setName("DownCamLights"); // Do not change this name without changing name in Actuator code above
			rm.addActuator(ra);
		}
		catch (Exception ex)
		{
			log("Exception in createTVM920Actuators() " + ex.getMessage());
		}
	
	}
	

	@Override
	public Wizard getConfigurationWizard() {
		return new TVMSettings(this);
		//createTVM920Nozzles();
		//createTVM920Feeders();
		//createTVM920Actuators();
		//return null;
	}

	@Override
	public String getPropertySheetHolderTitle() {
		return getClass().getSimpleName();
	}

	@Override
	public PropertySheetHolder[] getChildPropertySheetHolders() {
		return null;
	}

	@Override
	public PropertySheet[] getPropertySheets() {
		return new PropertySheet[] {new PropertySheetWizardAdapter(getConfigurationWizard())};
	}

	@Override
	public Action[] getPropertySheetHolderActions() {
		return null;
	}

	@Override
	public Icon getPropertySheetHolderIcon() {
		return null;
	}

}