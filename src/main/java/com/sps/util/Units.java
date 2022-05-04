package com.sps.util;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Units {

	public static String today( ) {
		return DateTimeFormatter.ofPattern("MM/dd/yy", Locale.ENGLISH).format(LocalDateTime.now());
	}

	public static String today(char separator) {
		return DateTimeFormatter.ofPattern("MM" + separator + "dd" + separator + "yy", Locale.ENGLISH).format(LocalDateTime.now());
	}
	
	public static String yesterday( ) {
		return todayMinus(1);
	}
	
	public static String todayMinus(int days) {
		return DateTimeFormatter.ofPattern("MM/dd/yy", Locale.ENGLISH).format(LocalDateTime.now().minusDays(days));
	}

	public static String formatDate(DateTime date) {
		return date != null ? date.toString(DateTimeFormat.shortDate()) : "";
	}
}
