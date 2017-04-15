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

import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Element;

public class TVM920Control {

	private static class Status {
		static byte[] lastStatusMessage = new byte[127];

		static private String dumpString(byte[] data) {
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

		static private boolean arraysAreSame(byte[] array1, byte[] array2) {
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
		static private void updateStatusMessage(byte[] newMsg) {

			if (arraysAreSame(newMsg, lastStatusMessage) == false) {
				Logger.debug("TVM920 Status Change: "
						+ LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")) + "\n"
						+ dumpString(newMsg));
			}

			lastStatusMessage = newMsg;
			// LastStatusTime = LocalDateTime.now();
		}

		//
		// Verifies a status message is recent
		//
		static private void verifyStatusGood() {
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

		static public int GetTheta0Ticks() {
			verifyStatusGood();

			// Preserve the sign using arithmetic shift. Note the "& 0xFF" is
			// important as it converts the value to an unsigned int
			int ticks = ((lastStatusMessage[0x7] & 0xFF) << 24) + ((lastStatusMessage[0x6] & 0xFF) << 16)
					+ ((lastStatusMessage[0x5] & 0xFF) << 8);
			ticks = ticks >> 8;

			Logger.debug(String.format("GetTheta0Ticks() returned %d [%x]", ticks, ticks));

			return ticks;
		}

		static public int GetTheta1Ticks() {
			verifyStatusGood();

			// Preserve the sign using arithmetic shift. Note the "& 0xFF" is
			// important as it converts the value to an unsigned int
			int ticks = ((lastStatusMessage[0xb] & 0xFF) << 24) + ((lastStatusMessage[0xa] & 0xFF) << 16)
					+ ((lastStatusMessage[0x9] & 0xFF) << 8);
			ticks = ticks >> 8;

			Logger.debug(String.format("GetTheta1Ticks() returned %d [%x]", ticks, ticks));

			return ticks;
		}

		static public int GetTheta2Ticks() {
			verifyStatusGood();

			// Preserve the sign using arithmetic shift. Note the "& 0xFF" is
			// important as it converts the value to an unsigned int
			int ticks = ((lastStatusMessage[0xf] & 0xFF) << 24) + ((lastStatusMessage[0xe] & 0xFF) << 16)
					+ ((lastStatusMessage[0xd] & 0xFF) << 8);
			ticks = ticks >> 8;

			Logger.debug(String.format("GetTheta2Ticks() returned %d [%x]", ticks, ticks));

			return ticks;
		}

		static public int GetTheta3Ticks() {
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
		static public int getXTicks() {
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
		static public int getYTicks() {
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
		static public int getZ01Ticks() {
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
		static public int getZ23Ticks() {
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
		static public boolean isZ01Home() {
			if ((lastStatusMessage[8] & 0x20) > 0)
				return true;
			else
				return false;
		}

		//
		// Indicates if Z34 is home
		//
		static public boolean isZ23Home() {
			if ((lastStatusMessage[8] & 0x40) > 0)
				return true;
			else
				return false;
		}

	} // End of static class to help with status

	// Location of hardware fiducial pin
	protected Location homingFiducialLocation = new Location(LengthUnit.Millimeters, 324, 112, 0, 0);

	private DatagramSocket Socket;
	private Lock NetLock = new ReentrantLock();
	private volatile boolean TerminateHeartbeatThread;
	private int HeartbeatInterval = 50;
	private boolean HeartbeatIsRunning = false;
	private boolean IsHomed = false;

	// These are measured values and will ultimately need a calibration
	private double TicksPerMM_X = 327.55;
	private double TicksPerMM_Y = 204.85;
	private double TicksPerMM_Z = 262.37;
	private double TicksPerDegree = 17.77 ; // 6400 ticks moves 360 degrees
	private double zArmLength = 10;         // arm length in mm

	// This is the max allowed. This applies AFTER we've been homed
	private double MAX_X = 470;
	private double MAX_Y = 510;
	private double MIN_Z = -17;

	// This is the max speed allowed on any operation before we've homed the
	// machine
	private double homingSpeed = 0.4;

	private final int HEADCOUNT = 4;

	private boolean isZAxisMoving = false;
	private boolean isXYThetaMoving = false;

	private boolean[] isZHome = new boolean[HEADCOUNT];

	private boolean isXStale, isYStale;
	private boolean[] isZStale = new boolean[HEADCOUNT];
	private boolean[] isThetaStale = new boolean[HEADCOUNT];

	private double xPosCached, yPosCached;
	private double[] zPosCached = new double[HEADCOUNT];
	private double[] thetaPosCached = new double[HEADCOUNT];

	// Vars used while emulating hardware
	private boolean emulateHardware = false;  
	private double emulatedX, emulatedY, emulatedTheta;

	//
	// Constructor
	//
	public TVM920Control() throws Exception {
		Socket = new DatagramSocket(8701);
		Socket.setSoTimeout(100);

		isXStale = true;
		isYStale = true;

		for (int i = 0; i < 4; i++) {
			isZStale[i] = true;
			isThetaStale[i] = true;
		}

		if (emulateHardware) {
			log("=====================================");
			log("=========EMULATED HARDWARE===========");
			log("=====================================");
		}
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
					HeartbeatIsRunning = true;
					Heartbeat();
					HeartbeatIsRunning = false;
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
	private void Heartbeat() {
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
	private void GetStatus() {
		sendReceiveUDP(new byte[] { 0, 0, 0, 0 });
	}

	//
	// Opens specified feeder. Feeder is zero based. 0..27 indicates
	// front feeder, 32..59 = rear feeders
	//
	public void feederOpen(int feederNumber) {
		byte[] data = new byte[12];

		if (feederNumber < 0 || feederNumber > 27)
			throw new IllegalArgumentException("TVM920Control:FeederOpen() Bad feederNumber index");

		data[0] = 0x5;

		int byteIndex = 10 - feederNumber / 8;
		byte bit = (byte) (0x1 << (feederNumber % 8));

		data[byteIndex] = bit;

		sendReceiveUDP(data);
	}

	//
	// Closes all feeders
	//

	public void feedersCloseAll() {
		byte[] data = new byte[12];

		data[0] = 0x5;

		sendReceiveUDP(data);
	}

	//
	// Open valve to allow suction/pick
	//
	public void pickOpen(int pickIndex) {
		if (pickIndex < 0 || pickIndex > 3)
			throw new IllegalArgumentException("Bad pick index");

		byte[] data = new byte[8];

		data[0] = 0x16; // GPIO Set

		data[4] = (byte) (0x1 << pickIndex);

		sendReceiveUDP(data);
	}

	//
	// Close valve to release pick
	//
	public void pickClose(int pickIndex) {
		if (pickIndex < 0 || pickIndex > 3)
			throw new IllegalArgumentException("Bad pick index");

		byte[] data = new byte[8];

		data[0] = 0x17; // GPIO Clear

		data[4] = (byte) (0x1 << pickIndex);

		sendReceiveUDP(data);
	}

	//
	// closes all picks
	//
	public void pickCloseAll() {
		byte[] data = new byte[8];

		data[0] = 0x17; // GPIO Clear

		data[4] = 0x0F;

		sendReceiveUDP(data);
	}

	//
	// Retract all Z to 0. Useful before a move
	//
	/*
	 * private void retractZ() { byte[] moveCmd = new byte[36]; moveCmd[0] =
	 * 0x0D;
	 * 
	 * moveCmd[2] = 0x30;
	 * 
	 * sendReceiveUDP(moveCmd); GetStatus();
	 * 
	 * while (Status.isZStopped() == false) { sleep(50); GetStatus(); } }
	 */

	//
	// Move Z relative to current location
	//
	public void moveZRel(int head, double z, double speed) {
		double absZ;

		absZ = z + getZPosMM(head);
		moveZAbs(head, absZ, speed);
	}

	//
	// Move Z absolute
	//
	public void moveZAbs(int head, double z, double speed) {
		log(String.format("TVM920: MoveZAbs(%d, %.3f, %.3f)", head, z, speed));

		if (Double.isNaN(z))
			return;

		if (head < 0 || head > 3) {
			throw new IllegalArgumentException("Head must be 0..3 inclusive");
		}

		if (z == 0 && isZHome[head])
			return;

		isZAxisMoving = true;

		byte[] moveCmd = new byte[36];
		moveCmd[0] = 0x0D;

		if (IsHomed) {
			if (z < MIN_Z)
				z = MIN_Z;

			if (z > 0)
				z = 0;
		}

		setSpeed(speed);

		motionEnable();

		if (head == 0 || head == 1)
			moveCmd[2] = 0x10;
		else if (head == 2 || head == 3)
			moveCmd[2] = 0x20;

		isZStale[head] = true;

		// Compute move
		//int zInt = (int) Math.round(z * TicksPerMM_Z);
		int zInt = distanceToTicks(z);

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

		isZAxisMoving = false;
		motionDisable();

		if (z == 0)
			isZHome[head] = true;
		else
			isZHome[head] = false;
	}

	//
	// Move XY relative to current location
	//
	private void moveXYThetaRel(double x, double y, int head, double theta, double speed) {
		GetStatus();

		log(String.format("TVM920: moveXYThetaRel(%.3f, %.3f, %d, %.3f, %.3f)", x, y, head, theta, speed));

		double absX = x + getXPosMM();
		double absY = y + getYPosMM();
		double absTheta = theta + getThetaPosDeg(head);

		moveXYThetaAbs(absX, absY, head, absTheta, speed);
	}

	//
	// MOve XY absolute. If any value == NaN, then that axis won't be moved
	//
	public void moveXYThetaAbs(double x, double y, int head, double theta, double speed) {
		// Verify we have something to do
		if (Double.isNaN(x) && Double.isNaN(y) && Double.isNaN(theta))
			return;

		log(String.format("TVM920: moveXYThetaAbs(%.3f, %.3f, %d, %.3f, %.3f)", x, y, head, theta, speed));

		if (emulateHardware) {
			sleep(3);
			emulatedX = x;
			emulatedY = y;
			emulatedTheta = theta;
		}

		// Clip values to table size.
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
				// If theta is specifed, the head needs to make sense. Note that
				// -1 specifies "no head" which
				// is valid
				if (head < -1 || head > 3)
					throw new IllegalArgumentException(
							"TVM920Control:MoveXYThetaAbs Bad head index: " + Integer.toString(head));
			}
		}

		// Not sure what this does...but it's there every time in WireShark.
		// BUGBUG: See if
		// this can be removed
		sendReceiveUDP(new byte[] { 1, 0, 0, 0, (byte) 0xf4, 0x10, 0, 0, 1, 0, 0, 0 });

		setSpeed(speed);

		motionEnable();
		isXYThetaMoving = true;

		byte[] moveCmd = new byte[36];
		moveCmd[0] = 0x0D;

		int xInt = 0, yInt = 0, thetaInt = 0;

		// Determine which axes to move
		if (Double.isNaN(x) == false) {
			isXStale = true;
			moveCmd[2] |= 0x80;
			xInt = (int) Math.round(x * TicksPerMM_X);
		}
		if (Double.isNaN(y) == false) {
			isYStale = true;
			moveCmd[2] |= 0x40;
			yInt = (int) Math.round(y * TicksPerMM_Y);
		}
		if (Double.isNaN(theta) == false && head != -1) {
			isThetaStale[head] = true;
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
		if (head == 0) {
			moveCmd[0x5] = (byte) (thetaInt >> 0);
			moveCmd[0x6] = (byte) (thetaInt >> 8);
			moveCmd[0x7] = (byte) (thetaInt >> 16);
		}

		// Load Theta 1
		if (head == 1) {
			moveCmd[0x9] = (byte) (thetaInt >> 0);
			moveCmd[0xa] = (byte) (thetaInt >> 8);
			moveCmd[0xb] = (byte) (thetaInt >> 16);
		}

		// Load Theta 2
		if (head == 2) {
			moveCmd[0xd] = (byte) (thetaInt >> 0);
			moveCmd[0xe] = (byte) (thetaInt >> 8);
			moveCmd[0xf] = (byte) (thetaInt >> 16);
		}

		// Load Theta 3
		if (head == 3) {
			moveCmd[0x11] = (byte) (thetaInt >> 0);
			moveCmd[0x12] = (byte) (thetaInt >> 8);
			moveCmd[0x13] = (byte) (thetaInt >> 16);
		}

		sendReceiveUDP(moveCmd);

		GetStatus();

		while (Status.isXYStopped() == false) {
			sleep(10);
			GetStatus();

			// Provide live updates to the Machine as the move progresses.
			// ((ReferenceMachine) Configuration.get().getMachine())
			// .fireMachineHeadActivity(hm.getHead());
		}

		isXYThetaMoving = false;
		motionDisable();
	}

	//
	// Disable safety lockout. This will enable movement commands.
	//
	void motionEnable() {
		sendReceiveUDP(new byte[] { 0xc, 0, 1, 0 });
	}

	//
	// Enabled safety lockout. After issuing this command, movement commands
	// don't do anything.
	//
	void motionDisable() {
		sendReceiveUDP(new byte[] { 0xc, 0, 0, 0 });
	}

	//
	// Gets the position from the last status message and converts to MM
	//
	public double getXPosMM() {
		double result = Double.MAX_VALUE;

		if (isXStale == false && isXYThetaMoving == false) {
			result = xPosCached;
		} else {
			if (emulateHardware) {
				sleep(3);
				result = emulatedX;
			}

			GetStatus();
			result = Status.getXTicks() / TicksPerMM_X;
			xPosCached = result;
			
			isXStale = isXYThetaMoving;
		}

		log(String.format("TVM920: GetXPosMM() returned %.3f", result));
		return result;
	}

	//
	// Gets the position from the last status message and converts to MM
	//
	public double getYPosMM() {
		double result = Double.MAX_VALUE;

		if (isYStale == false && isXYThetaMoving == false) {
			result = yPosCached;
		} else {
			if (emulateHardware) {
				sleep(3);
				result = emulatedX;
			}

			GetStatus();
			result = Status.getYTicks() / TicksPerMM_Y;
			yPosCached = result;
			
			isYStale = isXYThetaMoving;
		}

		log(String.format("TVM920: GetYPosMM() returned %.3f", result));
		return result;
	}
	
	private double ticksToDistance(int ticks){
		double degrees = ticks / TicksPerDegree;
		double distance;
	
		if (degrees <= -90)
			//distance = zArmLength * Math.sin(degrees / 180 * Math.PI);
			distance = 2*zArmLength - zArmLength * Math.sin(Math.PI + degrees / 180 * Math.PI);
		else
			//distance = 2 * zArmLength - Math.sin(Math.PI - degrees / 180 * Math.PI);
			distance = zArmLength * Math.sin(degrees / 180 * Math.PI);
		
		return distance;
	}
	
	// Distance is assumed to be a negative quantity (reference to Z home)
	private int distanceToTicks(double distance)
	{
		distance = distance;
		double degrees;
		
		if (distance < -zArmLength){
			degrees = (Math.PI - Math.asin((2*zArmLength + distance)/zArmLength)) / Math.PI * 180;
			degrees = -degrees;
		}
		else{
			degrees = Math.asin(distance/zArmLength) / Math.PI	* 180;
		}
		
		int ticks = (int)Math.round(degrees * TicksPerDegree);

		// Roundtrip it for accuracy
		//double debugDistance = ticksToDistance(ticks);
				
		return ticks;
	}
	

	//
	// Gets the position from the last status message and converts to MM. Note
	// that a positive value means raises nozzle 1 and lowers nozzle 2.
	//
	public double getZPosMM(int head) {
		if (head < 0 || head > 3)
			throw new IllegalArgumentException("TVM920Control:getZPosMM() Bad head index");

		if (isZStale[head] == false && isZAxisMoving == false) {
			return zPosCached[head];
		}

		GetStatus();
		if (head == 0 || head == 1)
			zPosCached[head] = ticksToDistance(Status.getZ01Ticks()) * (head == 1 ? -1.0 : 1.0);
		else
			zPosCached[head] = ticksToDistance(Status.getZ23Ticks()) * (head == 3 ? -1.0 : 1.0);

		log(String.format("TVM920: GetZPosMM() returned %.3f", zPosCached[head]));
		isZStale[head] = false;
		return zPosCached[head];
	}

	public double getThetaPosDeg(int head) {
		if (head < 0 || head > 3)
			throw new IllegalArgumentException("TVM920Control:getThetaPosDeg() Bad head index");

		if (isThetaStale[head] == false) {
			return thetaPosCached[head];
		}

		if (emulateHardware) {
			sleep(3);
			return emulatedTheta;
		}

		GetStatus();

		switch (head) {
		case 0:
			thetaPosCached[head] = Status.GetTheta0Ticks() / TicksPerDegree;
			break;
		case 1:
			thetaPosCached[head] = Status.GetTheta1Ticks() / TicksPerDegree;
			break;
		case 2:
			thetaPosCached[head] = Status.GetTheta2Ticks() / TicksPerDegree;
			break;
		case 3:
			thetaPosCached[head] = Status.GetTheta3Ticks() / TicksPerDegree;
			break;
		}

		isThetaStale[head] = false;

		return thetaPosCached[head];
	}

	//
	// Sets the XY position
	//
	private void setXYPosMM(double xPosMM, double yPosMM) {
		log(String.format("TVM920: SetXYPosMM(%.3f, %.3f)", xPosMM, yPosMM));

		isXStale = true;
		isYStale = true;

		int xInt = (int) Math.round(xPosMM * TicksPerMM_X);
		int yInt = (int) Math.round(yPosMM * TicksPerMM_Y);
		sendReceiveUDP(new byte[] { 8, 0, (byte) 0xC0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, (byte) (yInt >> 0), (byte) (yInt >> 8), (byte) (yInt >> 16), 0, (byte) (xInt >> 0),
				(byte) (xInt >> 8), (byte) (xInt >> 16) });
	}

	//
	// Sets all Z positions to 0
	//
	private void setZ1234PosZero() {
		for (int i = 0; i < HEADCOUNT; i++) {
			zPosCached[i] = 0;
			isZStale[i] = false;
		}

		sendReceiveUDP(new byte[] { 8, 0, 0x30, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0 });
	}

	//
	// Turns on/off down light and turns off up light
	//
	public void upLightOn(boolean turnOn) {
		// Always want DL off if controlling DL
		sendReceiveUDP(new byte[] { 0x17, 0x00, 0x00, 0x00, (byte) 0x80, 0x00, 0x00, 0x00 });
		
		if (turnOn){
			// Turn on UL
			sendReceiveUDP(new byte[] { 0x16, 0x00, 0x00, 0x00, (byte) 0x00, 0x01, 0x00, 0x00 });
		}
		else{
			// Turn off DL
			sendReceiveUDP(new byte[] { 0x17, 0x00, 0x00, 0x00, (byte) 0x00, 0x01, 0x00, 0x00 });
		}
	}

	//
	// Turns on up light and turns off down light
	//
	public void downLightOn(boolean turnOn) {
		// Always want UL off if controlling UL
		sendReceiveUDP(new byte[] { 0x17, 0x00, 0x00, 0x00, (byte) 0x00, 0x01, 0x00, 0x00 });
		
		if (turnOn){
			// Turn on DL
			sendReceiveUDP(new byte[] { 0x16, 0x00, 0x00, 0x00, (byte) 0x80, 0x00, 0x00, 0x00 });
		}
		else{
			// Turn off DL
			sendReceiveUDP(new byte[] { 0x17, 0x00, 0x00, 0x00, (byte) 0x80, 0x00, 0x00, 0x00 });
		}
	}

	//
	// Find Y home. TODO: Not sure the endstop code is needed? The endstop seems
	// to supress movement
	// that would move you FURTHER onto the endstop, but readily allows movement
	// that would move you
	// off of the endstop.
	//

	private void findXHome() {
		log(String.format("TVM920: findXHome()"));

		EndStopEnableAll(true);

		moveXYThetaRel(1000, 0, 0, Double.NaN, homingSpeed);

		EndStopEnableX(false);

		// Back off 5 mm from home. If this changes, the abs setting in
		// findXYZHome() needs to change
		moveXYThetaRel(-5, 0, 0, Double.NaN, homingSpeed);

		EndStopEnableAll(true);
	}

	private void findYHome() {
		log(String.format("TVM920: findYHome()"));

		EndStopEnableAll(true);

		moveXYThetaRel(0, 1000, 0, Double.NaN, homingSpeed);

		EndStopEnableY(false);

		// Back off 5 mm from home. If this changes, the abs setting in
		// findXYZHome() needs to change
		moveXYThetaRel(0, -5, 0, Double.NaN, homingSpeed);

		EndStopEnableAll(true);
	}

	private void FindZHome() {
		log(String.format("TVM920: FindZHome()"));

		// Verify we're on the stop
		GetStatus();

		// Walk off home by lowering nozzle 1 one mm at a time
		while (Status.isZ01Home() == true) {
			moveZRel(0, -1, homingSpeed);
			sleep(50);
			GetStatus();
		}

		// Walk back on home 0.2 mm at a time
		while (Status.isZ01Home() == false) {
			moveZRel(0, 0.1, homingSpeed);
			sleep(50);
			GetStatus();
		}

		// Walk off home buy lowering nozzle 4
		while (Status.isZ23Home() == true) {
			moveZRel(3, -1, homingSpeed);
			sleep(50);
			GetStatus();
		}

		// Walk back on home
		while (Status.isZ23Home() == false) {
			moveZRel(3, 0.1, homingSpeed);
			sleep(50);
			GetStatus();
		}

		setZ1234PosZero();
	}

	private void findXYZHome() {
		log(String.format("TVM920: findXYZHome()"));
		FindZHome();
		findYHome();
		findXHome();
		// setXYPosMM(465, 444);
		setXYPosMM(MAX_X - 5, MAX_Y - 5);

	}

	public void findHome() throws Exception {
		log(String.format("TVM920: FindHome()"));

		IsHomed = false;

		if (emulateHardware) {
			sleep(3);
			emulatedX = 0;
			emulatedY = 0;
			emulatedTheta = 0;
			return;
		}

		findXYZHome();

		Location fidLoc = homingFiducialLocation;

		if (fidLoc == null || fidLoc.getX() <= 0 || fidLoc.getY() <= 0 || fidLoc.getX() >= MAX_X
				|| fidLoc.getY() >= MAX_Y) {
			log("Homing Fiducial specification was outside of table range");
		} else {
			// Move where we think the hardware fiducial is located
			moveXYThetaAbs(fidLoc.getX(), fidLoc.getY(), -1, 0, 0.5);

			// Try to use vision to set
			Part homePart = Configuration.get().getPart("FIDUCIAL-HOME");
			if (homePart != null) {
				Configuration.get().getMachine().getFiducialLocator().getHomeFiducialLocation(homingFiducialLocation,
						homePart);
			} else {
				log("FIDUCIAL-HOME not found in Parts. Skipping hardware fiducial");
			}
		}

		setXYPosMM(fidLoc.getX(), fidLoc.getY());
		IsHomed = true;
	}

	public boolean checkEnabled() {
		return HeartbeatIsRunning;
	}

	//
	// Sets speeds for various operations. If we have not been homed, then speed
	// is locked at 40%. TODO: These
	// tables need to be fleshed out for other speed settings
	//
	private void setSpeed(double speed) {
		log(String.format("TVM920: SetSpeed(%.3f)", speed));

		if (speed < 0 || speed > 1.0)
			throw new IllegalArgumentException("Speed must be between 0.0 and 1.0 inclusive");

		// Speed options

		/*
		 * byte[] Pct30 = { 7, 0, 2, 0, 4, 0, 0, 0, 4, 0, 0, 0, 4, 0, 0, 0, 4,
		 * 0, 0, 0, 4, 0, 0, 0, 4, 0, 0, 0, 4, 0, 0, 0, 4, 0, 0, 0, (byte) 0xd0,
		 * 7, 0, 0, (byte) 0xd7, 7, 0, 0, (byte) 0xd0, 7, 0, 0, 0xd, 7, 0, 0,
		 * (byte) 0xe8, 3, 0, 0, (byte) 0xe8, 3, 00,(byte) 0xc4, 9, 0, 0, (byte)
		 * 0xc4, 9, 0, 0 };
		 */

		// 40% = speed option is what TVM920 uses for homing
		byte[] Pct40 = { 7, 0, 3, 0, 4, 0, 0, 0, 4, 0, 0, 0, 4, 0, 0, 0, 4, 0, 0, 0, 4, 0, 0, 0, 4, 0, 0, 0, 4, 0, 0, 0,
				4, 0, 0, 0, (byte) 0xa0, 0xf, 0, 0, (byte) 0xa0, 0xf, 0, 0, (byte) 0xa0, 0xf, 0, 0, (byte) 0xa0, 0xf, 0,
				0, (byte) 0xd0, 7, 0, 0, (byte) 0xd0, 7, 0, 0, (byte) 0xa0, 0xf, 0, 0, (byte) 0xa0, 0xf, 0, 0 };

		byte[] Pct50 = { 7, 0, 4, 0, 4, 0, 0, 0, 4, 0, 0, 0, 4, 0, 0, 0, 4, 0, 0, 0, 5, 0, 0, 0, 5, 0, 0, 0, 4, 0, 0, 0,
				4, 0, 0, 0, 0x70, 0x17, 0, 0, 0x70, 0x17, 0, 0, 0x70, 0x17, 0, 0, 0x70, 0x17, 0, 0, (byte) 0xb8, 0xb, 0,
				0, (byte) 0xb8, 0xb, 0, 0, 0x10, 0x27, 0, 0, 0x10, 0x27, 0, 0 };

		byte[] Pct80 = { 7, 0, 9, 0, 5, 0, 0, 0, 5, 0, 0, 0, 5, 0, 0, 0, 5, 0, 0, 0, 0x0c, 0, 0, 0, 0x0c, 0, 0, 0, 7, 0,
				0, 0, 7, 0, 0, 0, (byte) 0xe0, 0x2e, 0, 0, (byte) 0xe0, 0x2e, 0, 0, (byte) 0xe0, 0x2e, 0, 0,
				(byte) 0xe0, 0x2e, 0, 0, (byte) 0xc8, 0x32, 0, 0, (byte) 0xc8, 0x32, 0, 0, (byte) 0x98, (byte) 0xb7, 0,
				0, (byte) 0xb0, (byte) 0xb3, 0, 0 };

		// Lock speed if we're not home
		if (IsHomed == false)
			speed = 0.4;

		byte[] msg = new byte[0];

		if (speed >= 0.9) {
			msg = Pct80;
		} else if (speed >= 0.8 && speed < 0.9) {
			msg = Pct80;
		} else if (speed >= 0.7 && speed < 0.8) {
			msg = Pct50;
		} else if (speed >= 0.6 && speed < 0.7) {
			msg = Pct50;
		} else if (speed >= 0.5 && speed < 0.6) {
			msg = Pct50;
		} else if (speed >= 0.4 && speed < 0.5) {
			msg = Pct40;
		} else if (speed < 0.4) {
			msg = Pct40;
		}

		if (msg.length != 0x44) {
			throw new IllegalArgumentException(
					"Speed setting resulted in bad message length. Speed:" + Double.toString(speed));
		}

		sendReceiveUDP(msg);
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
