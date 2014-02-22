package com.iodice.rssreader;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class Fragment_Rss extends Fragment {
	
	private String author = new String();
	private String description = new String();
	private String title = new String();
	private String publishedDate = new String();
	private String url = new String();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		String key;
		String s;
		
       	super.onCreate(savedInstanceState);
       	// preserves state across orientation change
    	setRetainInstance(true);

    	// preserves state if fragment needs to be re-created
    	if (savedInstanceState != null) {
    		System.out.println("Inflating Fragment_Rss private data");
	    	key = getResources().getText(R.string.author).toString();
	    	s = savedInstanceState.getString(key);
	    	setAuthor(s);
	    	
	    	key = getResources().getText(R.string.description).toString();
	    	s = savedInstanceState.getString(key);
	    	setDescription(s);
	    	
	    	key = getResources().getText(R.string.title).toString();
	    	s = savedInstanceState.getString(key);
	    	setTitle(s);
	    	
	    	key = getResources().getText(R.string.publishedDate).toString();
	    	s = savedInstanceState.getString(key);
	    	setPublishedDate(s);
	    	
	    	key = getResources().getText(R.string.url).toString();
	    	s = savedInstanceState.getString(key);
	    	setURL(s);
    	}
	}
	
	// save information if fragment is destroyed by OS while in background
	public void onSaveInstanceState(Bundle outState) {		
		super.onSaveInstanceState(outState);
		System.out.println("Saving instance state via onSaveInstanceState");
		// save member variables so this fragment can be recreated with that 
		// data at a later time
		outState.putString(getResources().getText(R.string.author).toString(), 
				this.author);
		outState.putString(getResources().getText(R.string.description).toString(), 
				this.description);
		outState.putString(getResources().getText(R.string.title).toString(), 
				this.title);
		outState.putString(getResources().getText(R.string.publishedDate).toString(), 
				this.publishedDate);
		outState.putString(getResources().getText(R.string.url).toString(), 
				this.url);
	}
	private void hideView(View v) {
		v.setVisibility(View.GONE);
	}
	
	private void inflateViewWithData(View view) {
    	TextView tmp;
    	String tmpStr;
    	
    	// url needs to be hidden by default
    	tmp = (TextView)view.findViewById(R.id.rss_url);
    	tmp.setVisibility(View.GONE);
    	if (!this.url.equals(""))
    		tmp.setText(this.url);

        // query local parameters to see whether or not we have data to 
        // populate the fragment with
    	tmp = (TextView)view.findViewById(R.id.rss_author);
        if (!this.author.equals("")) {
        	tmpStr = getResources().getString(R.string.rss_author_by);
        	tmpStr += " " + this.author;
        	tmp.setText(tmpStr);
        } else
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
