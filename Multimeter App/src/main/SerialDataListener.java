package main;

import java.util.concurrent.atomic.AtomicBoolean;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

/**
 * The SerialDataListener class is used for binding to the currently open serial
 * port, and getting data from it. NOTE: The concept of a separate class for the
 * 'SerialPortDataListener' is modified off TP1 code which my team member
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
	private String line;

	private SerialTest serialTest;

	public SerialDataListener(AtomicBoolean quit, SerialTest serialTest) {
		this.quit = quit;
		this.serialTest = serialTest;

		errored = false;
		firstDisplay = "";
		secondDisplay = "";
		line="";
	}

	/**
	 * Handles a listening event (LISTENING_EVENT_DATA_AVAILABLE) on the serial
	 * port. Reads the data and displays it.
	 */
	@Override
	public void serialEvent(SerialPortEvent sEvent) {
		if (this.errored) {
			return;
		}

		// Reading data
		if (sEvent.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {

			// Getting the data from input stream
			char s = 0;
			StringBuilder input = new StringBuilder();
			serialTest.setReadFromSerial(sEvent.getSerialPort().getInputStream());

			try {
				while ((s = (char) serialTest.getReadFromSerial().read()) != -1 && !quit.get()) {
					input.append(s);

					// Reset the string if number of characters exceed 50.
					if (input.toString().trim().length() > 50) {
						input = new StringBuilder();
					}

					// If the string character is equal to a newline,
					// check the value of the combined characters up to that
					// point without the newline
					if (s == '\n') {
						line = input.toString().trim();
						
						// No check here for the time being
						System.err.println("\"" + line + "\"");
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
	 * Checks if the data received is the two-way check
	 * 
	 * @param receivedData
	 *            the data received from the input stream that's been stitched
	 *            into a String
	 */
	private void checkData(String receivedData) {
		
		if (isValidText(receivedData)) { // 5 -> [,C, ,C,]
			if (receivedData.charAt(1) == 'C' && receivedData.charAt(3) == 'C' && receivedData.length() == 5) {
				serialTest.setIsChecked(true);
				GuiController.instance.setConnectedMultimeterComponents(false);
			}
		}
	}

	/**
	 * Gets the data received from the input stream and uses it for it's
	 * intended purpose (i.e. multimeter display text).
	 * 
	 * @param receivedData
	 *            the data received from the input stream that's been stitched
	 *            into a String
	 */
	private void getData(String receivedData) {

		if (isValidText(receivedData) && !checkReceivedDataEnds(receivedData)) {
			if (receivedData.charAt(1) == 'D') {

				// Change multimeter display
				updateMultimeterDisplay(receivedData.substring(2));
			} else if (receivedData.charAt(1) == 'V' || receivedData.charAt(1) == 'W' || receivedData.charAt(1) == 'I'
					|| receivedData.charAt(1) == 'J' || receivedData.charAt(1) == 'R' || receivedData.charAt(1) == 'L'
					|| receivedData.charAt(1) == 'C') {

				// Change values received
				sortMultimeterMeasurements(receivedData.substring(1));
			} else if (receivedData.charAt(1) == 'B') {

				// Change brightness levels
				selectBrightnessPercentage(receivedData.substring(1));
			} else if (receivedData.charAt(1) == 'F') {

				// Change frequency rate
				selectFrequencyRate(receivedData.substring(1));
			} else {
				// Nothing
				checkData(receivedData);
			}
		}
	}

	/**
	 * A private helper function for 'getData' which selects the frequency rate
	 * iff the given String is valid.
	 * 
	 * @param receivedData
	 *            the data received from the input stream that's been stitched
	 *            into a String
	 */
	private void selectFrequencyRate(String receivedData) {
		boolean failedToDecode = false;
		int frequencyRate = 0;

		if (!failedToDecode) {

			// Get frequency rates
			try {
				frequencyRate = Integer.parseInt(receivedData.substring(3, receivedData.length() - 1).trim());
			} catch (NumberFormatException e) {

				// If data received from the serial connection was not the right
				// type
				failedToDecode = true;
			}

			// Check for valid index number
			failedToDecode = !(0 <= frequencyRate && frequencyRate < 9);

			if (!failedToDecode) {

				// Change brightness levels
				GuiController.instance.updateFrequencyRate(frequencyRate);
			}
		} else {
			System.err.println("***Failed to decode data: " + receivedData);
		}
	}

	/**
	 * A private helper function for 'getData' which selects the brightness
	 * display iff the given String is valid.
	 * 
	 * @param receivedData
	 *            the data received from the input stream that's been stitched
	 *            into a String
	 */
	private void selectBrightnessPercentage(String receivedData) {
		boolean failedToDecode = false;
		int brightnessLevel = 0;

		if (!failedToDecode) {

			// Get brightness level
			try {
				brightnessLevel = Integer.parseInt(receivedData.substring(3, receivedData.length() - 1).trim());
			} catch (NumberFormatException e) {

				// If data received from the serial connection was not the right
				// type
				failedToDecode = true;
			}

			// Check for valid index number
			failedToDecode = !(0 <= brightnessLevel && brightnessLevel <= 4);

			if (!failedToDecode) {

				// Change brightness levels
				GuiController.instance.updateBrightnessLevel(brightnessLevel);
			}
		} else {
			System.err.println("***Failed to decode data: " + receivedData);
		}
	}

	/**
	 * A private helper function for 'getData' which updates the multimeter
	 * display iff the given String is valid.
	 * 
	 * @param receivedData
	 *            the data received from the input stream that's been stitched
	 *            into a String
	 */
	private void updateMultimeterDisplay(String receivedData) {
		boolean failedToDecode = false;

		if (!failedToDecode) {

			if (receivedData.charAt(0) == '1' && !GuiController.instance.multimeterDisplay.isDisabled()) {
				receivedData = receivedData.replace("=", PLUS_MINUS_SYMBOL);

				firstDisplay = receivedData.substring(2, receivedData.length() - 1).trim() + "\n";
			} else if (receivedData.charAt(0) == '2' && !GuiController.instance.multimeterDisplay.isDisabled()) {
				receivedData = receivedData.replace(";", OHM_SYMBOL);

				secondDisplay = receivedData.substring(2, receivedData.length() - 1).trim();
			} else {
				failedToDecode = true;
			}

			// Update Multimeter display iif the values for the top and bottom
			// lines have been found
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
	 * Check that the first and last values of the received data has the
	 * expected value.
	 * 
	 * @param receivedData
	 *            the data received from the input stream that's been stitched
	 *            into a String
	 * @return false if the received data is valid, true if it isn't
	 */
	private boolean checkReceivedDataEnds(String receivedData) {
		return !receivedData.startsWith(PACKET_BOUNDARIES) && !receivedData.endsWith(PACKET_BOUNDARIES);
	}

	/**
	 * A private helper function for 'getData' which updates the graph display
	 * iff the given String is valid.
	 * 
	 * @param receivedData
	 *            the data received from the input stream that's been stitched
	 *            into a String
	 */
	private void sortMultimeterMeasurements(String receivedData) {

		boolean failedToDecode = false;
		double measurementDataValue = 0D;

		if (!failedToDecode) {

			// Check for voltage/current/resistance results.
			try {
				measurementDataValue = Double.parseDouble(receivedData.substring(3, receivedData.length() - 1).trim());
			} catch (NumberFormatException e) {
				System.err.println("Failed to decode data:" + measurementDataValue);

				// If data received from the serial connection was not the right
				// type
				failedToDecode = true;
			}

			if (!failedToDecode) {
				String unit = "";
				unit = Character.toString(receivedData.charAt(0));

				setupLogicContinuityBehaviours(unit);

				// Record and display updated results
				GuiController.instance.recordAndDisplayNewResult(measurementDataValue, unit);
			}
		} else {
			System.err.println("***Failed to decode data: " + receivedData);
		}
	}

	/**
	 * Sets up certain behaviours when in received Continuity/Logic values and
	 * when not
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
	 * Changes the displayed value of the AC/DC to the opposite of what it's
	 * currently receiving.
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
	 * A private helper function to 'determineValidText' which determines if the
	 * data received in valid.
	 * 
	 * @param receivedData
	 *            the data received from the input stream that's been stitched
	 *            into a String
	 * @return whether or not the string is valid (true if it is valid, false if
	 *         it isn't)
	 */
	private boolean isValidText(String receivedData) {
		return receivedData != null && !receivedData.equalsIgnoreCase("");
	}

	@Override
	public int getListeningEvents() {
		return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
	}
}
