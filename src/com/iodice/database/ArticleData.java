package com.iodice.database;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.util.Log;

public class ArticleData {

	private static final String TAG = "Fragment_Rss";
	
	private String author = new String();
	private String description = new String();
	private String title = new String();
	private String publishedDate = new String();
	private Date publishedDateObject = new Date();
	private String url = new String();
	private String ParentUrl = new String();
	@SuppressWarnings("unused")
	private String baseURL = new String();
	private boolean isCached = false;
	
    public void setAuthor(String s) {
    	assert(s != null);
    	this.author = s;
    }
    
    public String getAuthor() {
    	return this.author;
    }
    
    public void setDescription(String s) {
    	assert(s != null);
    	this.description = s;
    }
    public String getDescription() {
    	return this.description;
    }
    
    public void setTitle(String s) {
    	assert(s != null);
    	this.title = s;
    }
    public String getTitle() {
    	return this.title;
    }
    
    public void setPublishedDate(String s) {
    	assert(s != null);
    	this.publishedDate = s;
    	try {
			publishedDateObject = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH).parse(s);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
		}
    }
    public String getPublishedDate() {
    	return this.publishedDate;
    }
    
    public Date getPublishedDateObject() {
    	return this.publishedDateObject;
    }
    public void setURL(String s) {
    	assert(s != null);
    	this.url = s;
    	
    	// also update the base URL
    	URL url;
		try {
			url = new URL(s);
	    	this.baseURL =  url.getHost();
		} catch (MalformedURLException e) {
			System.out.println("MalformedURLException caught trying to extract base URL from " + s);
			e.printStackTrace();
		}
    }
    
    public String getURL() {
    	return this.url;
    }
    
    public void setIsCached(boolean b) {
    	this.isCached = b;
    }
    
    public boolean getIsCached() {
    	return this.isCached;
    }
    
    public void setParentURL(String s) {
    	assert(s != null);
    	this.ParentUrl = s;
    }
    
    public String getParentURL() {
    	return this.ParentUrl;
    }
}
