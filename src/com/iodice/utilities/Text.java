package com.iodice.utilities;

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

}
