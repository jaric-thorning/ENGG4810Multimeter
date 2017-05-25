package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;
import java.io.InputStreamReader;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 *  class which handles serial comms. Note this code is modified off TP1 code which my team member
 * originally wrote.
 */
/**
 * The SerialFramework class handles serial communications between software/hardware/firmware. NOTE: This code is
 * modified off TP1 code which my team member originally wrote.
 * 
 * @author dayakern
 *
 */
public class SerialFramework {

	private static ObservableList<String> portNames = FXCollections.observableArrayList();
	private final static String INIT_PORT_SELECTION = "Ports"; // Default first element
	private static SerialPort openSerialPort = null;
	private static BufferedReader readFromSerial = null;
	private static PrintWriter writeToSerial = null;

	private static boolean isChecked = false; // Two-way connection has been checked

	public static SerialPort getOpenSerialPort() {
		return openSerialPort;
	}

	/**
	 * Writes out specified code to remotely control the multimeter
	 * 
	 * @param output
	 *            the code to write out
	 */
	public static void writeCode(String output) {
		// System.out.println("output: " + output);

		try {
			// Write multimeter code over the open port
			writeToSerial = new PrintWriter(openSerialPort.getOutputStream(), true);
			writeToSerial.println(output);

			writeToSerial.close();
		} catch (NullPointerException e) {
			System.err.println("Port Cannot Be Written To");
		}
	}

