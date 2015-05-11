package org.recsyschallenge.helpers;

import static org.junit.Assert.*;

import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;

public class TimeStampHelperTests {
	private String dateString;

	@Before
	public void setUp() {
		// 2014-07-13T15:45:52.236Z
		// 2014-04-07T10:57:00.306Z
		// 2014-04-17T20:16:18.762Z
		// 2014-05-04T20:33:41.373Z
		dateString = "2014-07-13T15:45:52.236Z";
	}

	@Test
	public void testParseDate() throws ParseException, IOException {

		Date date = TimeStampHelper.parseDate(dateString);

		TimeZone utc = TimeZone.getTimeZone("UTC");
		Calendar cal = Calendar.getInstance(utc);
		cal.set(2014, 6, 13, 15, 45, 52);

		assertEquals(date.toString(), cal.getTime().toString());
	}

}
