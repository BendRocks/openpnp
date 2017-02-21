package org.openpnp.machine.reference.driver;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.pmw.tinylog.Logger;

public class TVM920Control {

	private static class Status {
		static byte[] LastStatusMessage;
		static LocalDateTime LastStatusTime = LocalDateTime.MIN;

		//
		// Called whenever a new status message is received
		//
		public static void UpdateStatusMessage(byte[] newMsg) {
			LastStatusMessage = newMsg;
			LastStatusTime = LocalDateTime.now();

		}

		//
		// Verifies a status message is recent
		//
		static void VerifyStatusGood() {
			if (Duration.between(LastStatusTime, LocalDateTime.now()).toMillis() > 100) {
				throw new IllegalStateException("Status message is stale. Operation cannot be determined.");
			}
		}

		// 
		// check if both X & Y motion have finished
		// 
		static public boolean IsXYStopped() {
			VerifyStatusGood();

			if ((LastStatusMessage[0x33] == 0) && // X Status
					(LastStatusMessage[0x32] == 0)) // Y Status
				return true;

			return false;
		}

		// check if all z motion has finished
		// 
		static public boolean IsZStopped() {
			VerifyStatusGood();

			if ((LastStatusMessage[0x31] == 0) && // NOZ34 status
					(LastStatusMessage[0x30] == 0)) // NOA12 status
				return true;

			return false;
		}

		//
		// Check if all theta (rotation) has stopped
		//
		static public boolean IsThetaStopped() {
			VerifyStatusGood();

			if ((LastStatusMessage[0x2F] == 0) && // Theta4 Status
					(LastStatusMessage[0x2E] == 0) && // Theta3 Status
					(LastStatusMessage[0x2D] == 0) && // Theta2 Status
					(LastStatusMessage[0x2C] == 0)) // Theta1 Status
				return true;

			return false;
		}

		//
		// Verify all motion has stopped
		// 
		static public boolean IsAllMotionStopped() {
			VerifyStatusGood();

			if (IsXYStopped() && IsZStopped() && IsThetaStopped())
				return true;

			return false;
		}

		//
		/// Get the X location in ticks
		//
		public static int GetXTicks() {
			VerifyStatusGood();

			// Preserve the sign using arithmetic shift
			int ticks = (LastStatusMessage[0x2b] << 24) + (LastStatusMessage[0x2a] << 16)
					+ (LastStatusMessage[0x29] << 8);
			ticks = ticks >> 8;

			return ticks;
		}

		//
		/// Get the Y location in ticks
		//
		public static int GetYTicks() {
			VerifyStatusGood();

			// Preserve the sign using arithmetic shift
			int ticks = (LastStatusMessage[0x27] << 24) + (LastStatusMessage[0x26] << 16)
					+ (LastStatusMessage[0x25] << 8);
			ticks = ticks >> 8;

			return ticks;
		}

		//
		/// Get the Z location in ticks
		//
		public static int GetZ12Ticks() {
			VerifyStatusGood();

			// Preserve the sign using arithmetic shift
			int ticks = (LastStatusMessage[0x17] << 24) + (LastStatusMessage[0x16] << 16)
					+ (LastStatusMessage[0x15] << 8);
			ticks = ticks >> 8;

			return ticks;
		}

		//
		/// Get the Z location in ticks
		//		
		public static int GetZ34Ticks() {
			VerifyStatusGood();

			// Preserve the sign using arithmetic shift
			int ticks = (LastStatusMessage[0x1B] << 24) + (LastStatusMessage[0x1A] << 16)
					+ (LastStatusMessage[0x19] << 8);
			ticks = ticks >> 8;

			return ticks;
		}

		//
		// Indicates if Z12 is home
		//
		public static boolean IsZ12Home() {
			if ((LastStatusMessage[8] & 0x20) > 0)
				return true;
			else
				return false;
		}

		//
		// Indicates if Z34 is home
		//
		public static boolean IsZ34Home() {
			if ((LastStatusMessage[8] & 0x40) > 0)
				return true;
			else
				return false;
		}

	} // End of static class to help with status
	
	

	DatagramSocket Socket;
	private Lock NetLock = new ReentrantLock();
	private volatile boolean TerminateHeartbeatThread;
	private int HeartbeatInterval = 25;
	boolean IsHomed = false;

