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
}
