package com.iodice.ui.feeds;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import com.iodice.application.MyApplication;
import com.iodice.database.FeedData;
import com.iodice.database.FeedOrm;
import com.iodice.rssreader.R;
import com.iodice.utilities.Callback;

public class FeedActivity extends Activity implements Callback, ActionBar.OnNavigationListener {
	
	private static final String TAG = "Activity_Home";
	private static final String FEED_LIST = "FEED_LIST";
	private List<String> spinnerListItems = null;

	/* supported callback method identifiers */
	public static final int CALLBACK_ADD_NEW_FEED = 0;
	public static final int CALLBACK_REPOPULATE_DATA_AND_REFRESH_CATEGORY_SELECTOR = 1;
	public static final int CALLBACK_REFRESH_CATEGORY_SELECTOR = 2;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.feed_activity);
		
		setupCategorySpinner();
		displayFeedList();
	}
	
		
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.feeds, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_add_feed:
                addFeed();
                return true;
                
            // TODO: remove when finished testing. THIS IS A PIECE OF TEST CODE
            case R.id.action_test_re_initialize:
            	FragmentManager fMan = getFragmentManager();
            	FeedList feedList = (FeedList) fMan.findFragmentByTag(FeedActivity.FEED_LIST);
        		if (feedList != null) {
                	((MyApplication)getApplication()).initDefaultFeeds(getApplicationContext());
                	feedList.setUpAdapter();
                	feedList.redrawListView();
                	handleCallbackEvent(FeedActivity.CALLBACK_REFRESH_CATEGORY_SELECTOR, null);
        		}
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
		AlertDialog.Builder alert = AddNewFeedDialog.getAddDialog(this);
		alert.show();
	}
	
	@Override
	// adds data to the database and updates the relevant views. Callbacks are:
	// 	CALLBACK_ADD_NEW_FEED:
	//		The object passed back is a reference to a Feed_Data 
	//		object that needs to be saved into the DB. 
	//
	//	CALLBACK_REPOPULATE_DATA_AND_REFRESH_CATEGORY_SELECTOR:
	//		Repopulate list with currently selected category & refresh the category selector
	//		to keep current selection (if still present)
	//	
	//	CALLBACK_REFRESH_CATEGORY_SELECTOR:
	//		Repopulate the category selector and redraw the list with 'all' showing
	public void handleCallbackEvent(int n, Object obj) {
		switch(n) {
		case FeedActivity.CALLBACK_ADD_NEW_FEED:
			ArrayList<FeedData> feedList = new ArrayList<FeedData>();
			FeedData newFeed = (FeedData) obj;
			feedList.add(newFeed);
			FeedOrm.saveFeeds(feedList, this);
			repopulateActiveList();
			return;
		
		case FeedActivity.CALLBACK_REPOPULATE_DATA_AND_REFRESH_CATEGORY_SELECTOR:
			// step 1: note the current category
			final ActionBar actionBar = getActionBar();
			int selectedListIndex = actionBar.getSelectedNavigationIndex();
			String selectedListValue = this.spinnerListItems.get(selectedListIndex);
			
			// step 2: refresh displayed data
			repopulateActiveList();
			// this call resets this.spinnerListItems to whatever is currently in the DB
			setupCategorySpinner();
			
			// step 3: check if the previous selected category still exists
			int newListIndex = this.spinnerListItems.indexOf(selectedListValue);
			if (newListIndex == -1)
				return;
			
			// step 4: the previous category is still defined, so select it
			actionBar.setSelectedNavigationItem(newListIndex);					
			return;
			
		case FeedActivity.CALLBACK_REFRESH_CATEGORY_SELECTOR:
			setupCategorySpinner();
			return;
			
		default:
			Log.e(TAG, "Unsupported callback was called!");
			throw new UnsupportedOperationException();
		}
	}
	
	// can be called if the data model is updated. the result will be that the currently
	// viewed fragment (if there is one) will re-draw with new data
	private void repopulateActiveList() {
		FragmentManager fMan = getFragmentManager();
		FeedList listFrag = null;
		
		// Step 1. Identify if any fragments we care about are loaded in the fragment manager
		if (fMan.findFragmentByTag(FeedActivity.FEED_LIST) != null)
			listFrag = (FeedList) fMan.findFragmentByTag(FeedActivity.FEED_LIST);

		
		if (listFrag == null)
			return;
		
		// Step 2. Reset the fragments adapter and redraw it
		final ActionBar actionBar = getActionBar();
		int position = actionBar.getSelectedNavigationIndex();
		String category = this.spinnerListItems.get(position);
		// "all" is not a real category, so account for it
		if (category == this.getText(R.string.all))
			category = "*";
		
		listFrag.loadCategory(category);		
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
		FeedList currentList = (FeedList) fMan.findFragmentByTag(FeedActivity.FEED_LIST);

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
		currentList.loadCategory(category);		
		return true; 
	}
	
	
	private void displayFeedList() {
		// add fragment to apropriate layout item
		FragmentTransaction fTrans;
		FragmentManager fMan = getFragmentManager();
				
		// fragContainer is null until something is added to it
		if (fMan.findFragmentByTag(FeedActivity.FEED_LIST) == null) {			
			FeedList list = new FeedList();
			fTrans = fMan.beginTransaction();
			fTrans.add(R.id.feed_list_container, list, FeedActivity.FEED_LIST);
			fTrans.commit();
		}
	}


}
