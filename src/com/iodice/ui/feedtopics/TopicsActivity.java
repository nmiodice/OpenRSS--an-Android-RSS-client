package com.iodice.ui.feedtopics;

import java.util.ArrayList;

import android.app.FragmentManager;
import android.database.Cursor;
import android.widget.ArrayAdapter;

import com.iodice.database.SearchesOrm;
import com.iodice.rssreader.R;
import com.iodice.ui.articles.ArticleActivity;
import com.iodice.ui.articles.ArticleList;
import com.iodice.ui.base.MultiselectList.MySimpleCursorAdapter;

public class TopicsActivity extends ArticleActivity {
	
	public boolean isActionBarNavDrawerIndicatorVisible() {
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

	@Override
	public boolean onSpinnerItemClick(int position, long id) {
		FragmentManager fMan = getFragmentManager();
		ArticleList articleList = (ArticleList) fMan.findFragmentByTag(ArticleActivity.LIST);
		if (articleList != null && this.spinnerListItemPrimaryKeys != null) {
			String filterTerm = this.spinnerListItemPrimaryKeys.get(position);
			MySimpleCursorAdapter adapt = (MySimpleCursorAdapter)articleList.getListAdapter();
			adapt.getFilter().filter(filterTerm);
		}
		return true;
	}

	@Override
	public AdapterListPair backgroundSpinnerQuery() {
		// populate list data
		Cursor c = SearchesOrm.selectAll(getApplicationContext());
		ArrayList<String> items = new ArrayList<String>();
		items.add(getString(R.string.all));
		
		
		c.moveToFirst();
		while(!c.isAfterLast()) {
		     items.add(c.getString(c.getColumnIndex(SearchesOrm.COLUMN_SEARCH_TERM)));
		     c.moveToNext();
		}
		
		// note: actionBar.getThemedContext() ensures the correct colors based on the action bar theme
		ArrayAdapter<String> aAdpt = new ArrayAdapter<String>(getActionBar().getThemedContext(),
				android.R.layout.simple_list_item_1, 
				android.R.id.text1, 
				items);
		AdapterListPair queryPair = new AdapterListPair();
		queryPair.setAdapter(aAdpt);
		queryPair.setComparisonKeyList(items);
		return queryPair;
	}
}
