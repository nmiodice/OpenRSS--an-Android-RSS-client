package com.iodice.ui.home;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Feed_Data {
	private String url = new String();
	private String name = new String();
	private ArrayList<String> group = new ArrayList<String>();
	
	public Feed_Data(){};
	public Feed_Data(String name, ArrayList<String> group, String url) {
		assert (name != null && group != null && url != null);
		
		setURL(url);
		setGroups(group);
		setName(name);
	}
	
	public void setName(String s) {
		if (s == null)
			return;
		
		if (s.length() > 0)
			s = s.substring(0,1).toUpperCase(Locale.US) + s.substring(1);
		this.name = s;
	}
	public String getName() {
		return this.name;
	}
	
	
	public void setURL(String s) {
		if (s != null)
			this.url = s;
	}
	public String getURL() {
		return this.url;
	}
	
	public void setGroups(ArrayList<String> s) {
		if (s != null)
			this.group = s;
	}
	public List<String> getGroups() {
		return this.group;
	}
	
	
}
