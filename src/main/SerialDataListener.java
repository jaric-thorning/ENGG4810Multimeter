package main;

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

	private static final String OHM_SYMBOL = Character.toString((char) 8486);
	private static final String PLUS_MINUS_SYMBOL = Character.toString((char) 177);

	// Whether the port connection has bugged out.
	private boolean errored;
	private String firstDisplay;
	private String secondDisplay;

	private AtomicBoolean quit;

	private SerialTest serialTest;

	public SerialDataListener(AtomicBoolean quit, SerialTest serialTest) {
		this.quit = quit;
		this.serialTest = serialTest;

		errored = false;
		firstDisplay = "";
		secondDisplay = "";
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

			try {

				while ((s = (char) serialTest.getReadFromSerial().read()) != 0 && !quit.get()) {

					input.append(s);

					if (s == '\n') {
						String line = input.toString().trim();

						// System.err.println("\"" + line + "\"");

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

			// TODO: add received C
			// TODO: check for frequency
			// TODO: check for brightness
			if (receivedData.charAt(1) == 'D') { // Change multimeter display

				updateMultimeterDisplay(receivedData.substring(2));
			} else if (receivedData.charAt(1) == 'V' || receivedData.charAt(1) == 'W' || receivedData.charAt(1) == 'I'
					|| receivedData.charAt(1) == 'J' || receivedData.charAt(1) == 'R' || receivedData.charAt(1) == 'L'
					|| receivedData.charAt(1) == 'C') { // Change values received

				sortMultimeterMeasurements(receivedData.substring(1));
			} else if (receivedData.charAt(1) == 'F') {

				selectBrightnessPercentage(receivedData.substring(1));
			} else {
				// IGNORE
			}
		}
	}

	/**
	 * A private helper function for 'getData' which selects the brightness display iff the given String is valid.
	 * 
	 * @param receivedData the data received from the input stream that's been stitched into a String
	 */
	private void selectBrightnessPercentage(String receivedData) {

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

				setupLogicContinuityBehaviours(unit);

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
	 * Sets up certain behaviours when in received Continuity/Logic values and when not
	 * 
	 * @param unit
	 *            the received unit of the data
	 */
	private void setupLogicContinuityBehaviours(String unit) {

		// Make sure plot looks OK.
		if (unit.equals("L") || unit.equals("C")) {
			GuiController.instance.yAxis.setAutoRanging(false);
			GuiController.instance.yAxis.setUpperBound(1.5D);
			GuiController.instance.yAxis.setLowerBound(-1.5D);
			GuiController.instance.yAxis.setMinorTickCount(10);
			GuiController.instance.yAxis.setTickUnit(5D);

		} else { // when not in continuity/logic behaviour

			// Update AC/DC switch buttons
			determineACDCMode(unit);
			GuiController.instance.yAxis.setAutoRanging(true);
		}
	}

	/**
	 * Changes the displayed value of the AC/DC to the opposite of what it's currently receiving.
	 * 
	 * @param unit
	 *            the received unit of the data
	 */
	private void determineACDCMode(String unit) {
		if (unit.equals("W") || unit.equals("J")) {
			GuiController.instance.driveACMode();
		} else {
			GuiController.instance.driveDCMode();
		}
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
