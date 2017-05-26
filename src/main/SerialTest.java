package main;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import com.fazecast.jSerialComm.SerialPort;

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
public class SerialTest {

	private static ObservableList<String> portNames = FXCollections.observableArrayList();

	private static SerialPort openSerialPort = null;
	// private static BufferedReader readFromSerial = null;
	private static InputStream readFromSerial = null;
	private static PrintWriter writeToSerial = null;

	private static boolean isChecked = false; // Two-way connection has been checked

	private static SerialDataListener listener = new SerialDataListener();

	public static InputStream getReadFromSerial() {
		return readFromSerial;
	}

	public static void setReadFromSerial(InputStream receivedValue) {
		readFromSerial = receivedValue;
	}

	public static void setIsChecked(boolean newValue) {
		isChecked = newValue;
	}

	/**
	 * Closes open serial port, if it is still open.
	 */
	public static void closeOpenPort() {
		isChecked = false;

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
			System.out.println("...closing openSerialport");
			openSerialPort = null;
		}

		// Close buffered reader
		if (readFromSerial != null) {
			try {
				readFromSerial.close();
				System.out.println("...losing readFromSerialReader");
			} catch (IOException e) {
				e.printStackTrace();
			}

			readFromSerial = null;
		}

		// Close printer writer
		if (writeToSerial != null) {
			writeToSerial.close();
			System.out.println("...closing writeToSerial");

			writeToSerial = null;
		}
	}

	// FIXME: TEST FOR TWO-WAY
	/**
	 * Handles the changing of the serial port selection. If there are ports (with valid names) it binds the serial port
	 * (refreshes the port list if it didn't bind); otherwise is closes the open port.
	 */
	public static void selectPort() {
		System.out.println("Changed to this port: " + GuiController.instance.portsAvailable.getValue());

		SerialPort[] ports = SerialPort.getCommPorts();
		if (GuiController.instance.portsAvailable.getValue() != null
				&& !GuiController.instance.portsAvailable.getValue().equalsIgnoreCase("")) {
			for (SerialPort serialPort : ports) {

				// Within the list of ports, check the selected port
				if (serialPort.getDescriptivePortName().equals(GuiController.instance.portsAvailable.getValue())) {
					System.out.println("Opening Serial Port " + serialPort.getSystemPortName() + "...");

					// Check if the port was opened
					if (checkOpenPort(serialPort)) {
						System.out.println("Success.");

						if (checkConnection()) { // CHECK THERE'S DATA + TWO-WAY CONNECTION
							GuiController.instance.setConnectedModeStatus(false); // Enable
						} else {
							System.out.println("Failed to receive data from port");
							GuiController.instance.setConnectedModeStatus(true); // Disable
						}

						return;

					} else {
						System.out.println("Failed to open port.");
						GuiController.instance.setConnectedModeStatus(true); // Disable
						return;
					}
				}
			}

			refreshSelectablePortsList(); // PROBATION
		}
	}

	/**
	 * A private helper function to 'selectPort', which opens the given port.
	 * 
	 * @param serialPort
	 *            the port that needs to be opened
	 * @return whether or not the serial port was opened successfully (true it did, false it didn't)
	 */
	private static boolean checkOpenPort(SerialPort serialPort) {
		closeOpenPort(); // Close any previously opened ports

		if (!(serialPort.openPort() && bindListen(serialPort))) {
			return false; // Port wasn't successful at opening and adding a listener
		}

		openSerialPort = serialPort;

		return true;
	}

	private static boolean bindListen(SerialPort serialPort) {
		System.out.println("Binding to Serial Port " + serialPort.getSystemPortName() + "...");

		serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);
		// serialPort.setBaudRate(9600);

		// Add the data listener to the data. [also loads in the data]
		if (!serialPort.addDataListener(listener)) {
			serialPort.closePort();
			return false;
		}

		return true;
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
			System.out.println("....writing to port");
			writeToSerial.close();
		} catch (NullPointerException e) {
			System.err.println("Port Cannot Be Written To");
		}
	}

	private static boolean checkConnection() {
		System.out.println("...Checking connection");

		long initialTime = System.nanoTime(); // current time
		long time = 0;
		double timeOut = 5e+8; // 1/2 a second
		writeCode("|C|");

		// FIXME: THERE STILL ARE DELAYS HERE
		while (time < timeOut) {
			time = System.nanoTime() - initialTime;
		}

		// need to check value
		if (isChecked) {
			System.out.println("CONNECTION");
			return true;
		} else {
			System.out.println("NO TWO-WAY");
			return false;
		}

	}

	/**
	 * Refreshes the existing ports list to include any new ports detected, as well as closes any open ports.
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

		closeOpenPort(); // Close any open ports
		GuiController.instance.setConnectedModeStatus(true); // Disable connected mode components

		portNames.clear();

		SerialPort[] ports = SerialPort.getCommPorts();

		int portCounter = 0;
		for (SerialPort serialPort : ports) {
			portNames.add(serialPort.getDescriptivePortName());
			portCounter++;
		}

		GuiController.instance.portsAvailable.setItems(portNames);
		GuiController.instance.portsAvailable.setVisibleRowCount(portCounter + 1);
	}
}
