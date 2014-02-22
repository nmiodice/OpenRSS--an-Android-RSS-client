package com.iodice.utilities;

import org.jsoup.Jsoup;

public class Text {
	
	public static String removeHTML(String s) {
		return Jsoup.parse(s).text();
	}
	
	public static String makeFilesystemSafe(String s) {
		return s.replaceAll("\\W+", "");
	}

}
