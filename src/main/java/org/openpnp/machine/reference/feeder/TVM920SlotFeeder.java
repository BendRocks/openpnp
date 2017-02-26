package org.openpnp.machine.reference.feeder;

import org.openpnp.ConfigurationListener;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.driver.TVM920Driver;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Nozzle;
import org.simpleframework.xml.core.Commit;

public class TVM920SlotFeeder extends ReferenceSlotAutoFeeder {
	
	/*
	 @Override
	 @Commit
	    public void commit() {
	        Configuration.get().addListener(new ConfigurationListener() {
	            @Override
	            public void configurationLoaded(Configuration configuration) throws Exception {
	            }
	            
	            @Override
	            public void configurationComplete(Configuration configuration) throws Exception {
	                setBank(getBanks().get(bankId));
	                setFeeder(getBank().getFeeder(feederId));
	            }
	        });
	        
	        super.commit();
	    }
	    */
	
	
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
