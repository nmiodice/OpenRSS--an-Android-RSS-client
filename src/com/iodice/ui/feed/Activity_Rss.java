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
import android.content.pm.ActivityInfo;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndEntry;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndFeed;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.SyndFeedInput;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.XmlReader;
import com.iodice.database.rssOrm;
import com.iodice.rssreader.R;
import com.iodice.utilities.ObservableScrollView;
import com.iodice.utilities.ScrollViewListener;
import com.iodice.utilities.Sys;
import com.iodice.utilities.Text;



public class Activity_Rss extends Activity implements ScrollViewListener {
	
	private static final String TAG = "Activity_Rss";
	private List<String> latestFeedUrlList;
	private List<Fragment_Rss> lastDataPull;
	private final int RSS_FRAGMENT_ADD_INTERVAL = 20;
	private long nextScrollViewUpdate = System.currentTimeMillis();
	private rssUpdateTask rssUpdate;	
	
	@Override
	@SuppressWarnings("unchecked")
	protected void onCreate(Bundle savedInstanceState) {
    	ViewGroup loadingMessage;
    	
		// show the Up button in the action bar.
    	super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rss);
		
		loadingMessage = (ViewGroup) findViewById(R.id.rss_loading_content);
		loadingMessage.setVisibility(View.GONE);
		
		// sets up scroll listener for list of fragments
		ObservableScrollView fragScrollView = (ObservableScrollView) findViewById(R.id.rss_fragment_scrollview);
		fragScrollView.setScrollViewListener(this);
		
		// get list of feed URLs
		Intent intent = getIntent();
		List<String> newFeedUrlList = intent.getStringArrayListExtra(getResources().getString(R.string.rss_url_intent));
		
