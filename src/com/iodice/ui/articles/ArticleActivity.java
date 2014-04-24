package com.iodice.ui.articles;

import android.widget.ArrayAdapter;

import com.iodice.ui.rsstopics.TopicsActivity;


/* this is essentially a standard article list but without any spinner
 * to go along with it. All of the methods here must override the ones
 * related to spinners so that one is not added.
 */
public class ArticleActivity extends TopicsActivity {
	@Override
	public void setupCategorySpinner() {}
	@Override
	public void setupCategorySpinnerWithSelection(String selection) {}
	@Override
	public boolean onSpinnerItemClick(int position, long id) {
		return false;
	}
	@Override
	public ArrayAdapter<String> backgroundSpinnerQuery() {
		return null;
	}
}