	/**
	 * Refreshes the existing ports list to include any new ports detected.
	 */
	public static void refreshSelectablePortsList() {
		if (!Platform.isFxApplicationThread()) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					refreshSelectablePortsList();
				}
			});
			return;
		}

		portNames.clear();
		portNames.add(INIT_PORT_SELECTION);

		SerialPort[] ports = SerialPort.getCommPorts();

		int portCounter = 0;
		for (SerialPort serialPort : ports) {
			portNames.add(serialPort.getDescriptivePortName());
			portCounter++;
		}

		GuiController.instance.portsAvailable.setItems(portNames);
		GuiController.instance.portsAvailable.setValue(portNames.get(0));
		GuiController.instance.portsAvailable.setVisibleRowCount(portCounter + 1);
	}

	/**
	 * Closes open serial port, if it is still open.
	 */
	public static void closeOpenPort() {

		if (!Platform.isFxApplicationThread()) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					closeOpenPort();
				}
			});
			return;
		}

		if (openSerialPort != null) {
			openSerialPort.closePort();
			System.out.println("closing openSerialport");
			openSerialPort = null;
		}

		// Close buffered reader
		if (readFromSerial != null) {
			try {
				readFromSerial.close();
				System.out.println("closing readFromSerialReader");
			} catch (IOException e) {
				e.printStackTrace();
			}

			readFromSerial = null;
		}

		// Close write port
		if (writeToSerial != null) {
			writeToSerial.close();
			System.out.println("closing writeToSerial");
			writeToSerial = null;
		}

	}

	// FIXME: TEST FOR TWO-WAY
	/**
	 * Handles the changing of the serial port selection. If there are ports (with valid names) it binds the serial port
	 * (refreshes the port list if it didn't bind); otherwise is closes the open port.
	 */
	public static void selectPort() {
		if (GuiController.instance.portsAvailable.getValue() != null
				&& !GuiController.instance.portsAvailable.getValue().equalsIgnoreCase("")
				&& !GuiController.instance.portsAvailable.getValue().equals(INIT_PORT_SELECTION)) {
			System.out.println("Changed to this port: " + GuiController.instance.portsAvailable.getValue());

			SerialPort[] ports = SerialPort.getCommPorts();
			for (SerialPort serialPort : ports) {
				if (serialPort.getDescriptivePortName().equals(GuiController.instance.portsAvailable.getValue())) {
					System.out.println("Binding to Serial Port " + serialPort.getSystemPortName() + "...");
					if (bindListen(serialPort)) {
						System.out.println("Success.");

						if (checkTwoWays()) {
							System.out.println("Two-way connection");
						} else {
							System.out.println("No two-way connection");
						}
						GuiController.instance.connRBtn.setDisable(false);
					} else {
						GuiController.instance.connRBtn.setDisable(true);
						System.out.println("Failed to bind to Serial.");
						refreshSelectablePortsList();
					}
					return;
				}
			}
			System.out.println("Invalid Serial!");
			refreshSelectablePortsList();
		} else {
			GuiController.instance.connRBtn.setDisable(true);
			closeOpenPort();
			System.out.println("Not a port - close any open ports");
		}
	}

	/**
	 * A private helper function to 'selectPort', which binds to the serial port.
	 * 
	 * @param serialPort
	 *            the port that needs to be binded to.
	 * @return whether or not the serial port was binded to successfully (true if it did, false if if didn't).
	 */
	static boolean bindListen(SerialPort serialPort) {
		closeOpenPort();

		if (!serialPort.openPort()) {
			return false;
		}

		serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);
		serialPort.setBaudRate(9600);

		// Add the data listener to the data. [also loads in the data]
		if (!serialPort.addDataListener(new SerialFramework.SerialDataListener())) {
			serialPort.closePort();
			return false;
		}

		openSerialPort = serialPort;

		return true;
	}

	/**
	 * A private helper function to 'selectPort' that determines whether or not there is a two-way connection
	 * 
	 * @return true if there is, false otherwise
	 */
	private static boolean checkTwoWays() {
		writeCode("[C]");

		long initialTime = System.currentTimeMillis(); // current time

		while (true) {
			long time = System.currentTimeMillis() - initialTime;
			double elapsedSeconds = time / 1000.0; // elapsed seconds

			if (isChecked) {
				return true;
			}

			if (elapsedSeconds >= 0.5 && !isChecked) {
				// System.out.println("T: " + elapsedSeconds);
				return false;
			}

			// System.out.println(" SC: " + elapsedSeconds);
		}
	}

	/**
	 * A class used for binding the serial port, and getting data from it.
	 */
	private static class SerialDataListener implements SerialPortDataListener {
		private static final String PACKET_OPEN = "[";
		private static final String PACKET_CLOSE = "]";
		private static final char MODE = 'M';
		private static final char FREQUENCY = 'F';

		/** The buffer for building whole packets received over the serial port. */
		private String serialBuffer = "";

		/** Whether the port connection has bugged out. */
		private boolean errored = false;

		/**
		 * Handles an event on the serial port. Reads the data and displays it.
		 */
		@Override
		public void serialEvent(SerialPortEvent sEvent) {
			if (this.errored) {
				return;
			}

			// Reading data
			if (sEvent.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
				if (sEvent.getSerialPort().bytesAvailable() < 0) {
					this.errored = true;
					System.err.println("Error 3. reading from port");
					closeOpenPort();

					return;
				}

				// Read by line
				try {
					readFromSerial = new BufferedReader(new InputStreamReader(sEvent.getSerialPort().getInputStream()));

					if (GuiController.instance.connRBtn.isSelected()) {
						getData(readFromSerial.readLine());
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					// e.printStackTrace();
					System.err.println("IO Exception, no bytes");
				}
			}

			if (sEvent.getEventType() == SerialPort.LISTENING_EVENT_DATA_WRITTEN) {
				System.out.println("All bytes were successfully transmitted!");
			}
		}

		/**
		 * Checks the validity of a supplied string. To be used in getData(), as a means to determine whether the data
		 * has been decoded successfully.
		 * 
		 * @param str
		 *            the string which needs it's validity checked.
		 * @return whether or not the string is valid (true if it is valid, false if it isn't).
		 */
		private boolean isValidText(String str) {
			return str != null && !str.equalsIgnoreCase("");
		}

		private boolean getData(String text) {

			// System.out.println("T: |" + text + "|");
			int openPacket = text.indexOf(PACKET_OPEN);
			int closePacket = text.indexOf(PACKET_CLOSE);

			if (openPacket < 0 || closePacket < 0) {
				return false;
			}
			if (closePacket < openPacket) {
				text = text.substring(openPacket);
				return true;
			}

			String data = text.substring(openPacket + 1, closePacket);

			if (data.charAt(0) == 'C') { // Check that it's two-way
				// TODO: Check completed, it's two-way
				isChecked = true;
			} else if (data.charAt(0) == 'F') {// Change displayed frequency settings

				// FIXME:
				// sortSampleFrequency(data, openPacket, closePacket);
			}
			if (data.charAt(0) == 'S') { // Change multimeter settings
				System.out.println("SWEET");
				sortMultimeterCommand(data, openPacket, closePacket);
			} else if (data.charAt(0) == 'V' || data.charAt(0) == 'I' || data.charAt(0) == 'R') {

				// Change values received
				sortMultimeterMeasurements(data, openPacket, closePacket);
			} else {
				// stuff
			}

			text = text.substring(closePacket + 1); // not sure about this

			return true;
		}

		private void sortSampleFrequency(String data, int openPacket, int closePacket) {
			boolean failedToDecode = false;

			// Check for frequency
			String trimmedData = data.substring(2, data.length());

			System.out.println("trimmedData: " + trimmedData);
			if (!isValidText(trimmedData)) {
				failedToDecode = true;
			}

			char frequencyType = 0;

			// If the next bits of data were not matching to any frequency types
			if (!(trimmedData.charAt(0) == 'A' || trimmedData.charAt(0) == 'B' || trimmedData.charAt(0) == 'C'
					|| trimmedData.charAt(0) == 'D' || trimmedData.charAt(0) == 'E' || trimmedData.charAt(0) == 'F'
					|| trimmedData.charAt(0) == 'G' || trimmedData.charAt(0) == 'H')) {
				failedToDecode = true;
			} else {
				frequencyType = trimmedData.charAt(0);
			}

			// Change the mode to either resistance, current, voltage, logic or continuity
			if (!failedToDecode) {
				switch (frequencyType) {
				case 'A':
					// GuiController.instance.driveCurrent();
					break;
				case 'B':
					// GuiController.instance.driveVoltage();
					break;
				case 'C':
					// GuiController.instance.driveResistance();
					break;
				case 'D':
					// GuiController.instance.driveContinuity();
					break;
				case 'E':
					// GuiController.instance.driveLogic();
					break;
				case 'F':
					// GuiController.instance.driveResistance();
					break;
				case 'G':
					// GuiController.instance.driveContinuity();
					break;
				case 'H':
					// GuiController.instance.driveLogic();
					break;
				default:
					// FIXME: INVALID THING HERE
					failedToDecode = true;
					break;
				}
			} else {
				System.out.println(
						"______Failed to decode data:\"" + serialBuffer.substring(openPacket, closePacket + 1) + "\"");
			}
		}

		/**
		 * A private helper function for 'getData' which checks that the command received is valid and executes it
		 * 
		 * @param data
		 *            the data read in serially
		 * @param openPacket
		 *            index of opening brace
		 * @param closePacket
		 *            index of closing brace
		 */
		private void sortMultimeterCommand(String data, int openPacket, int closePacket) {
			boolean failedToDecode = false;

			// Check for multimeter display commands.
			String trimmedData = data.substring(2, data.length());

			if (!isValidText(trimmedData)) {
				failedToDecode = true;
			}

			char modeType = 0;

			// If data received from the serial connection was not the right type
			if (trimmedData.charAt(0) != MODE)
				failedToDecode = true;

			// If the next bits of data were not matching to any mode type
			if (!(trimmedData.charAt(2) == 'I' || trimmedData.charAt(2) == 'V' || trimmedData.charAt(2) == 'R'
					|| trimmedData.charAt(2) == 'C' || trimmedData.charAt(2) == 'L')) {
				failedToDecode = true;
			} else {
				modeType = trimmedData.charAt(2);
			}

			// Change the mode to either resistance, current, voltage, logic or continuity
			if (!failedToDecode) {
				switch (modeType) {
				case 'I':
					GuiController.instance.driveCurrent();
					break;
				case 'V':
					GuiController.instance.driveVoltage();
					break;
				case 'R':
					GuiController.instance.driveResistance();
					break;
				case 'C':
					GuiController.instance.driveContinuity();
					break;
				case 'L':
					GuiController.instance.driveLogic();
					break;
				default:
					// FIXME: INVALID THING HERE
					failedToDecode = true;
					break;
				}
			} else {
				System.out.println(
						"******Failed to decode data:\"" + serialBuffer.substring(openPacket, closePacket + 1) + "\"");
			}
		}

		private void sortMultimeterMeasurements(String data, int openPacket, int closePacket) {
			boolean failedToDecode = false;

			// Check for voltage/current/resistance results.
			String trimmedData = data.substring(3, data.length());
			if (!isValidText(trimmedData)) {
				failedToDecode = true;
			}

			double measurementDataValue = 0D;
			try {
				measurementDataValue = Double.parseDouble(trimmedData);
			} catch (NumberFormatException e) {

				// If data received from the serial connection was not the right type
				failedToDecode = true;
			}

			if (!failedToDecode) {
				// RECORD AND DISPLAY NEW RESULTS OF TIME/ETC, ETC
				String unit = Character.toString(data.charAt(0));

				GuiController.instance.recordAndDisplayNewResult(measurementDataValue, unit);
			} else {
				System.out.println("Failed to decode data.");
				// + serialBuffer.substring(openPacket, closePacket + 1) + "\"");
			}
		}

		/**
		 * Indicates what event types to listen for.
		 */
		@Override
		public int getListeningEvents() {
			return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
		}
	}
}
