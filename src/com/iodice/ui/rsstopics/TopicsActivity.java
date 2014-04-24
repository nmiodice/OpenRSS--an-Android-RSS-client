package com.iodice.ui.rsstopics;

import java.util.ArrayList;

import android.database.Cursor;
import android.widget.ArrayAdapter;

import com.iodice.database.SearchesOrm;
import com.iodice.rssreader.R;
import com.iodice.ui.articles.ArticleActivity;

public class TopicsActivity extends ArticleActivity {

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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ArrayAdapter<String> backgroundSpinnerQuery() {
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

		spinnerListItems = items;
		return aAdpt;
	}
}
