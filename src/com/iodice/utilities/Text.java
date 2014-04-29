package com.iodice.utilities;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.jsoup.Jsoup;

import android.database.DatabaseUtils;
import android.util.Log;

public class Text {
	
	public static String removeHTMLAndStrip(String s) {
		s = Jsoup.parse(s).text();
		s = s.trim();
		return s;
	}
	
	public static String makeFilesystemSafe(String s) {
		return s.replaceAll("\\W+", "");
	}
	
	public static String escapeString(String s) {
		return DatabaseUtils.sqlEscapeString(s);
	}
	
	// clean punctuation and other crap. returns a list of lowercase plain text words
	public static List<String> getCleanStringListAsLowercase(String s, String delimiter) {
		if (s == null)
			return null;
		s = s.toLowerCase(Locale.US);
		s = s.trim();
		s = removeHTMLAndStrip(s);
		s = s.replaceAll("[^a-zA-Z0-9 \\.]", "");
		List<String> items = Arrays.asList(s.split(delimiter));
		return items;
	}

	
	public static String toFirstLetterUppercase(String s) {
		List<String> strings = Arrays.asList(s.split(" "));
		int size = strings.size();
		
		String w;
		String output = "";
		
		for (int i = 0; i < size; i++) {
			if (i != 0)
				output += " ";

			w = strings.get(i);
			
			Log.i("TEXT", "i = " + i);
			Log.i("TEXT", "output = " + output);
			Log.i("TEXT", "w = " + w);
			Log.i("TEXT", "output = " + output);
			Log.i("TEXT", "\n\n");
			Log.i("TEXT", "\n\n");
			
			if (w.length() == 0)
				continue;
			if (w.length() == 1)
				w = w.toUpperCase(Locale.getDefault());
			else {
				w = w.substring(0, 1).toUpperCase(Locale.getDefault()) 
						+ w.substring(1).toLowerCase(Locale.getDefault());
			}
			output += w;
		}
		return output;
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
