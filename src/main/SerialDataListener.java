package main;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

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
	private static final String PACKET_BOUNDARIES = "|";
	private static final String PACKET_OPEN = "[";
	private static final String PACKET_CLOSE = "]";

	private static final String OHM_SYMBOL = Character.toString((char) 8486);
	private static final String PLUS_MINUS_SYMBOL = Character.toString((char) 177);

	// Whether the port connection has bugged out.
	private boolean errored = false;
	private String firstDisplay = "";
	private String secondDisplay = "";

	private AtomicBoolean quit;

	private SerialTest serialTest;

	public SerialDataListener(AtomicBoolean quit, SerialTest serialTest) {
		this.quit = quit;
		this.serialTest = serialTest;
	}

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
				serialTest.closeOpenPort();

				return;
			}

			// Getting the data from input stream
			char s = 0;
			StringBuilder input = new StringBuilder();
			serialTest.setReadFromSerial(sEvent.getSerialPort().getInputStream());

			try { // Thread.sleep(20);
					// FIXME: won't quit while -> closeport
				while ((s = (char) serialTest.getReadFromSerial().read()) != 0 && !quit.get()) {

					input.append(s);

					if (s == '\n') {
						String line = input.toString().trim();

						getData(line);
						input = new StringBuilder();
					}
				}

				serialTest.getReadFromSerial().close();
			} catch (Exception e) {
				this.errored = true;
				System.err.println("Error reading from port");
				serialTest.closeOpenPort();

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

		if (isValidText(receivedData) && !checkReceivedDataEnds(receivedData)) {
			//System.out.println("\"" + receivedData + "\"");

			if (receivedData.charAt(1) == 'D') { // Change multimeter display

				updateMultimeterDisplay(receivedData.substring(2));
			} else if (receivedData.charAt(1) == 'V' || receivedData.charAt(1) == 'W' || receivedData.charAt(1) == 'I'
					|| receivedData.charAt(1) == 'J' || receivedData.charAt(1) == 'R') { // Change values received
				
				sortMultimeterMeasurements(receivedData.substring(1));
			} else {
				// IGNORE
			}
		} else if (isValidText(receivedData) && !checkReceivedCommands(receivedData)) {

			// Check if there's two-way connection
			if (receivedData.length() == 3 && receivedData.substring(1, 1).equals("C")) {
				serialTest.setIsChecked(true);
			} else if (receivedData.charAt(1) == 'S') {// Change multimeter settings

				sortMultimeterCommand(receivedData);
			} else {
				System.err.println(".....something else");
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
		boolean failedToDecode = false;

		if (!failedToDecode) {

			if (receivedData.charAt(0) == '1' && !GuiController.instance.multimeterDisplay.isDisabled()) {
				receivedData = receivedData.replace("=", PLUS_MINUS_SYMBOL);

				firstDisplay = receivedData.substring(2, receivedData.length() - 1).trim() + "\n";
			} else if (receivedData.charAt(0) == '2' && !GuiController.instance.multimeterDisplay.isDisabled()) {
				receivedData = receivedData.replace(";", OHM_SYMBOL);

				secondDisplay = receivedData.substring(2, receivedData.length() - 1).trim() + "\n";
			} else {
				failedToDecode = true;
			}
			// System.out.println("\"" + firstDisplay + "\"");
			// System.out.println("\"" + secondDisplay + "\"");

			// Update Multimeter display iif the values for the top and bottom lines have been found
			if (!firstDisplay.isEmpty() && !secondDisplay.isEmpty()) {
				GuiController.instance.multimeterDisplay.setText(firstDisplay + secondDisplay);
				firstDisplay = "";
				secondDisplay = "";
			}

		} else {
			System.err.println("***Failed to decode data: " + receivedData);
		}
	}

	/**
	 * Check that the first and last values of the received data has the expected value.
	 * 
	 * @param receivedData
	 *            the data received from the input stream that's been stitched into a String
	 * @return false if the received data is valid, true if it isn't
	 */
	private boolean checkReceivedDataEnds(String receivedData) {
		return !receivedData.startsWith(PACKET_BOUNDARIES) && !receivedData.endsWith(PACKET_BOUNDARIES);
	}

	/**
	 * Check that the first and last values of the received data has the expected value.
	 * 
	 * @param receivedData
	 *            the data received from the input stream that's been stitched into a String
	 * @return false if the received data is valid, true if it isn't
	 */
	private boolean checkReceivedCommands(String receivedData) {
		return !receivedData.startsWith(PACKET_OPEN) && !receivedData.endsWith(PACKET_CLOSE);
	}

	/**
	 * A private helper function for 'getData' which updates the graph display iff the given String is valid.
	 * 
	 * @param receivedData
	 *            the data received from the input stream that's been stitched into a String
	 */
	private void sortMultimeterMeasurements(String receivedData) {

		boolean failedToDecode = false;
		double measurementDataValue = 0D;

		if (!failedToDecode) {

			// Check for voltage/current/resistance results.
			try {
				measurementDataValue = Double.parseDouble(receivedData.substring(3, receivedData.length() - 1).trim());
			} catch (NumberFormatException e) {

				// If data received from the serial connection was not the right type
				failedToDecode = true;
			}

			if (!failedToDecode) {
				String unit = "";

				unit = Character.toString(receivedData.charAt(0));

				if (receivedData.charAt(0) == 'W' || receivedData.charAt(0) == 'J') {
					GuiController.instance.driveACDCMode();
				}
				// Record and display updated results
				GuiController.instance.recordAndDisplayNewResult(measurementDataValue, unit);
			} else {
				System.err.println("Failed to decode data:" + measurementDataValue);
			}
		} else {
			System.err.println("***Failed to decode data: " + receivedData);
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

		boolean failedToDecode = !isValidText(trimmedData);

		char modeType = 0;

		// System.err.println("H: " + trimmedData);

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

	@Override
	public int getListeningEvents() {
		return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
	}
}
