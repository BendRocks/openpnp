package org.openpnp.model;

import org.openpnp.model.Length;
import org.simpleframework.xml.Element;


public class PCBPanel extends AbstractModelObject {
	@Element
	private int columns = 1;
	@Element
	private int rows = 1;
	
	@Element
	private Length xGap;
	@Element
	private Length yGap;
	
	@Element
	private Location fid1;
	@Element
	private Location fid2;	
	
	public PCBPanel() {
        xGap = new Length(0, LengthUnit.Millimeters);
        yGap = new Length(0, LengthUnit.Millimeters);
        fid1 = new Location(LengthUnit.Millimeters);
        fid2 = new Location(LengthUnit.Millimeters);
    }
	
	public PCBPanel(int cols, int rows, Length xGap, Length yGap, Location fid1, Location fid2){
		this.columns = cols; 
		this.rows = rows;
		this.xGap = xGap; 
		this.yGap = yGap;
		this.fid1 = fid1; 
		this.fid2 = fid2;
	}
	
	public int getColumns() {
        return columns;
    }
	
	public void setColumns(int cols) {
        //Object oldValue = this.columns;
        this.columns = cols;
        //firePropertyChange("PanelGeometry", oldValue, cols);
	}
	
	public int getRows() {
        return rows;
    }
	
	public void setRows(int rows) {
		 //Object oldValue = this.rows;
	     this.rows = rows;
	     //firePropertyChange("PanelGeometry", oldValue, rows);
	}
	
	public Length getXGap() {
        return xGap;
    }
	
	public void setXGap(Length length) {
		 //Object oldValue = this.xGap;
	     this.xGap = length;
	     //firePropertyChange("PanelGap", oldValue, length);
	}
	
	public Length getYGap() {
        return yGap;
    }
	
	public void setYGap(Length length) {
		 //Object oldValue = this.yGap;
	     this.yGap = length;
	     //firePropertyChange("PanelGap", oldValue, length);
	}	
	
	public Location getFid1() {
        return fid1;
    }
	
	public void setFid1(Location fid) {
		 Object oldValue = this.fid1;
	     this.fid1 = fid;
	     //firePropertyChange("PanelFid", oldValue, fid);
	}	
	
	public Location getFid2() {
        return fid2;
    }
	
	public void setFid2(Location fid) {
		 Object oldValue = this.fid2;
	     this.fid2 = fid;
	     //firePropertyChange("PanelFid", oldValue, fid);
	}	
	
	
}
