package main;

public enum MultimeterCodes {
	// Measurement Modes
	VOLTAGE ("[S M V]"), 
	CURRENT ("[S M I]"), 
	RESISTANCE ("[S M R]"), 
	CONTINUITY ("[S M C]"), 
	LOGIC ("[S M L]"), 
	
	// Sample rate 
	SAMPLE_RATE_A ("[S F A]"), 
	SAMPLE_RATE_B ("[S F B]"), 
	SAMPLE_RATE_C ("[S F C]"), 
	SAMPLE_RATE_D ("[S F D]"), 
	SAMPLE_RATE_E ("[S F E]"), 
	SAMPLE_RATE_F ("[S F F]"), 
	SAMPLE_RATE_G ("[S F G]"), 
	SAMPLE_RATE_H ("[S F H]"),
	
	// Display full multimeter text
	MULTIMETER_DISPLAY ("[S D T]");
	
	private final String code; // Code to write across serial comms
	
	MultimeterCodes(String code) {
		this.code = code;
	}
	
	protected String getCode() {
		return code;
	}
}
