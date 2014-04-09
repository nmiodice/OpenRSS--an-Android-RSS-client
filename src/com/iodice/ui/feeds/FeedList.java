package com.iodice.ui.feeds;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.view.ActionMode;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.iodice.database.FeedOrm;
import com.iodice.rssreader.R;
import com.iodice.ui.ListBase;
import com.iodice.ui.articles.ArticleActivity;
import com.iodice.utilities.Callback;


@SuppressLint("UseValueOf")
public class FeedList extends ListBase {
	
	private final String TAG = "Feed_List";
    
    
	@Override
	public void onSingleItemClick(View view) {
		Intent intent = new Intent(getActivity(), ArticleActivity.class);
		String key = getResources().getString(R.string.rss_url_intent);
		
		List<String> rssFeeds = new ArrayList<String>();
		TextView innerView = (TextView)view.findViewById(R.id.feed_url);
		rssFeeds.add(innerView.getText().toString());

	    intent.putStringArrayListExtra(key, (ArrayList<String>) rssFeeds);
		startActivity(intent);		
	}
	

	@Override
	public void cabOnMultipleItemClick() {
		Intent intent = new Intent(getActivity(), ArticleActivity.class);
		String key = getResources().getString(R.string.rss_url_intent);
		List<String> rssFeeds = new ArrayList<String>();
		
        ListView v = getListView();
        int vCnt = v.getCount();
        Cursor c;
        
        // step 1. Loop through cursor and add links associated with selected row elements
        // 	to a list
        for (int i = 0; i < vCnt; i++) {
        	c = (Cursor)v.getItemAtPosition(i);
        	if (c == null) {
        		Log.e(TAG, "Error: Cursor element is null: should never happen!");
        		continue;
        	}
        	
        	if (this.selectedListItems.contains(i))
        		rssFeeds.add(c.getString(c.getColumnIndex(FeedOrm.COLUMN_URL)));
        }
        
        int size = rssFeeds.size();
        
        if (size == 0) {
			Toast.makeText(getActivity().getApplicationContext(), getResources().getText(R.string.no_selections).toString(),  Toast.LENGTH_SHORT).show();
        	return;
        }
        
        // step 2. load the list
	    intent.putStringArrayListExtra(key, (ArrayList<String>) rssFeeds);
		startActivity(intent);
    }
	
	@Override
	public boolean cabRespondToMenuItemClick(ActionMode mode,
			MenuItem item) {
		switch (item.getItemId()) {
	        case R.id.action_load_selected:
	            cabOnMultipleItemClick();
	            mode.finish();
	            return true;
	            
	        case R.id.action_select_all:
	        	selectAll();
	        	return true;
	        	
	        case R.id.action_deselect_all:
	        	deselectAll();
	        	return true;
	        	
	        case R.id.action_remove_selected:
	        	if (selectedListItems.isEmpty())
	        		return false;
	        	deleteSelected();
	            mode.finish();
	        	return true; 
	        	
	        default:
	            return false;
		}
	}
    
    public void deleteSelected() {
        ListView v = getListView();
        Cursor c;
        String link;
    	int vCnt = v.getCount();
    	
        // step 1. Loop through cursor and add links associated with selected row elements
        // 	to a list
        for (int i = 0; i < vCnt; i++) {
        	c = (Cursor)v.getItemAtPosition(i);
        	if (c == null) {
        		Log.e(TAG, "Error: Cursor element is null: should never happen!");
        		continue;
        	}
        	
        	if (this.selectedListItems.contains(i)) {
        		link = c.getString(c.getColumnIndex(FeedOrm.COLUMN_URL));
        		Log.i(TAG, "Deleting link " + link);
        		FeedOrm.deleteFeedWithLink(link, v.getContext());
        	}
        }
        // step 2. Call the callback function to refresh the currently selected group
		Callback callbackInterface = (Callback) getActivity();
		callbackInterface.handleCallbackEvent(1, null);
    }
    
    
// Contextual action bar setup
    @Override
    public void setUpAdapter() {
		String[] columns;
		int[] to;
		Cursor cursor;		
		
		columns = new String[] {
			FeedOrm.COLUMN_NAME,
			FeedOrm.COLUMN_URL
		};
		to = new int[] { 
		    R.id.feed_name,
		    R.id.feed_url
		};
		cursor = FeedOrm.selectAllOrderBy(getActivity().getApplicationContext(), FeedOrm.COLUMN_NAME);

		// create the adapter using the cursor pointing to the desired data 
		// as well as the layout information
		setAdapter(cursor, columns, to, R.layout.feed_list_row);
    }
    
    // refresh current data with a new selection based on category
    public void loadCategoryData(String category) {
    	if (category == null)
    		return;
    	Cursor c;
    	if (category == "*")
    		c = FeedOrm.selectAllOrderBy(getActivity().getApplicationContext(), FeedOrm.COLUMN_NAME);
    	else
    		c = FeedOrm.selectAllOrderByWhere(getActivity().getApplicationContext(), FeedOrm.COLUMN_NAME, category);
    	
    	/* c == null when there are no feeds with that category. Alert the UI to redraw its category selector 
    	 * and redraw what it wants upon an empty set 
    	 */
    	if (c == null) {
    		Callback callbackInterface = (Callback) getActivity();
    		callbackInterface.handleCallbackEvent(2, null);
    		return;
    	}
    		
    		
    	this.replaceCurrentData(c);
    }


	@Override
	public int cabGetMenuLayoutId() {
		return R.menu.feeds_cab;
	}
}
