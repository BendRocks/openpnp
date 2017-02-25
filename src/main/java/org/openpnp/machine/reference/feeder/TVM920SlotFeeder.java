package org.openpnp.machine.reference.feeder;

import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.driver.TVM920Driver;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Nozzle;

public class TVM920SlotFeeder extends ReferenceSlotAutoFeeder {
	
    @Override
    public void feed(Nozzle nozzle) throws Exception {
        if (getFeeder() == null) {
            throw new Exception("No feeder loaded in slot.");
        }
        
        ReferenceMachine rm = (ReferenceMachine) Configuration.get().getMachine();
        TVM920Driver drv = (TVM920Driver)rm.getDriver();
        drv.FeederOpen();
        //super.feed(nozzle);
    }

    @Override
    public void postPick(Nozzle nozzle) throws Exception {
        if (getFeeder() == null) {
            throw new Exception("No feeder loaded in slot.");
        }
        //super.postPick(nozzle);
        ReferenceMachine rm = (ReferenceMachine) Configuration.get().getMachine();
        TVM920Driver drv = (TVM920Driver)rm.getDriver();
        drv.FeederOpen();        
    }

}
