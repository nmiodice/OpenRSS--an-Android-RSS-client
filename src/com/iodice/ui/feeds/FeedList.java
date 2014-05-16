package com.iodice.ui.feeds;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import android.view.ActionMode;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.iodice.database.FeedOrm;
import com.iodice.rssreader.R;
import com.iodice.ui.articles.ArticleActivityByUrl;
import com.iodice.ui.base.AnimatedEntryList;
import com.iodice.utilities.Callback;
import com.iodice.utilities.ConfirmDeleteDialog;
import com.iodice.utilities.SelectorRefreshCallback;


@SuppressLint("UseValueOf")
public class FeedList extends AnimatedEntryList implements Callback {
	
	private final String TAG = "Feed_List";
	private static final int CALLBACK_INITIATE_DELETE_TASK = 0;
    
    
	@Override
	public void onSingleItemClick(View view) {
		Intent intent = new Intent(getActivity(), ArticleActivityByUrl.class);
		String feedUrlKey = ArticleActivityByUrl.INTENT_EXTRA_URL_LIST;
		String feedNameKey = ArticleActivityByUrl.INTENT_EXTRA__FEED_NAME_LIST;
		
		ArrayList<String> feedUrls = new ArrayList<String>();
		ArrayList<String> feedNames = new ArrayList<String>();
		
		TextView innerView = (TextView)view.findViewById(R.id.feed_url);
		feedUrls.add(innerView.getText().toString());
		
		innerView = (TextView)view.findViewById(R.id.feed_name);
		feedNames.add(innerView.getText().toString());

	    intent.putStringArrayListExtra(feedUrlKey, feedUrls);
	    intent.putStringArrayListExtra(feedNameKey, feedNames);

		startActivity(intent);		
	}
	

	@Override
	public void cabMultiselectPrimaryAction() {
		Intent intent = new Intent(getActivity(), ArticleActivityByUrl.class);
		String feedUrlKey = ArticleActivityByUrl.INTENT_EXTRA_URL_LIST;
		String feedNameKey = ArticleActivityByUrl.INTENT_EXTRA__FEED_NAME_LIST;

		ArrayList<String> selectedFeedUrls = (ArrayList<String>)this.getSelectedUrlData(FeedOrm.COLUMN_URL);
		ArrayList<String> selectedFeedNames = (ArrayList<String>)this.getSelectedUrlData(FeedOrm.COLUMN_NAME);
		
        int size = selectedFeedUrls.size();
        
        if (size == 0) {
			Toast.makeText(getActivity().getApplicationContext(), getResources().getText(R.string.no_selections).toString(),  Toast.LENGTH_SHORT).show();
        	return;
        }
        
        // step 2. load the list
	    intent.putStringArrayListExtra(feedUrlKey, (ArrayList<String>) selectedFeedUrls);
	    intent.putStringArrayListExtra(feedNameKey, selectedFeedNames);

		startActivity(intent);
    }
	
	@Override
	public boolean cabOnMenuItemClicked(ActionMode mode,
			MenuItem item) {
		switch (item.getItemId()) {
	        case R.id.action_load_selected:
	            cabMultiselectPrimaryAction();
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
	        	deleteFeedsWithUrls(this.getSelectedUrlData(FeedOrm.COLUMN_URL));
	            mode.finish();
	        	return true; 
	        case R.id.action_save_group:
	        	saveSelectedAsGroup();
	            mode.finish();
	        	return true;
	        	
	        default:
	            return false;
		}
	}
    
	// a url list must be provided because it is likely that the list has been
	// cleared out (by ending the contextual action bar mode) prior to the
	// actual call to delete is made
    private void deleteFeedsWithUrls(List<String> selectedUrlList) {
    	Context context = getActivity();
    	AlertDialog alertDialog = 
    			ConfirmDeleteDialog.getDeleteDialog(context, 
    					this, 
    					FeedList.CALLBACK_INITIATE_DELETE_TASK,
    					selectedUrlList);
    	alertDialog.show();
    }
    
    private void saveSelectedAsGroup() {
    	List<String> selectedUrls = this.getSelectedUrlData(FeedOrm.COLUMN_URL);
		AlertDialog.Builder alert = AddNewGroupingDialog.getAddDialog(selectedUrls,
				getActivity(),
				(SelectorRefreshCallback)getActivity());
		alert.show();
    }
    
    private List<String> getSelectedUrlData(String columnName) {
    	ArrayList<String> urlDataList = new ArrayList<String>();
    	ListView v = getListView();
        int vCnt = v.getCount();
        Cursor c;
        
    	if (this.isInActionMode == false)
    		return urlDataList;

        
        // step 1. Loop through cursor and add links associated with selected row elements
        // 	to a list
        for (int i = 0; i < vCnt; i++) {
        	c = (Cursor)v.getItemAtPosition(i);
        	if (c == null) {
        		Log.e(TAG, "Error: Cursor element is null: should never happen!");
        	} else if (this.selectedListItems.contains(i))
        		urlDataList.add(c.getString(c.getColumnIndex(columnName)));
        }
    	return urlDataList;
    }
    
    
    @Override
    public void setUpAdapter() {
		PopulateListData asyncTask = new PopulateListData();
		asyncTask.execute();
    }
    
