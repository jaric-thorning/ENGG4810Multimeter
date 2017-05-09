package main;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class IsoTime {
	String formattedDate = "";
	LocalDateTime date;
	DateTimeFormatter formatter;

	public IsoTime(LocalDateTime date, DateTimeFormatter formatter) {
		this.date = date;
		this.formatter = formatter;
	}

	@Override
	public String toString() {
		// LocalDateTime.now();
		// DateTimeFormatter.ISO_LOCAL_DATE_TIME;
		formattedDate = date.format(formatter);
		return formattedDate;
	}
	
	/**
	 * double java.lang.Double.parseDouble(String s) throws NumberFormatException

Returns a new double initialized to the value represented by the specified String, as performed by the valueOf method of class Double.

Parameters:
s the string to be parsed.
Returns:
the double value represented by the string argument.
Throws:
NullPointerException - if the string is null
NumberFormatException - if the string does not contain a parsable double.
Since:
1.2
See Also:
java.lang.Double.valueOf(String)
	 */

	public static IsoTime parseIsoTime(String s) {
		LocalDateTime date = LocalDateTime.parse(s);
		DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
		IsoTime newIsoTime = new IsoTime(date, formatter);
		return newIsoTime;
	}
}
