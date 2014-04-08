package com.iodice.utilities;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.jsoup.Jsoup;

import android.database.DatabaseUtils;

public class Text {
	
	public static String removeHTML(String s) {
		return Jsoup.parse(s).text();
	}
	
	public static String makeFilesystemSafe(String s) {
		return s.replaceAll("\\W+", "");
	}
	
	public static String escapeString(String s) {
		return DatabaseUtils.sqlEscapeString(s);
	}

	// display only the first x characters of the description while also displaying a full
	// last word. For example, if text = "hello world" and x = 8, the method returns "hello"
	public static String limitTextCharacters(String text, int x) {
		if (x <= 0)
			return "";
		
		if (text.length() <= x)
			return text;

		// use x + 1 because it helps detect if the last set of characters is a full word 
		String tmp = text.substring(0, x + 1);
		
		// trim down to last full word
		while (tmp.endsWith(" ") == false && tmp.length() > 0) {
			tmp = tmp.substring(0, tmp.length() - 1);
		}
		// get rid of last space
		tmp = tmp.substring(0, tmp.length() - 1);
		return tmp;
	}
	
	public static String datetimeToSQLDateString(Date date) {
		if (date == null)
			return "";
		SimpleDateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
	    return iso8601Format.format(date);
	}
	
	public static String sqlStringToDisplayString(String sqlDateString) {
		return "";
	}
	
}
