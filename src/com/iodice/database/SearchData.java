package com.iodice.database;

import com.iodice.utilities.Text;

public class SearchData {
	
	private String searchterm = new String();
	
	public void setSearchTerm(String s) {
		if (s == null)
			return;
		s = Text.toFirstLetterUppercase(s);
		this.searchterm = s;
	}
	public String getSearchTerm() {
		return this.searchterm;
	}
	
}

