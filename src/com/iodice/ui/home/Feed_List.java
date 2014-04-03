package com.iodice.ui.home;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.iodice.database.feedsOrm;
import com.iodice.rssreader.R;
import com.iodice.ui.feed.Activity_Rss;
import com.iodice.utilities.callback;


@SuppressLint("UseValueOf")
public class Feed_List extends List_Base {
	
	private final String TAG = "Feed_List";
    
    
	@Override
	public void onSingleItemClick(View view) {
		Intent intent = new Intent(getActivity(), Activity_Rss.class);
		String key = getResources().getString(R.string.rss_url_intent);
		
		List<String> rssFeeds = new ArrayList<String>();
		TextView innerView = (TextView)view.findViewById(R.id.feed_url);
		rssFeeds.add(innerView.getText().toString());

	    intent.putStringArrayListExtra(key, (ArrayList<String>) rssFeeds);
		startActivity(intent);		
	}
	

	@Override
	public void onMultipleItemClick() {
		Intent intent = new Intent(getActivity(), Activity_Rss.class);
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
        		rssFeeds.add(c.getString(c.getColumnIndex(feedsOrm.COLUMN_URL)));
        }
        
        int size = rssFeeds.size();
        
        if (size == 0) {
			Toast.makeText(getActivity().getApplicationContext(), getResources().getText(R.string.no_selections).toString(),  Toast.LENGTH_SHORT).show();
        	return;
        }
        
        
        for (int i = 0; i < size; i++) {
        	Log.i(TAG, "Loading " + rssFeeds.get(i));
        }
        
        // step 2. load the list
	    intent.putStringArrayListExtra(key, (ArrayList<String>) rssFeeds);
		startActivity(intent);
    }
	
	
    @Override
    // this logic is applied to each list row whenever the list needs to be re-drawn. It tracks whether or not
    // a row should have its checkboxes visible and/or checked
    public View onListElementRedraw(int position, View v, ViewGroup parent) {
    	CheckBox bx = (CheckBox) v.findViewById(R.id.item_checkbox);

    	Log.i(TAG, "redrawing element " + position);

		if (isInActionMode) {
	    	bx.setVisibility(View.VISIBLE);
	    	if (selectedListItems.contains(position)) {
	    		bx.setChecked(true);
	    	} else {
	    		// The view may have been converted. If so, the checkbox needs to be
	    		// manually unchecked
	    		bx.setChecked(false);
	    	}
	    }
		return v;
    }

	/* if all are selected, deselect all */
    void selectAll() {
        ListView v = getListView();
        int vCnt = v.getCount();
        
        if (this.selectedListItems.size() == vCnt) {
        	deselectAll();
        	return;
        }
        // clear first in order to avoid repeating items in the list
        this.selectedListItems.clear();
        for (int i = 0; i < vCnt; i++)
        	this.selectedListItems.add(i);
        redrawListView();
    }

    void deselectAll() {
        selectedListItems.clear();
        redrawListView();
    }
    
    public void selectCheckBox(int position) {
    	ListView v = getListView();
        CheckBox bx; 

		// select list item. Need to account for the the position reported being the actual list position, and
        // getChildAt(position) returning the position relative to the list items visible on the screen. 
    	bx = (CheckBox) v.getChildAt(position - v.getFirstVisiblePosition()).findViewById(R.id.item_checkbox);
        
        if (selectedListItems.contains(position) == false) {
        	selectedListItems.add(position);
            bx.setChecked(true);
        } else {
        	selectedListItems.remove(new Integer(position));
        	bx.setChecked(false);
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
        		link = c.getString(c.getColumnIndex(feedsOrm.COLUMN_URL));
        		Log.i(TAG, "Deleting link " + link);
        		feedsOrm.deleteFeedWithLink(link, v.getContext());
        	}
        }
        // step 2. Call the callback function to refresh the currently selected group
		callback callbackInterface = (callback) getActivity();
		callbackInterface.respondToEvent(1, null);
    }
    
    
// Contextual action bar setup
    @Override
    public void setUpAdapter() {
		String[] columns;
		int[] to;
		Cursor cursor;		
		
		columns = new String[] {
			feedsOrm.COLUMN_NAME,
			feedsOrm.COLUMN_URL
		};
		to = new int[] { 
		    R.id.feed_name,
		    R.id.feed_url
		};
		cursor = feedsOrm.selectAllOrderBy(getActivity().getApplicationContext(), feedsOrm.COLUMN_NAME);

		// create the adapter using the cursor pointing to the desired data 
		// as well as the layout information
		setAdapter(cursor, columns, to, R.layout.feed_list_view);
    }
    
    // refresh current data with a new selection based on category
    public void loadCategoryData(String category) {
    	if (category == null)
    		return;
    	Cursor c;
    	if (category == "*")
    		c = feedsOrm.selectAllOrderBy(getActivity().getApplicationContext(), feedsOrm.COLUMN_NAME);
    	else
    		c = feedsOrm.selectAllOrderByWhere(getActivity().getApplicationContext(), feedsOrm.COLUMN_NAME, category);
    	this.replaceCurrentData(c);
    }

    @Override
	// sets up contextual action bar actions
	public MultiChoiceModeListener getChoiceListener() {
		return new MultiChoiceModeListener() {
			
		    @Override
	        // Inflate the menu for the CAB
		    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		    	
		    	Log.i(TAG, "Creating contextual action bar");
		        
		    	MenuInflater inflater = mode.getMenuInflater();
		        inflater.inflate(R.menu.main_name_context, menu);
		        ListView v = getListView();
		        int vCnt = v.getCount();
		        View child;
		        CheckBox bx;
		        
		        isInActionMode = true;
		        
		        for (int i = 0; i < vCnt; i++) {
		        	child = v.getChildAt(i);
		        	if (child == null)
		        		continue;
		        	bx = (CheckBox) child.findViewById(R.id.item_checkbox);
		        	Log.i(TAG, "tst");
		        	bx.setVisibility(View.VISIBLE);
		        	bx.setChecked(false);
		        }
		        
		        return true;
		    }
		
		    @Override
	        // Respond to clicks on the actions in the CAB
		    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		        switch (item.getItemId()) {
		            case R.id.action_load_selected:
		                onMultipleItemClick();
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
		            	//redrawListView();
		            	return true; 
		            	
		            default:
		                return false;
		        }
		    }
			
			@Override
	        // Here you can do something when items are selected/de-selected,
	        // such as update the title in the CAB
		    public void onItemCheckedStateChanged(ActionMode mode, int position,
		                                          long id, boolean checked) {
				selectCheckBox(position);
			}
		    @Override
	        // Here you can make any necessary updates to the activity when
	        // the CAB is removed. By default, selected items are deselected/unchecked.
		    public void onDestroyActionMode(ActionMode mode) {
		        ListView v = getListView();
		        int vCnt = v.getChildCount();
		        CheckBox bx; 

		        // also deselect the checkboxes because they dont correspond to the CAB's notion
		        // of what is selected
		        for (int i = 0; i < vCnt; i++) {
		        	bx = (CheckBox) v.getChildAt(i).findViewById(R.id.item_checkbox);
		        	bx.setChecked(false);
		        	bx.setVisibility(View.GONE);
		        }
		        
		        // clear out selected items
		        selectedListItems.clear();
		        isInActionMode = false;
		    }
		    @Override
	        // Here you can perform updates to the CAB due to
	        // an invalidate() request
		    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		    	return false;
		    }
		
		};
	}

}
