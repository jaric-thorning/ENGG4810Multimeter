package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStreamReader;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * A class which handles serial comms.
 */
public class SerialFramework {

	private static ObservableList<String> portNames = FXCollections.observableArrayList();
	// final static String INIT_PORT_SELECTION = "None"; // Default first element
	private static SerialPort openSerialPort = null;
	private static BufferedReader readFromSerial = null;
	private static PrintWriter writeToSerial = null;

	public static SerialPort getOpenSerialPort() {
		return openSerialPort;
	}

	// FIXME: while haven't received change, keep sending code
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
	 * Resets the selected port to default first element (None).
	 */
	// public static void resetSelectablePortsList() {
	// if (portNames.size() >= 1)
	// //GuiController.getInstance().portsAvailable.setValue(portNames.get(0));
	// }

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

		SerialPort[] ports = SerialPort.getCommPorts();

		for (SerialPort serialPort : ports) {
			portNames.add(serialPort.getDescriptivePortName());
		}
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
			openSerialPort = null;
		}

		// FIXME:
		if (readFromSerial != null) {
			try {
				readFromSerial.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			readFromSerial = null;
		}

		if (writeToSerial != null) {
			writeToSerial.close();
			writeToSerial = null;
		}

	}

	/**
	 * Handles the changing of the serial port selection. If there are ports (with valid names) it
	 * binds the serial port (refreshes the port list if it didn't bind); otherwise is closes the
	 * open port.
	 */
	public static void selectPort() {
		// if (GuiController.getInstance().portsAvailable.getValue() != null
		// && !GuiController.getInstance().portsAvailable.getValue().equalsIgnoreCase("")
		// && !GuiController.getInstance().portsAvailable.getValue().equals(INIT_PORT_SELECTION)) {
		// System.out.println("Changed to this port: " +
		// GuiController.getInstance().portsAvailable.getValue());

		SerialPort[] ports = SerialPort.getCommPorts();
		for (SerialPort serialPort : ports) {
			System.out.println("PORTS: " + serialPort);
			if (serialPort.getSystemPortName().contains("tty.usbmodem")) {// serialPort.getDescriptivePortName()equals(GuiController.getInstance().portsAvailable.getValue()))
																			// {
				System.out.println(
						"Binding to Serial Port " + serialPort.getSystemPortName() + "...");
				if (bindListen(serialPort)) {
					System.out.println("Success.");
				} else {
					System.out.println("Failed to bind to Serial.");
					refreshSelectablePortsList();
				}
				return;
			}
		}
		System.out.println("Invalid Serial!");
		refreshSelectablePortsList();

		// else {
		// closeOpenPort();
		// System.out.println("Not a port - close any open ports");
		// }
	}

	/**
	 * A private helper function for changePorts(), which binds to the serial port.
	 * 
	 * @param serialPort
	 *            the port that needs to be binded to.
	 * @return whether or not the serial port was binded to successfully (true if it did, false if
	 *         if didn't).
	 */
	static boolean bindListen(SerialPort serialPort) {
		closeOpenPort();
		if (!serialPort.openPort()) {
			return false;
		}

		// Add the data listener to the data. [also loads in the data]
		if (!serialPort.addDataListener(new SerialFramework.SerialDataListener())) {
			serialPort.closePort();
			return false;
		}

		openSerialPort = serialPort;
		// Ensure that a read call always returns at least 1 byte of valid data
		openSerialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);
		openSerialPort.setBaudRate(9600);

		return true;
	}

	/**
	 * A class used for binding the serial port, and getting data from it.
	 */
	private static class SerialDataListener implements SerialPortDataListener {
		private static final String PACKET_OPEN = "[";
		private static final String PACKET_CLOSE = "]";
		private static final char MODE = 'M';

		/** The buffer for building whole packets received over the serial port. */
		private String serialBuffer = "";

		/** Whether the port connection has bugged out. */
		private boolean errored = false;

		/**
		 * Handles an event on the serial port. Reads the data and displays it.
		 */
		@Override
		public void serialEvent(SerialPortEvent sEvent) {
			if (this.errored)
				return;

			// Reading data
			if (sEvent.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
				if (sEvent.getSerialPort().bytesAvailable() < 0) {
					this.errored = true;
					System.err.println("Error 3. reading from port");
					closeOpenPort();
					// GuiController.getInstance().refreshPorts();
					return;
				}

				// Read by line
				try {
					readFromSerial = new BufferedReader(
							new InputStreamReader(sEvent.getSerialPort().getInputStream()));
					getData(readFromSerial.readLine());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					// e.printStackTrace();
					System.err.println("IO Exception, no bytes");
				}
			}
		}

		/**
		 * Checks the validity of a supplied string. To be used in getData(), as a means to
		 * determine whether the data has been decoded successfully.
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

			if (data.charAt(0) == 'S') { // Change multimeter settings
				//System.out.println("------SETTING: " + data);
				// System.out.println("THIS IS S: " + data.charAt(0));

				sortMultimeterCommand(data, openPacket, closePacket);
			} else if (data.charAt(0) == 'V' || data.charAt(0) == 'C' || data.charAt(0) == 'R') {
				//System.out.println("	DATA: " + data);
				// Check for multimeter results.
				sortMultimeterMeasurements(data, openPacket, closePacket);
			}

			text = text.substring(closePacket + 1); // not sure about this
			return true;
		}

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

			// If the next bits of data were not matching to any measurement type
			if (!(trimmedData.charAt(2) == 'C' || trimmedData.charAt(2) == 'V'
					|| trimmedData.charAt(2) == 'R')) {
				failedToDecode = true;
			} else {
				modeType = trimmedData.charAt(2);
			}

			// CHANGE MODE TO EITHER RESISTANCE, VOLTAGE, CURRENT
			if (!failedToDecode) {
				switch (modeType) {
				case 'C':
					//System.out.println("CHANGE SETTINGS -> MODE -> CURRENT");
					GuiController.instance.driveCurrent();
					break;
				case 'V':
					GuiController.instance.driveVoltage();
					break;
				case 'R':
					GuiController.instance.driveResistance();
					break;
				default:
					// FIXME: INVALID THING HERE
					// failedToDecode = true;
					break;
				}
			} else {
				System.out.println("******Failed to decode data:\""
						+ serialBuffer.substring(openPacket, closePacket + 1) + "\"");
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
