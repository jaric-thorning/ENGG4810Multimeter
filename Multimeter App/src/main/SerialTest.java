package main;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fazecast.jSerialComm.SerialPort;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * The SerialTest class handles serial communications between
 * software/hardware/firmware. NOTE: The concept of binding to the port, closing
 * the ports and refreshing the ports is modified off TP1 code which my team
 * member originally wrote.
 * 
 * @modifier/@author dayakern
 *
 */
public class SerialTest {
	private static final int BAUD_RATE = 38400;

	private ObservableList<String> portNames;
	private SerialPort openSerialPort;
	private InputStream readFromSerial;
	private PrintWriter writtenToSerial;

	// A flag for two-way connection
	private boolean isChecked;

	// Listens for available data
	private SerialDataListener dataListener;

	public SerialTest(AtomicBoolean quit) {
		this.dataListener = new SerialDataListener(quit, this);

		readFromSerial = null;
		openSerialPort = null;
		writtenToSerial = null;

		isChecked = false;

		portNames = FXCollections.observableArrayList();
	}

	/**
	 * Gets the 'readFromSerial' input stream.
	 *
	 * @return the input stream the data will be flowing into
	 */
	public InputStream getReadFromSerial() {
		return readFromSerial;
	}

	/**
	 * Sets the 'readFromSerial' to a new value.
	 *
	 * @param newValue
	 *            the value which the input stream will have
	 */
	public void setReadFromSerial(InputStream newValue) {
		readFromSerial = newValue;
	}

	/**
	 * Sets the value 'isChecked' to a new value.
	 * 
	 * @param newValue
	 *            whether or not there's a two-way connection ( true if there's
	 *            a two-way connection, false otherwise)
	 */
	public void setIsChecked(boolean newValue) {
		isChecked = newValue;
	}

	/**
	 * Gets the 'isChecked' boolean value.
	 * 
	 * @return the value of the two-way flag
	 */
	public boolean getIsChecked() {
		return isChecked;
	}

	/**
	 * Closes open serial port, if it is still open.
	 */
	public void closeOpenPort() {
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

		// Close open port
		if (openSerialPort != null) {
			openSerialPort.closePort();
			System.out.println("...closing openSerialport");
			openSerialPort = null;
		}

		// Close input stream reader
		if (readFromSerial != null) {
			try {
				readFromSerial.close();
				System.out.println("...closing readFromSerialReader");
			} catch (IOException e) {
				e.printStackTrace();
			}

			readFromSerial = null;
		}

		// Close printer writer
		if (writtenToSerial != null) {
			writtenToSerial.close();
			System.out.println("...closing writeToSerial");

			writtenToSerial = null;
		}
	}

	/**
	 * Handles the changing of the serial port selection. If there are ports
	 * (with valid names) it binds the serial port (refreshes the port list if
	 * it didn't bind); otherwise is closes the open port.
	 */
	public void selectPort() {
		System.out.println("Changed to this port: " + GuiController.instance.portsAvailable.getValue());

		SerialPort[] ports = SerialPort.getCommPorts();
		if (GuiController.instance.portsAvailable.getValue() != null
				&& !GuiController.instance.portsAvailable.getValue().equalsIgnoreCase("")) {
			for (SerialPort serialPort : ports) {

				// Within the list of ports, check the selected port
				if (serialPort.getDescriptivePortName().equals(GuiController.instance.portsAvailable.getValue())) {
					System.out.println("Opening Serial Port " + serialPort.getSystemPortName() + "...");

					// Check if the port was opened and if it binded to the
					// listener
					if (checkOpenPort(serialPort)) {
						System.out.println("Success.");
						
						// Left as enabled for now
						GuiController.instance.setConnectedModeComponents(false);

						// TODO: send the check bit code, and check a second later if
						// it's been received repeat this every minute
						return;

					} else {
						System.out.println("Failed to open port.");

						// Disable components
						GuiController.instance.setConnectedMultimeterComponents(true);
						return;
					}
				}
			}

			refreshSelectablePortsList();
		}
	}

	/**
	 * A private helper function to 'selectPort' which opens the given port and
	 * adds a data listener to the port.
	 * 
	 * @param serialPort
	 *            the port that will be opened
	 * @return whether or not the serial port was opened and had a listener
	 *         binded to it successfully (true it did, false it didn't)
	 */
	private boolean checkOpenPort(SerialPort serialPort) {
		closeOpenPort(); // Close any previously opened ports

		if (!(serialPort.openPort() && bindListen(serialPort))) {

			return false; // Port wasn't successful at opening and adding a
							// listener
		}

		openSerialPort = serialPort;

		return true;
	}

	/**
	 * A private helper function to 'checkOpenPort' which binds a listener to
	 * the serial port.
	 * 
	 * @param serialPort
	 *            the port that needs to be binded to
	 * @return whether or not the serial port was binded to successfully (true
	 *         if it did, false if if didn't)
	 */
	private boolean bindListen(SerialPort serialPort) {
		System.out.println("Binding to Serial Port " + serialPort.getSystemPortName() + "...");

		serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);
		serialPort.setBaudRate(BAUD_RATE);

		// Add the data listener to the data (also loads in the data)
		if (!serialPort.addDataListener(dataListener)) {
			serialPort.closePort();
			return false;
		}

		return true;
	}

	/**
	 * Writes out specified code to remotely control the multimeter.
	 * 
	 * @param output
	 *            the code to write out
	 */
	public void writeCode(String output) {
		try {

			// Write multimeter code over the open port
			writtenToSerial = new PrintWriter(openSerialPort.getOutputStream(), true);
			writtenToSerial.println(output);

			System.out.println("....writing to port");
			writtenToSerial.close();
		} catch (NullPointerException e) {
			System.err.println("Port Cannot Be Written To");
		}
	}

	/**
	 * Refreshes the existing ports list to include any new ports detected, as
	 * well as closes any open ports.
	 */
	public void refreshSelectablePortsList() {
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

		// Disable connected mode components
		GuiController.instance.setConnectedMultimeterComponents(true);
		GuiController.instance.setConnectedModeComponents(true);
		portNames.clear();

		SerialPort[] ports = SerialPort.getCommPorts();

		int portCounter = 0;
		for (SerialPort serialPort : ports) {
			portNames.add(serialPort.getDescriptivePortName());
			portCounter++;
		}

		GuiController.instance.portsAvailable.setItems(portNames);
		GuiController.instance.portsAvailable.setVisibleRowCount(portCounter + 1);
		
		// Just to have the controls already set.
		GuiController.instance.setConnectedMultimeterComponents(false);
		GuiController.instance.setConnectedModeComponents(false);
	}
}
