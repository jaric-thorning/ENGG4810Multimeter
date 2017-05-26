package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

/**
 * A class used for binding the serial port, and getting data from it.
 */
public class SerialDataListener implements SerialPortDataListener {
	private static final String OHM_SYMBOL = Character.toString((char) 8486);
	private static final String PLUS_MINUS_SYMBOL = Character.toString((char) 177);

	private static final char MODE = 'M';
	private static final char FREQUENCY = 'F';
	private static final String TWO_WAY_CHECK = "C";

	private static String displayText = "";
	private static int counter = 0;

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
				SerialTest.closeOpenPort();

				return;
			}

			char s = 0;
			StringBuilder input = new StringBuilder();

			try {
				SerialTest.setReadFromSerial(sEvent.getSerialPort().getInputStream());

				while ((s = (char) SerialTest.getReadFromSerial().read()) != '|') {
					input.append(s);

					if (input.length() > 20) {
						input = new StringBuilder();
					}
				}

				getData(input.toString());

				SerialTest.getReadFromSerial().close();
			} catch (IOException e) {

				// e.printStackTrace();
				this.errored = true;
				System.err.println("Error 3. reading from port");
				SerialTest.closeOpenPort();

				return;
			}
		}
	}

	private void getData(String receivedData) {
		if (receivedData.length() > 0) {
			
			// Check if there's two way.
			receivedTwoWayCheck(receivedData);

			if (receivedData.charAt(0) == 'D') {
				
				updateMultimeterDisplay(receivedData.substring(1));
			} else if (receivedData.charAt(0) == 'V' || receivedData.charAt(0) == 'I'
					|| receivedData.charAt(0) == 'R') {

				// Change values received
				sortMultimeterMeasurements(receivedData);
			} else if (receivedData.charAt(0) == 'S') {

				// Change multimeter settings
				sortMultimeterCommand(receivedData);
			}

			System.err.println("|" + receivedData + "|");
		}
	}

	private void sortMultimeterMeasurements(String data) {

		boolean failedToDecode = determineValidText(data);

		// Check for voltage/current/resistance results.
		String trimmedData = data.substring(3, data.length()).trim();

		System.err.println(trimmedData);
//		double measurementDataValue = 0D;
//		try {
//			measurementDataValue = Double.parseDouble(trimmedData.substring(3).trim());
//		} catch (NumberFormatException e) {
//
//			// If data received from the serial connection was not the right type
//			failedToDecode = true;
//		}
//
//		if (!failedToDecode) {
//			// RECORD AND DISPLAY NEW RESULTS OF TIME/ETC, ETC
//			String unit = Character.toString(data.charAt(0));
//			System.err.println(measurementDataValue);
//			GuiController.instance.recordAndDisplayNewResult(measurementDataValue, unit);
//		} else {
//			System.out.println("Failed to decode data.");
//			// + serialBuffer.substring(openPacket, closePacket + 1) + "\"");
//		}
	}

	/**
	 * Determines if the given String matches the two-way connection code
	 * 
	 * @param receivedData
	 *            the data received from the input stream that's been stitched into a String
	 */
	private void receivedTwoWayCheck(String receivedData) {
		boolean failedToDecode = determineValidText(receivedData);

		if (!failedToDecode) {
			if (receivedData.length() == 1 && receivedData.equals(TWO_WAY_CHECK)) {
				SerialTest.setIsChecked(true);
			}
		}
	}

	/**
	 * Updates the multimeter display iff the given String is valid
	 * 
	 * @param trimmedData
	 *            data received from the input stream that's been stitched into a String and trimmed
	 */
	private void updateMultimeterDisplay(String trimmedData) {
		boolean failedToDecode = determineValidText(trimmedData);

		if (!failedToDecode) {

			if (trimmedData.charAt(0) == '1') {
				trimmedData = trimmedData.replace("=", PLUS_MINUS_SYMBOL);
				
				displayText = trimmedData.substring(2);
				displayText += "\n";
				counter++;
			} else if (trimmedData.charAt(0) == '2') {
				trimmedData = trimmedData.replace(";", OHM_SYMBOL);
				
				displayText += trimmedData.substring(2); // FIXME: SOMETHING FUNKY HAPPENING HERE.
				counter++;
			} else {
				failedToDecode = true;
			}

			// Update the display when the first and second display lines have been extracted
			if (counter == 2) {
				GuiController.instance.multimeterDisplay.setText(displayText);
				counter = 0;
				displayText = "";
			}

		} else {
			System.err.println("***Failed to decode data: " + trimmedData);
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
	private void sortMultimeterCommand(String data) {
		
		// Check for multimeter display commands.
		String trimmedData = data.substring(2, data.length());

		boolean failedToDecode = determineValidText(trimmedData);

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
				failedToDecode = true;
				break;
			}
		} else {
			System.out.println("******Failed to decode data:" + trimmedData);
		}
	}

	/**
	 * Checks the validity of a supplied string. To be used in getData(), as a means to determine whether the data has
	 * been decoded successfully.
	 * 
	 * @param receivedData
	 *            the string which needs it's validity checked.
	 * @return whether or not the string is valid (true if it is valid, false if it isn't).
	 */
	private boolean isValidText(String receivedData) {
		return receivedData != null && !receivedData.equalsIgnoreCase("");
	}

	/**
	 * Determines the validity of the given String.
	 * 
	 * @param receivedData
	 *            the string that's to be checked for validity
	 * @return true if it's not valid, false otherwise
	 */
	private boolean determineValidText(String receivedData) {
		if (!isValidText(receivedData)) {
			return true;
		}

		return false;
	}

	/**
	 * Indicates what event types to listen for.
	 */
	@Override
	public int getListeningEvents() {
		return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
	}
}
