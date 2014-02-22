package com.iodice.rssreader;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

public class Activity_Home extends Activity {
	
	
		
		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.activity_main);

		}
		
	    @Override
	    public boolean onCreateOptionsMenu(Menu menu) {
	        // Inflate the menu; this adds items to the action bar if it is present.
	        getMenuInflater().inflate(R.menu.main, menu);
	        return true;
	    }
	    
		// updates RSS feed & rescrolls to the top of the ScrollView containing it
		public void loadFeed(View view) {
			Intent intent = new Intent(this, Activity_Rss.class);
			String key = getResources().getString(R.string.rss_url_intent);
			
			// --start tmp code 
			// this should be replaced with a call to getTag() or something
			List<String> rssFeeds = new ArrayList<String>();
			//rssFeeds.add("http://feeds.wired.com/wired/index");
			//rssFeeds.add("http://feeds.bbci.co.uk/news/world/rss.xml");
			rssFeeds.add("http://rss.cnn.com/rss/cnn_topstories.rss");
			// --end tmp code
			
		    intent.putStringArrayListExtra(key, (ArrayList<String>) rssFeeds);
			startActivity(intent);
		}
		
		protected void onSaveInstanceState(Bundle outState) {
			super.onSaveInstanceState(outState);
		}
		
		protected void onRestoreInstanceState(Bundle savedInstanceState) {
		    super.onRestoreInstanceState(savedInstanceState);
		}
	
}
