package org.recsyschallenge.helpers;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class TimeStampHelper {
	private static DateFormat df;
	private static GregorianCalendar cal;

	static {
		TimeZone utc = TimeZone.getTimeZone("UTC");
		df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		df.setTimeZone(utc);
		cal = new GregorianCalendar(utc);
	}
	
	private TimeStampHelper() { };
	
	public static Date parseDate(String input) throws ParseException {
		cal.setTime(df.parse(input));
		return cal.getTime();
	}
}
