package com.iodice.database;

public class SearchData {
	
	private String name = new String();
	private String searchterm = new String();
	
	public void setName(String n) {
		if (n != null)
			this.name = n;
	}
	public String getName() {
		return this.name;
	}
	public void setSearchTerm(String s) {
		if (s != null)
			this.searchterm = s;
	}
	public String getSearchTerm() {
		return this.searchterm;
	}
	
}

