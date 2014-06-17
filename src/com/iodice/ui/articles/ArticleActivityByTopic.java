package com.iodice.ui.articles;


import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar.OnNavigationListener;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.iodice.database.SearchesOrm;
import com.iodice.rssreader.R;
import com.iodice.ui.base.CabMultiselectList.MySimpleCursorAdapter;
import com.iodice.utilities.ConfirmationDialog;

public class ArticleActivityByTopic extends ArticleActivityByUrl {
	
	private final static int CALLBACK_DELETE_CURRENT_SPINNER_ITEM = 20;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		this.filterListInclusive = true;
		this.searchBarEnabled = false;
		super.onCreate(savedInstanceState);
	}
	
	public boolean isActionBarDrawerIndicatorVisible() {
		return true;
	}
	@Override
	public int[] getViewsToHidewOnDrawerOpen() {
		return new int[] {
				R.id.action_refresh,
				R.id.action_delete_searches,
				R.id.action_refresh,
		        R.id.action_settings,
		        R.id.action_view_read,
		};	
	}
	
	// displayed in top right of activity, lists categories by name. upon select, filter list to just those categories
	public void setupCategorySpinner() {
		PopulateActionBarSpinner asyncTask = new PopulateActionBarSpinner();
		asyncTask.setOnNavigationListener(this);
		asyncTask.execute();		
	}
	
	public void setupCategorySpinnerWithSelection(String selection) {
		PopulateActionBarSpinner asyncTask = new PopulateActionBarSpinner();
		asyncTask.setOnNavigationListener(this);
		if (selection != null)
			asyncTask.setSelectionIfPossible(selection);
		asyncTask.execute();
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.articles_with_spinner_filter, menu);
        return true;
    }
    
	@Override
	/**
	 * Handle a callback event, or pass to parent.
	 */
	public void handleCallbackEvent(int n, Object obj) {
		switch (n) {
			case ArticleActivityByTopic.CALLBACK_DELETE_CURRENT_SPINNER_ITEM:
				this.deleteSelectedSpinnerItem();
				return;
			default:
				super.handleCallbackEvent(n, obj);
		}
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		boolean wasHandled = super.onOptionsItemSelected(item);
		if (wasHandled)
			return wasHandled;
		
		switch (item.getItemId()) {
			// delete the current selected key
			case R.id.action_delete_searches:
				int currentKeyIdx = getSelectedNavigationIndex();
				String currentKey = getSpinnerListPrimaryKeys().get(currentKeyIdx);
				String all = this.getString(R.string.all);
				
				if (!currentKey.equals(all)) {
			    	AlertDialog alertDialog = ConfirmationDialog.getDeleteDialog(this, 
	    					this,
	    					ArticleActivityByTopic.CALLBACK_DELETE_CURRENT_SPINNER_ITEM, 
	    					null);
			    	alertDialog.show();
				} else {
					CharSequence text = this.getText(R.string.cannot_delete_all);
					int duration = Toast.LENGTH_SHORT;
					Toast toast = Toast.makeText(this, text, duration);
					toast.show();
				}

				return true;
			default:
				return false;
		}
	}
	
	private void deleteSelectedSpinnerItem() {
		int currentKeyIdx = getSelectedNavigationIndex();
		String currentKey = getSpinnerListPrimaryKeys().get(currentKeyIdx);
		
		DeleteSavedSearchAndRepopulate asyncTask = new DeleteSavedSearchAndRepopulate();
		asyncTask.setOnNavigationListener(this);
		asyncTask.setPrimaryKeyToDelete(currentKey);
		asyncTask.execute();
	}
	
    /**
     * Configures the current search bar to listen for text changes & trigger a
     * query on the current list of articles. The currently selected topic is
     * also included as a filter term
     */
	/* SEARCH BAR IS DISABLED
	protected void addSearchBarListener() {
    	EditText txtBox = (EditText)findViewById(R.id.article_search_box_text);
    	if (this.searchBarListener != null)
    		txtBox.removeTextChangedListener(this.searchBarListener);
    	this.searchBarListener = new TextWatcher() {
    	    @Override
    	    public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
    			FragmentManager fMan = getFragmentManager();
    			ArticleList articleList = (ArticleList) fMan.findFragmentByTag(ArticleActivity.LIST);
    			if (articleList != null) {
    				MySimpleCursorAdapter adapt = (MySimpleCursorAdapter)articleList.getListAdapter();
    				
    				String selectedTopic = getSpinnerListPrimaryKeys().
    						get(getSelectedNavigationIndex()); 
    				adapt.getFilter().filter(cs + " " + selectedTopic);
    			}
    	    }
    	    @Override
    	    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
    	    @Override
    	    public void afterTextChanged(Editable arg0) {}
    	};
    	txtBox.addTextChangedListener(searchBarListener);
    }
	*/
	@Override
	public boolean onSpinnerItemClick(int position, long id) {		
		FragmentManager fMan = getFragmentManager();
		ArticleList articleList = (ArticleList) fMan.findFragmentByTag(ArticleActivityByUrl.LIST);
		
		if (articleList != null && this.spinnerListItemPrimaryKeys != null) {
			String filterTerms = getFilterFromSpinnerList(position);
			
			// give a bogus query if there is nothing asked for, otherwise the
			// list will show every article
			// TODO: improve on the logic to accomplish the described goal
			if (filterTerms.equals(""))
				filterTerms = "abcdefghijklmnopqrstuvwqyz";
			
			MySimpleCursorAdapter adapt = (MySimpleCursorAdapter)articleList.getListAdapter();
			adapt.getFilter().filter(filterTerms);
		}
		return true;
	}

	private String getFilterFromSpinnerList(int position) {
		String filterTerms = "";
		
		if (position >= 0 && position < spinnerListItemPrimaryKeys.size())
			filterTerms = spinnerListItemPrimaryKeys.get(position);
		if (filterTerms == getText(R.string.all)) {
			filterTerms = "";
			int size = this.spinnerListItemPrimaryKeys.size();
			
			for (int i = 0; i < size; i++) {
				// should skip the "all" entry
				if (i == position)
					continue;
				filterTerms += spinnerListItemPrimaryKeys.get(i);
				filterTerms += ", ";
			}
		}
		/*
		// add the current search text to the filter terms as well
		EditText searchText = (EditText)findViewById(R.id.article_search_box_text);
		if (searchText != null) {
			filterTerms += " ";
			filterTerms += searchText.getText().toString();
		} 
		*/
		return filterTerms;
	}
	
	/**
	 * Requery the database for current spinner list keys
	 */
	@Override
	public List<String> getSpinnerListPrimaryKeys() {
		// populate list data
		Cursor c = SearchesOrm.selectAll(getApplicationContext());
		ArrayList<String> items = new ArrayList<String>();
		items.add(getString(R.string.all));
		c.moveToFirst();
		while(!c.isAfterLast()) {
		     items.add(c.getString(c.getColumnIndex(SearchesOrm.COLUMN_SEARCH_TERM)));
		     c.moveToNext();
		}
		return items;
	}
	
	@Override
	public String getSpinnerTitleText() {
		return getText(R.string.saved_searches).toString();
	}
	
	/**
	 * Refilter the list based on the currently selected search. No need to call
	 * the superclass because this activity filters based on something entireley
	 * different (spinner list, not search bar)
	 */
	@Override
	protected void refilterArticles() {
		int currPos = this.getSelectedNavigationIndex();
		// second parameter is unused
		onSpinnerItemClick(currPos, -1);
	}
	
	/**
	 * A simple async task that will delete a saved search and then repopulate the
	 * spinner with the current list
	 * 
	 * @author Nicholas M. Iodice
	 *
	 */
	public class DeleteSavedSearchAndRepopulate extends AsyncTask<Void, Void, Void> {

		private OnNavigationListener listener = null;
		private String deleteKey = null;
		private String redrawWith = null;
		private Context context = null;
		
		public void setOnNavigationListener(OnNavigationListener listener) {
			this.listener = listener;
		}
		
		public void setContext(Context context) {
			this.context = context;
		}
		
		/**
		 * Delete the specified key
		 * @param key
		 */
		public void setPrimaryKeyToDelete(String key) {
			this.deleteKey = key;
		}
		
		/**
		 * Redraw the spinner with this key selected (if possible)
		 * @param key
		 */
		public void setRedrawKey(String key) {
			this.redrawWith = key;
		}
		
		// process db request in background thread
		@Override
		protected Void doInBackground(Void... params) {
			if (deleteKey == null)
				return null;
			SearchesOrm.deleteSearchesWhereNameIs(deleteKey, context);
			return null;
		}
		
	    /**
	     * Redraw the spinner via a new async task
	     */
		@Override
		protected void onPostExecute(Void param) {
			// if listener == null, the activity has ended
			if (listener == null)
				return;
			
			PopulateActionBarSpinner asyncTask = new PopulateActionBarSpinner();
			asyncTask.setOnNavigationListener(listener);
			if (redrawWith != null)
				asyncTask.setSelectionIfPossible(redrawWith);
			asyncTask.execute();
		}
	
	}
}
