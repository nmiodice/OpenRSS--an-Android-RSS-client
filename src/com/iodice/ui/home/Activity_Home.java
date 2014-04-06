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
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.iodice.database.FeedOrm;
import com.iodice.database.OrmBase;
import com.iodice.network.FeedUpdateService;
import com.iodice.rssreader.R;
import com.iodice.utilities.callback;

public class Activity_Home extends Activity implements callback, ActionBar.OnNavigationListener

 {
	
	private static final String TAG = "Activity_Home";
	private static final String FEED_LIST = "FEED_LIST";
	private List<String> spinnerListItems = null;
	
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
		List<FeedData> rssFeeds = new ArrayList<FeedData>();
		
		ArrayList<String> tech = new ArrayList<String>();
		ArrayList<String> news = new ArrayList<String>();
		ArrayList<String> reddit = new ArrayList<String>();
		ArrayList<String> sports = new ArrayList<String>();
		
		tech.add("Technology");
		news.add("News");
		reddit.add("Reddit");
		reddit.add("Technology");
		sports.add("Sports");
		
		rssFeeds.add(new FeedData("Apple", tech, "ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/limit=10/xml"));
		rssFeeds.add(new FeedData("Wired", tech, "http://feeds.wired.com/wired/index"));
		rssFeeds.add(new FeedData("BBC world", news, "http://feeds.bbci.co.uk/news/world/rss.xml"));
		rssFeeds.add(new FeedData("CNN", news, "http://rss.cnn.com/rss/cnn_topstories.rss"));
		rssFeeds.add(new FeedData("New York Times", news, "http://feeds.nytimes.com/nyt/rss/HomePage"));
		rssFeeds.add(new FeedData("USA Today", news, "http://rssfeeds.usatoday.com/usatoday-NewsTopStories"));
		rssFeeds.add(new FeedData("NPR", news, "http://www.npr.org/rss/rss.php?id=1001"));
		rssFeeds.add(new FeedData("Reuters", news, "http://feeds.reuters.com/reuters/topNews"));
		rssFeeds.add(new FeedData("BBC America", news, "http://newsrss.bbc.co.uk/rss/newsonline_world_edition/americas/rss.xml"));
		rssFeeds.add(new FeedData("/r/androiddev", reddit, "http://www.reddit.com/r/androiddev/.rss"));
		rssFeeds.add(new FeedData("Yahoo Skiing", sports, "http://sports.yahoo.com/ski/rss.xml"));
		rssFeeds.add(new FeedData("Y.Combinator", tech, "https://news.ycombinator.com/rss"));

		saveFeeds(rssFeeds);
	}
	
	private void saveFeeds(List<FeedData> rssFeeds) {
		int length = rssFeeds.size();
		SQLiteDatabase db = OrmBase.getWritableDatabase(getApplicationContext());
		int cnt = 0;
		for (int i = 0; i < length; i++) {
			try {
				FeedOrm.insertFeed(rssFeeds.get(i), db);
				cnt++;
			} catch (Exception e) {
				if (e.getMessage().contains("code 19")) {
					Context context = getApplicationContext();
					CharSequence text = getText(R.string.add_feed_fail_message);
					int duration = Toast.LENGTH_SHORT;

					Toast toast = Toast.makeText(context, text, duration);
					toast.show();
				} else {
					Log.e(TAG, "Error saving feed. SQLiteDatabase error: " + e.getMessage());
				}
			}
		}
		db.close();
		Log.i(TAG, "Initialized " + cnt + " feeds in database");
		
        // reset shared preference that tells the system whether or not all saved feeds have an active cache
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(getString(R.string.prefs_article_table_not_yet_updated), false);
		editor.commit();
		Log.i(TAG, "Reset 'article table cache' shared preference");
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.feed_activity);
		
		// check to see if first startup logic is needed
		SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
		boolean defaultVal = true;
		boolean isFirstRun = sharedPref.getBoolean(getString(R.string.prefs_first_run), defaultVal);
		if (isFirstRun)
			init();
		setupCategorySpinner();
		displayListView();
		
		// finally, start article update service
		FeedUpdateService.startUpdatingAllFeeds(getApplicationContext());
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
	//
	//	1. Repopulate list with currently selected category
	//	2. Repopulate the category selector and redraw the list with 'all' showing
	public void respondToEvent(int n, Object obj) {
		switch(n) {
		case 0:
			ArrayList<FeedData> feedList = new ArrayList<FeedData>();
			FeedData newFeed = (FeedData) obj;
			feedList.add(newFeed);
			saveFeeds(feedList);
			repopulateActiveList();
			return;
		
		case 1:
			repopulateActiveList();
			return;
			
		case 2:
			setupCategorySpinner();
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
		FeedList listFrag = null;
		
		// Step 1. Identify if any fragments we care about are loaded in the fragment manager
		if (fMan.findFragmentByTag(Activity_Home.FEED_LIST) != null)
			listFrag = (FeedList) fMan.findFragmentByTag(Activity_Home.FEED_LIST);

		
		if (listFrag == null)
			return;
		
		// Step 2. Reset the fragments adapter and redraw it
		final ActionBar actionBar = getActionBar();
		int position = actionBar.getSelectedNavigationIndex();
		String category = this.spinnerListItems.get(position);
		// "all" is not a real category, so account for it
		if (category == this.getText(R.string.all))
			category = "*";
		
		listFrag.loadCategoryData(category);		
	}

	// displayed in top right of activity, lists categories by name. upon select, filter list to just those categories
	private void setupCategorySpinner() {
		// Set up the action bar to show a dropdown list.
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		// populate list data
		Cursor c = FeedOrm.selectAllCategories(getApplicationContext());
		ArrayList<String> items = new ArrayList<String>();
		items.add(this.getString(R.string.all));
		
		
		c.moveToFirst();
		while(!c.isAfterLast()) {
		     items.add(c.getString(c.getColumnIndex(FeedOrm.getCategoryTableCategoryKey())));
		     c.moveToNext();
		}
		
		// note: actionBar.getThemedContext() ensures the correct colors based on the action bar theme
		ArrayAdapter<String> aAdpt = new ArrayAdapter<String>(actionBar.getThemedContext(),
				android.R.layout.simple_list_item_1, 
				android.R.id.text1, 
				items);

		actionBar.setListNavigationCallbacks(aAdpt, this);
		this.spinnerListItems = items;
	}
	
	@Override
	// when a spinner item is selected, this method is called. returns true if the
	// event was handled, false otherwise
	public boolean onNavigationItemSelected(int position, long id) {
		FragmentManager fMan = getFragmentManager();
		FeedList currentList = (FeedList) fMan.findFragmentByTag(Activity_Home.FEED_LIST);

		if (this.spinnerListItems == null) {
			Log.e(TAG, "spinner items are null! This should never happen");
			return false;
		} else if (position < 0 || position >= this.spinnerListItems.size()) {
			Log.e(TAG, "item position in onNavigationItemSelected is out of range");
			return false;
		} else if (currentList == null) {
			Log.e(TAG, "currently active list is null. This should never happen");
			return false;
		}
		String category = this.spinnerListItems.get(position);
		
		// "all" is not a real category, so account for it
		if (category == this.getText(R.string.all))
			category = "*";
		currentList.loadCategoryData(category);		
		return true; 
	}
	
	
	private void displayListView() {
		// add fragment to apropriate layout item
		FragmentTransaction fTrans;
		FragmentManager fMan = getFragmentManager();
				
		// fragContainer is null until something is added to it
		if (fMan.findFragmentByTag(Activity_Home.FEED_LIST) == null) {
			Log.i(TAG, "Adding fragment to feed_list_container");
			
			FeedList list = new FeedList();
			fTrans = fMan.beginTransaction();
			fTrans.add(R.id.feed_list_container, list, Activity_Home.FEED_LIST);
			fTrans.commit();
		}
	}


}
