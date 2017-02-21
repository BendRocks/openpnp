package org.openpnp.machine.reference.driver;

import java.io.IOException;
import java.util.HashMap;

import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.ReferencePasteDispenser;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Head;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.PropertySheetHolder.PropertySheet;
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
		}
		catch (Exception ex)
		{
			
		}
	}
	
	@Override
	public void close() throws IOException {
		hw.StopHeartbeat();

	}	
	
	protected Location getHeadLocation(Head head) {
        Location l = headLocations.get(head);
        if (l == null) {
            l = new Location(LengthUnit.Millimeters, 0, 0, 0, 0);
            setHeadLocation(head, l);
        }
        return l;
    }
	
	protected void setHeadLocation(Head head, Location l) {
        headLocations.put(head, l);
    }
	


	@Override
	public void home(ReferenceHead head) throws Exception {
		hw.FindHome();
		setHeadLocation(head, getHeadLocation(head).derive(hw.GetXPosMM(), hw.GetYPosMM(), 0.0, 0.0));
	}

	@Override
	public void moveTo(ReferenceHeadMountable hm, Location location, double speed) throws Exception {
		Logger.debug("moveTo({}, {}, {})", hm, location, speed);
        checkEnabled();

        // Subtract the offsets from the incoming Location. This converts the
        // offset coordinates to driver / absolute coordinates.
        location = location.subtract(hm.getHeadOffsets());

        // Convert the Location to millimeters, since that's the unit that
        // this driver works in natively.
        location = location.convertToUnits(LengthUnit.Millimeters);

        // Get the current location of the Head that we'll move
        Location hl = getHeadLocation(hm.getHead());

        //if (feedRateMmPerMinute > 0) {
        //    simulateMovement(hm, location, hl, speed);
        //}
        
        hw.MoveXYAbs(location.getX(), location.getY(), speed);

        // Now that movement is complete, update the stored Location to the new
        // Location, unless the incoming Location specified an axis with a value
        // of NaN. NaN is interpreted to mean "Don't move this axis" so we don't
        // update the value, either.
        
        hl = hl.derive(hw.GetXPosMM(), hw.GetYPosMM(), 0.0, 0.0);

        setHeadLocation(hm.getHead(), hl);
	}

	@Override
	public Location getLocation(ReferenceHeadMountable hm) {
		return getHeadLocation(hm.getHead()).add(hm.getHeadOffsets());
	}

	@Override
	public void pick(ReferenceNozzle nozzle) throws Exception {
		//hw.PickOpen(nozzle.getID);

	}

	@Override
	public void place(ReferenceNozzle nozzle) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void actuate(ReferenceActuator actuator, boolean on) throws Exception {
		if (actuator.getName().charAt(0) == 'A')
		{
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

	}

	@Override
	public void setEnabled(boolean enabled) throws Exception {
		if (enabled)
		{
			hw.StartHeartbeat();
			Logger.debug("Heartbeat started");
		}
		else{
			hw.StopHeartbeat();
			Logger.debug("Heartbeat stopped");
		}
	}
	
	private boolean checkEnabled()
	{
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
	
	@Override
	public Wizard getConfigurationWizard() {
		// TODO Auto-generated method stub
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
		return new PropertySheet[] {new PropertySheetWizardAdapter(getConfigurationWizard())};
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
