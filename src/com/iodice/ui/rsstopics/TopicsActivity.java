package com.iodice.ui.rsstopics;

import java.util.ArrayList;
import java.util.List;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import com.iodice.database.SearchesOrm;
import com.iodice.rssreader.R;
import com.iodice.ui.articles.ArticleActivity;
import com.iodice.ui.articles.ArticleList;
import com.iodice.ui.base.NavigationDrawerWithSpinner;
import com.iodice.utilities.ListRefreshCallback;

public class TopicsActivity extends ArticleActivity implements ListRefreshCallback {
	private final static String TAG = "TopicsActivity";
	private static final String LIST = "LIST";

	

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

	@Override
	public int getViewLayoutId() {
		return R.layout.article_activity;
	}

	@Override
	public int[] getViewsToHidewOnNavigationBarOpen() {
		return new int[]{};
	}

	@Override
	protected boolean isActionBarNavDrawerIndicatorVisible() {
		return true;
	}
	
	private void redrawActiveArticleListWithCachedData() {
		FragmentManager fMan = getFragmentManager();
		ArticleList articleList = (ArticleList) fMan.findFragmentByTag(TopicsActivity.LIST);
		
		// fragContainer is null until something is added to it
		if (articleList != null) {
			articleList.setLoadState(false);

			articleList.setUpAdapter();
			articleList.redrawListView();
			// load state may have been set to true if the list was requested to update
			// its data from the web
		}
	}
	
	public void handleCallbackEvent(int n, Object obj) {
		switch (n) {
			case ArticleActivity.CALLBACK_REDRAW_WITH_CACHED_DATA:
				this.redrawActiveArticleListWithCachedData();
				return;

			default:
				throw new UnsupportedOperationException();
		}
	}

	@Override
	public void refreshCurrentList() {
		this.handleCallbackEvent(ArticleActivity.CALLBACK_REDRAW_WITH_CACHED_DATA, null);
	}

}
