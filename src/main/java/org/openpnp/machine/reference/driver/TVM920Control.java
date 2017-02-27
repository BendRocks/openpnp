package org.openpnp.machine.reference.driver;

import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.pmw.tinylog.Logger;

public class TVM920Control {

	private static class Status {
		static byte[] lastStatusMessage = new byte[127];
		// static LocalDateTime LastStatusTime = LocalDateTime.MIN;

		static String dumpString(byte[] data) {
			StringBuilder sb = new StringBuilder(1024);
			for (int i = 0; i < data.length; i++) {

				if ((i % 16) == 0)
					sb.append(String.format("\n%02X  ", i / 16));

				if ((i % 8 == 0) && (i % 16 != 0) && (i != 0))
					sb.append("  ");

				sb.append(String.format("%02x", data[i]));
				sb.append(' ');
			}

			return sb.toString();
		}

		static boolean arraysAreSame(byte[] array1, byte[] array2) {
			if (array1 == null || array2 == null)
				return true;

			if (array1.length != array2.length)
				return false;

			for (int i = 0; i < array1.length; i++) {
				if (array1[i] != array2[i])
					return false;
			}

			return true;

		}

		//
		// Called whenever a new status message is received
		//
		public static void updateStatusMessage(byte[] newMsg) {

			if (arraysAreSame(newMsg, lastStatusMessage) == false) {
				Logger.debug("TVM920 Status Change: \n" + dumpString(newMsg));
			}

			lastStatusMessage = newMsg;
			// LastStatusTime = LocalDateTime.now();
		}

		//
		// Verifies a status message is recent
		//
		static void verifyStatusGood() {
			/*
			 * if (Duration.between(LastStatusTime,
			 * LocalDateTime.now()).toMillis() > 100) { throw new
			 * IllegalStateException("Status message is stale. Operation cannot be determined."
			 * ); }
			 */
		}

		//
		// check if both X & Y motion have finished
		//
		static public boolean isXYStopped() {
			verifyStatusGood();

			if ((lastStatusMessage[0x33] == 0) && // X Status
					(lastStatusMessage[0x32] == 0)) // Y Status
				return true;

			return false;
		}

		// check if all z motion has finished
		//
		static public boolean isZStopped() {
			verifyStatusGood();

			if ((lastStatusMessage[0x31] == 0) && // Z34 status
					(lastStatusMessage[0x30] == 0)) // Z12 status
				return true;

			return false;
		}

		//
		// Check if all theta (rotation) has stopped
		//
		static public boolean isThetaStopped() {
			verifyStatusGood();

			if ((lastStatusMessage[0x2F] == 0) && // Theta4 Status
					(lastStatusMessage[0x2E] == 0) && // Theta3 Status
					(lastStatusMessage[0x2D] == 0) && // Theta2 Status
					(lastStatusMessage[0x2C] == 0)) // Theta1 Status
				return true;

			return false;
		}

		//
		// Verify all motion has stopped
		//
		static public boolean isAllMotionStopped() {
			verifyStatusGood();

			if (isXYStopped() && isZStopped() && isThetaStopped())
				return true;

			return false;
		}

		public static int GetTheta0Ticks() {
			verifyStatusGood();

			// Preserve the sign using arithmetic shift. Note the "& 0xFF" is
			// important as it converts the value to an unsigned int
			int ticks = ((lastStatusMessage[0x7] & 0xFF) << 24) + ((lastStatusMessage[0x6] & 0xFF) << 16)
					+ ((lastStatusMessage[0x5] & 0xFF) << 8);
			ticks = ticks >> 8;

			Logger.debug(String.format("GetTheta0Ticks() returned %d [%x]", ticks, ticks));

			return ticks;
		}

		public static int GetTheta1Ticks() {
			verifyStatusGood();

			// Preserve the sign using arithmetic shift. Note the "& 0xFF" is
			// important as it converts the value to an unsigned int
			int ticks = ((lastStatusMessage[0xb] & 0xFF) << 24) + ((lastStatusMessage[0xa] & 0xFF) << 16)
					+ ((lastStatusMessage[0x9] & 0xFF) << 8);
			ticks = ticks >> 8;

			Logger.debug(String.format("GetTheta1Ticks() returned %d [%x]", ticks, ticks));

			return ticks;
		}

