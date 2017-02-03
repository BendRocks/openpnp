/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference.camera;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeSupport;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.filechooser.FileSystemView;

import org.openpnp.CameraListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.camera.wizards.ImageCameraConfigurationWizard;
import org.openpnp.machine.reference.camera.wizards.TVM920ConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

public class TVM920Camera extends ReferenceCamera implements Runnable {
	
	// Connect to external DLL
	static public interface DLLLib extends Library {
		
        DLLLib INSTANCE = (DLLLib)Native.loadLibrary("TVM920Vision", DLLLib.class);

        void Stop();
        int Add(int x, int y);
        void SetInput(int input);
        void GetData(byte[] buffer, IntByReference byteCount);  
        Pointer GetPng(IntByReference byteCount);  
        
        static BufferedImage GetBitmapFromData(byte[] data)
        {
        	int width = (((int)data[0] & 0xFF) << 8) + ((int)data[1] & 0xFF);
        	int height = (((int)data[2] & 0xFF) << 8) + ((int)data[3] & 0xFF);
        	
        	BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        	byte[] imagedat = ((java.awt.image.DataBufferByte)bi.getRaster().getDataBuffer()).getData();
        	System.arraycopy(data, 4, imagedat, 0 , data.length-4);

        	return bi;  
        }
    }
	
	
	
    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    @Attribute(required = false)
    private int fps = 24;

    @Element
    private String sourceUri = "classpath://samples/pnp-test/pnp-test.png";

    @Attribute(required = false)
    private int width = 640;

    @Attribute(required = false)
    private int height = 480;
    
    private byte[] TVMFrameBuffer = new byte[720*576*4+4];

    private BufferedImage source;

    private Thread thread;
   

    public TVM920Camera() {    	
    	setUnitsPerPixel(new Location(LengthUnit.Millimeters, 0.04233, 0.04233, 0, 0));
        try {
        	//sourceUri = System.getProperty("user.home") + "\\LastImage.png";
            //setSourceUri(sourceUri);
        }
        catch (Exception e) {
            
        }
    }

    @SuppressWarnings("unused")
    @Commit
    private void commit() throws Exception {
        setSourceUri(sourceUri);
    }

    @Override
    public synchronized void startContinuousCapture(CameraListener listener, int maximumFps) {
        start();
        super.startContinuousCapture(listener, maximumFps);
    }

    @Override
    public synchronized void stopContinuousCapture(CameraListener listener) {
        super.stopContinuousCapture(listener);
        if (listeners.size() == 0) {
            stop();
        }
    }

    private synchronized void stop() {
    	//DLLLib.INSTANCE.Stop();
        if (thread != null && thread.isAlive()) { 
            thread.interrupt();
            try {
                thread.join();
            }
            catch (Exception e) {

            }
            thread = null;
        }
    }

    private synchronized void start() {
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public void setSourceUri(String sourceUri) throws Exception {
        String oldValue = this.sourceUri;
        this.sourceUri = sourceUri;
        pcs.firePropertyChange("sourceUri", oldValue, sourceUri);
        initialize();
    }

    @Override
    public synchronized BufferedImage internalCapture() {        
        try
        {
	        	IntByReference iref = new IntByReference();
	        	DLLLib.INSTANCE.GetData(TVMFrameBuffer, iref);
	        	source = DLLLib.GetBitmapFromData(TVMFrameBuffer);
        }
        catch (Exception e){
        	source = null;
    	}
      
        return transformImage(source);
    }

    private synchronized void initialize() throws Exception {
        stop();
        
        /*

        if (sourceUri.startsWith("classpath://")) {
            source = ImageIO.read(getClass().getClassLoader()
                    .getResourceAsStream(sourceUri.substring("classpath://".length())));
        }
        else {
            source = ImageIO.read(new URL(sourceUri));
        }*/

        if (listeners.size() > 0) {
            start();
        }
    }


    public void run() {
        while (!Thread.interrupted()) {
            BufferedImage frame = internalCapture();
            broadcastCapture(frame);
            try {
                Thread.sleep(1000 / fps);
            }
            catch (InterruptedException e) {
                return;
            }
        }
        
        DLLLib.INSTANCE.Stop();
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new TVM920ConfigurationWizard(this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        // TODO Auto-generated method stub
        return null;
    }
}

