package org.openpnp.machine.reference.driver;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.ReferencePasteDispenser;
import org.openpnp.machine.reference.driver.GcodeDriver.Axis;
import org.openpnp.machine.reference.feeder.ReferenceAutoFeeder;
import org.openpnp.machine.reference.feeder.ReferenceSlotAutoFeeder;
import org.openpnp.machine.reference.feeder.ReferenceSlotAutoFeeder.Bank;
import org.openpnp.machine.reference.feeder.ReferenceSlotAutoFeeder.Feeder;
import org.openpnp.machine.reference.feeder.TVM920SlotFeeder;
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

	public TVM920Driver() {
		try {
			hw = new TVM920Control();
			hw.Init();
		} catch (Exception ex) {

		}
	}

	private void Log(String s) {
		Logger.debug(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")) + " " + s);
	}

	@Override
	public void close() throws IOException {
		hw.StopHeartbeat();
	}

	protected Location getHeadLocation(Head head) {
		Log(String.format("getHeadLocation()", head));
		Location l = headLocations.get(head);
		if (l == null) {
			l = new Location(LengthUnit.Millimeters, 0, 0, 0, 0);
			setHeadLocation(head, l);
		}
		return l;
	}

	protected void setHeadLocation(Head head, Location l) {
		Log(String.format("setHeadLocation()", head, l));
		headLocations.put(head, l);
	}

	@Override
	public void home(ReferenceHead head) throws Exception {
		Log(String.format("Home()", head));
		// Find home location
		hw.FindHome();

		// Update home location. Pull this from hardware
		setHeadLocation(head, getHeadLocation(head).derive(hw.GetXPosMM(), hw.GetYPosMM(), 0.0, 0.0));
	}

	public void FeederOpen() {

	}

	public void FeederClose() {

	}

	@Override
	public void moveTo(ReferenceHeadMountable hm, Location location, double speed) throws Exception {
		Log(String.format("moveTo()", hm, location, speed));
		checkEnabled();

		// Subtract the offsets from the incoming Location. This converts the
		// offset coordinates to driver / absolute coordinates.
		location = location.subtract(hm.getHeadOffsets());

		// Convert the Location to millimeters, since that's the unit that
		// this driver works in natively.
		location = location.convertToUnits(LengthUnit.Millimeters);

		// Get the current location of the Head that we'll move
		Location hl = getHeadLocation(hm.getHead());

		double x = location.getX();
		double y = location.getY();
		double z = location.getZ();
		// double rotation = location.getRotation();

		// if (feedRateMmPerMinute > 0) {
		// simulateMovement(hm, location, hl, speed);
		// }

		hw.MoveXYAbs(x, y, speed);

		// Now that movement is complete, update the stored Location to the new
		// Location, unless the incoming Location specified an axis with a value
		// of NaN. NaN is interpreted to mean "Don't move this axis" so we don't
		// update the value, either.

		hl = hl.derive(Double.isNaN(x) ? null : hw.GetXPosMM(), Double.isNaN(y) ? null : hw.GetYPosMM(), 0.0, 0.0);

		setHeadLocation(hm.getHead(), hl);
	}

	@Override
	public Location getLocation(ReferenceHeadMountable hm) {
		Log(String.format("getLocation()", hm));
		return getHeadLocation(hm.getHead()).add(hm.getHeadOffsets());
	}

	@Override
	public void pick(ReferenceNozzle nozzle) throws Exception {
		Log(String.format("pick()", nozzle));

	}

	@Override
	public void place(ReferenceNozzle nozzle) throws Exception {
		Log(String.format("place()", nozzle));

	}

	@Override
	public void actuate(ReferenceActuator actuator, boolean on) throws Exception {
		Log(String.format("actuate()", actuator, on));
		if (actuator.getName().charAt(0) == 'A') {
			int index = Character.getNumericValue(actuator.getName().charAt(1)) - 1;
			if (on)
				hw.PickOpen(index);
			else
				hw.PickClose(index);
		}
	}

	@Override
	public void actuate(ReferenceActuator actuator, double value) throws Exception {
		// TODO Auto-generated method stub
		Log("setEnabled()");
	}

	@Override
	public void setEnabled(boolean enabled) throws Exception {
		Log(String.format("actuate()", enabled));
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
		// TODO Auto-generated method stub

	}

	private void createTVM920Nozzles() {
		ReferenceMachine rm = (ReferenceMachine) Configuration.get().getMachine();
		ReferenceHead rh = (ReferenceHead) rm.getHeads().get(0);

		// Add 4 new nozzles with required TVM characteristics. Then we'll
		// delete entries starting at 0 until we have the last 4 we created.
		// The system doesn't like deleting everything first for some reason
		for (int i = 0; i < 4; i++) {
			ReferenceNozzle rn = new ReferenceNozzle();

			rn.setPickDwellMilliseconds(100);
			rn.setPlaceDwellMilliseconds(100);

			rn.setName("NOZ" + Integer.toString(i));

			// Ballpark the locations. This will be different on all machines
			switch (i) {
			case 0:
				rn.setHeadOffsets(new Location(LengthUnit.Millimeters, -20, -10, 0, 0));
				break;

			case 1:
				rn.setHeadOffsets(new Location(LengthUnit.Millimeters, -10, -10, 0, 0));
				break;

			case 2:
				rn.setHeadOffsets(new Location(LengthUnit.Millimeters, -10, -10, 0, 0));
				break;

			case 3:
				rn.setHeadOffsets(new Location(LengthUnit.Millimeters, -20, -10, 0, 0));
				break;
			}

			try {
				rh.addNozzle(rn);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		while (rh.getNozzles().size() > 4)
			rh.removeNozzle(rh.getNozzles().get(0));
	}

	private void createTVM920Feeders() {
		try {
			ReferenceMachine rm = (ReferenceMachine) Configuration.get().getMachine();
			ReferenceHead rh = (ReferenceHead) rm.getHeads().get(0);

			// Make sure we have a part named null. The initialized slot will always point to this, 
			// otherwise the SlotFeeder cannot be activated
			Configuration cfg = Configuration.get();
			Part nullPart = cfg.getPart("NULL");
			
			if ( nullPart == null)
			{
				nullPart = new Part("NULL");
				nullPart.setName("NULL");
				cfg.addPart(nullPart);
			}
					
			// Create the built-in front and back feeders
			TVM920SlotFeeder frontFeeders = new TVM920SlotFeeder();
			frontFeeders.setName("F");
			frontFeeders.setEnabled(true);
			
			TVM920SlotFeeder rearFeeders = new TVM920SlotFeeder();
			rearFeeders.setName("R");
			rearFeeders.setEnabled(true);
			
			// Remove all banks except one	
			while (TVM920SlotFeeder.getBanks().size() > 1)
			{
				TVM920SlotFeeder.getBanks().remove(1);
			}
			
			// Set the name of this bank to FRONT
			TVM920SlotFeeder.getBanks().get(0).setName("FRONT");
			
			// Now add a second bank for the REAR
			Bank b = new Bank();
			b.setName("REAR");
			TVM920SlotFeeder.getBanks().add(b);

			frontFeeders.setBank(TVM920SlotFeeder.getBanks().get(0));
			rearFeeders.setBank(TVM920SlotFeeder.getBanks().get(1));
			
			TVM920SlotFeeder.getBanks().get(0).getFeeders().clear();
			TVM920SlotFeeder.getBanks().get(1).getFeeders().clear();
			
			for (int i=0; i<4; i++)
			{
				Feeder f = new Feeder();
				Feeder r = new Feeder();
				
				f.setName("F"+Integer.toString(i));
				f.setPart(nullPart);
				// First feeder pick location is the origin for the bank. 
				f.setOffsets(new Location(LengthUnit.Millimeters, i*18, 0, -12, 0));
				TVM920SlotFeeder.getBanks().get(0).getFeeders().add(f);
				
				r.setName("R"+Integer.toString(i));
				r.setPart(nullPart);
				f.setOffsets(new Location(LengthUnit.Millimeters, -i*18, 0, -12, 0));
				TVM920SlotFeeder.getBanks().get(1).getFeeders().add(r);
			}
			
			rm.addFeeder(frontFeeders);
			rm.addFeeder(rearFeeders);
			
			
		} catch (Exception ex) {

		}

	}

	@Override
	public Wizard getConfigurationWizard() {
		// TODO Auto-generated method stub
		createTVM920Nozzles();
		createTVM920Feeders();

		return null;
	}

	@Override
	public String getPropertySheetHolderTitle() {
		// TODO Auto-generated method stub
		return getClass().getSimpleName();
	}

	@Override
	public PropertySheetHolder[] getChildPropertySheetHolders() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PropertySheet[] getPropertySheets() {
		// TODO Auto-generated method stub
		return new PropertySheet[] { new PropertySheetWizardAdapter(getConfigurationWizard()) };
	}

	@Override
	public Action[] getPropertySheetHolderActions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Icon getPropertySheetHolderIcon() {
		// TODO Auto-generated method stub
		return null;
	}

}