		// check for savedInstanceState == null avoids reloading on something like a screen rotation
		if (savedInstanceState == null && newFeedUrlList != null && newFeedUrlList.size() > 0) {
			latestFeedUrlList = newFeedUrlList;
			rssUpdate = new rssUpdateTask();
			rssUpdate.execute(newFeedUrlList);
		}
			
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_rss_menu, menu);
        return true;
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

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.reload_rss_content:
	            this.updateRSS();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
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
	protected void onSaveInstanceState(Bundle outState) {
		final ObservableScrollView sv;
		final String key_scroll = getResources().getText(R.string.scroll_position).toString();
		final String key_urls = getResources().getText(R.string.url_list).toString();
		final String key_lastData = getResources().getText(R.string.last_data_pull).toString();
		final String key_rss_update_async_task = getResources().getText(R.string.rss_update_async_task).toString();

	    Log.w(TAG, "Saving activity state");

		
		super.onSaveInstanceState(outState);

		// save scrollview position -- useful during orientation switch
		sv = (ObservableScrollView) findViewById(R.id.rss_fragment_scrollview);
		assert(sv != null);
		outState.putIntArray(key_scroll,
				new int[]{ sv.getScrollX(), sv.getScrollY()});
		
		// save current list of URLs: this is used to refresh content and also load in cached results on a screen
		// rotate or whenever an action dies
		outState.putStringArrayList(key_urls, (ArrayList<String>) this.latestFeedUrlList);
		outState.putParcelableArrayList(key_lastData, (ArrayList<? extends Parcelable>) this.lastDataPull);
		
		// if a background asynctask was cancelled, we need to know that. save it here. This avoids problems
		// when an async task was launched, canceled, and then the activity needs to be re-crated (for example,
		// when the orientation changes
		outState.putBoolean(key_rss_update_async_task, rssUpdate.isCancelled());
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		final ObservableScrollView sv;
		final int[] position;
		final String key_scroll = getResources().getText(R.string.scroll_position).toString();
		final String key_urls = getResources().getText(R.string.url_list).toString();
		final String key_lastData = getResources().getText(R.string.last_data_pull).toString();
		final String key_rss_update_async_task = getResources().getText(R.string.rss_update_async_task).toString();

		Log.i(TAG, "Restoring activity state");
		
	    super.onRestoreInstanceState(savedInstanceState);
	    position = savedInstanceState.getIntArray(key_scroll);
	    
	    sv = (ObservableScrollView) findViewById(R.id.rss_fragment_scrollview);
    	assert(sv != null);
    
	    // if we saved a value, load it (prevents improper loading on first load)
	    if(position != null) {
	    	sv.post(new Runnable() {
	            @Override
				public void run() {
	                sv.scrollTo(position[0], position[1]);
	            }
	        });
	    }
	    
	    // updates url list and then refreshes cache
	    this.latestFeedUrlList = savedInstanceState.getStringArrayList(key_urls);
	    this.lastDataPull = savedInstanceState.getParcelableArrayList(key_lastData);
	    
		// if a background asynctask was cancelled, we need to know that. This avoids problems
		// when an async task was launched, canceled, and then the activity needs to be re-crated (for example,
		// when the orientation changes
	    if (this.rssUpdate == null)
	    	this.rssUpdate = new rssUpdateTask();
	    if (savedInstanceState.getBoolean(key_rss_update_async_task))
	    	this.rssUpdate.cancel(false);
	}
	

	// loads more data if the end of the scrollview is approaching
	@Override
	public void onScrollChanged(ObservableScrollView scrollView, int x, int y,
			int oldx, int oldy) {
		View view = scrollView.getChildAt(scrollView.getChildCount() - 1);
		
	    int diff = (view.getBottom() - (scrollView.getHeight() + scrollView.getScrollY()));
	    
	    if (diff < 3000) {
	    	try {
	    		// use try catch to avoid "frgment added more than one time" incase multiple refreshes
	    		// are done in a row. 
	    		addFragmentsToScrollList(lastDataPull, RSS_FRAGMENT_ADD_INTERVAL);
	    	} catch (Exception e) {
	    	    Log.w(TAG, "onScrolLChange likely called too quickly in succession. Error adding fragment");
	    	}
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
	
	private void addFragmentsToScrollList(List<Fragment_Rss> resultList, int count) {
		FragmentTransaction fTrans;
		FragmentManager fMan;
		LinearLayout fragLayout = (LinearLayout) findViewById(R.id.rss_fragment_container);
		int numChildren = fragLayout.getChildCount();
		Fragment_Rss frag;
		
		if (resultList == null)
			return;
		
		if (System.currentTimeMillis() < this.nextScrollViewUpdate)
			return;
		long start = System.currentTimeMillis();
		this.nextScrollViewUpdate = start + 1*750; // 1 second(s) = 1 * 1000 ms/sec
		
		Log.i(TAG, "Loading more RSS content");
		// begin fragment transaction to add fragment items to the UI
		fMan = getFragmentManager();
		fTrans = fMan.beginTransaction();
		
		// loop through the resultset and add each fragment to the UI		
		for (int i = numChildren; i < count + numChildren; i++) {
			if (i >= resultList.size()) {
				break;
			}
			
			frag = resultList.get(i);
			fTrans.add(R.id.rss_fragment_container, frag);
		}

		fTrans.commit();
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
			int listSize = resultList.size();
			final ScrollView sv;
			final LinearLayout fLayout;
			final View loadingMessage;
			FragmentManager fMan;
			
			Log.i(TAG, "onPostExecute() running");

			
			// if the activity is no longer visible, back out now to avoid a crash
			if(rssUpdate.isCancelled()) {
				Log.w(TAG, "This task was cancelled. Backing out early in onPostExecute method");
				return;
			}
			lastDataPull = resultList;
		
			loadingMessage = findViewById(R.id.rss_loading_content);
			assert(loadingMessage != null);
			loadingMessage.setVisibility(View.GONE);
			
			fLayout = (LinearLayout) findViewById(R.id.rss_fragment_container);
			sv = (ScrollView) findViewById(R.id.rss_fragment_scrollview);
			assert(sv != null);
			assert(fLayout != null);

			// pop from the back stack if there is already content loaded. This prevents reloading
			// the same rss feed twice from duplicating their associated fragments on the screen.
			fMan = getFragmentManager();
			
			if (fMan.getBackStackEntryCount() > 0)
				fMan.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
				
			if (listSize == 0) {
				Toast.makeText(getApplicationContext(), getResources().getText(R.string.no_results).toString(),  Toast.LENGTH_LONG).show();
				return;
			}
			
			// erase whatever in the fragment's parent layout
			fLayout.removeAllViews();
			addFragmentsToScrollList(resultList, RSS_FRAGMENT_ADD_INTERVAL);
			// reset scroll
			sv.scrollTo(0, 0);
			
			// now that the fragments are added, start bringing the ScrollView into view
			sv.setVisibility(View.VISIBLE);
			Animation anim_3 = AnimationUtils.loadAnimation(getApplicationContext(), R.animator.fade_in);
			sv.startAnimation(anim_3);
			
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
				
				// step 3. convert results into appropriate list form. pass false to indicate
				// that the feeds are not cached results
				results = syndFeedToFragmentList(syndFeed, false);
				System.out.println("Successfully read data from " + url);
				
			} catch (Exception e) {
				
				if (queryCacheInFailCase) {
					System.out.println("Failed to acquire web data for " + this.url + "... Trying to retrieve from cache");
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
						
		    tmpStr = Text.removeHTML(entry.getTitle());
		    frag.setTitle(tmpStr);
			
		    tmpStr = Text.removeHTML(entry.getAuthor());
		    frag.setAuthor(tmpStr);
		    
		    tmpStr = Text.removeHTML(entry.getDescription().getValue());
		    frag.setDescription(tmpStr);
		    
		    tmpStr = Text.removeHTML(entry.getPublishedDate().toString());
		    frag.setPublishedDate(tmpStr);
		    
		    tmpStr = Text.removeHTML(entry.getLink());
		    frag.setURL(tmpStr);
		    
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
			SQLiteDatabase db = rssOrm.getWritableDatabase(this.context);
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