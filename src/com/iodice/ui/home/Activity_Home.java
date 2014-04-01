package com.iodice.ui.home;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import com.iodice.database.feedsOrm;
import com.iodice.rssreader.R;
import com.iodice.utilities.callback;

public class Activity_Home extends Activity implements callback, ActionBar.OnNavigationListener

 {
	
	private static final String TAG = "Activity_Home";
	private static final String NAME_LIST_FRAG_TAG = "NAME_LIST";
	private static final String CATEGORY_LIST_FRAG_TAG = "CATEGORY_LIST";
	
	// only run one time, during the applications first run. initiated from onCreate()
	private void init() {
		SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);

		Log.i(TAG, "First run detected. Initializing data");
		initDefaultFeeds();

		// update that the app has been run
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putBoolean(getString(R.string.prefs_first_run), false);
		editor.commit();
	}
	
	private void initDefaultFeeds() {
		Log.i(TAG, "Initializing default RSS feeds in database");
		List<Feed_Data> rssFeeds = new ArrayList<Feed_Data>();
		
		ArrayList<String> tech = new ArrayList<String>();
		ArrayList<String> news = new ArrayList<String>();
		ArrayList<String> reddit = new ArrayList<String>();
		ArrayList<String> sports = new ArrayList<String>();
		
		tech.add("Technology");
		news.add("News");
		reddit.add("Reddit");
		reddit.add("Technology");
		sports.add("Sports");
		
		rssFeeds.add(new Feed_Data("Apple", tech, "ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/limit=10/xml"));
		rssFeeds.add(new Feed_Data("Wired", tech, "http://feeds.wired.com/wired/index"));
		rssFeeds.add(new Feed_Data("BBC world", news, "http://feeds.bbci.co.uk/news/world/rss.xml"));
		rssFeeds.add(new Feed_Data("CNN", news, "http://rss.cnn.com/rss/cnn_topstories.rss"));
		rssFeeds.add(new Feed_Data("New York Times", news, "http://feeds.nytimes.com/nyt/rss/HomePage"));
		rssFeeds.add(new Feed_Data("USA Today", news, "http://rssfeeds.usatoday.com/usatoday-NewsTopStories"));
		rssFeeds.add(new Feed_Data("NPR", news, "http://www.npr.org/rss/rss.php?id=1001"));
		rssFeeds.add(new Feed_Data("Reuters", news, "http://feeds.reuters.com/reuters/topNews"));
		rssFeeds.add(new Feed_Data("BBC America", news, "http://newsrss.bbc.co.uk/rss/newsonline_world_edition/americas/rss.xml"));
		rssFeeds.add(new Feed_Data("/r/androiddev", reddit, "http://www.reddit.com/r/androiddev/.rss"));
		rssFeeds.add(new Feed_Data("Yahoo Skiing", sports, "http://sports.yahoo.com/ski/rss.xml"));
		rssFeeds.add(new Feed_Data("Y.Combinator", tech, "https://news.ycombinator.com/rss"));

		saveFeeds(rssFeeds);
	}
	
	private void saveFeeds(List<Feed_Data> rssFeeds) {
		int length = rssFeeds.size();
		SQLiteDatabase db = feedsOrm.getWritableDatabase(getApplicationContext());
		int cnt = 0;
		for (int i = 0; i < length; i++) {
			try {
				feedsOrm.insertFeed(rssFeeds.get(i), db);
				cnt++;
			} catch (Exception e) {
				System.out.println("Error saving feed. SQLiteDatabase error: " + e.getMessage());
			}
		}
		db.close();
		Log.i(TAG, "Initialized " + cnt + " feeds in database");
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// check to see if first startup logic is needed
		SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
		boolean defaultVal = true;
		boolean isFirstRun = sharedPref.getBoolean(getString(R.string.prefs_first_run), defaultVal);
		if (isFirstRun)
			init();
		setupCategorySpinner();
		displayListViewTEMP();
	}
		
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_add_feed:
                addFeed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
	    super.onRestoreInstanceState(savedInstanceState);
	}
	
	// this brings up an input menu where the user can add new feed information. The response
	// is handled by 'respondToEvent.'
	private void addFeed() {
		Log.i(TAG, "Adding feed");
		
		AlertDialog.Builder alert = AddNewFeedView.getAddDialog(this);
		alert.show();
	}
	
	@Override
	// adds data to the database and updates the relevant views. Callbacks are:
	// 	0. The object passed back is a reference to a Feed_Data object that needs to be
	//		saved into the DB. 
	public void respondToEvent(int n, Object obj) {
		switch(n) {
		case 0:
			ArrayList<Feed_Data> feedList = new ArrayList<Feed_Data>();
			Feed_Data newFeed = (Feed_Data) obj;
			feedList.add(newFeed);
			saveFeeds(feedList);
			repopulateActiveList();
			return;
			
		default:
			Log.e(TAG, "Unsupported callback was called!");
			assert(false);
			return;
		}

	}
	
	// can be called if the data model is updated. the result will be that the currently
	// viewed fragment (if there is one) will re-draw with new data
	private void repopulateActiveList() {
		FragmentManager fMan = getFragmentManager();
		List_Base listFrag = null;
		
		// Step 1. Identify if any fragments we care about are loaded in the fragment manager
		if (fMan.findFragmentByTag(Activity_Home.NAME_LIST_FRAG_TAG) != null)
			listFrag = (List_Base) fMan.findFragmentByTag(Activity_Home.NAME_LIST_FRAG_TAG);
		else if (fMan.findFragmentByTag(Activity_Home.CATEGORY_LIST_FRAG_TAG) != null)
			listFrag = (List_Base) fMan.findFragmentByTag(Activity_Home.CATEGORY_LIST_FRAG_TAG);
		
		if (listFrag == null)
			return;
		
		// Step 2. Reset the fragments adapter and redraw it
		listFrag.setUpAdapter();
		listFrag.redrawListView();
	}

	// displayed in top right of activity, lists categories by name. upon select, filter list to just those categories
	private void setupCategorySpinner() {
		// Set up the action bar to show a dropdown list.
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		// populate list data
		Cursor c = feedsOrm.selectAllCategories(getApplicationContext());
		ArrayList<String> items = new ArrayList<String>();
		c.moveToFirst();
		
		while(!c.isAfterLast()) {
		     items.add(c.getString(c.getColumnIndex(feedsOrm.getCategoryTableCategoryKey())));
		     c.moveToNext();
		}
		
		ArrayAdapter<String> aAdpt = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, items);
		//actionBar.set
		actionBar.setListNavigationCallbacks(aAdpt, this);
	}
	
	private void displayListViewTEMP() {
		// add fragment to apropriate layout item
		FragmentTransaction fTrans;
		FragmentManager fMan = getFragmentManager();
				
		// fragContainer is null until something is added to it
		if (fMan.findFragmentByTag(Activity_Home.NAME_LIST_FRAG_TAG) == null) {
			Log.i(TAG, "Adding fragment to feed_list_container");
			fTrans = fMan.beginTransaction();
			fTrans.add(R.id.feed_list_container, new Feed_List(), Activity_Home.NAME_LIST_FRAG_TAG);
			fTrans.commit();
		}
	}

	@Override
	// when a spinner item is selected, this method is called
	public boolean onNavigationItemSelected(int position, long id) {
		Log.i(TAG, "position " + position + " pressed, id = " + id);
		return false;
	}
}
