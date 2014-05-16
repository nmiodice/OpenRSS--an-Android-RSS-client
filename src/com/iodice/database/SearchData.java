package com.iodice.database;

public class SearchData {
	
	private String searchterm = new String();
	
	public void setSearchTerm(String s) {
		if (s != null)
			this.searchterm = s;
	}
	public String getSearchTerm() {
		return this.searchterm;
	}
	
}

