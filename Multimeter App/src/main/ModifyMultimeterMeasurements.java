package main;

import javafx.scene.chart.NumberAxis;
import javafx.scene.control.RadioButton;

/**
 * The ModifyMultimeterMeasurements class deals with checking and displaying any
 * y-unit values.
 * 
 * @author dayakern
 *
 */
public class ModifyMultimeterMeasurements {
	private static final String OHM_SYMBOL = Character.toString((char) 8486);

	public ModifyMultimeterMeasurements() {

	}

	/**
	 * A private helper function for 'exportMaskData' which converts the
	 * selected y-unit value to the correct mask export file format if there has
	 * been no data pre-loaded before this point.
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
		} else {
			modifiedYUnit = ".";// FIXME
		}

		return modifiedYUnit;
	}

	/**
	 * A private helper function for 'exportMaskData' which converts the stored
	 * y-units to the correct mask export file format.
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
		} else if (unit.contains(OHM_SYMBOL)) { // Ohm
			modifiedYUnit = "Ohm";
		} else {
			modifiedYUnit = ".";
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
		if (!(checkYUnitChangesVoltage(unit) && checkYUnitChangesCurrent(unit) && checkYUnitChangesResistance(unit)
				&& checkYUnitChangesContinuity(unit) && checkYUnitChangesLogic(unit))) {
			return false;
		}

		return true;
	}

	/**
	 * Determines if the y-value unit has changed to voltage if it's currently
	 * not voltage
	 * 
	 * @param unit
	 *            the y-unit received in the data sent across
	 * @return true if there has been no change, false otherwise
	 */
	private boolean checkYUnitChangesVoltage(String unit) {
		if ((unit.equals("V") || unit.equals("W")) && !GuiController.instance.voltage) {
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
	 * Determines if the y-value unit has changed to current if it's currently
	 * not current
	 * 
	 * @param unit
	 *            the y-unit received in the data sent across
	 * @return true if there has been no change, false otherwise
	 */
	private boolean checkYUnitChangesCurrent(String unit) {
		if ((unit.equals("I") || unit.equals("J")) && !GuiController.instance.current) {
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
	 * Determines if the y-value unit has changed to resistance if it's
	 * currently not resistance
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
	 * Determines if the y-value unit has changed to logic if it's currently not
	 * logic
	 * 
	 * @param unit
	 *            the y-unit received in the data sent across
	 * @return true if there has been no change, false otherwise
	 */
	private boolean checkYUnitChangesLogic(String unit) {
		if (unit.equals("L") && !GuiController.instance.logic) {
			System.err.println("LOGIC MODE");
			GuiController.instance.voltage = false;
			GuiController.instance.current = false;
			GuiController.instance.resistance = false;
			GuiController.instance.continuity = false;
			GuiController.instance.logic = true;

			return false;
		}

		return true;
	}

	/**
	 * Determines if the y-value unit has changed to continuity if it's
	 * currently not continuity
	 * 
	 * @param unit
	 *            the y-unit received in the data sent across
	 * @return true if there has been no change, false otherwise
	 */
	private boolean checkYUnitChangesContinuity(String unit) {
		if (unit.equals("C") && !GuiController.instance.continuity) {
			System.err.println("CONT MODE");
			GuiController.instance.voltage = false;
			GuiController.instance.current = false;
			GuiController.instance.resistance = false;
			GuiController.instance.continuity = true;
			GuiController.instance.logic = false;

			return false;
		}

		return true;
	}

	/**
	 * Converts the y-unit received through the serial connection to the format
	 * it will appear when saved out to a file.
	 * 
	 * @param unit
	 *            the y-unit to format
	 * @return the formatted y-unit
	 */
	protected String getUnitToSave(String unit) {
		if (unit.equals("V") || unit.equals("W")) {
			return "V";
		} else if (unit.equals("I") || unit.equals("J")) { // need to convert to
															// milliamps
			return "mA";
		} else if (unit.equals("R")) { // need to convert to Ohm
			return "Ohm"; // TODO: make sure that kOhm is saved out too?
		} else {
			return ".";
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
	protected void convertMeasurementYUnit(String value, NumberAxis yAxis) {
		String displayedYUnit = value;
		System.out.println("Displayed Unit: " + displayedYUnit);

		// Convert Ohm to symbol.
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
		} else if (value.equals("L") || value.equals("C")) {
			displayedYUnit = "."; // Logic and Continuity modes have no unit
		} else {
			displayedYUnit = "";
		}

		yAxis.setLabel("Measurements [" + displayedYUnit + "]");
	}
}
