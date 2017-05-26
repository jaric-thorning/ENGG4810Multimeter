package main;

import java.io.IOException;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

/**
 * The SerialDataListener class is used for binding to the currently open serial port, and getting data from it. NOTE:
 * The concept of a separate class for the 'SerialPortDataListener' is modified off TP1 code which my team member
 * originally wrote.
 * 
 * @modifier/@author dayakern
 *
 */
public class SerialDataListener implements SerialPortDataListener {
	private static final String PACKET_OPEN = "|";

	private static final String OHM_SYMBOL = Character.toString((char) 8486);
	private static final String PLUS_MINUS_SYMBOL = Character.toString((char) 177);

	private static final char MODE = 'M';

	// Whether the port connection has bugged out.
	private boolean errored = false;

	/**
	 * Handles a listening event (LISTENING_EVENT_DATA_AVAILABLE) on the serial port. Reads the data and displays it.
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
				System.err.println("Error reading from port");
				SerialTest.closeOpenPort();

				return;
			}

			// Getting the data from input stream
			char s = 0;
			StringBuilder input = new StringBuilder();

			try {
				SerialTest.setReadFromSerial(sEvent.getSerialPort().getInputStream());

				// FIXME: make a bit more robust
				while ((s = (char) SerialTest.getReadFromSerial().read()) != '|') {
					input.append(s);

					if (input.length() > 20) {
						input = new StringBuilder();
					}
				}

				getData(input.toString());

				SerialTest.getReadFromSerial().close();
			} catch (IOException e) {

				this.errored = true;
				System.err.println("Error reading from port");
				SerialTest.closeOpenPort();

				return;
			}
		}
	}

	/**
	 * Gets the data received from the input stream and uses it for it's intended purpose (i.e. multimeter display
	 * text).
	 * 
	 * @param receivedData
	 *            the data received from the input stream that's been stitched into a String
	 */
	private void getData(String receivedData) {

		// Check if there's two-way connection
		receivedTwoWayCheck(receivedData);

		if (receivedData.length() > 4) { // FIXME: Change this
			if (receivedData.charAt(0) == 'D') { // Change multimeter display

				updateMultimeterDisplay(receivedData.substring(1));
			} else if (receivedData.charAt(0) == 'V' || receivedData.charAt(0) == 'I'
					|| receivedData.charAt(0) == 'R') { // Change values received

				sortMultimeterMeasurements(receivedData);
			} else if (receivedData.charAt(0) == 'S') {// Change multimeter settings
				System.out.println("RECEIVING CODES");
				// FIXME
				// sortMultimeterCommand(receivedData);
			}

			System.err.println(receivedData);
		}
	}

	/**
	 * A private helper function for 'getData' which determines if the given String matches the two-way connection code.
	 * 
	 * @param receivedData
	 *            the data received from the input stream that's been stitched into a String
	 */
	private void receivedTwoWayCheck(String receivedData) {
		boolean failedToDecode = determineValidText(receivedData);

		if (!failedToDecode) {
			if (receivedData.length() == 1 && receivedData.equals("C")) {
				SerialTest.setIsChecked(true);
			}
		}
	}

	/**
	 * A private helper function for 'getData' which updates the multimeter display iff the given String is valid.
	 * 
	 * @param receivedData
	 *            the data received from the input stream that's been stitched into a String
	 */
	private void updateMultimeterDisplay(String receivedData) {
		boolean failedToDecode = determineValidText(receivedData);

		if (!failedToDecode) {

			if (receivedData.charAt(0) == '1' && !GuiController.instance.multimeterDisplay.isDisabled()) {
				receivedData = receivedData.replace("=", PLUS_MINUS_SYMBOL);
				GuiController.instance.multimeterDisplay.setText(receivedData.substring(2).trim() + "\n");
			} else if (receivedData.charAt(0) == '2' && !GuiController.instance.multimeterDisplay.isDisabled()) {
				receivedData = receivedData.replace(";", OHM_SYMBOL);

				GuiController.instance.multimeterDisplay.appendText(receivedData.substring(2).trim());
			} else {
				failedToDecode = true;
			}

		} else {
			System.err.println("***Failed to decode data: " + receivedData);
		}
	}

