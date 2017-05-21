package main;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * The ISOTimeInterval class represents an ISO time interval formatter.
 * 
 * @author dayakern
 *
 */
public class ISOTimeInterval {
	String formattedDate = "";
	LocalDateTime date;
	DateTimeFormatter formatter;

	private static final String ISO_FORMATTER = "yyyy-MM-dd'T'HH:mm:ss.S";

	/**
	 * Extracts the date and formatter to be used when formatting the date.
	 * 
	 * @param date
	 *            the date that needs to be formatted
	 * @param formatter
	 *            the type of formatter
	 */
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
	 * @return the newly formatted string in ISO time interval formatting
	 * @throws NullPointerException
	 *             if the string is null
	 * @throws DateTimeParseException
	 *             if the string cannot be parsed
	 */
	public static ISOTimeInterval parseISOTime(String s) throws NullPointerException, DateTimeParseException {
		LocalDateTime date = LocalDateTime.parse(s);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(ISO_FORMATTER);
		ISOTimeInterval newIsoTime = new ISOTimeInterval(date, formatter);

		return newIsoTime;
	}
}
