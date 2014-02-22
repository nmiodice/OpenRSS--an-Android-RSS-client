package com.iodice.rssreader;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ScrollView;
import android.widget.Toast;

import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndEntry;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndFeed;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.SyndFeedInput;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.XmlReader;
import com.iodice.utilities.SyndFeedWriterThread;
import com.iodice.utilities.Text;
import com.iodice.utilities.Tuple;
import com.iodice.utilities.Sys;



public class Activity_Rss extends Activity {
	
	private List<String> latestFeedUrlList;
	private enum FEED_STATUS {
		FAILED,	/* failed/success reported by individual threads */
		SUCCESS_WEB,
		SUCCESS_CACHED,
		SOME_CACHED, /* some/all/none cached represents aggregate */
		ALL_CACHED,
		NONE_CACHED
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected void onCreate(Bundle savedInstanceState) {
    	ViewGroup loadingMessage;
    	
		// show the Up button in the action bar.
    	super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rss);
		
		loadingMessage = (ViewGroup) findViewById(R.id.rss_loading_content);
		loadingMessage.setVisibility(View.GONE);
		
		// get list of feed URLs
		Intent intent = getIntent();
		List<String> newFeedUrlList = intent.getStringArrayListExtra(getResources().getString(R.string.rss_url_intent));
		
