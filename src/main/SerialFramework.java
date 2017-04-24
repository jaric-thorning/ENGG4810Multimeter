package main;

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
	final static String INIT_PORT_SELECTION = "None"; // Default first element
	private static SerialPort OpenSerialPort = null;

	/**
	 * Resets the selected port to default first element (None).
	 */
//	public static void resetSelectablePortsList() {
//		if (portNames.size() >= 1)
//			//GuiController.getInstance().portsAvailable.setValue(portNames.get(0));
//	}

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

//		GuiController.getInstance().portsAvailable.setItems(portNames);
//		GuiController.getInstance().portsAvailable.setValue(portNames.get(0));
//		GuiController.getInstance().portsAvailable.setVisibleRowCount(portCounter + 1);
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

		if (OpenSerialPort != null) {
			OpenSerialPort.closePort();
			OpenSerialPort = null;
		}
	}

	/**
	 * Handles the changing of the serial port selection. If there are ports (with valid names) it binds the serial port
	 * (refreshes the port list if it didn't bind); otherwise is closes the open port.
	 */
	public static void changePorts() {
//		if (GuiController.getInstance().portsAvailable.getValue() != null
//				&& !GuiController.getInstance().portsAvailable.getValue().equalsIgnoreCase("")
//				&& !GuiController.getInstance().portsAvailable.getValue().equals(INIT_PORT_SELECTION)) {
//			System.out.println("Changed to this port: " + GuiController.getInstance().portsAvailable.getValue());

			SerialPort[] ports = SerialPort.getCommPorts();
			for (SerialPort serialPort : ports) {
				//if (serialPort.getDescriptivePortName().equals(GuiController.getInstance().portsAvailable.getValue())) {
					System.out.println("Binding to Serial Port " + serialPort.getSystemPortName() + "...");
					if (bindListen(serialPort)) {
						System.out.println("Success.");
					} else {
						System.out.println("Failed to bind to Serial.");
						refreshSelectablePortsList();
					}
					return;
				//}
			}
			System.out.println("Invalid Serial!");
			refreshSelectablePortsList();
//		} else {
//			closeOpenPort();
//			System.out.println("Not a port - close any open ports");
//		}
	}

	/**
	 * A private helper function for changePorts(), which binds to the serial port.
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
		if (!serialPort.addDataListener(new SerialFramework.SerialDataListener())) {
			serialPort.closePort();
			return false;
		}

		OpenSerialPort = serialPort;
		//RecordedResults.shutdownRecordedResultsThread();
		//GuiModel.getInstance().clearData();

		return true;
	}

	/**
	 * A class used for binding the serial port, and getting data from it.
	 */
	private static class SerialDataListener implements SerialPortDataListener {
		private static final String PACKET_OPEN = "[";
		private static final String PACKET_CLOSE = "]";
		private static final String PACKET_DELIMITER = ",";

		/** The buffer for building whole packets received over the serial port. */
		private String serialBuffer = "";
		
		/** whether the port connection has bugged out. */
		private boolean errored = false;

		/**
		 * Handles an event on the serial port. Reads the data and displays it.
		 */
		@Override
		public void serialEvent(SerialPortEvent sEvent) {
			if (this.errored)
				return;
			if (sEvent.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
				if (sEvent.getSerialPort().bytesAvailable() < 0) {
					this.errored = true;
					System.err.println("Error 3. reading from port");
					closeOpenPort();
					//GuiController.getInstance().refreshPorts();
					return;
				}
				byte[] readBuffer = new byte[sEvent.getSerialPort().bytesAvailable()];
				sEvent.getSerialPort().readBytes(readBuffer, readBuffer.length);
				String text = new String(readBuffer);
				handleString(text);
			}
		}

		/**
		 * Buffers and interprets the data sent over the serial port from the weather MCU.
		 * 
		 * @param text
		 *            the data that was read in from the serial port.
		 */
		private void handleString(String text) {
			serialBuffer += text;
			System.out.println(" SERIAL" + serialBuffer);
//			System.out.println("Serial: \"" + text.replace("\r", "\\r").replace("\n", "\\n") + "\"");

//			// Clear the buffer if random text comes through for a while (not actually valid weather packets).
//			if (serialBuffer.length() > 100 && serialBuffer.indexOf(PACKET_OPEN) == -1
//					&& serialBuffer.indexOf(PACKET_CLOSE) == -1) {
//				serialBuffer = "";
//				return;
//			}
			while (getData())
				;
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

		/**
		 * Gets the data through the serial port connection.
		 * 
		 * @return true if the data failed to decode, and false if the data failed to decode.
		 */
		private boolean getData() {
			int openPacket = serialBuffer.indexOf(PACKET_OPEN);
			int closePacket = serialBuffer.indexOf(PACKET_CLOSE);

			if (openPacket < 0 || closePacket < 0)
				return false;
			if (closePacket < openPacket) {
				serialBuffer = serialBuffer.substring(openPacket);
				return true;
			}

			boolean failedToDecode = false;

			String[] data = serialBuffer.substring(openPacket + 1, closePacket).split(PACKET_DELIMITER);
			if (data.length != 3) {
				failedToDecode = true;
			}
			for (int i = 0; i < data.length; i++) {
				if (!isValidText(data[i])) {
					failedToDecode = true;
					break;
				}
			}

			try {
				int temperature = 0;
				double windSpeed = 0.0F;
				int luminosity = 0;
				
				temperature = Integer.parseInt(data[0]);
				windSpeed = Double.parseDouble(data[1]);
				luminosity = Integer.parseInt(data[2]);
			} catch (NumberFormatException e) {
				
				// If data received from the serial connection was not the right type
				failedToDecode = true;
			}

			if (!failedToDecode) {
				// RECORD AND DISPLAY NEW RESULTS OF TIME/ETC, ETC
				//GuiController.getInstance().recordAndDisplayNewResult(temperature, windSpeed, luminosity, true);
			} else {
				System.out.println(
						"Failed to decode data:\"" + serialBuffer.substring(openPacket, closePacket + 1) + "\"");
			}

			serialBuffer = serialBuffer.substring(closePacket + 1);
			return true;
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
