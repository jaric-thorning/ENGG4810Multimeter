package main;

import java.text.DecimalFormat;

import javafx.scene.chart.NumberAxis;
import javafx.scene.control.RadioButton;

import javafx.scene.control.TextArea;

//TODO: CHECK I STILL NEED THIS...
/**
 * The ModifyMultimeterMeasurements class deals with checking and displaying any y-unit values.
 * 
 * @author dayakern
 *
 */
public class ModifyMultimeterMeasurements {
	private static final String OHM_SYMBOL = Character.toString((char) 8486);
//	private static final DecimalFormat MEASUREMENT_DECIMAL = new DecimalFormat("0.000");

	public ModifyMultimeterMeasurements() {

	}

	/**
	 * A private helper function for 'exportMaskData' which converts the selected y-unit value to the correct mask
	 * export file format if there has been no data pre-loaded before this point.
	 * 
	 * @param maskVRBtn
	 *            if voltage should be the saved y-unit of the mask
	 * @param maskARBtn
	 *            if current should be the saved y-unit of the mask
	 * @param maskORBtn
	 *            if resistance should be the saved y-unit of the mask
	 * @return the correctly modified unit to save
	 */
	public String modifyMaskUnit(RadioButton maskVRBtn, RadioButton maskARBtn, RadioButton maskORBtn) {
		String modifiedYUnit = "";

		if (maskVRBtn.isSelected()) { // V
			modifiedYUnit = "V";
		} else if (maskARBtn.isSelected()) { // mA
			modifiedYUnit = "A";
		} else if (maskORBtn.isSelected()) { // Ohm
			modifiedYUnit = "Ohm";
		}

		return modifiedYUnit;
	}

	/**
	 * A private helper function for 'exportMaskData' which converts the stored y-units to the correct mask export file
	 * format.
	 * 
	 * @param unit
	 *            the stored y-unit value of the y-axis value
	 * @return the correctly modified unit to save
	 */
	public String modifyMaskUnit(String unit) {
		String modifiedYUnit = "";

		if (unit.contains("V")) { // V
			modifiedYUnit = unit;
		} else if (unit.contains("A")) { // mA
			modifiedYUnit = "A";
		} else if (unit.contains(OHM_SYMBOL)) { // FIXME: find out which symbol will represent ohms
			modifiedYUnit = "Ohm";
		}

		return modifiedYUnit;
	}

	/**
	 * Determines if the y-value unit has changed upon acquisition.
	 * 
	 * @param unit
	 *            the y-unit received in the data sent across
	 * @return if there have been any changes in the y-unit data
	 */
	public boolean validateYAxisUnits(String unit) {
		if (!(checkYUnitChangesVoltage(unit) && checkYUnitChangesCurrent(unit) && checkYUnitChangesResistance(unit))) {
			return false;
		}

		return true;
	}

	/**
	 * Determines if the y-value unit has changed to voltage if it's currently not voltage
	 * 
	 * @param unit
	 *            the y-unit received in the data sent across
	 * @return true if there has been no change, false otherwise
	 */
	private boolean checkYUnitChangesVoltage(String unit) {
		if ((unit.equals("V") || unit.equals("W")) && !GuiController.instance.voltage) {
			System.err.println("V RMS CHANGE");
			GuiController.instance.voltage = true;
			GuiController.instance.current = false;
			GuiController.instance.resistance = false;
			GuiController.instance.continuity = false;
			GuiController.instance.logic = false;

			return false;
		}

		return true;
	}

	/**
	 * Determines if the y-value unit has changed to current if it's currently not current
	 * 
	 * @param unit
	 *            the y-unit received in the data sent across
	 * @return true if there has been no change, false otherwise
	 */
	private boolean checkYUnitChangesCurrent(String unit) {
		if ((unit.equals("I") || unit.equals("J")) && !GuiController.instance.current) {
			System.err.println("mA RMS CHANGE");
			GuiController.instance.voltage = false;
			GuiController.instance.current = true;
			GuiController.instance.resistance = false;
			GuiController.instance.continuity = false;
			GuiController.instance.logic = false;

			return false;
		}

		return true;
	}