		// check for savedInstanceState == null avoids reloading on something like a screen rotation
		if (savedInstanceState == null && newFeedUrlList != null && newFeedUrlList.size() > 0) {
			latestFeedUrlList = newFeedUrlList;
			rssUpdateTask rssUpdate = new rssUpdateTask();
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
	@SuppressWarnings("unchecked")
	public void updateRSS() {
		rssUpdateTask rssUpdate = new rssUpdateTask();
		rssUpdate.execute(this.latestFeedUrlList);
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
		final ScrollView sv;
		final String key = getResources().getText(R.string.scroll_position).toString();
		
		super.onSaveInstanceState(outState);

		// save scrollview position -- useful during orientation switch
		sv = (ScrollView) findViewById(R.id.rss_fragment_scrollview);
		assert(sv != null);
		outState.putIntArray(key,
				new int[]{ sv.getScrollX(), sv.getScrollY()});
	}
	
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		final ScrollView sv;
		final int[] position;
		final String key = getResources().getText(R.string.scroll_position).toString();
		
	    super.onRestoreInstanceState(savedInstanceState);
	    position = savedInstanceState.getIntArray(key);
	    
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
	
	
	
	
	
	
	private class rssUpdateTask extends AsyncTask<List<String>, Void, List<Fragment_Rss>> {

		/* means, did ANY connection not succede? */
		private boolean connectionSucceded = false;
		private FEED_STATUS status;

		
		@Override
		// turn on the loading message in UI
		protected void onPreExecute() {
			ViewGroup loadingMessage;
			loadingMessage = (ViewGroup) findViewById(R.id.rss_loading_content);
			loadingMessage.setAlpha(1);
			loadingMessage.setVisibility(View.VISIBLE);
		}

		// given a list of URLs, this method returns a list of associated SyndFeed objects
		// the return value here is a list of syndfeed objects + a status code indicating
		// how they were obtained
		private Tuple<FEED_STATUS, List<SyndFeed>> getSyndFeedList(List<String> urlList) {
			ExecutorService executor = Executors.newFixedThreadPool(10);
			CompletionService<Tuple<FEED_STATUS, SyndFeed>> pool = new ExecutorCompletionService<Tuple<FEED_STATUS, SyndFeed>>(executor);
			List<SyndFeed> feedList = new ArrayList<SyndFeed>();
			int listLength;
	        Callable<Tuple<FEED_STATUS, SyndFeed>> worker;
	        Tuple<FEED_STATUS, SyndFeed> tmpSyndFeedTuple;
	        FEED_STATUS status;
	        int numCached = 0;
	        
	        listLength = urlList.size();
	        
	        // kick off threads
			for (int i = 0; i < listLength; i++) {
				worker = new RssConnection(urlList.get(i), getApplicationContext());
				pool.submit(worker);
			}
			
			// shut it down LEMON!
	        executor.shutdown();
	        
	        // build SyndFeed list and set success/status flags as needed
	        status = FEED_STATUS.NONE_CACHED; /* assume all is well, change flags if not */
			for (int i = 0; i < listLength; i++) {

				try {
					tmpSyndFeedTuple = pool.take().get();
					if (tmpSyndFeedTuple.x == FEED_STATUS.SUCCESS_WEB)
						feedList.add(tmpSyndFeedTuple.y);
					else if (tmpSyndFeedTuple.x == FEED_STATUS.SUCCESS_CACHED) {
						status = FEED_STATUS.SOME_CACHED;
						numCached++;
						feedList.add(tmpSyndFeedTuple.y);
					} else if (tmpSyndFeedTuple.x == FEED_STATUS.FAILED) {
						// for the sake of counting how many successful connections are
						// cached, we can just increment numCached in a fail case because
						// what we care about as being successful is essentially now one less
						numCached++;
					} else {
						assert (false);
					}
				} catch (Exception e) {
					e.printStackTrace();
					assert(false);
				}
			}
	        
			if (numCached == listLength)
				status = FEED_STATUS.ALL_CACHED;
			
	        return new Tuple<FEED_STATUS, List<SyndFeed>>(status, feedList);
		}
		
		@Override
		// query address and extract the response as a String type
		protected List<Fragment_Rss> doInBackground(List<String>... urlListArr) {
			List<String> urlList = urlListArr[0];
			List<Fragment_Rss> fragList;
			List<SyndFeed> feedList;
			Tuple<FEED_STATUS, List<SyndFeed>> rssFetchResults;
	        
			// query for results and get the returned RSS feeds and the status
			rssFetchResults = getSyndFeedList(urlList);
			this.status = rssFetchResults.x;
			feedList = rssFetchResults.y;
			
			// detect if some data was not able to be loaded
			if (feedList.size() < urlList.size())
				this.connectionSucceded = false;
			else
				this.connectionSucceded = true;
			
			fragList = syndFeedListToFragList(feedList);			
			return fragList;
		}
		
		
		private Fragment_Rss syndFeedEntryToRssFragment(SyndEntry entry) {
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
		    
		    return frag;
		}
		
		// converts a lsit of RSS SyndFeed objects into a list of fragments. The feed entries are
		// put into the list in a round robin order, so the fragments are mixed
		private List<Fragment_Rss> syndFeedListToFragList(List<SyndFeed> feedList) {
			List<Fragment_Rss> fragList = new ArrayList<Fragment_Rss>();
			Fragment_Rss tmpFrag = null;
			SyndFeed tmpSyndFeed = null;
			SyndEntry tmpSyndEntry = null;
			int numFeeds;
			int maxFeedLength = 0;
			
			assert(feedList != null);
			numFeeds = feedList.size();
			
			// figure out longest SyndFeed
			for (int i = 0; i < numFeeds; i++) {
				if (feedList.get(i).getEntries().size() > maxFeedLength)
					maxFeedLength = feedList.get(i).getEntries().size();
			}
			
			// loop through list of SyndFeed objects and add Fragment_Rss objects
			// in an order such that the feeds are woven together. This allows the user
			// to see more than one feed that is mashed together with another (or more)
			for (int i = 0; i < maxFeedLength; i++) {

				if (i * numFeeds > getResources().getInteger(R.integer.aprox_max_feed_list_length))
					return fragList;
				
				// loop through each SyndFeed
				for (int j = 0; j < numFeeds; j++) {
					tmpSyndFeed = feedList.get(j);
					if (i < tmpSyndFeed.getEntries().size()) {
						tmpSyndEntry = (SyndEntry)tmpSyndFeed.getEntries().get(i);
						tmpFrag = syndFeedEntryToRssFragment(tmpSyndEntry);
						fragList.add(tmpFrag);
					}
				}
			}
			return fragList;
		}
		
		// update UI with new content and shut off the loading view
		protected void onPostExecute(List<Fragment_Rss> resultList) {
			int listSize = resultList.size();
			final ScrollView sv = (ScrollView) findViewById(R.id.rss_fragment_scrollview);
			final View loadingMessage;
			FragmentManager fMan;
			FragmentTransaction fTrans;
			loadingMessage = (View) findViewById(R.id.rss_loading_content);
			
			assert(sv != null);
			assert(loadingMessage != null);
			
			loadingMessage.setVisibility(View.GONE);
				
			if (listSize == 0) {
				Toast.makeText(getApplicationContext(), getResources().getText(R.string.no_results).toString(),  Toast.LENGTH_LONG).show();
				return;
			}
			
			// even if the check evaluates to true, there are still feeds to load because listSize > 0
			if (this.connectionSucceded == false) {
				Toast.makeText(getApplicationContext(), getResources().getText(R.string.partial_results).toString(),  Toast.LENGTH_LONG).show();
			}
			
			// pop from the back stack if there is already content loaded. This prevents reloading
			// the same rss feed twice from duplicating their associated fragments on the screen.
			fMan = getFragmentManager();
			if (fMan.getBackStackEntryCount() > 0)
				fMan.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			
			
			// begin fragment transaction to add fragment items to the UI
			fTrans = fMan.beginTransaction();

			// loop through the resultset and add each fragment to the U
			for (Fragment_Rss rssFrag : resultList)
				fTrans.add(R.id.rss_fragment_container, rssFrag);

			// adding to back stack is necessary so that duplicate data can be popped off when
			// refreshing content
			fTrans.addToBackStack(null);
			fTrans.commit();
			// update UI now, not when the scheduler decides	
			fMan.executePendingTransactions();
			// reset scroll
			sv.scrollTo(0, 0);
			
			// now that the fragments are added, start bringing the ScrollView into view
			sv.setVisibility(View.VISIBLE);
			Animation anim_3 = AnimationUtils.loadAnimation(getApplicationContext(), R.animator.fade_in);
			sv.startAnimation(anim_3);
		}
	} // end rssUpdateTask
	
	
	// designed to be a background thread that retrieves a SyndFeed corresponding
	// to an RSS link. a status is also returned as part of the touple, so the caller
	// knows if it is a freshly downloaded feed, cached feed, or it failed completely
	private class RssConnection implements Callable<Tuple<FEED_STATUS, SyndFeed>> {
		
		private String url = null;
		private SyndFeed syndFeed = null;
		private Context context;	/* used to check if the device is online */
		
		public RssConnection(String url, Context context) {
			assert(url != null);
			this.url = url;
			this.context = context;
		}
		
		// cache an rss feed to disk using the link as a filename
		private void writeCache() {
			SyndFeedWriterThread writer;

			try {
				// cache directory recommended by Android documentation specifically for
				// temporary files
				writer = new SyndFeedWriterThread(getCacheDir().getAbsolutePath(), Text.makeFilesystemSafe(url), this.syndFeed);
				(new Thread(writer)).start();
			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		}
		
		// attempt to read a cached file
		private SyndFeed readCache() {
			File fp;
			SyndFeed feed = null;
			SyndFeedInput input = new SyndFeedInput();

			try {
				fp = new File(getCacheDir(), Text.makeFilesystemSafe(this.url));
				feed = input.build(fp);
			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
			
			return feed;
		}
		
		@Override
		public Tuple<FEED_STATUS, SyndFeed> call() {
			URL url = null;
			URLConnection urlConnection = null;
			XmlReader xmlRdr = null;
			SyndFeedInput input = null;
			FEED_STATUS status;

			
			try {
				// without web connection, there is no point in trying the URL and waiting to timeout.
				// this check avoids the unnecessary wait
				if (Sys.isOnline(this.context) == false) {
					System.out.println("Reading cached data for URL " + this.url);
					this.syndFeed = readCache();
					System.out.println("Done reading data for " + this.url);
					status = FEED_STATUS.SUCCESS_CACHED;
				} else {
					System.out.println("Attempting to read data from " + this.url);
					// if we have web connection, give it a try. if this fails, try to get the cached
					// result. if that fails, return a failed status, which is propegated to the caller
					// and eventually UI
					url = new URL(this.url);
					urlConnection = url.openConnection();
					urlConnection.setConnectTimeout(R.integer.http_timeout);
					urlConnection.setReadTimeout(R.integer.http_timeout);
					
					xmlRdr = new XmlReader(url);
					input = new SyndFeedInput();
					this.syndFeed = input.build(xmlRdr);
					status = FEED_STATUS.SUCCESS_WEB;
				}
			} catch (Exception e) { 
				e.printStackTrace();
				/* who knows what happened here... just set it to null to be safe */
				this.syndFeed = null;
				this.syndFeed = readCache();
				if (this.syndFeed == null) {
					System.out.println("Failed to acquire web or cached data for " + this.url);
					status = FEED_STATUS.FAILED;
				} else {
					status = FEED_STATUS.SUCCESS_CACHED;
					System.out.println("Failed to acquire web (but succeeded in acquiring cached) data for " + this.url);
				}
			}
			// do not re-write unless this is a new result. this check is the same as 'if (status == FEED_STATUS.SUCCESS_CACHED)' 
			if (this.syndFeed != null && status != FEED_STATUS.SUCCESS_CACHED)
				writeCache();
			return new Tuple<FEED_STATUS, SyndFeed>(status, this.syndFeed);
		}
	} // end RssConnection
	
} // end rssActivity