		public static int GetTheta2Ticks() {
			verifyStatusGood();

			// Preserve the sign using arithmetic shift. Note the "& 0xFF" is
			// important as it converts the value to an unsigned int
			int ticks = ((lastStatusMessage[0xf] & 0xFF) << 24) + ((lastStatusMessage[0xe] & 0xFF) << 16)
					+ ((lastStatusMessage[0xd] & 0xFF) << 8);
			ticks = ticks >> 8;

			Logger.debug(String.format("GetTheta2Ticks() returned %d [%x]", ticks, ticks));

			return ticks;
		}

		public static int GetTheta3Ticks() {
			verifyStatusGood();

			// Preserve the sign using arithmetic shift. Note the "& 0xFF" is
			// important as it converts the value to an unsigned int
			int ticks = ((lastStatusMessage[0x13] & 0xFF) << 24) + ((lastStatusMessage[0x12] & 0xFF) << 16)
					+ ((lastStatusMessage[0x11] & 0xFF) << 8);
			ticks = ticks >> 8;

			Logger.debug(String.format("GetTheta3Ticks() returned %d [%x]", ticks, ticks));

			return ticks;
		}

		//
		/// Get the X location in ticks
		//
		public static int getXTicks() {
			verifyStatusGood();

			// Preserve the sign using arithmetic shift. Note the "& 0xFF" is
			// important as it converts the value to an unsigned int
			int ticks = ((lastStatusMessage[0x2b] & 0xFF) << 24) + ((lastStatusMessage[0x2a] & 0xFF) << 16)
					+ ((lastStatusMessage[0x29] & 0xFF) << 8);
			ticks = ticks >> 8;

			Logger.debug(String.format("GetXTicks() returned %d [%x]", ticks, ticks));

			return ticks;
		}

		//
		/// Get the Y location in ticks
		//
		public static int getYTicks() {
			verifyStatusGood();

			// Preserve the sign using arithmetic shift. Note the "& 0xFF" is
			// important as it converts the value to an unsigned int
			int ticks = ((lastStatusMessage[0x27] & 0xFF) << 24) + ((lastStatusMessage[0x26] & 0xFF) << 16)
					+ ((lastStatusMessage[0x25] & 0xFF) << 8);
			ticks = ticks >> 8;

			return ticks;
		}

		//
		/// Get the Z location in ticks
		//
		public static int getZ01Ticks() {
			verifyStatusGood();

			// Preserve the sign using arithmetic shift. Note the "& 0xFF" is
			// important as it converts the value to an unsigned int
			int ticks = ((lastStatusMessage[0x1F] & 0xFF) << 24) + ((lastStatusMessage[0x1E] & 0xFF) << 16)
					+ ((lastStatusMessage[0x1D] & 0xFF) << 8);
			ticks = ticks >> 8;

			return ticks;
		}

		//
		/// Get the Z location in ticks
		//
		public static int getZ23Ticks() {
			verifyStatusGood();

			// Preserve the sign using arithmetic shift. Note the "& 0xFF" is
			// important as it converts the value to a signed int
			int ticks = ((lastStatusMessage[0x23] & 0xFF) << 24) + ((lastStatusMessage[0x22] & 0xFF) << 16)
					+ ((lastStatusMessage[0x21] & 0xFF) << 8);
			ticks = ticks >> 8;

			return ticks;
		}

		//
		// Indicates if Z12 is home
		//
		public static boolean isZ01Home() {
			if ((lastStatusMessage[8] & 0x20) > 0)
				return true;
			else
				return false;
		}

		//
		// Indicates if Z34 is home
		//
		public static boolean isZ23Home() {
			if ((lastStatusMessage[8] & 0x40) > 0)
				return true;
			else
				return false;
		}

	} // End of static class to help with status

	DatagramSocket Socket;
	private Lock NetLock = new ReentrantLock();
	private volatile boolean TerminateHeartbeatThread;
	private int HeartbeatInterval = 50;
	private boolean IsHomed = false;

	// These are measured values and will ultimately need a calibration
	private double TicksPerMM_X = 327.55;
	private double TicksPerMM_Y = 204.85;
	private double TicksPerMM_Z = 262.37;
	private double TicksPerDegree = 17.77; // 0x1900 -> 6400 moves 360 degrees

