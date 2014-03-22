package com.iodice.ui.feed;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.iodice.rssreader.R;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

@SuppressLint("ValidFragment")
public class Fragment_Rss extends Fragment implements OnLongClickListener, Parcelable, Comparable<Fragment_Rss> {
	
	private static final String TAG = "Fragment_Rss";
	
	private String author = new String();
	private String description = new String();
	private String title = new String();
	private String publishedDate = new String();
	private Date publishedDateObject = new Date();
	private String url = new String();
	private String ParentUrl = new String();
	private String baseURL = new String();
	private boolean isCached = false;
	
	public Fragment_Rss() {
		super();
	}
	
	@Override
	public int compareTo(Fragment_Rss other) {
		try {
			return other.getPublishedDateObject().compareTo(this.publishedDateObject);
		} catch (Exception e) {
			return 0;
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		String key;
		String s;
		
       	super.onCreate(savedInstanceState);
       	// preserves state across orientation change
    	setRetainInstance(true);

    	// preserves state if fragment needs to be re-created
    	if (savedInstanceState != null) {
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
	@Override
	public void onSaveInstanceState(Bundle outState) {		
		super.onSaveInstanceState(outState);
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
    	
    	// this fragment should handle a click to itself
    	view.setOnLongClickListener(this);
    	
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
        if (!this.description.equals("")) {
        	
        	// display only the first 200 characters of the description. add an elipsis at the end
        	// of the last word in the first 200 characters
        	tmpStr = this.description.substring(0, Math.min(this.description.length(), 200));
        	if (tmpStr.length() < this.description.length()) {
        		do {
        			tmpStr = tmpStr.substring(0, tmpStr.length() - 1);
        			if (tmpStr.equals("") == true)
        				break;
        		} while (tmpStr.endsWith(" ") == false);
        		
        		if (tmpStr.length() > 0)
        			tmpStr = tmpStr.substring(0, tmpStr.length() - 1);
        		
        		tmpStr = tmpStr + "...";
        	}
        	tmp.setText(tmpStr);
        }
        else
        	hideView(tmp);
        
    	tmp = (TextView)view.findViewById(R.id.rss_title);
        if (!this.title.equals("")) {
        	tmp.setText(this.title);
	        // Add this.url as a tag in the title view so that the title's onclick
        	// handler can open the url in a browser
	        tmp.setTag(this.url);
        } else
        	hideView(tmp);
        
        tmp = (TextView)view.findViewById(R.id.rss_base_url);
        if(!this.baseURL.equals("")) {
        	tmp.setText(this.baseURL);
	        // Add this.url as a tag in the title view so that the title's onclick
	    	// handler can open the url in a browser
	        tmp.setTag(this.url);
        } else
       		hideView(tmp);      
        
    	tmp = (TextView)view.findViewById(R.id.rss_published_date);
    	if (!this.publishedDate.equals("")) {
    		tmp.setText(this.publishedDate);
	        // Add this.url as a tag in the title view so that the title's onclick
	    	// handler can open the url in a browser
	        tmp.setTag(this.url);
    	} else
    		hideView(tmp);
	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
    	
        View view = inflater.inflate(R.layout.fragment_rss, container, false);
        inflateViewWithData(view);
        
        return view;
    }
    
    // 
	@Override
	public boolean onLongClick(View v) {
		Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
		sharingIntent.setType("text/plain");
		String shareBody = this.getDescription() + "\n\n" + this.getURL();
		sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, this.getTitle());
		sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
		
		startActivity(Intent.createChooser(sharingIntent, "Share via"));
		return false;
	}
    
    
    
    
    
    
    // these setter methods are only valid prior to invocation of onCreateView. Updating these
    // after onCreateView is called wont update the UI
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



    protected Fragment_Rss(Parcel in) {
        author = in.readString();
        description = in.readString();
        title = in.readString();
        publishedDate = in.readString();
        url = in.readString();
        ParentUrl = in.readString();
        baseURL = in.readString();
        isCached = in.readByte() != 0x00;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(author);
        dest.writeString(description);
        dest.writeString(title);
        dest.writeString(publishedDate);
        dest.writeString(url);
        dest.writeString(ParentUrl);
        dest.writeString(baseURL);
        dest.writeByte((byte) (isCached ? 0x01 : 0x00));
    }

    public static final Parcelable.Creator<Fragment_Rss> CREATOR = new Parcelable.Creator<Fragment_Rss>() {
        @Override
        public Fragment_Rss createFromParcel(Parcel in) {
            return new Fragment_Rss(in);
        }

        @Override
        public Fragment_Rss[] newArray(int size) {
            return new Fragment_Rss[size];
        }
    };


}