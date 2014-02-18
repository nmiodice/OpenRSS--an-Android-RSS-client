package com.iodice.rssreader;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndEntry;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndFeed;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.SyndFeedInput;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.XmlReader;

public class Activity_Rss extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
    	ViewGroup loadingMessage;
    	
		// show the Up button in the action bar.
    	super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rss);
		
		loadingMessage = (ViewGroup) findViewById(R.id.rss_loading_content);
		loadingMessage.setVisibility(View.GONE);
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
	
	
	public void updateRSS(View view) {
		rssUpdateTask rssUpdate = new rssUpdateTask();
		rssUpdate.execute("http://feeds.bbci.co.uk/news/rss.xml");
	}
	
	public void openFeed(View v) {
		Intent browserIntent;
		String feedURL = (String) v.getTag();

		try {
			browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(feedURL));
			startActivity(browserIntent);
			} catch (ActivityNotFoundException e) {
			  Toast.makeText(getApplicationContext(), "No application can handle this request,"
			    + " Please install a webbrowser",  Toast.LENGTH_LONG).show();
			  e.printStackTrace();
			}	
		}
	
	protected void onSaveInstanceState(Bundle outState) {
		ScrollView sv;
		
		super.onSaveInstanceState(outState);

		// save scrollview position -- useful during orientation switch
		sv = (ScrollView) findViewById(R.id.rss_fragment_scrollview);
		assert(sv != null);
		outState.putIntArray("@string/scroll_position",
				new int[]{ sv.getScrollX(), sv.getScrollY()});
	}
	
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		final ScrollView sv;
		final int[] position;
		
	    super.onRestoreInstanceState(savedInstanceState);
	    position = savedInstanceState.getIntArray("ARTICLE_SCROLL_POSITION");
	    
	    // if we saved a value, load it (prevents improper loading on first load)
	    if(position != null) {
	    	sv = (ScrollView) findViewById(R.id.rss_fragment_scrollview);
	    	assert(sv != null);
	        
	    	sv.post(new Runnable() {
	            public void run() {
	                sv.scrollTo(position[0], position[1]);
	            }
	        });
	    }
	}
	
	
	
	
	
	
	private class rssUpdateTask extends AsyncTask<String, Void, List<Fragment_Rss>> {
		
		// checked in onPostExecute
		private boolean connSucceeded = false;
		

		@Override
		// turn on the loading message
		protected void onPreExecute() {
			ViewGroup loadingMessage;
			loadingMessage = (ViewGroup) findViewById(R.id.rss_loading_content);
			loadingMessage.setVisibility(View.VISIBLE);
		}

		
		@Override
		// query address and extract the response as a String type
		protected List<Fragment_Rss> doInBackground(String... urlStrArr) {
			String urlStr = urlStrArr[0];
			List<Fragment_Rss> fragList = new ArrayList<Fragment_Rss>();
			URL url = null;
			XmlReader xmlRdr = null;
			SyndFeedInput input = null;
			SyndFeed feed = null;
			
			try {
				url = new URL(urlStr);
				xmlRdr = new XmlReader(url);

				input = new SyndFeedInput();
				feed = input.build(xmlRdr);
				connSucceeded = true;
			
			// if an error is found, abort with an empty return set
			} catch (Exception e) {
				connSucceeded = false;
				return fragList;
			}
			
			// get dynamic list of rss fragments, which will be published to the UI via onPostExecute
			fragList = rssToFragmentList(feed, fragList);
			return fragList;
		}
		
		@SuppressWarnings("unchecked")
		private List<Fragment_Rss> rssToFragmentList(SyndFeed feed, List<Fragment_Rss> list) {
			Fragment_Rss frag;
			TextView tmp;
			assert(feed != null);
			
			
			for (SyndEntry entry : (List<SyndEntry>) feed.getEntries()) {
			    frag = new Fragment_Rss();
			    
			    tmp = (TextView) findViewById(R.id.rss_title);
			    frag.setTitle(entry.getTitle());
				
			    tmp = (TextView) findViewById(R.id.rss_author);
			    frag.setAuthor(entry.getAuthor());
			    
			    tmp = (TextView) findViewById(R.id.rss_description);
			    frag.setDescription(entry.getDescription().getValue());
			    
			    tmp = (TextView) findViewById(R.id.rss_published_date);
			    frag.setPublishedDate(entry.getPublishedDate().toString());
			    
			    tmp = (TextView) findViewById(R.id.rss_url);
			    frag.setURL(entry.getLink());

			    
			    list.add(frag);
			}
			
			return list;
		}
		
		// update UI with new content
		protected void onPostExecute(List<Fragment_Rss> resultList) {
			int listSize = resultList.size();
			
			// content is not being loaded anymore, so turn the view invisible
			ViewGroup loadingMessage;
			loadingMessage = (ViewGroup) findViewById(R.id.rss_loading_content);
			loadingMessage.setVisibility(View.GONE);
			
			// back out early if there are no results to display
			if (connSucceeded == false || listSize == 0)
				return;
			
			// pop from the back stack if there is already content loaded. This prevents reloading
			// the same rss feed twice from duplicating their associated fragments on the screen.
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager.getBackStackEntryCount() > 0)
				fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

			// begin fragment transaction to add fragment items to the UI
			FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

			// loop through the resultset and add each fragment to the U
			for (Fragment_Rss rssFrag : resultList)
				fragmentTransaction.add(R.id.rss_fragment_container, rssFrag);

			// adding to back stack is necessary so that duplicate data can be popped off when
			// refreshing content
			fragmentTransaction.addToBackStack(null);
			fragmentTransaction.commit();
			// update UI now, not when the scheduler decides	
			fragmentManager.executePendingTransactions();
		}
	} // end rssUpdateTask 
} // end rssActivity

