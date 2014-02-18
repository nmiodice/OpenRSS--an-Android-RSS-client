package com.iodice.rssreader;

import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class Fragment_Rss extends Fragment {
	
	private String author = new String();
	private String description = new String();
	private String title = new String();
	private String publishedDate = new String();
	private String url = new String();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
       	super.onCreate(savedInstanceState);
    	setRetainInstance(true);
	}
	
	private void hideView(View v) {
		v.setVisibility(View.GONE);
	}
	
	private void unHideView(View v) {
		v.setVisibility(View.VISIBLE);
	}
	
	private void inflateViewWithData(View view) {
    	TextView tmp;
    	
    	// url needs to be hidden by default
    	tmp = (TextView)view.findViewById(R.id.rss_url);
    	tmp.setVisibility(View.GONE);
    	if (!this.url.equals(""))
    		tmp.setText(this.url);

        // query local parameters to see whether or not we have data to 
        // populate the fragment with
    	tmp = (TextView)view.findViewById(R.id.rss_author);
        if (!this.author.equals("")) 
        	tmp.setText(this.author);
        else
        	hideView(tmp);

    	tmp = (TextView)view.findViewById(R.id.rss_description);        
        if (!this.description.equals(""))
        	tmp.setText(this.description);
        else
        	hideView(tmp);
        
    	tmp = (TextView)view.findViewById(R.id.rss_title);
        if (!this.title.equals(""))
        	tmp.setText(this.title);
        else
        	hideView(tmp);
        
        // Add this.url as a tag in the title so that the title's onclick handler
    	// can open the url in a browser
        tmp.setTag(this.url);
        
    	tmp = (TextView)view.findViewById(R.id.rss_published_date);
    	if (!this.publishedDate.equals(""))
    		tmp.setText(this.publishedDate);
    	else
    		hideView(tmp);
	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
    	
        View view = inflater.inflate(R.layout.fragment_rss, container, false);
        inflateViewWithData(view);
        
        return view;
    }
    
    // these setter methods are only valid prior to invocation of onCreateView
    public void setAuthor(String s) {
    	assert(s != null);
    	this.author = s;
    }
    
    public void setDescription(String s) {
    	assert(s != null);
    	this.description = s;
    }
    
    public void setTitle(String s) {
    	assert(s != null);
    	this.title = s;
    }
    
    public void setPublishedDate(String s) {
    	assert(s != null);
    	this.publishedDate = s;
    }
    
    public void setURL(String s) {
    	assert(s != null);
    	this.url = s;
    }

}