	/**
	 * A private helper function for 'getData' which updates the graph display iff the given String is valid.
	 * 
	 * @param receivedData
	 *            the data received from the input stream that's been stitched into a String
	 */
	private void sortMultimeterMeasurements(String receivedData) {

		boolean failedToDecode = determineValidText(receivedData);

		// Check for voltage/current/resistance results.
		String trimmedData = receivedData.substring(3, receivedData.length()).trim();

		double measurementDataValue = 0D;
		try {
			measurementDataValue = Double.parseDouble(trimmedData);// trimmedData.substring(3).trim());
		} catch (NumberFormatException e) {

			// If data received from the serial connection was not the right type
			failedToDecode = true;
		}

		if (!failedToDecode) {

			// Record and display updated results
			String unit = Character.toString(receivedData.charAt(0));
			System.err.println(measurementDataValue);
			GuiController.instance.recordAndDisplayNewResult(measurementDataValue, unit);
		} else {
			System.out.println("Failed to decode data:" + measurementDataValue);
		}
	}

	/**
	 * A private helper function for 'getData' which checks that the command to control the multimeter is valid and
	 * executes it
	 * 
	 * @param receivedData
	 *            the data received from the input stream that's been stitched into a String
	 */
	private void sortMultimeterCommand(String receivedData) {

		// Check for multimeter display commands.
		String trimmedData = receivedData.substring(2, receivedData.length());

		boolean failedToDecode = determineValidText(trimmedData);

		char modeType = 0;

		System.err.println("H: " + trimmedData);

		// // If data received from the serial connection was not the right type
		// if (trimmedData.charAt(0) != MODE)
		// failedToDecode = true;
		//
		// // If the next bits of data were not matching to any mode type
		// if (!(trimmedData.charAt(2) == 'I' || trimmedData.charAt(2) == 'V' || trimmedData.charAt(2) == 'R'
		// || trimmedData.charAt(2) == 'C' || trimmedData.charAt(2) == 'L')) {
		// failedToDecode = true;
		// } else {
		// modeType = trimmedData.charAt(2);
		// }
		//
		// // Change the mode to either resistance, current, voltage, logic or continuity
		// if (!failedToDecode) {
		// switch (modeType) {
		// case 'I':
		// GuiController.instance.driveCurrent();
		// break;
		// case 'V':
		// GuiController.instance.driveVoltage();
		// break;
		// case 'R':
		// GuiController.instance.driveResistance();
		// break;
		// case 'C':
		// GuiController.instance.driveContinuity();
		// break;
		// case 'L':
		// GuiController.instance.driveLogic();
		// break;
		// default:
		// failedToDecode = true;
		// break;
		// }
		// } else {
		// System.out.println("******Failed to decode data:" + trimmedData);
		// }
	}

	/**
	 * A private helper function to 'determineValidText' which determines if the data received in valid.
	 * 
	 * @param receivedData
	 *            the data received from the input stream that's been stitched into a String
	 * @return whether or not the string is valid (true if it is valid, false if it isn't)
	 */
	private boolean isValidText(String receivedData) {
		return receivedData != null && !receivedData.equalsIgnoreCase("");
	}

	/**
	 * Determines the validity of the received data
	 * 
	 * @param receivedData
	 *            the data received from the input stream that's been stitched into a String
	 * @return true if it's not valid, false otherwise
	 */
	private boolean determineValidText(String receivedData) {
		if (!isValidText(receivedData)) {
			return true;
		}

		return false;
	}

	@Override
	public int getListeningEvents() {
		return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
	}
}
