package com.iodice.ui.topics;


import java.util.ArrayList;
import java.util.List;

import android.app.FragmentManager;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.EditText;

import com.iodice.database.SearchesOrm;
import com.iodice.rssreader.R;
import com.iodice.ui.articles.ArticleActivity;
import com.iodice.ui.articles.ArticleList;
import com.iodice.ui.base.CabMultiselectList.MySimpleCursorAdapter;

public class TopicsActivity extends ArticleActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		this.filterListInclusive = true;
		this.showSearchBar = false;
		super.onCreate(savedInstanceState);
	}
	
	public boolean isActionBarDrawerIndicatorVisible() {
		return true;
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
		ArticleList articleList = (ArticleList) fMan.findFragmentByTag(ArticleActivity.LIST);
		
		if (articleList != null && this.spinnerListItemPrimaryKeys != null) {
			String filterTerms = getFilterFromSpinnerList(position);
			MySimpleCursorAdapter adapt = (MySimpleCursorAdapter)articleList.getListAdapter();
			adapt.getFilter().filter(filterTerms);
		}
		return true;
	}

	private String getFilterFromSpinnerList(int position) {
		String filterTerms = this.spinnerListItemPrimaryKeys.get(position);
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
		// add the current search text to the filter terms as well
		EditText searchText = (EditText)findViewById(R.id.article_search_box_text);
		if (searchText != null) {
			filterTerms += " ";
			filterTerms += searchText.getText().toString();
		}
		return filterTerms;
	}
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
}