    // refresh current data with a new selection based on category
    public void loadCategory(String category) {
    	if (category == null)
    		return;
    	Cursor c;
    	if (category == "*")
    		c = FeedOrm.selectAllOrderBy(getActivity().getApplicationContext(), FeedOrm.COLUMN_NAME);
    	else
    		c = FeedOrm.selectAllOrderByWhereCategoryIs(getActivity().getApplicationContext(), FeedOrm.COLUMN_NAME, category);
    	
    	/* c == null when there are no feeds with that category. Alert the UI to redraw its category selector 
    	 * and redraw what it wants upon an empty set 
    	 */
    	if (c == null) {
    		Callback callbackInterface = (Callback) getActivity();
    		callbackInterface.handleCallbackEvent(FeedActivity.CALLBACK_REFRESH_CATEGORY_SELECTOR, category);
    		Log.i(TAG, "callback interface");
    		return;
    	}
    		
    	Log.i(TAG, "replacing data set");
    	this.replaceCurrentData(c);
    }


	@Override
	public int cabGetMenuLayoutId() {
		return R.menu.feeds_cab;
	}
	
    @Override
    public View onListElementRedraw(int position, View convertView, ViewGroup parent) { 
    	convertView = super.onListElementRedraw(position, convertView, parent);
    	if (convertView != null) {
    		int listItemCount = this.getListAdapter().getCount();
    		
    		// the list should always end with an item colored in zebra color # 2
    		if (listItemCount % 2 == 0) {
		    	if (position % 2 == 1)
		    		convertView.setBackgroundColor(getResources().getColor(R.color.rss_list_zebra_color_2));
		    	else
		    		convertView.setBackgroundColor(getResources().getColor(R.color.rss_list_zebra_color_1));    			
    		} else {
		    	if (position % 2 == 0)
		    		convertView.setBackgroundColor(getResources().getColor(R.color.rss_list_zebra_color_2));
		    	else
		    		convertView.setBackgroundColor(getResources().getColor(R.color.rss_list_zebra_color_1));
    		}
    	} else
	    	throw new NullPointerException();
	    
    	return convertView;
    }

	@Override
	/* n = CALLBACK_INITIATE_DELETE_TASK: 
	 * 	delete the links contained in the referenced object
	 */
	public void handleCallbackEvent(int n, Object obj)
			throws UnsupportedOperationException {
		switch (n) {
			case FeedList.CALLBACK_INITIATE_DELETE_TASK:
				assert (obj != null);
				@SuppressWarnings("unchecked")
				List<String> toDelete = (List<String>) obj;
				
				DeleteSelectedFeeds task = new DeleteSelectedFeeds();
				task.setSelectedUrlList(toDelete);
				task.execute();
				return;
			
			default:
				throw new UnsupportedOperationException();
		}
	}
	
	
	
	
	/**
	 * A simple async task used to delete currently selected feed definitions. Principle
	 * caller is FeedList.deleteSelectedUrls().
	 * 
	 * @author Nicholas M. Iodice
	 */
	private class DeleteSelectedFeeds extends AsyncTask<Void, Void, Boolean> {
		private static final String TAG = "DeleteSelectedFeeds";
		Context context = null;
		List<String> selectedUrlList = null;
		
		// necessary to call this if invoked from any thread other than the main UI thread.
		public void setSelectedUrlList(List<String> selectedUrlList) {
			this.selectedUrlList = selectedUrlList;
		}
		
		
		// get selected URLs from UI thread
		protected void onPreExecute() {
			// query for selected URLs only if not set explicitly. This should only happen
			// when the task is called from the main UI thread
			if (this.selectedUrlList == null)
				selectedUrlList = getSelectedUrlData(FeedOrm.COLUMN_URL);
	        context = getListView().getContext();	        
		}
		
		// process db request in background thread
		protected Boolean doInBackground(Void... arg0) {
			if (this.context == null) {
				Log.e(TAG, "Error: context is null!");
				return false;
			} else if (this.selectedUrlList == null) {
				Log.e(TAG, "Error: selectedUrlList is null!");
				return false;
			}
			
	        int numSelected = selectedUrlList.size();
	        String link;
	    	
	        // step 1: delte all selected feeds
	        for (int i = 0; i < numSelected; i++) {
	        	link = selectedUrlList.get(i);
	    		FeedOrm.deleteFeedWithLink(link, context);
	    		Log.i(TAG, "Deleted link " + link);
	        }
			return true;
		}
		
        // Call the callback function to refresh UI
		protected void onPostExecute(Boolean success) {
			if (!success) {
				Log.e(TAG, "Error: Async Task failed to delete");
				return;
			}
			Callback callbackInterface = (Callback) getActivity();
			if (callbackInterface != null)
				callbackInterface.handleCallbackEvent(FeedActivity.CALLBACK_REPOPULATE_DATA_AND_REFRESH_CATEGORY_SELECTOR, null);
			else 
				Log.w(TAG, "Detected null callbackInterface! Cannot update UI thread.");
		}
	}
	
	// allows the list to process a query asynchronously, cutting down work in the main UI thread
	private class PopulateListData extends AsyncTask<Void, Void, Cursor> {
		
		protected void onPreExecute() {
			setLoadState(false);
		}
		
		// process db request in background thread
		protected Cursor doInBackground(Void... arg0) {
			Cursor cursor;		
			cursor = FeedOrm.selectAllOrderBy(getActivity().getApplicationContext(), FeedOrm.COLUMN_NAME);
			return cursor;
		}
		
		protected void onPostExecute(Cursor cursor) {
			String[] columns;
			int[] to;
			
			columns = new String[] {
				FeedOrm.COLUMN_NAME,
				FeedOrm.COLUMN_URL
			};
			to = new int[] { 
			    R.id.feed_name,
			    R.id.feed_url
			};
			// create the adapter using the cursor pointing to the desired data 
			// as well as the layout information
			setAdapter(cursor, columns, to, R.layout.feed_list_row);
		}
	}

}
