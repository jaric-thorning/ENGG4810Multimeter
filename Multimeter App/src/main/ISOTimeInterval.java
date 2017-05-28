package main;

import java.time.Duration;
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
	private LocalDateTime date;
	private DateTimeFormatter formatter;

	private static final String ISO_FORMATTER = "yyyy-MM-dd'T'HH:mm:ss.SSS";

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

	/**
	 * Gets the value of the LocalDateTime.
	 * 
	 * @return the LocalDateTime belonging to this ISOTimeInterval
	 */
	public LocalDateTime getDate() {
		return date;
	}

	/**
	 * Converts the ISO formatted time the data points are received at to seconds. To be used as the x-value
	 * 
	 * @param startDate
	 *            when the first data point is received
	 * @param endDate
	 *            when the last data point is received (continuously updates)
	 * @return the number of seconds between the first point and the latest last point
	 */
	public static Double xValue(LocalDateTime startDate, LocalDateTime endDate) {
		Double seconds = 0D;

		// Convert to seconds
		Duration duration = Duration.between(startDate, endDate);
		seconds = duration.toMillis() / 1000D;

		return seconds;
	}

	@Override
	public String toString() {
		String formattedDate = date.format(formatter);
		return formattedDate;
	}

	/**
	 * Parses a string to be a 'yyyy-MM-dd'T'HH:mm:ss.SSS' formatted ISO time interval.
	 * 
	 * @param stringToParse
	 *            the string to be parsed
	 * @return the newly formatted string in ISO time interval formatting
	 * @throws NullPointerException
	 *             if the string is null
	 * @throws DateTimeParseException
	 *             if the string cannot be parsed
	 */
	public static ISOTimeInterval parseISOTime(String stringToParse)
			throws NullPointerException, DateTimeParseException {
		LocalDateTime date = LocalDateTime.parse(stringToParse);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(ISO_FORMATTER);
		ISOTimeInterval newIsoTime = new ISOTimeInterval(date, formatter);

		return newIsoTime;
	}
}
