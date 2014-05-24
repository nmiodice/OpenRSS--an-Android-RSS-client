package com.iodice.database;

import java.util.ArrayList;
import java.util.List;

import com.iodice.utilities.Text;

public class FeedData {
	private String url = new String();
	private String name = new String();
	private ArrayList<String> group = new ArrayList<String>();
	
	public FeedData(){};
	public FeedData(String name, ArrayList<String> group, String url) {
		assert (name != null && group != null && url != null);
		
		setURL(url);
		setGroups(group);
		setName(name);
	}
	
	public void setName(String s) {
		if (s == null)
			return;
		
		s = Text.toFirstLetterUppercase(s);
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
		int numGrps = s.size();
		String grp;
		for (int i = 0; i < numGrps; i++) {
			grp = s.get(i);
			grp = Text.toFirstLetterUppercase(grp);
			s.set(i, grp);
		}
		if (s != null)
			this.group = s;
	}
	public List<String> getGroups() {
		return this.group;
	}
	
	
}
