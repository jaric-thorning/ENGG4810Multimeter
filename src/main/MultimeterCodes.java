package main;

/**
 * The MultimeterCodes enum class holds all the codes that drive the multimeter from the software -> hardware end, as
 * well as hardware-> software end.
 * 
 * @author dayakern
 *
 */
public enum MultimeterCodes {

	// Measurement modes
	VOLTAGE("S M V"), VOLTAGE_RMS("S M W"), CURRENT("S M I"), CURRENT_RMS("S M J"), RESISTANCE("S M R"), CONTINUITY(
			"S M C"), LOGIC("S M L"),

	// Sample rates
	SAMPLE_RATE_A("A"), SAMPLE_RATE_B("B"), SAMPLE_RATE_C("C"), SAMPLE_RATE_D("D"), SAMPLE_RATE_E("E"), SAMPLE_RATE_F(
			"F"), SAMPLE_RATE_G("G"), SAMPLE_RATE_H("H"), SAMPLE_RATE_I("I"),

	// Brightness percentage
	BRIGHTNESS_0("0"), BRIGHTNESS_1("1"), BRIGHTNESS_2("2"), BRIGHTNESS_3("3"), BRIGHTNESS_4("4"),

	// Check that writing works
	TWO_WAY_CHECK("[T]");

	private final String code; // Code to write across serial comms

	/**
	 * Extracts the code value to be used when driving the multimeter.
	 * 
	 * @param code
	 *            the value to determine the multimeter action
	 */
	MultimeterCodes(String code) {
		this.code = code;
	}

	/**
	 * Gets the code to drive the multimeter.
	 * 
	 * @return the code
	 */
	protected String getCode() {
		return code;
	}
}
