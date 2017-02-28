package org.openpnp.machine.reference.feeder;

import java.util.List;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.driver.TVM920Driver;
import org.openpnp.machine.reference.feeder.wizards.ReferenceSlotAutoFeederConfigurationWizard;
import org.openpnp.machine.reference.feeder.wizards.TVM920SlotAutoFeederConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Nozzle;
import org.simpleframework.xml.core.Commit;

public class TVM920SlotAutoFeeder extends ReferenceSlotAutoFeeder {

	private int lastFeederIndex = -1;
	
	private TVM920SlotAutoFeeder GetSlotByFeederID(List<org.openpnp.spi.Feeder> list, String feederID) {
		for (int i = 0; i < list.size(); i++) {
			try {
				if (list.get(i) instanceof TVM920SlotAutoFeeder) {
					TVM920SlotAutoFeeder f = (TVM920SlotAutoFeeder) list.get(i);
					if (f.getFeeder().getName().compareTo(feederID) == 0)
						return (TVM920SlotAutoFeeder) list.get(i);
				}
			} catch (Exception ex) {

			}
		}

		return null;
	}

	@Override
	public void feed(Nozzle nozzle) throws Exception {
		if (getFeeder() == null) {
			throw new Exception("No feeder loaded in slot.");
		}

		
		ReferenceMachine rm = (ReferenceMachine) Configuration.get().getMachine();
		TVM920Driver drv = (TVM920Driver) rm.getDriver();

		// Find out what slot this feeder is in
		TVM920SlotAutoFeeder slot = GetSlotByFeederID(rm.getFeeders(), getFeeder().getName());

		if (slot != null) {
			
			// Feeder name in the format of 'F07' or 'R13'
			String feederName = slot.getName().substring(1, 3);
			int feederIndex = Integer.valueOf(feederName);

			if (slot.getName().charAt(0) == 'R') {
				feederIndex += 32;
			}
			
			// This logic implements a push-push for the UI
			// . The first push opens the feeder, the second push
			// closes the feeder (as does a postPick). The UI has a place where the user can push 
			// a button once to advance a feeder. This function is called, but there's no way to 
			// know when the feeder can be closed. So, the push-push lets the user open and close 
			// feeders from that single UI button. When running a job, this might cause a problem
			// if the job told the feeder to advance twice. The first woudl open the feeder, the
			// second would close the feeder, and then the head would lower on the protective finger
			// Need to try this and see how it does.
			if (lastFeederIndex == feederIndex){
				drv.feedersCloseAll();
			}
			else{ 
				drv.feederOpen(feederIndex);
				lastFeederIndex = feederIndex;
			}
		}
		//super.feed(nozzle);
	}

	@Override
	public void postPick(Nozzle nozzle) throws Exception {
		if (getFeeder() == null) {
			throw new Exception("No feeder loaded in slot.");
		}
		// super.postPick(nozzle);
		ReferenceMachine rm = (ReferenceMachine) Configuration.get().getMachine();
		TVM920Driver drv = (TVM920Driver) rm.getDriver();
		drv.feedersCloseAll();
		lastFeederIndex = -1;
	}

	@Override
	public Wizard getConfigurationWizard() {
		return new TVM920SlotAutoFeederConfigurationWizard(this);
	}

}