	/**
	 * Determines if the y-value unit has changed to resistance if it's currently not resistance
	 * 
	 * @param unit
	 *            the y-unit received in the data sent across
	 * @return true if there has been no change, false otherwise
	 */
	private boolean checkYUnitChangesResistance(String unit) {
		if (unit.equals("R") && !GuiController.instance.resistance) {
			GuiController.instance.voltage = false;
			GuiController.instance.current = false;
			GuiController.instance.resistance = true;
			GuiController.instance.continuity = false;
			GuiController.instance.logic = false;

			return false;
		}

		return true;
	}

	/**
	 * Converts the y-unit received through the serial connection to the format it will appear when saved out to a file.
	 * 
	 * @param unit
	 *            the y-unit to format
	 * @return the formatted y-unit
	 */
	protected String getUnitToSave(String unit) { // FIXME
		if (unit.equals("V") || unit.equals("W")) {
			return "V";
		} else if (unit.equals("I") || unit.equals("J")) { // need to convert to milliamps
			return "mA";
		} else if (unit.equals("R")) { // need to convert to Ohm
			return "Ohm";
		} else {
			// FIXME:
			System.err.println("888");
			return "";
		}
	}

	/**
	 * Sets the y-axis label measurement value for data coming in from a file.
	 * 
	 * @param value
	 *            the y-unit value
	 * @param yAxis
	 *            the y-axis
	 */
	protected void convertMeasurementYUnit(String value, NumberAxis yAxis) {//FIXME
		String displayedYUnit = value;
		System.out.println("Displayed Unit: " + displayedYUnit);

		// Convert Ohm to Ohm symbol.
		if (value.equals("Ohm")) {
			displayedYUnit = OHM_SYMBOL;
		}

		yAxis.setLabel("Measurements [" + displayedYUnit + "]");
	}

	/**
	 * Sets the y-axis label measurement value for data coming in serially.
	 * 
	 * @param value
	 *            the y-unit value
	 * @param yAxis
	 *            the y-axis
	 */
	protected void convertYUnit(String value, NumberAxis yAxis) {
		String displayedYUnit = "";

		// Convert Ohm to Ohm symbol.
		if (value.equals("R")) {
			displayedYUnit = OHM_SYMBOL;
		} else if (value.equals("I")) {
			displayedYUnit = "mA";
		} else if (value.equals("J")) {
			displayedYUnit = "mA RMS";
		} else if (value.equals("V")) {
			displayedYUnit = value;
		} else if (value.equals("W")) {
			displayedYUnit = "V RMS";
		} else {
			displayedYUnit = "";
		}

		// System.err.println("Displayed Unit: " + displayedYUnit); // FIXME
		yAxis.setLabel("Measurements [" + displayedYUnit + "]");
	}
	// public String getVoltageRange(double dataValue) {
	// if (dataValue >= -1 && dataValue <= 1) { // +- 1 range
	// return "1";
	// } else if (dataValue >= -5 && dataValue <= 5) { // +- 5 range
	// return "5";
	// } else if (dataValue >= -12 && dataValue <= 12) { // +- 12 range
	// return "12";
	// } else if (dataValue < -12 && dataValue > 12) { // FIXME: MAKE IT SO EVERYTHING IS LIKE THAT
	// return "OL";
	// } else {
	// return "";
	// }
	// }

	// public String getCurrentRange(double dataValue) {
	// if (dataValue >= -10 && dataValue <= 10) { // +- 10 mA range
	// return "10";
	// } else if (dataValue >= -200 && dataValue <= 200) { // +- 200 mA range
	// return "200";
	// } else if (dataValue < -200 && dataValue > 200) { // FIXME: MAKE WHOLE MULTIMETER SET-TEXT
	// return "OL";
	// } else {
	// return "";
	// }
	// }

