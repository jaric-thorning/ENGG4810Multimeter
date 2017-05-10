package main;

/**
 * A class which handles the creation and parsing of date/time in ISO format.
 */

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ISOTimeInterval {
	String formattedDate = "";
	LocalDateTime date;
	DateTimeFormatter formatter;

	private static final String ISO_FORMATTER = "yyyy-MM-dd'T'HH:mm:ss.S";

	public ISOTimeInterval(LocalDateTime date, DateTimeFormatter formatter) {
		this.date = date;
		this.formatter = formatter;
	}

	@Override
	public String toString() {
		formattedDate = date.format(formatter);
		return formattedDate;
	}

	/**
	 * Parses a string to be a 'yyyy-MM-dd'T'HH:mm:ss.S' formatted ISO time interval.
	 * 
	 * @param s
	 *            the string to be parsed
	 * @return the new ISO time interval with the value of the string.
	 * @throws NullPointerException
	 *             if the string is null
	 * @throws DateTimeParseException
	 *             if the string cannot be parsed
	 */
	public static ISOTimeInterval parseISOTime(String s)
			throws NullPointerException, DateTimeParseException {
		LocalDateTime date = LocalDateTime.parse(s);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(ISO_FORMATTER);
		ISOTimeInterval newIsoTime = new ISOTimeInterval(date, formatter);

		return newIsoTime;
	}
}