	// These are measured values and will ultimately need a calibration
	double TicksPerMM_X = 327.55;
	double TicksPerMM_Y = 204.85;
	double TicksPerMM_Z = 262.37;

	// This is the max allowed. This applies AFTER we've been homed
	double MAX_X = 470;
	double MAX_Y = 449;
	double MIN_Z = -17;

	//
	// Constructor
	//
	public TVM920Control() throws Exception {
		Socket = new DatagramSocket(8701);
		Socket.setSoTimeout(100);
	}

	//
	// Sleep util used by all functions so we don't need the 
	// Java try/catch around basic operations
	//
	private void Sleep(int mSeconds) {
		try {
			Thread.sleep(mSeconds);
		} catch (InterruptedException ie) {

		}
	}

	//
	// Send and receieve data. Note we do NOT want the main thread and the heartbeat thread to 
	// be pre-empting each other inside here, so a lock is used to ensure the send/receive is 
	// atomic. There's also a Sleep to ensure if a thread is pending on the lock, there will be
	// at few mS at least between a message coming in and one going back out so that the 
	// TVM920 control board has time to respond
	//
	private byte[] SendReceiveUDP(byte[] data) {
		byte[] rxData;

		try {
			NetLock.lock();

			SendUDP(data);
			rxData = ReceiveUDP();

			Sleep(3);
		} finally {
			NetLock.unlock();
		}

		Sleep(2);

		return rxData;
	}

