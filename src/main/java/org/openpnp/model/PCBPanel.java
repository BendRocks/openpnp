package org.openpnp.model;

import org.openpnp.model.Length;
import org.simpleframework.xml.Element;


public class PCBPanel extends AbstractModelObject {
	@Element
	private int Columns = 1;
	@Element
	private int Rows = 1;
	
	@Element
	private Length XGap;
	@Element
	private Length YGap;
	
	@Element
	private Location Fid1;
	@Element
	private Location Fid2;	
	
	public PCBPanel() {
        XGap = new Length(0, LengthUnit.Millimeters);
        YGap = new Length(0, LengthUnit.Millimeters);
    }
	
	
	public PCBPanel(int cols, int rows, Length xGap, Length yGap, Location fid1, Location fid2)
	{
		Columns = cols; Rows = rows;
		XGap = xGap; YGap = yGap;
		Fid1 = fid1; Fid2 = fid2;
	}
	
	public int getColumns() {
        return Columns;
    }
	
	public void setColumns(int cols) {
        Object oldValue = this.Columns;
        this.Columns = cols;
        firePropertyChange("PanelGeometry", oldValue, cols);
	}
	
	public int getRows() {
        return Rows;
    }
	
	public void setRows(int rows) {
		 Object oldValue = this.Rows;
	     this.Rows = rows;
	     firePropertyChange("PanelGeometry", oldValue, rows);
	}
	
	public Length getXGap() {
        return XGap;
    }
	
	public void setXGap(Length length) {
		 Object oldValue = this.XGap;
	     this.XGap = length;
	     firePropertyChange("PanelGap", oldValue, length);
	}
	
	public Length getYGap() {
        return YGap;
    }
	
	public void setYGap(Length length) {
		 Object oldValue = this.YGap;
	     this.YGap = length;
	     firePropertyChange("PanelGap", oldValue, length);
	}	
	
	public Location getFid1() {
        return Fid1;
    }
	
	public void setFid1(Location fid) {
		 Object oldValue = this.Fid1;
	     this.Fid1 = fid;
	     firePropertyChange("PanelFid", oldValue, fid);
	}	
	
	public Location getFid2() {
        return Fid2;
    }
	
	public void setFid2(Location fid) {
		 Object oldValue = this.Fid2;
	     this.Fid2 = fid;
	     firePropertyChange("PanelFid", oldValue, fid);
	}	
	
	
}
