package org.openpnp.machine.reference.driver.wizards;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.driver.AbstractSerialPortDriver;
import org.openpnp.machine.reference.driver.TVM920Driver;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.util.MovableUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class TVMSettings extends AbstractConfigurationWizard {
    
	private final TVM920Driver driver;
	
	private Location n0Mark;
	
	private JCheckBox nz0Pick, nz1Pick, nz2Pick, nz3Pick;
	
    public TVMSettings(TVM920Driver driver) {
        this.driver = driver;

        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "TVM920 Driver", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("right:default"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
        }));

        //JLabel lblPortName = new JLabel("Port");
        //panel.add(lblPortName, "2, 2, right, default");
        
        JButton btnCreateNozzles = new JButton("Create Nozzles/Feeders/Actuators");
        btnCreateNozzles.setAction(createDefaultsAction);
        panel.add(btnCreateNozzles, "2, 2");
        
        JLabel instructionLbl = new JLabel("Before marking locations, first ensure N0 offset is correct");
        panel.add(instructionLbl, "2, 4");
        
        JButton btnMarkNZ0 = new JButton("Mark NZ0 Location");
        btnMarkNZ0.setAction(markNZ0LocationAction);
        panel.add(btnMarkNZ0, "2, 6");
        
        JButton btnMarkNZ1 = new JButton("Mark NZ1 Location");
        btnMarkNZ1.setAction(markNZ1LocationAction);
        panel.add(btnMarkNZ1, "2, 8");
        
        JButton btnMarkNZ2 = new JButton("Mark NZ2 Location");
        btnMarkNZ2.setAction(markNZ2LocationAction);
        panel.add(btnMarkNZ2, "2, 10");
        
        JButton btnMarkNZ3 = new JButton("Mark NZ3 Location");
        btnMarkNZ3.setAction(markNZ3LocationAction);
        panel.add(btnMarkNZ3, "2, 12");
        
        JButton feederCloseAllBtn = new JButton("Close All Feeders");
        feederCloseAllBtn.setAction(closeAllFeedersAction);
        panel.add(feederCloseAllBtn, "2, 14");
        
        nz0Pick = new JCheckBox("nz0 pick");
        nz0Pick.setAction(nz0CheckboxAction);
        panel.add(nz0Pick, "2, 16");
        
        nz1Pick = new JCheckBox("nz1 pick");
        nz1Pick.setAction(nz1CheckboxAction);
        panel.add(nz1Pick, "2, 18");
        
        nz2Pick = new JCheckBox("nz2 pick");
        nz2Pick.setAction(nz1CheckboxAction);
        panel.add(nz2Pick, "2, 20");
        
        nz3Pick = new JCheckBox("nz3 pick");
        nz3Pick.setAction(nz1CheckboxAction);
        panel.add(nz3Pick, "2, 22");
        
        JButton moveToHomeBtn = new JButton("Move to Home Btn");
        moveToHomeBtn.setAction(moveToHomeAction);
        panel.add(moveToHomeBtn, "2, 24");        
    }
    
    private Action createDefaultsAction = new AbstractAction("Create Defaults") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
        	driver.createTVM920Nozzles();
        	driver.createTVM920Feeders();
        	driver.createTVM920Actuators();
        }
    };
    
    private Action markNZ0LocationAction = new AbstractAction("Mark NZ0") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
    		ReferenceMachine rm = (ReferenceMachine) Configuration.get().getMachine();
    		ReferenceHead rh = (ReferenceHead) rm.getHeads().get(0);
    		ReferenceNozzle rn = (ReferenceNozzle)rh.getNozzles().get(0);
    		n0Mark = rn.getLocation();
        }
    };
    
    private Action markNZ1LocationAction = new AbstractAction("Mark NZ1") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
    		ReferenceMachine rm = (ReferenceMachine) Configuration.get().getMachine();
    		ReferenceHead rh = (ReferenceHead) rm.getHeads().get(0);
    		ReferenceNozzle n0 = (ReferenceNozzle) rh.getNozzles().get(0);
    		ReferenceNozzle n1 = (ReferenceNozzle) rh.getNozzles().get(1);
    		
    		Location delta = n0.getLocation().subtract(n0Mark);
    		
    		n1.setHeadOffsets(n0.getHeadOffsets().subtract(delta));
        }
    };
    
    private Action markNZ2LocationAction = new AbstractAction("Mark NZ2") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
    		ReferenceMachine rm = (ReferenceMachine) Configuration.get().getMachine();
    		ReferenceHead rh = (ReferenceHead) rm.getHeads().get(0);
    		ReferenceNozzle n0 = (ReferenceNozzle) rh.getNozzles().get(0);
    		ReferenceNozzle n2 = (ReferenceNozzle) rh.getNozzles().get(2);
    		
    		Location delta = n0.getLocation().subtract(n0Mark);
    		
    		n2.setHeadOffsets(n0.getHeadOffsets().subtract(delta));
        }
    };
    
    private Action markNZ3LocationAction = new AbstractAction("Mark NZ3") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
    		ReferenceMachine rm = (ReferenceMachine) Configuration.get().getMachine();
    		ReferenceHead rh = (ReferenceHead) rm.getHeads().get(0);
    		ReferenceNozzle n0 = (ReferenceNozzle) rh.getNozzles().get(0);
    		ReferenceNozzle n3 = (ReferenceNozzle) rh.getNozzles().get(3);
    		
    		Location delta = n0.getLocation().subtract(n0Mark);
    		
    		n3.setHeadOffsets(n0.getHeadOffsets().subtract(delta));
        }
    };
    
    private Action closeAllFeedersAction = new AbstractAction("Close All Feeders") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
    		driver.feedersCloseAll();
        }
    };
    
    private Action nz0CheckboxAction = new AbstractAction("nz0 pick") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
    		ReferenceMachine rm = (ReferenceMachine) Configuration.get().getMachine();
    		ReferenceHead rh = (ReferenceHead) rm.getHeads().get(0);
    		
    		if (nz0Pick.isSelected())
    		{
    			try {
					driver.pick((ReferenceNozzle) rh.getNozzles().get(0));
				} catch (Exception e) {

				}
    		}
    		else
    		{
    			try {
					driver.place((ReferenceNozzle) rh.getNozzles().get(0));
				} catch (Exception e) {

				}
    		}
    		
        }
    };   
    
    
    
    private Action nz1CheckboxAction = new AbstractAction("nz1 pick") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
    		ReferenceMachine rm = (ReferenceMachine) Configuration.get().getMachine();
    		ReferenceHead rh = (ReferenceHead) rm.getHeads().get(0);
    		
    		if (nz0Pick.isSelected())
    		{
    			try {
					driver.pick((ReferenceNozzle) rh.getNozzles().get(1));
				} catch (Exception e) {

				}
    		}
    		else
    		{
    			try {
					driver.place((ReferenceNozzle) rh.getNozzles().get(1));
				} catch (Exception e) {

				}
    		}
    		
        }
    };   
    
    private Action nz2CheckboxAction = new AbstractAction("nz2 pick") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
    		ReferenceMachine rm = (ReferenceMachine) Configuration.get().getMachine();
    		ReferenceHead rh = (ReferenceHead) rm.getHeads().get(0);
    		
    		if (nz0Pick.isSelected())
    		{
    			try {
					driver.pick((ReferenceNozzle) rh.getNozzles().get(2));
				} catch (Exception e) {

				}
    		}
    		else
    		{
    			try {
					driver.place((ReferenceNozzle) rh.getNozzles().get(2));
				} catch (Exception e) {

				}
    		}
    		
        }
    };
    
    private Action nz3CheckboxAction = new AbstractAction("nz3 pick") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
    		ReferenceMachine rm = (ReferenceMachine) Configuration.get().getMachine();
    		ReferenceHead rh = (ReferenceHead) rm.getHeads().get(0);
    		
    		if (nz0Pick.isSelected())
    		{
    			try {
					driver.pick((ReferenceNozzle) rh.getNozzles().get(3));
				} catch (Exception e) {

				}
    		}
    		else
    		{
    			try {
					driver.place((ReferenceNozzle) rh.getNozzles().get(3));
				} catch (Exception e) {

				}
    		}
    		
        }
    }; 
    
    private Action moveToHomeAction = new AbstractAction("Move to home") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
        	Location loc = driver.getHomeLocation();
        	
        	ReferenceMachine rm = (ReferenceMachine) Configuration.get().getMachine();
        	ReferenceHead rh = (ReferenceHead) rm.getHeads().get(0);
        	
        	try{
        		MovableUtils.moveToLocationAtSafeZ(rh.getDefaultCamera(), loc);
        	}
        	catch (Exception ex){
        		
        	}
        }
    };
    
    
    
   
    @Override
    public void createBindings() {
        IntegerConverter integerConverter = new IntegerConverter();

       
    }
}
