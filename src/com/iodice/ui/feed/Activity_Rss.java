package com.iodice.ui.feed;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndEntry;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndFeed;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.SyndFeedInput;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.XmlReader;
import com.iodice.database.ormBase;
import com.iodice.database.rssOrm;
import com.iodice.rssreader.R;
import com.iodice.ui.home.Activity_Home;
import com.iodice.ui.home.Feed_List;
import com.iodice.utilities.Sys;
import com.iodice.utilities.Text;



public class Activity_Rss extends Activity {
	
	private static final String TAG = "Activity_Rss";
	private static final String LIST = "LIST";
	private List<String> latestFeedUrlList;
	private rssUpdateTask rssUpdate;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rss);
		displayListView();
	}
	
	private void displayListView() {
		// add fragment to apropriate layout item
		FragmentTransaction fTrans;
		FragmentManager fMan = getFragmentManager();
				
		// fragContainer is null until something is added to it
		if (fMan.findFragmentByTag(Activity_Rss.LIST) == null) {
			Log.i(TAG, "Adding fragment to feed_list_container");
			
			Fragment_List list = new Fragment_List();
			fTrans = fMan.beginTransaction();
			fTrans.add(R.id.rss_fragment_container, list, Activity_Rss.LIST);
			fTrans.commit();
		}
	}

	
	// updates RSS feed & rescrolls to the top of the ScrollView containing it
	public void updateRSS() {
		
		if (this.latestFeedUrlList != null) {
			Log.i(TAG, "updateRSS triggered");
			private_updateRSS(false);
		}
	}
	
	// update using cached RSS feeds. the data is put into the 'lastDataPull' parameter
	private void updateCachedRssResults() {
		private_updateRSS(true);
	}
	
	@SuppressWarnings("unchecked")
	private void private_updateRSS(boolean cacheOnly) {
		if (this.latestFeedUrlList != null) {
			Log.i(TAG, "updateCachedRssResults triggered");
			
			// avoid eager users who update more than once
			if (this.rssUpdate.getStatus() == AsyncTask.Status.RUNNING)
				return;
			
			// always need to reset the rssUpdate task because it can only be executed one time
			this.rssUpdate = new rssUpdateTask();
			if (cacheOnly)
				this.rssUpdate.setUpdateCacheOnly(true);
			this.rssUpdate.execute(this.latestFeedUrlList);
		}
	}

	
	public void openLinkInBrowser(View v) {
		Intent browserIntent;
		String feedURL = (String) v.getTag();

		Log.i(TAG, "Opening feed: " + feedURL);
		
		try {
			browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(feedURL));
			startActivity(browserIntent);
			} catch (ActivityNotFoundException e) {
			  Toast.makeText(getApplicationContext(), "No application can handle this request,"
			    + " Please install a webbrowser",  Toast.LENGTH_LONG).show();
			  e.printStackTrace();
			} catch (Exception e) {
				e.getMessage();
				e.printStackTrace();
			}
		}
	
	

	
	@Override
	protected void onPause() {
		super.onPause();
		
		// cancel any bakckground tasks so they do not crash the applicaiton when they come back and
		// try to operate on the paused activity
		/*if (this.rssUpdate.getStatus() == AsyncTask.Status.PENDING || 
				this.rssUpdate.getStatus() == AsyncTask.Status.RUNNING) {
		*/
		if (this.rssUpdate == null)
			return;
		Log.i(TAG, "async task status = " + this.rssUpdate.getStatus());
		if (this.rssUpdate.getStatus() == AsyncTask.Status.RUNNING) {
			this.rssUpdate.cancel(false);
			Log.w(TAG, "An AsyncTask was running and had to be canceled because the parent activity is no longer active");
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if (this.rssUpdate == null) {
			// this should not happen, and will result in a segmentation fault
			Log.e(TAG, "onResume called but rssUpdate is null");
			return;
		}
		if (this.rssUpdate.isCancelled()) {
			// if there was an update when the activity was stopped, the onPostExecute method of the task never ran
			// and the loading message was never turned invisible
			Log.w(TAG, "A canceled background task was detected during onResume");
			ViewGroup loadingMessage;
			loadingMessage = (ViewGroup) findViewById(R.id.rss_loading_content);
			loadingMessage.setVisibility(View.GONE);
			
			// we need to also unlock the screen orientation
			unlockScreenOrientation();
			
			// now display any data we have. this should be cached
			updateCachedRssResults();
			
			// now that we cleaned up the asynctask, we can re-set the canceled flag
			//this.rssUpdate.setUpdateCacheOnly(false);
			//this.rssUpdate.cancel(false);
		}
	}
	

	
	// helper method to lock screen orientation. Should call unlockScreenOrientation
	// shortly after making this to avoid a UI lock in one orientation.
	private void lockScreenOrientation() {
	    int orientation = getRequestedOrientation();
	    int rotation = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
	    switch (rotation) {
	    case Surface.ROTATION_0:
	        orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
	        break;
	    case Surface.ROTATION_90:
	        orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
	        break;
	    case Surface.ROTATION_180:
	        orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
	        break;
	    default:
	        orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
	        break;
	    }
	    setRequestedOrientation(orientation);
	    Log.i(TAG, "Screen orientation locked");
	}
	 
	private void unlockScreenOrientation() {
	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
	    Log.i(TAG, "Screen orientation unlocked");
	}
	

	
	
	
	
	
	
	
	
	
	
	private class rssUpdateTask extends AsyncTask<List<String>, Void, List<Fragment_Rss>> {
		
		private final String TAG = "rssUpdateTask";

		// if set to true, then each thread will update via cache. The results will be then stored
		// in the parent activities 'lastDataPull' parameter
		private boolean updateCacheOnly = false;
		
		
		public void setUpdateCacheOnly(boolean b) {
			this.updateCacheOnly = b;
		}
		
		@Override
		// turn on the loading message in UI
		protected void onPreExecute() {
			ViewGroup loadingMessage;
			
			Log.i(TAG, "onPreExecute() running");
			loadingMessage = (ViewGroup) findViewById(R.id.rss_loading_content);
			loadingMessage.setAlpha(1);
			loadingMessage.setVisibility(View.VISIBLE);
			
			// this prevents the calling activity from getting destroyed when the orientation changes
			lockScreenOrientation();
		}
	
		
		// weaves fragments together in a roudn robin approach. Parameters are not durable and will
		// be modified.
		private List<Fragment_Rss> spliceFragmentLists(List<List<Fragment_Rss>> listOfFragmentLists) {
			int numInnerLists;
			int innerListSize;
			List<Fragment_Rss> innerList = null;
			List<Fragment_Rss> results = new ArrayList<Fragment_Rss>();
			HashSet<String> hs = new HashSet<String>();
			
			assert(listOfFragmentLists != null);
			
			// aggregate feeds into parent list, but do not add duplicates
			numInnerLists = listOfFragmentLists.size();
			for (int i = 0; i < numInnerLists; i++) {
				
				innerList = listOfFragmentLists.get(i);
				innerListSize = innerList.size(); 
				for (int j = 0; j < innerListSize; j++) {
					// de-duplicate feeds using the URL hash because duplicate URLs mean duplicate feeds
					if (hs.add(innerList.get(j).getURL())) {
						results.add(innerList.get(j));
					} 
				}
			}
			
			Collections.sort(results);
			return results;
		}
		
		// This method plays a major role in collecting RSS data in the background. This handles fetching
		// the data from each URL in the background -- either cached or from the web
		private List<List<Fragment_Rss>> threadedRssRequest(List<String> urlList) {
			int numUrls;
			List<List<Fragment_Rss>> listOfFragmentLists;	/* holds the return of each threads query */
			Callable<List<Fragment_Rss>> worker;
			
			assert (urlList != null);
			numUrls = urlList.size();
			
			// the executor and pool are used so that a return value can be obtained from the thread.Call() method
			ExecutorService executor = Executors.newFixedThreadPool(numUrls);
			CompletionService<List<Fragment_Rss>> pool = new ExecutorCompletionService<List<Fragment_Rss>>(executor);
			
			for (int i = 0; i < numUrls; i++) {
				worker = new RssConnection(urlList.get(i), getApplicationContext(), this.updateCacheOnly);
				pool.submit(worker);
			}
			// shut it down LEMON!
	        executor.shutdown();
	        
	        // aggregate thread return lists and splice them together
	        listOfFragmentLists = new ArrayList<List<Fragment_Rss>>();
	        List<Fragment_Rss> threadResult;
	        try {
		        for (int i = 0; i < numUrls; i++) {
		        	threadResult = pool.take().get();
		        	listOfFragmentLists.add(threadResult);
		        }
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
	        
	        return listOfFragmentLists;
		}
		
		// given a list of RSS Feed URLs, create a spliced list of Fragment_Rss items
		private List<Fragment_Rss> aggregateFeeds(List<String> urlList) {
			List<List<Fragment_Rss>> listOfFragmentLists;
			List<Fragment_Rss> results = null;
			
			assert (urlList != null);

			// an unsorted list of rss fragments corresponding to a particular URL
			listOfFragmentLists = threadedRssRequest(urlList);
	        results = spliceFragmentLists(listOfFragmentLists);
	        
	        // cache results
	        (new Thread(new resultCacheThread(results, getApplicationContext()))).start();
	        return results;
		}
			
		@Override
		// query address and extract the response as a String type
		protected List<Fragment_Rss> doInBackground(List<String>... urlListArr) {
			List<String> urlList = urlListArr[0];
			List<Fragment_Rss> fragList = null;
			
			Log.i(TAG, "doInBackground() running");
			
			assert (urlList != null);
			
			fragList = aggregateFeeds(urlList);
		    Log.i(TAG, "Returning results to UI");
			return fragList;
		}

		
		// update UI with new content and shut off the loading view
		@Override
		protected void onPostExecute(List<Fragment_Rss> resultList) {

			
			// finally, unlock the orientation
			unlockScreenOrientation();
		}
	} // end rssUpdateTask
	
	
		
	
	// designed to be a background thread that retrieves a List<Fragment_Rss> corresponding
	// to an RSS link. a status is also returned as part of the touple, so the caller
	// knows if it is a freshly downloaded feed, cached feed, or it failed completely
	private class RssConnection implements Callable<List<Fragment_Rss>> {
		private static final String TAG = "RssConnection";
		private String url = null;
		private Context context;	/* used to check if the device is online */
		private boolean queryCacheOnly = false;
		
		public RssConnection(String url, Context context, boolean queryCacheOnly) {
			assert(url != null);
			this.url = url;
			this.context = context;
			
			// set true if the thread should only update from cached results in the sql table
			this.queryCacheOnly = queryCacheOnly;
		}
		
		
		@Override
		public List<Fragment_Rss> call() {
			List<Fragment_Rss> results;
			
			if (this.queryCacheOnly == true || Sys.isOnline(this.context) == false)
				results = queryCache(this.url);
			else
				results = queryWeb(this.url, true);
			
			// this is only true if the web query failed to return web content and cached content
			if (results == null)
				results = new ArrayList<Fragment_Rss>();
			return results;
		}
				
		
		private List<Fragment_Rss> queryCache(String url) {
			List<Fragment_Rss> results = new ArrayList<Fragment_Rss>();
		    Log.i(TAG, "Reading cached data for parentUrl " + this.url);

			results = rssOrm.selectWhere(this.context, "parentUrl = " + Text.escapeString(this.url));
			return results;
		}


		// queries for web content, but optionally queries cache in a failure case
		private List<Fragment_Rss> queryWeb(String url, boolean queryCacheInFailCase) {
			List<Fragment_Rss> results = null;
			SyndFeedInput input;
			URLConnection urlConnection;
			
			System.out.println("Attempting to read data from " + url);
			try {
				// step 1. create connection
				URL urlObj = new URL(url);
				urlConnection = urlObj.openConnection();
				
				urlConnection.setConnectTimeout(10);
				urlConnection.setReadTimeout(10);
				
				// step 2. set up call to Rome API
				XmlReader xmlRdr = new XmlReader(urlObj);
				input = new SyndFeedInput();
				SyndFeed syndFeed = input.build(xmlRdr);
				Log.i(TAG, "1");
				// step 3. convert results into appropriate list form. pass false to indicate
				// that the feeds are not cached results
				results = syndFeedToFragmentList(syndFeed, false);
				Log.i(TAG, "2");
				Log.i(TAG,"Successfully read data from " + url);
				
			} catch (Exception e) {
				//Log.e(TAG,e.getMessage());
				e.printStackTrace();
				Log.e(TAG,"Failed to acquire web data for " + this.url + "... Trying to retrieve from cache");
				
				if (queryCacheInFailCase) {
					results = queryCache(url);
				} else
					// in a total failure case, we can just return an empty list
					results = new ArrayList<Fragment_Rss>();
			}
			
			return results;
		}
		
		
		private List<Fragment_Rss> syndFeedToFragmentList(SyndFeed feed, boolean isCached) {
			Fragment_Rss tmpFrag;
			List<Fragment_Rss> results = new ArrayList<Fragment_Rss>();
			
			assert(feed != null);
			
			@SuppressWarnings("unchecked")
			List<SyndEntry> entries = feed.getEntries();
			int numEntries = entries.size();
			
			for (int i = 0; i < numEntries; i++) {
				tmpFrag = syndEntryToFrag(entries.get(i), isCached);
				results.add(tmpFrag);
			}
			
			return results;
		}
		
		private Fragment_Rss syndEntryToFrag(SyndEntry entry, boolean isCached) {
			Fragment_Rss frag = new Fragment_Rss();
			String tmpStr;
			
			if (entry.getTitle() != null) {
				tmpStr = Text.removeHTML(entry.getTitle());
		    	frag.setTitle(tmpStr);
			}
			
			if (entry.getAuthor() != null) {
				tmpStr = Text.removeHTML(entry.getAuthor());
				frag.setAuthor(tmpStr);
			}
		    
			if (entry.getDescription() != null) {
				tmpStr = Text.removeHTML(entry.getDescription().getValue());
				frag.setDescription(tmpStr);
			}
		    
		    if (entry.getPublishedDate() != null) {
		    	tmpStr = Text.removeHTML(entry.getPublishedDate().toString());
		    	frag.setPublishedDate(tmpStr);
		    }
		    
		    if (entry.getLink() != null) {
		    	tmpStr = Text.removeHTML(entry.getLink());
		    	frag.setURL(tmpStr);
		    }
		    
		    frag.setIsCached(isCached);
		    
		    frag.setParentURL(this.url);
		    
		    return frag;
		}
		
	} // end RssConnection
	
	
	
	// updates cache after new content is pulled in through the web
	public class resultCacheThread implements Runnable {

		private List<Fragment_Rss> fragments;
		private Context context;
		
		public resultCacheThread(List<Fragment_Rss> fragments, Context context) {
			this.fragments = fragments;
			this.context = context;
		}

		@Override
		public void run() {
			Fragment_Rss frag;
			
			assert(fragments != null);
			assert(context != null);
			
			int numEntries = this.fragments.size();
			
			// refresh DB with cache
			SQLiteDatabase db = ormBase.getWritableDatabase(this.context);
			for (int i = 0; i < numEntries; i++) {
				frag = fragments.get(i);
				assert (frag != null);
				try {
					if (frag.getIsCached() == false)
						rssOrm.insertRss(frag, db);
				} catch (SQLiteException e) {
					if (!e.getMessage().contains("code 19"))
							System.out.println("Error commiting rss fragment data. SQLiteDatabase error: " + e.getMessage());
				}
			}
			db.close();
		}
		
	} // end resultCacheThread
} // end rssActivity