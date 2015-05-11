package org.recsyschallenge.helpers;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class TimeStampHelper {
	private final static DateFormat df;
	private final static TimeZone utc = TimeZone.getTimeZone("UTC");

	static {
		df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		df.setTimeZone(utc);
	}

	private TimeStampHelper() {
	};

	public synchronized static Date parseDate(String input)
			throws ParseException {
		GregorianCalendar cal = new GregorianCalendar(utc);
		cal.setTime(df.parse(input));
		return cal.getTime();
	}
}