	// /**
	// * Changes the y-axis label if the units change + the units' range.
	// *
	// * @param dataValue
	// * the y-axis value.
	// */
	// public String getResistanceRange(double dataValue) {
	// if (dataValue >= 0 && dataValue <= 1) { // 0 - 1 kOhm range (1 Ohm = 0.001 kOhm)
	// return "0 - 1k" + OHM_SYMBOL;
	// } else if (dataValue > 1 && dataValue <= 10) {
	// return "1k" + OHM_SYMBOL + " - 10k" + OHM_SYMBOL;
	// } else if (dataValue > 10 && dataValue <= 100) {
	// return "10k" + OHM_SYMBOL + " - 100k" + OHM_SYMBOL;
	// } else if (dataValue > 100 && dataValue <= 1000) { // 0 - 1 MOhm range (1 kOhm = 0.001 MOhm)
	// return "100k" + OHM_SYMBOL + " - 1M" + OHM_SYMBOL;
	// } else if (dataValue > 1000) { // FIXME: MAKE WHOLE MULTIMETER SET-TEXT
	// return "OL";
	// } else {
	// return "";
	// }
	// // if (dataValue >= 0 && dataValue <= 1000) { // 0 - 1 kOhm range (1 Ohm = 0.001 kOhm)
	// // return "0 - 1k" + OHM_SYMBOL;
	// // } else if (dataValue > 1000 && dataValue <= 1000000) { // 0 - 1 MOhm range (1 kOhm = 0.001
	// // // MOhm)
	// // return "0 - 1M" + OHM_SYMBOL;
	// // } else if (dataValue > 1000000) { // FIXME: MAKE WHOLE MULTIMETER SET-TEXT
	// // return "OL";
	// // } else {
	// // return "";
	// // }
	// }

	// public String convertRange(Double dataValue) {
	// if (dataValue >= 0 && dataValue < 1000) {
	// return "k" + OHM_SYMBOL;
	// } else if (dataValue == 1000) {
	// return "M" + OHM_SYMBOL;
	// } else {
	// return "";
	// }
	// }

	// if (dataValue < 1000) {
	// String kOhm = "k" + OHM_SYMBOL;
	// // yAxis.setLabel("Measurements [" + OHM_SYMBOL + "]");
	// return kOhm + ": " + (dataValue /= 1000).toString() + kOhm;
	// // } else if (dataValue >= 1000 && dataValue <= 1000000) {
	// // yAxis.setLabel("Measurements [" + "k" + OHM_SYMBOL + "]");
	// // return (dataValue /= 1000);
	// } else if (dataValue >= 1000 && dataValue <= 1000000) {
	// String MOhm = "M" + OHM_SYMBOL;
	// // yAxis.setLabel("Measurements [" + "M" + OHM_SYMBOL + "]");
	// return MOhm + ": " + (dataValue /= 1000000).toString() + MOhm;
	// } else {
	// return "";
	// }
	// }

	// /**
	// * A private helper function to 'updateYAxisLabel' which modifies text for displaying the multimeter values.
	// *
	// * @param unit
	// * the abbreviated forms of voltage, current and resistance.
	// * @return an extended string version of the unit.
	// */
	// private String getUnit(String unit) {
	// if (unit.equals("V")) {
	// return "Voltage";
	// } else if (unit.equals("I")) {
	// return "Current";
	// } else if (unit.equals("R")) {
	// return "Res";
	// } else {
	// return "";
	// }
	// }

	// /**
	// * Updates the multimeter text display to match the data coming through the serial channel.
	// *
	// * @param multimeterReading
	// * the area which displays the multimeter readings
	// * @param unit
	// * the unit of the y-axis values.
	// */
	// protected void updateYAxisLabel(Double multimeterReading, String unit, TextArea multimeterDisplay,
	// NumberAxis yAxis) {
	//
	// // if (GuiController.instance.voltage) {
	// // multimeterDisplay.setText(getUnit(unit) + " ( " + PLUS_MINUS_SYMBOL + getVoltageRange(multimeterReading)
	// // + " )\nV: " + multimeterReading + unit);
	// //
	// // yAxis.setLabel("Measurements [V]");
	// // } else if (GuiController.instance.current) {
	// // multimeterDisplay.setText(getUnit(unit) + " ( " + PLUS_MINUS_SYMBOL + getCurrentRange(multimeterReading)
	// // + " )\nmA: " + multimeterReading);// + "mA");
	// //
	// // yAxis.setLabel("Measurements [mA]");
	// // } else if (GuiController.instance.resistance || GuiController.instance.continuity) { // TODO CHECK THIS WORKS
	// // multimeterDisplay.setText(
	// // getUnit(unit) + " ( " + getResistanceRange(multimeterReading) + " )\nR: " + multimeterReading +
	// // convertRange(multimeterReading));
	// // // multimeterDisplay.setText(getUnit(unit) + " ( " + getResistanceRange(multimeterReading) + " )" + "\n"
	// // // + convertRange(multimeterReading));
	// //
	// // yAxis.setLabel("Measurements [" + OHM_SYMBOL + "]");
	// // } else {
	// // // TODO: NOT SURE WHAT TO PUT HERE.
	// // }
	// }
}