	//
	// Not for general use in this class
	//
	private void SendUDP(byte[] data) {
		try {
			DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("192.168.0.8"), 8701);
			Socket.send(packet);
		} catch (Exception e) {
			Logger.debug("TVM920: Send UDP exception: " + e.toString());
		}
	}

	//
	// Not for general use in this class. TODO: Some cleanup on return value when
	// nothing has been received. Currently, a big empty array is returned which 
	// will appear as a status with everything zero'd. This isn't ideal. 
	//
	private byte[] ReceiveUDP() {
		try {
			byte[] buffer = new byte[1024];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

			byte[] data = packet.getData();

			if (packet.getData()[0] == 0) {
				Status.UpdateStatusMessage((byte[]) data.clone());
			}

			return packet.getData();
		} catch (Exception e) {
			Logger.debug("TVM920: Recv UDP exception: " + e.toString());

		}

		// Nothing was received. Return empty array
		return new byte[512];
	}

	//
	// Starts the heartbeat thread. 
	//
	public void StartHeartbeat() {
		TerminateHeartbeatThread = false;
		try {
		    new Thread(){
		    	public void run(){
		    		Heartbeat();
		    	}
		    }.start();
		} catch (Exception ex) {
			Logger.debug("TVM920: Heartbeat failed to start. " + ex.toString());
		}
	}

	//
	// Stops the heartbeat thread. 
	//
	public void StopHeartbeat() {
		TerminateHeartbeatThread = true;
	}

	//
	// Started from above. Runs every 15 mS or so. While this thread is running, the 
	// green button on the TVM920 will glow solid. When this thread is NOT running, 
	// the green button will flash
	//
	public void Heartbeat() {
		int count = 0;
		// This thread probably eats 1-2 mS every HeartbeatInterval. It should run at
		// an elevated priority. TODO: Study if HeartbeatInterval can be pushed to 50
		// mS or so. From memory it seems like it could
		Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 1);
		while (TerminateHeartbeatThread == false) {
			try {
				//GetStatus();
				Sleep(HeartbeatInterval);
				++count;
				
				if (count % 200 == 0)
					Logger.debug("TVM920: Heartbeat running.");
				
			} catch (Exception e) {
				// Likely no connection. Wait a while and try again
				Sleep(100);
			}
		}

		TerminateHeartbeatThread = false;
		Logger.debug("TVM920: Hearbeat thread terminated.");
	}

	//
	// Asks for status, and also handles the processing of status. The roundtrip time on this is a mS 
	// or two under windows
	//
	void GetStatus() {
		SendReceiveUDP(new byte[] { 0, 0, 0, 0 });
	}

	//
	// Opens specified feeder
	//
	public void FeederOpen(int feederNumber) {
		byte[] data = new byte[12];

		data[0] = 0x5;

		int byteIndex = 10 - feederNumber / 8;
		byte bit = (byte) (0x1 << (feederNumber % 8));

		data[byteIndex] = bit;

		SendReceiveUDP(data);
	}

	//
	// Closes all feeders
	//
	public void FeederClose() {
		byte[] data = new byte[12];

		data[0] = 0x5;

		SendReceiveUDP(data);
	}

	//
	// Open valve to allow suction/pick
	//
	public void PickOpen(int pickNumber) {
		if (pickNumber > 3)
			throw new IllegalArgumentException("Bad pick index");

		byte[] data = new byte[8];

		data[0] = 0x16; // GPIO Set

		data[4] = (byte) (0x1 << pickNumber);

		SendReceiveUDP(data);
	}

	//
	// Close valve to release pick
	// 
	public void PickClose(int pickNumber) {
		if (pickNumber > 3)
			throw new IllegalArgumentException("Bad pick index");

		byte[] data = new byte[8];

		data[0] = 0x17; // GPIO Clear

		data[4] = (byte) (0x1 << pickNumber);

		SendReceiveUDP(data);
	}

	// 
	// closes all picks
	//
	public void PickCloseAll() {
		byte[] data = new byte[8];

		data[0] = 0x17; // GPIO Clear

		data[4] = 0x0F;

		SendReceiveUDP(data);
	}

	// 
	// Retract all Z to 0. Useful before a move
	//
	void RetractZ() {
		byte[] moveCmd = new byte[36];
		moveCmd[0] = 0x0D;

		moveCmd[2] = 0x30;

		SendReceiveUDP(moveCmd);
		GetStatus();

		while (Status.IsZStopped() == false) {
			Sleep(50);
			GetStatus();
		}
	}

	//
	// Move Z relative to current location
	//
	public void MoveZRel(int head, double z, double speed) {
		double absZ;

		if (head == 0)
			absZ = z + GetZ12PosMM();
		else if (head == 1)
			absZ = z + GetZ12PosMM();
		else if (head == 2)
			absZ = z + GetZ34PosMM();
		else if (head == 3)
			absZ = z + GetZ34PosMM();
		else
			throw new IllegalArgumentException("Invalid head number in MoveZRel");

		MoveZAbs(head, absZ, speed);
	}

	//
	// Move Z absolute
	//
	public void MoveZAbs(int head, double z, double speed) {
		byte[] moveCmd = new byte[36];
		moveCmd[0] = 0x0D;

		if (IsHomed) {
			if (z < MIN_Z)
				z = MIN_Z;

			if (z > 0)
				z = 0;
		}

		SetSpeed(speed);

		MotionEnable();

		if (head == 0 || head == 1)
			moveCmd[2] = 0x10;
		else if (head == 2 || head == 3)
			moveCmd[2] = 0x20;
		else
			throw new IllegalArgumentException("Head must be 0..3 inclusive");

		// Compute move
		int zInt = (int) Math.round(z * TicksPerMM_Z);

		if (head == 0) {
			moveCmd[0x15] = (byte) (zInt >> 0);
			moveCmd[0x16] = (byte) (zInt >> 8);
			moveCmd[0x17] = (byte) (zInt >> 16);
		} else if (head == 1) {
			zInt = -zInt;
			moveCmd[0x15] = (byte) (zInt >> 0);
			moveCmd[0x16] = (byte) (zInt >> 8);
			moveCmd[0x17] = (byte) (zInt >> 16);
		} else if (head == 2) {
			moveCmd[0x19] = (byte) (zInt >> 0);
			moveCmd[0x1A] = (byte) (zInt >> 8);
			moveCmd[0x1B] = (byte) (zInt >> 16);
		} else if (head == 3) {
			zInt = -zInt;
			moveCmd[0x19] = (byte) (zInt >> 0);
			moveCmd[0x1A] = (byte) (zInt >> 8);
			moveCmd[0x1B] = (byte) (zInt >> 16);
		}

		SendReceiveUDP(moveCmd);
		GetStatus();

		while (Status.IsZStopped() == false) {
			Sleep(HeartbeatInterval);
			GetStatus();
		}

		MotionDisable();
	}

	//
	// Move XY relative to current location
	// 
	public void MoveXYRel(double x, double y, double speed) {
		GetStatus();

		double absX = x + GetXPosMM();
		double absY = y + GetYPosMM();

		MoveXYAbs(absX, absY, speed);
	}

	//
	// MOve XY absolute
	//
	void MoveXYAbs(double x, double y, double speed) {
		if (IsHomed) {
			if (x > MAX_X)
				x = MAX_X;

			if (x < 0)
				x = 0;

			if (y > MAX_Y)
				y = MAX_Y;

			if (y < 0)
				y = 0;
		}

		// Query front panel lock button? Not sure if needed
		SendReceiveUDP(new byte[] { 1, 0, 0, 0, (byte) 0xf4, 0x10, 0, 0, 1, 0, 0, 0 });

		SetSpeed(speed);

		MotionEnable();
		RetractZ();

		byte[] moveCmd = new byte[36];
		moveCmd[0] = 0x0D;

		moveCmd[2] = (byte) 0xC0;
		int xInt = (int) Math.round(x * TicksPerMM_X);
		int yInt = (int) Math.round(y * TicksPerMM_Y);

		moveCmd[0x1D] = (byte) (yInt >> 0);
		moveCmd[0x1E] = (byte) (yInt >> 8);
		moveCmd[0x1F] = (byte) (yInt >> 16);

		moveCmd[0x21] = (byte) (xInt >> 0);
		moveCmd[0x22] = (byte) (xInt >> 8);
		moveCmd[0x23] = (byte) (xInt >> 16);

		SendReceiveUDP(moveCmd);

		GetStatus();

		while (Status.IsXYStopped() == false) {
			Sleep(HeartbeatInterval);
			GetStatus();
		}

		MotionDisable();
	}

	//
	// Disable safety lockout
	//
	void MotionEnable() {
		SendReceiveUDP(new byte[] { 0xc, 0, 1, 0 });
	}

	//
	// Enabled safety lockout
	//
	void MotionDisable() {
		SendReceiveUDP(new byte[] { 0xc, 0, 0, 0 });
	}

	//
	// Gets the position from the last status message and converts to MM
	//
	public double GetXPosMM() {
		GetStatus();
		return Status.GetXTicks() / TicksPerMM_X;
	}

	//
	// Gets the position from the last status message and converts to MM
	//
	public double GetYPosMM() {
		GetStatus();
		return Status.GetYTicks() / TicksPerMM_Y;
	}


	//
	// Gets the position from the last status message and converts to MM
	//
	public double GetZ12PosMM() {
		GetStatus();
		return Status.GetZ12Ticks() / TicksPerMM_Z;
	}

	//
	// Gets the position from the last status message and converts to MM
	//
	public double GetZ34PosMM() {
		GetStatus();
		return Status.GetZ34Ticks() / TicksPerMM_Z;
	}

	//
	// Sets the XY position
	//
	public void SetXYPosMM(double xPosMM, double yPosMM) {
		int xInt = (int) Math.round(xPosMM * TicksPerMM_X);
		int yInt = (int) Math.round(yPosMM * TicksPerMM_Y);
		SendReceiveUDP(new byte[] { 8, 0, (byte) 0xC0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, (byte) (yInt >> 0), (byte) (yInt >> 8), (byte) (yInt >> 16), 0, (byte) (xInt >> 0),
				(byte) (xInt >> 8), (byte) (xInt >> 16) });
	}

	//
	// Sets all Z positions to 0
	//
	public void SetZ1234PosZero() {
		SendReceiveUDP(new byte[] { 8, 0, 0x30, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0 });
	}

	//
	// Turns on down light
	//
	public void DownLightOn(boolean turnOn) {
		if (turnOn)
			SendReceiveUDP(new byte[] { 0x16, 0x00, 0x00, 0x00, (byte) 0x80, 0x00, 0x00, 0x00 });
		else
			SendReceiveUDP(new byte[] { 0x17, 0x00, 0x00, 0x00, (byte) 0x80, 0x00, 0x00, 0x00 });
	}

	//
	// Turns on up light
	//
	public void UpLightOn(boolean turnOn) {
		if (turnOn)
			SendReceiveUDP(new byte[] { 0x16, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00 });
		else
			SendReceiveUDP(new byte[] { 0x17, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00 });
	}

	//
	// Find Y home. TODO: Not sure the endstop code is needed? The endstop seems to supress movement
	// that would move you FURTHER onto the endstop, but readily allows movement that would move you
	// off of the endstop.
	//
	void FindYHome() {
		EndStopEnableAll(true);

		MoveXYRel(0, 1000, 0.2);

		EndStopEnableY(false);

		MoveXYRel(0, -5, 0.2);

		EndStopEnableAll(true);
	}

	void FindXHome() {
		EndStopEnableAll(true);

		MoveXYRel(1000, 0, 0.2);

		EndStopEnableX(false);

		MoveXYRel(-5, 0, 0.2);

		EndStopEnableAll(true);
	}

	void FindZHome() {
		// Verify we're on the stop
		GetStatus();

		// Walk off home
		while (Status.IsZ12Home() == true) {
			MoveZRel(0, -5, 0.2);
			Sleep(100);
			GetStatus();
		}

		// Walk back on home
		while (Status.IsZ12Home() == false) {
			MoveZRel(0, 0.5, 0.2);
			Sleep(100);
			GetStatus();
		}

		// Walk off home
		while (Status.IsZ34Home() == true) {
			MoveZRel(3, -5, 0.2);
			Sleep(100);
			GetStatus();
		}

		// Walk back on home
		while (Status.IsZ34Home() == false) {
			MoveZRel(3, 0.5, 0.2);
			Sleep(100);
			GetStatus();
		}

		SetZ1234PosZero();
	}

	public void FindXYHome() {
		FindZHome();
		FindYHome();
		FindXHome();
		SetXYPosMM(465, 444);
		IsHomed = true;
	}

	public void FindHome() {
		FindXYHome();
	}

	public boolean checkEnabled() {
		try{
			Status.VerifyStatusGood();
			return true;
		}
		catch (Exception ex)
		{
			
		}
		
		return false;
	}

	//
	// Sets speeds for various operations. If we have not been homed, then speed is locked at 30%. TODO: These
	// tables need to be fleshed out for other speed settings
	//
	void SetSpeed(double speed) {
		if (speed < 0 || speed > 1.0)
			throw new IllegalArgumentException("Speed must be between 0.0 and 1.0 inclusive");

		// Speed options
		byte[] Pct30 = { 7, 0, 2, 0, 4, 0, 0, 0, 4, 0, 0, 0, 4, 0, 0, 0, 4, 0, 0, 0, 4, 0, 0, 0, 4, 0, 0, 0, 4, 0, 0, 0,
				4, 0, 0, 0, (byte) 0xd0, 7, 0, 0, (byte) 0xd7, 7, 0, 0, (byte) 0xd0, 7, 0, 0, 0xd, 7, 0, 0, (byte) 0xe8,
				3, 0, 0, (byte) 0xe8, 3, 00, (byte) 0xc4, 9, 0, 0, (byte) 0xc4, 9, 0, 0 };

		byte[] Pct50 = { 7, 0, 4, 0, 4, 0, 0, 0, 4, 0, 0, 0, 4, 0, 0, 0, 4, 0, 0, 0, 5, 0, 0, 0, 5, 0, 0, 0, 4, 0, 0, 0,
				4, 0, 0, 0, 0x70, 0x17, 0, 0, 0x70, 0x17, 0, 0, 0x70, 0x17, 0, 0, 0x70, 0x17, 0, 0, (byte) 0xb8, 0xb, 0,
				0, (byte) 0xb8, 0xb, 0, 0, 0x10, 0x27, 0, 0, 0x10, 0x27, 0, 0 };

		byte[] Pct80 = { 7, 0, 9, 0, 5, 0, 0, 0, 5, 0, 0, 0, 5, 0, 0, 0, 5, 0, 0, 0, 0x0c, 0, 0, 0, 0x0c, 0, 0, 0, 7, 0,
				0, 0, 7, 0, 0, 0, (byte) 0xe0, 0x2e, 0, 0, (byte) 0xe0, 0x2e, 0, 0, (byte) 0xe0, 0x2e, 0, 0,
				(byte) 0xe0, 0x2e, 0, 0, (byte) 0xc8, 0x32, 0, 0, (byte) 0xc8, 0x32, 0, 0, (byte) 0x98, (byte) 0xb7, 0,
				0, (byte) 0xb0, (byte) 0xb3, 0, 0 };

		if (IsHomed == false)
			speed = 0.2;

		if (speed >= 0.9) {
			SendReceiveUDP(Pct80);
		} else if (speed >= 0.8 && speed < 0.9) {
			SendReceiveUDP(Pct80);
		} else if (speed >= 0.7 && speed < 0.8) {
			SendReceiveUDP(Pct50);
		} else if (speed >= 0.6 && speed < 0.7) {
			SendReceiveUDP(Pct50);
		} else if (speed >= 0.5 && speed < 0.6) {
			SendReceiveUDP(Pct50);
		} else if (speed >= 0.4 && speed < 0.5) {
			SendReceiveUDP(Pct30);
		} else if (speed < 0.4) {
			SendReceiveUDP(Pct30);
		}
	}

	public int ReadRegister(int register) {
		byte[] data = SendReceiveUDP(
				new byte[] { 01, 00, 00, 00, (byte) (register), (byte) (register >> 8), 00, 00, 00, 00, 00, 00 });
		return (data[11] << 24) + (data[10] << 16) + (data[9] << 8) + (data[8] << 0);
	}

	public void WriteRegister(int register, int data) {
		SendReceiveUDP(new byte[] { 01, 00, 00, 00, (byte) (register), (byte) (register >> 8), 00, 00,
				(byte) (data >> 0), (byte) (data >> 8), (byte) (data >> 16), (byte) (data >> 24) });
	}

	void EndStopEnableX(boolean enable) {
		if (enable)
			WriteRegister(0x1f6, 0);
		else
			WriteRegister(0x1f6, 0x01000000);
	}

	void EndStopEnableY(boolean enable) {
		if (enable)
			WriteRegister(0x1f5, 0);
		else
			WriteRegister(0x1f5, 0x01000000);
	}

	void EndStopEnableZ(boolean enable) {
		if (enable)
			WriteRegister(0x1f4, 0);
		else
			WriteRegister(0x1f4, 0x01000000);
	}

	void EndStopEnableAll(boolean enable) {
		EndStopEnableX(enable);
		EndStopEnableY(enable);
		EndStopEnableZ(enable);
	}

	//
	// Initializes machine. Heartbeat should not yet be active when this is
	//
	public void Init() {
		int sleepTime = 10;

		try {
			SendReceiveUDP(new byte[] { 0, 0, 0, 0 });
			Sleep(sleepTime);

			SendReceiveUDP(new byte[] { 0x17, 00, 00, 00, 0x10, 00, 00, 00 });
			Sleep(sleepTime);

			SendReceiveUDP(new byte[] { 0x02, 00, 00, 00, (byte) 0xf5, 0x01, 00, 00, 00, 00, 00, 00 });
			Sleep(sleepTime);

			SendReceiveUDP(new byte[] { 0x02, 00, 00, 00, (byte) 0xf6, 0x01, 00, 00, 00, 00, 00, 00 });
			Sleep(sleepTime);

			SendReceiveUDP(new byte[] { 0x07, 0x00, 0x04, 00, 0x04, 00, 00, 00, 0x04, 00, 00, 00, 0x04, 00, 00, 00,
					0x04, 00, 00, 00, 0x05, 00, 00, 00, 0x05, 00, 00, 00, 0x04, 00, 00, 00, 0x04, 00, 00, 00, 0x70,
					0x17, 00, 00, 0x70, 0x17, 00, 00, 0x70, 0x17, 00, 00, 0x70, 0x17, 00, 00, (byte) 0xb8, 0x0b, 00, 00,
					(byte) 0xb8, 0x0b, 00, 00, 0x10, 0x27, 00, 00, 0x10, 0x27, 00, 00 });
			Sleep(sleepTime);

			byte[] data = new byte[] { 01, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00 };

			for (int i = 0; i < 48; i++) {
				data[5] = (byte) i;
				SendReceiveUDP(data);
				Sleep(sleepTime);
			}

			Sleep(500);

			SendReceiveUDP(new byte[] { 0x0c, 00, 00, 00 });
			Sleep(sleepTime);

			SendReceiveUDP(new byte[] { 0x17, 00, 00, 00, 0x30, 00, 00, 00 });
			Sleep(sleepTime);

			SendReceiveUDP(new byte[] { 02, 00, 00, 00, (byte) 0xf5, 01, 00, 00, 00, 00, 00, 00 });
			Sleep(sleepTime);

			SendReceiveUDP(new byte[] { 02, 00, 00, 00, (byte) 0xf6, 01, 00, 00, 00, 00, 00, 00 });
			Sleep(sleepTime);

			SendReceiveUDP(new byte[] { 0x16, 00, 00, 00, 00, 01, 00, 00 });
			Sleep(sleepTime);

			SendReceiveUDP(new byte[] { 0x17, 00, 00, 00, 80, 00, 00, 00 });
			Sleep(sleepTime);
		} catch (Exception ex) {

		}
	}

}