	// This is the max allowed. This applies AFTER we've been homed
	private double MAX_X = 470;
	private double MAX_Y = 449;
	private double MIN_Z = -17;

	private boolean emulateHardware = true;
	private double emulatedX, emulatedY;

	//
	// Constructor
	//
	public TVM920Control() throws Exception {
		Socket = new DatagramSocket(8701);
		Socket.setSoTimeout(100);
		log("=====================================");
		log("=========EMULATED HARDWARE===========");
		log("=====================================");
	}

	//
	// Sleep util used by all functions so we don't need the
	// Java try/catch around basic operations
	//
	private void sleep(int mSeconds) {
		try {
			Thread.sleep(mSeconds);
		} catch (InterruptedException ie) {

		}
	}

	private void log(String s) {
		Logger.debug(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")) + " " + s);
	}

	//
	// Send and receieve data. Note we do NOT want the main thread and the
	// heartbeat thread to
	// be pre-empting each other inside here, so a lock is used to ensure the
	// send/receive is
	// atomic. There's also a Sleep to ensure if a thread is pending on the
	// lock, there will be
	// at few mS at least between a message coming in and one going back out so
	// that the
	// TVM920 control board has time to respond
	//
	private byte[] sendReceiveUDP(byte[] data) {
		byte[] rxData;

		try {
			NetLock.lock();

			sendUDP(data);
			rxData = receiveUDP();

			sleep(3);
		} finally {
			NetLock.unlock();
		}

		sleep(2);

		return rxData;
	}

	//
	// Not for general use in this class
	//
	private void sendUDP(byte[] data) {
		try {
			DatagramPacket txPacket = new DatagramPacket(data, data.length, InetAddress.getByName("192.168.0.8"), 8701);
			Socket.send(txPacket);
		} catch (Exception e) {
			Logger.debug("TVM920: Send UDP exception: " + e.toString());
		}
	}

	//
	// Not for general use in this class. TODO: Some cleanup on return value
	// when
	// nothing has been received. Currently, a big empty array is returned which
	// will appear as a status with everything zero'd. This isn't ideal.
	//
	private byte[] receiveUDP() {

		if (emulateHardware) {
			sleep(5);
			return new byte[128];
		}

		try {
			byte[] buffer = new byte[1024];
			DatagramPacket rxPacket = new DatagramPacket(buffer, buffer.length);
			Socket.receive(rxPacket);
			// byte[] rxData = rxPacket.getData();
			byte[] rxData = Arrays.copyOf(rxPacket.getData(), rxPacket.getLength());

			if (rxData[0] == 0) {
				Status.updateStatusMessage((byte[]) rxData.clone());
			}

			return rxData;
		} catch (Exception e) {
			Logger.debug("TVM920: Recv UDP exception: " + e.toString());

		}

		// Nothing was received. Return null
		return null;
	}

	//
	// Starts the heartbeat thread.
	//
	public void startHeartbeat() {
		TerminateHeartbeatThread = false;
		try {
			new Thread() {
				public void run() {
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
	// Started from above. Runs every 15 mS or so. While this thread is running,
	// the
	// green button on the TVM920 will glow solid. When this thread is NOT
	// running,
	// the green button will flash
	//
	public void Heartbeat() {
		int count = 0;
		// This thread probably eats 1-2 mS every HeartbeatInterval. It should
		// run at
		// an elevated priority. TODO: Study if HeartbeatInterval can be pushed
		// to 50
		// mS or so. From memory it seems like it could
		Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 1);

		while (TerminateHeartbeatThread == false) {
			try {
				GetStatus();
				sleep(HeartbeatInterval);
				++count;

				if (count % 200 == 0) {
					if (emulateHardware) {
						log("=====================================");
						log("=========EMULATED HARDWARE===========");
						log("=====================================");
					}
					log("TVM920: Heartbeat running.");
				}

			} catch (Exception e) {
				// Likely no connection. Wait a while and try again
				sleep(100);
			}
		}

		TerminateHeartbeatThread = false;
		log("TVM920: Hearbeat thread terminated.");
	}

	//
	// Asks for status, and also handles the processing of status. The roundtrip
	// time on this is a mS
	// or two under windows
	//
	void GetStatus() {
		sendReceiveUDP(new byte[] { 0, 0, 0, 0 });
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

		sendReceiveUDP(data);
	}

	//
	// Closes all feeders
	//
	/*
	 * public void FeederClose() { byte[] data = new byte[12];
	 * 
	 * data[0] = 0x5;
	 * 
	 * SendReceiveUDP(data); }
	 */

	//
	// Open valve to allow suction/pick
	//
	public void PickOpen(int pickNumber) {
		if (pickNumber > 3)
			throw new IllegalArgumentException("Bad pick index");

		byte[] data = new byte[8];

		data[0] = 0x16; // GPIO Set

		data[4] = (byte) (0x1 << pickNumber);

		sendReceiveUDP(data);
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

		sendReceiveUDP(data);
	}

	//
	// closes all picks
	//
	public void PickCloseAll() {
		byte[] data = new byte[8];

		data[0] = 0x17; // GPIO Clear

		data[4] = 0x0F;

		sendReceiveUDP(data);
	}

	//
	// Retract all Z to 0. Useful before a move
	//
	void RetractZ() {
		byte[] moveCmd = new byte[36];
		moveCmd[0] = 0x0D;

		moveCmd[2] = 0x30;

		sendReceiveUDP(moveCmd);
		GetStatus();

		while (Status.isZStopped() == false) {
			sleep(50);
			GetStatus();
		}
	}

	//
	// Move Z relative to current location
	//
	public void MoveZRel(int head, double z, double speed) {
		double absZ;

		absZ = z + GetZPosMM(head);
		MoveZAbs(head, absZ, speed);
	}

	//
	// Move Z absolute
	//
	public void MoveZAbs(int head, double z, double speed) {
		log(String.format("TVM920: MoveZAbs(%d, %.3f, %.3f)", head, z, speed));

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

		sendReceiveUDP(moveCmd);
		GetStatus();

		while (Status.isZStopped() == false) {
			sleep(10);
			GetStatus();
		}

		MotionDisable();
	}

	//
	// Move XY relative to current location
	//
	public void moveXYThetaRel(double x, double y, int head, double theta, double speed) {
		GetStatus();

		log(String.format("TVM920: moveXYThetaRel(%.3f, %.3f, %d, %.3f, %.3f)", x, y, head, theta, speed));

		double absX = x + GetXPosMM();
		double absY = y + GetYPosMM();
		double absTheta = theta + GetThetaPosDeg(head);

		moveXYThetaAbs(absX, absY, head, absTheta, speed);
	}

	//
	// MOve XY absolute. If any value == NaN, then that axis won't be moved
	//
	void moveXYThetaAbs(double x, double y, int head, double theta, double speed) {

		// Verify we have something to do
		if (Double.isNaN(x) && Double.isNaN(y) && Double.isNaN(theta))
			return;
		
		log(String.format("TVM920: MoveXYAbs(%.3f, %.3f, %d, %.3f, %.3f)", x, y, head, theta, speed));

		if (emulateHardware) {
			sleep(3);
			emulatedX = x;
			emulatedY = y;
		}

		if (IsHomed) {
			if (Double.isNaN(x) == false) {
				if (x > MAX_X)
					x = MAX_X;

				if (x < 0)
					x = 0;
			}

			if (Double.isNaN(y) == false) {
				if (y > MAX_Y)
					y = MAX_Y;

				if (y < 0)
					y = 0;
			}

			if (Double.isNaN(theta) == false) {
				while (theta < 0)
					theta += 360;

				while (theta >= 360)
					theta -= 360;
			}
		}

		// Query front panel lock button? Not sure if needed
		sendReceiveUDP(new byte[] { 1, 0, 0, 0, (byte) 0xf4, 0x10, 0, 0, 1, 0, 0, 0 });

		SetSpeed(speed);

		MotionEnable();
		RetractZ();

		byte[] moveCmd = new byte[36];
		moveCmd[0] = 0x0D;

		int xInt = 0, yInt = 0, thetaInt = 0;

		// Determine which axes to move
		if (Double.isNaN(x) == false) {
			moveCmd[2] |= 0x80;
			xInt = (int) Math.round(x * TicksPerMM_X);
		}
		if (Double.isNaN(y) == false) {
			moveCmd[2] |= 0x40;
			yInt = (int) Math.round(y * TicksPerMM_Y);
		}
		if (Double.isNaN(theta) == false) {
			thetaInt = (int) Math.round(theta * TicksPerDegree);
			switch (head) {
			case 0:
				moveCmd[2] |= 1;
				break;
			case 1:
				moveCmd[2] |= 2;
				break;
			case 2:
				moveCmd[2] |= 4;
				break;
			case 3:
				moveCmd[2] |= 8;
				break;
			}
		}

		moveCmd[0x1D] = (byte) (yInt >> 0);
		moveCmd[0x1E] = (byte) (yInt >> 8);
		moveCmd[0x1F] = (byte) (yInt >> 16);

		moveCmd[0x21] = (byte) (xInt >> 0);
		moveCmd[0x22] = (byte) (xInt >> 8);
		moveCmd[0x23] = (byte) (xInt >> 16);

		// Load Theta 0
		moveCmd[0x5] = (byte) (thetaInt >> 0);
		moveCmd[0x6] = (byte) (thetaInt >> 8);
		moveCmd[0x7] = (byte) (thetaInt >> 16);

		// Load Theta 1
		moveCmd[0x9] = (byte) (thetaInt >> 0);
		moveCmd[0xa] = (byte) (thetaInt >> 8);
		moveCmd[0xb] = (byte) (thetaInt >> 16);

		// Load Theta 2
		moveCmd[0xe] = (byte) (thetaInt >> 0);
		moveCmd[0xe] = (byte) (thetaInt >> 8);
		moveCmd[0xf] = (byte) (thetaInt >> 16);

		// Load Theta 3
		moveCmd[0x11] = (byte) (thetaInt >> 0);
		moveCmd[0x12] = (byte) (thetaInt >> 8);
		moveCmd[0x13] = (byte) (thetaInt >> 16);

		sendReceiveUDP(moveCmd);

		GetStatus();

		while (Status.isXYStopped() == false) {
			sleep(10);
			GetStatus();
		}

		MotionDisable();
	}

	//
	// Disable safety lockout. This will enable movement commands.
	//
	void MotionEnable() {
		sendReceiveUDP(new byte[] { 0xc, 0, 1, 0 });
	}

	//
	// Enabled safety lockout. After issuing this command, movement commands
	// don't do anything.
	//
	void MotionDisable() {
		sendReceiveUDP(new byte[] { 0xc, 0, 0, 0 });
	}

	public double GetThetaPosDeg(int head) {
		if (emulateHardware) {
			sleep(3);
			return emulatedX;
		}

		GetStatus();
		int thetaTicks = 0;

		switch (head) {
		case 0:
			thetaTicks = Status.GetTheta0Ticks();
			break;
		case 1:
			thetaTicks = Status.GetTheta1Ticks();
			break;
		case 2:
			thetaTicks = Status.GetTheta2Ticks();
			break;
		case 3:
			thetaTicks = Status.GetTheta3Ticks();
			break;
		}
		
		return thetaTicks / TicksPerDegree;
	}

	//
	// Gets the position from the last status message and converts to MM
	//
	public double GetXPosMM() {
		if (emulateHardware) {
			sleep(3);
			return emulatedX;
		}

		GetStatus();
		double d = Status.getXTicks() / TicksPerMM_X;
		log(String.format("TVM920: GetXPosMM() returned %.3f", d));

		return d;
	}

	//
	// Gets the position from the last status message and converts to MM
	//
	public double GetYPosMM() {
		if (emulateHardware) {
			sleep(3);
			return emulatedY;
		}

		GetStatus();
		double d = Status.getYTicks() / TicksPerMM_Y;
		log(String.format("TVM920: GetYPosMM() returned %.3f", d));

		return d;
	}

	//
	// Gets the position from the last status message and converts to MM. Note
	// that a positive value means raises nozzle 1 and lowers nozzle 2.
	//
	public double GetZPosMM(int head) {
		if (head < 0 || head > 3)
			throw new IllegalArgumentException("Head must be 0..3 inclusive");

		GetStatus();

		double d;

		if (head == 0 || head == 1)
			d = Status.getZ01Ticks() / TicksPerMM_Z * (head == 1 ? -1.0 : 1.0);
		else
			d = Status.getZ23Ticks() / TicksPerMM_Z * (head == 3 ? -1.0 : 1.0);

		log(String.format("TVM920: GetZPosMM() returned %.3f", d));
		return d;
	}

	//
	// Sets the XY position
	//
	public void SetXYPosMM(double xPosMM, double yPosMM) {
		log(String.format("TVM920: SetXYPosMM(%.3f, %.3f)", xPosMM, yPosMM));

		int xInt = (int) Math.round(xPosMM * TicksPerMM_X);
		int yInt = (int) Math.round(yPosMM * TicksPerMM_Y);
		sendReceiveUDP(new byte[] { 8, 0, (byte) 0xC0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, (byte) (yInt >> 0), (byte) (yInt >> 8), (byte) (yInt >> 16), 0, (byte) (xInt >> 0),
				(byte) (xInt >> 8), (byte) (xInt >> 16) });
	}

	//
	// Sets all Z positions to 0
	//
	public void SetZ1234PosZero() {
		sendReceiveUDP(new byte[] { 8, 0, 0x30, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0 });
	}

	//
	// Turns on down light and turns off up light
	//
	public void DownLightOn(boolean turnOn) {
		if (turnOn)
			sendReceiveUDP(new byte[] { 0x16, 0x00, 0x00, 0x00, (byte) 0x80, 0x00, 0x00, 0x00 });
		else
			sendReceiveUDP(new byte[] { 0x17, 0x00, 0x00, 0x00, (byte) 0x80, 0x00, 0x00, 0x00 });
	}

	//
	// Turns on up light and turns off down light
	//
	public void UpLightOn(boolean turnOn) {
		if (turnOn)
			sendReceiveUDP(new byte[] { 0x16, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00 });
		else
			sendReceiveUDP(new byte[] { 0x17, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00 });
	}

	//
	// Find Y home. TODO: Not sure the endstop code is needed? The endstop seems
	// to supress movement
	// that would move you FURTHER onto the endstop, but readily allows movement
	// that would move you
	// off of the endstop.
	//
	void FindYHome() {
		log(String.format("TVM920: FindYHome()"));

		EndStopEnableAll(true);

		moveXYThetaRel(0, 1000, 0, Double.NaN, 0.2);

		EndStopEnableY(false);

		moveXYThetaRel(0, -5, 0, Double.NaN, 0.2);

		EndStopEnableAll(true);
	}

	void FindXHome() {
		log(String.format("TVM920: FindXHome()"));

		EndStopEnableAll(true);

		moveXYThetaRel(1000, 0,  0, Double.NaN, 0.2);

		EndStopEnableX(false);

		moveXYThetaRel(-5, 0, 0, Double.NaN,  0.2);

		EndStopEnableAll(true);
	}

	void FindZHome() {
		log(String.format("TVM920: FindZHome()"));

		// Verify we're on the stop
		GetStatus();

		// Walk off home by lowering nozzle 1
		while (Status.isZ01Home() == true) {
			MoveZRel(0, -1, 0.2);
			sleep(50);
			GetStatus();
		}

		// Walk back on home
		while (Status.isZ01Home() == false) {
			MoveZRel(0, 0.1, 0.2);
			sleep(50);
			GetStatus();
		}

		// Walk off home buy lower nozzle 4
		while (Status.isZ23Home() == true) {
			MoveZRel(3, -1, 0.2);
			sleep(50);
			GetStatus();
		}

		// Walk back on home
		while (Status.isZ23Home() == false) {
			MoveZRel(3, 0.1, 0.2);
			sleep(50);
			GetStatus();
		}

		SetZ1234PosZero();
	}

	public void FindXYHome() {
		log(String.format("TVM920: FindXY()"));
		FindZHome();
		FindYHome();
		FindXHome();
		SetXYPosMM(465, 444);
		IsHomed = true;
	}

	public void FindHome() {
		log(String.format("TVM920: FindHome()"));
		FindXYHome();
	}

	public boolean checkEnabled() {
		try {
			Status.verifyStatusGood();
			return true;
		} catch (Exception ex) {

		}

		return false;
	}

	//
	// Sets speeds for various operations. If we have not been homed, then speed
	// is locked at 30%. TODO: These
	// tables need to be fleshed out for other speed settings
	//
	void SetSpeed(double speed) {
		log(String.format("TVM920: SetSpeed(%.3f)", speed));

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
			sendReceiveUDP(Pct80);
		} else if (speed >= 0.8 && speed < 0.9) {
			sendReceiveUDP(Pct80);
		} else if (speed >= 0.7 && speed < 0.8) {
			sendReceiveUDP(Pct50);
		} else if (speed >= 0.6 && speed < 0.7) {
			sendReceiveUDP(Pct50);
		} else if (speed >= 0.5 && speed < 0.6) {
			sendReceiveUDP(Pct50);
		} else if (speed >= 0.4 && speed < 0.5) {
			sendReceiveUDP(Pct30);
		} else if (speed < 0.4) {
			sendReceiveUDP(Pct30);
		}
	}

	public int ReadRegister(int register) {
		byte[] data = sendReceiveUDP(
				new byte[] { 01, 00, 00, 00, (byte) (register), (byte) (register >> 8), 00, 00, 00, 00, 00, 00 });
		return (data[11] << 24) + (data[10] << 16) + (data[9] << 8) + (data[8] << 0);
	}

	public void WriteRegister(int register, int data) {
		sendReceiveUDP(new byte[] { 01, 00, 00, 00, (byte) (register), (byte) (register >> 8), 00, 00,
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
		log(String.format("TVM920: Init()"));

		int sleepTime = 10;

		try {
			sendReceiveUDP(new byte[] { 0, 0, 0, 0 });
			sleep(sleepTime);

			sendReceiveUDP(new byte[] { 0x17, 00, 00, 00, 0x10, 00, 00, 00 });
			sleep(sleepTime);

			sendReceiveUDP(new byte[] { 0x02, 00, 00, 00, (byte) 0xf5, 0x01, 00, 00, 00, 00, 00, 00 });
			sleep(sleepTime);

			sendReceiveUDP(new byte[] { 0x02, 00, 00, 00, (byte) 0xf6, 0x01, 00, 00, 00, 00, 00, 00 });
			sleep(sleepTime);

			sendReceiveUDP(new byte[] { 0x07, 0x00, 0x04, 00, 0x04, 00, 00, 00, 0x04, 00, 00, 00, 0x04, 00, 00, 00,
					0x04, 00, 00, 00, 0x05, 00, 00, 00, 0x05, 00, 00, 00, 0x04, 00, 00, 00, 0x04, 00, 00, 00, 0x70,
					0x17, 00, 00, 0x70, 0x17, 00, 00, 0x70, 0x17, 00, 00, 0x70, 0x17, 00, 00, (byte) 0xb8, 0x0b, 00, 00,
					(byte) 0xb8, 0x0b, 00, 00, 0x10, 0x27, 00, 00, 0x10, 0x27, 00, 00 });
			sleep(sleepTime);

			byte[] data = new byte[] { 01, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00 };

			for (int i = 0; i < 48; i++) {
				data[5] = (byte) i;
				sendReceiveUDP(data);
				sleep(sleepTime);
			}

			sleep(500);

			sendReceiveUDP(new byte[] { 0x0c, 00, 00, 00 });
			sleep(sleepTime);

			sendReceiveUDP(new byte[] { 0x17, 00, 00, 00, 0x30, 00, 00, 00 });
			sleep(sleepTime);

			sendReceiveUDP(new byte[] { 02, 00, 00, 00, (byte) 0xf5, 01, 00, 00, 00, 00, 00, 00 });
			sleep(sleepTime);

			sendReceiveUDP(new byte[] { 02, 00, 00, 00, (byte) 0xf6, 01, 00, 00, 00, 00, 00, 00 });
			sleep(sleepTime);

			sendReceiveUDP(new byte[] { 0x16, 00, 00, 00, 00, 01, 00, 00 });
			sleep(sleepTime);

			sendReceiveUDP(new byte[] { 0x17, 00, 00, 00, 80, 00, 00, 00 });
			sleep(sleepTime);
		} catch (Exception ex) {

		}
	}

}
