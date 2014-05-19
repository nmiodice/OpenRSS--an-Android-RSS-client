package com.iodice.ui.articles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.iodice.application.SharedPrefsHelper;
import com.iodice.database.ArticleOrm;
import com.iodice.rssreader.R;
import com.iodice.ui.base.AnimatedEntryList;
import com.iodice.utilities.ListRefreshCallback;
import com.iodice.utilities.Text;

public class ArticleList extends AnimatedEntryList {
	
	private static final String TAG = "ArticleList";
	private List<String> articleURLList;
	private Typeface headline_font = null;
	
	// used primarly to avoid an infinite loop that can be caused if the feed has no
	// data, tries to re-query the web, and then fails again. Without keeping track
	// of the first failure, the loop wont exit
	private int loadFailCount = 0;
	private List<String> filterTerms = new ArrayList<String>();
	private List<String> columnsToFilterOn = Arrays.asList(new String[] {
			ArticleOrm.COLUMN_TITLE,
			ArticleOrm.COLUMN_DESCRIPTION,
			//ArticleOrm.COLUMN_PARENT_URL,
			//ArticleOrm.COLUMN_URL,
			});
	
	// If true, filtering will be based on 'include if any term matches.' If false,
	// filtering will be more strict and require each term to appear
	private boolean filterInclusive = false;
	private static final String ARTICLE_LIST_TAG = "ARTICLE_LIST_TAG";
	
	// this listview uses a tile layout, so the default divider isnt necessary
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		getListView().setDivider(null);
		getListView().setDividerHeight(0);
	}
	
	@Override
	// restores the article list if the parent activity has been terminated prior to invocation.
	// then call superclass's method to redraw the fragment
	public void onActivityCreated(Bundle savedInstanceState) {
		if (this.articleURLList == null && savedInstanceState != null) {
			this.articleURLList = savedInstanceState.getStringArrayList(ARTICLE_LIST_TAG);
		}
		super.onActivityCreated(savedInstanceState);
	}
		
	public void onSaveInstanceState (Bundle outState) {
		if (this.articleURLList == null)
			return;
		
		// the list of URLs will otherwise be lost if the parent activity is terminated by the OS
		outState.putStringArrayList(ARTICLE_LIST_TAG, (ArrayList<String>) this.articleURLList);
	}
	
	public void setFilterTerms(String contains) {
		contains = contains.trim();
		
		if (contains.length() == 0) {
			this.filterTerms = new ArrayList<String>();
		} else {
			// TODO: is this if statement necessary?
			if (contains.endsWith(",") == false)
				contains += ",";
			
			this.filterTerms = Text.getCleanStringListAsLowercase(contains.toString(), " ");  
		}
	}
		
	@Override
	// load article in browser, if avaliable
	public void onSingleItemClick(View view) {
		Intent browserIntent;
		TextView txtview = (TextView) view.findViewById(R.id.rss_url);
		String feedURL = txtview.getText().toString();

		Log.i(TAG, "Opening feed: " + feedURL + " in default browser");
		
		try {
			browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(feedURL));
			startActivity(browserIntent);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(getActivity().getApplicationContext(), R.string.no_installed_app,  Toast.LENGTH_LONG).show();
			e.printStackTrace();
		} catch (Exception e) {
			e.getMessage();
			e.printStackTrace();
		}
	}

	@Override
	// share selected article content
	public void cabMultiselectPrimaryAction() {
		Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
		sharingIntent.setType("text/plain");
		
        ListView v = getListView();
        Cursor c;
        String shareBody = "";
        int size = selectedListItems.size();
        
        // do nothing with an empty selection
        if (size == 0) {
			Toast.makeText(getActivity().getApplicationContext(), getResources().getText(R.string.no_selections).toString(),  Toast.LENGTH_SHORT).show();
    		return;
    	// format the subject = title
        } else if (size == 1) {
        	c = (Cursor)v.getItemAtPosition(selectedListItems.get(0));
     		shareBody = c.getString(c.getColumnIndex(ArticleOrm.COLUMN_DESCRIPTION));
     		shareBody += "\n" + c.getString(c.getColumnIndex(ArticleOrm.COLUMN_URL));
     		
    		sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, c.getString(c.getColumnIndex(ArticleOrm.COLUMN_TITLE)));
    		sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
    	// list the details of each together with a unique subject line
        } else {
            for (int i = 0; i < size; i++) {
            	c = (Cursor)v.getItemAtPosition(i);
            	if (c == null) {
            		Log.e(TAG, "Error: Cursor element is null: should never happen!");
            		continue;
            	}
        		shareBody += c.getString(c.getColumnIndex(ArticleOrm.COLUMN_TITLE));
        		shareBody += "\n" + c.getString(c.getColumnIndex(ArticleOrm.COLUMN_DESCRIPTION));
        		shareBody += "\n" + c.getString(c.getColumnIndex(ArticleOrm.COLUMN_URL));
        		shareBody += "\n\n";
            }
    		sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, this.getText(R.string.article_share_subject));
    		sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        }		
		startActivity(Intent.createChooser(sharingIntent, getString(R.string.share)));
	}

	@Override
	public void setUpAdapter() {
		String[] columns;
		int[] to;
		Cursor cursor = null;		
		
		columns = new String[] {
			ArticleOrm.COLUMN_TITLE,
			ArticleOrm.COLUMN_URL,
			ArticleOrm.COLUMN_PARENT_URL,
			ArticleOrm.COLUMN_AUTHOR,
			ArticleOrm.COLUMN_DESCRIPTION,
			ArticleOrm.COLUMN_PUBLISHED_DATE
		};
		
		to = new int[] { 
		    R.id.rss_title,
		    R.id.rss_url,
		    R.id.rss_base_url,
		    R.id.rss_author,
		    R.id.rss_description,
		    R.id.rss_published_date
		};
		
		cursor = getUpdatedQuery();
		Log.i(TAG, "" + cursor.getCount() + " articles loaded");

		// if there isnt any data, attempt a web query one time and then fail to load
		// data if the web query is unsuccessful. 
		// The case where multiple feeds are selected but not all feeds are cached in the D
		// 	is not handled in the current implementation. Background services regularly update
		//  the data & when a feed is added, an update is triggerd, so its likely not very necessary
		//  to handle this unhandled case
		if (cursor.getCount() == 0 && this.loadFailCount == 0) {
			this.loadFailCount++;
			ListRefreshCallback callbackInterface = (ListRefreshCallback) getActivity();
			callbackInterface.refreshCurrentList(false);
		} else {
			// create the adapter using the cursor pointing to the desired data 
			// as well as the layout information
			setAdapter(cursor, columns, to, getListItemLayoutID());
			MySimpleCursorAdapter adapt = (MySimpleCursorAdapter) this.getListAdapter();
			
			// set up the filter callback so that filtering can be handled asynchronously
			Log.i(TAG, "Setting query provider");
			adapt.setFilterQueryProvider(getFilterQueryProvider());
		}
	}
	
	/**
	 * Get an up-to-date query according to the current data set
	 * @return
	 */
	public Cursor getUpdatedQuery() {
		int maxArticles = SharedPrefsHelper.getNumArticlesToLoad(getActivity());
		Cursor cursor = ArticleOrm.selectWhereParentLinkIs(getActivity().getApplicationContext(), 
				this.articleURLList, 
				maxArticles);
		return cursor;
	}
	
	protected int getListItemLayoutID() {
		return R.layout.article_list_row;
	}
	
	/**
	 * only valid after the adapter is refreshed because the adapter setup code creates
	 * the filter query provider, which utilizes this value at creation time
	 */
	public void setFilterInclusive(boolean b) {
		this.filterInclusive = b;
	}
	
	/**
	 * Defines the query to be run when a filter is called. If this method is triggered
	 * multiple times in quick succession, only the last call will generate a valid
	 * filter query. Therefore, this can be called many times quickly without worrying
	 * about stale queries becoming active
	 * 
	 * @return
	 */
	private FilterQueryProvider getFilterQueryProvider() {
		return new FilterQueryProvider() {
			public Cursor runQuery(CharSequence constraint) {
				setFilterTerms(constraint.toString());
				
				int maxArticles = SharedPrefsHelper.getNumArticlesToLoad(getActivity());
				Cursor c = ArticleOrm.selectWhereParentLinkIsAndContains(getActivity(), 
						articleURLList, 
						filterTerms, 
						columnsToFilterOn,
						filterInclusive,
						maxArticles);
				Log.i(TAG, "cursor = " + c);
				Log.i(TAG, "filterTerms = " + filterTerms.toString());
				return c;
	         }
		};
	}

	@Override
	public View onListElementRedraw(int position, View convertView,
			ViewGroup parent) {
		// important to always call the parent, as it takes care of redrawing the checkboxes accurately
		// when in action mode
		View v = super.onListElementRedraw(position, convertView, parent);
		if (hiddenListItems.contains(position))
			return v;
		
		// the headline font is different. Lazy loading is ideal here because
		// this instance can be shared. This allows the animation to not get
		// bogged down ith disk i/o & object creation overhead
		if (headline_font == null) {
			Activity activity = getActivity();
			headline_font = Typeface.createFromAsset(
					activity.getAssets(), 
					activity.getText(R.string.tile_font_heavy).toString());
		}
		
		// hide any empty views
		TextView tmp = (TextView) v.findViewById(R.id.rss_author);
		if (tmp.getText().equals(""))
			tmp.setVisibility(View.GONE);
		else
			tmp.setVisibility(View.VISIBLE);

    	tmp = (TextView) v.findViewById(R.id.rss_description);
    	String desc = tmp.getText().toString();
        if (!desc.equals("")) {
        	int maxLen = getResources().getInteger(R.integer.article_description_max_length);
        	if (desc.length() > maxLen) {
        		desc = Text.limitTextCharacters(desc, maxLen);
        		desc += "...";
        	}
        	tmp.setText(desc);
        }
        else
        	tmp.setVisibility(View.GONE);
        
    	tmp = (TextView) v.findViewById(R.id.rss_title);
		if (tmp.getText().equals(""))
			tmp.setVisibility(View.GONE);
		else {
			tmp.setTypeface(headline_font);
			tmp.setVisibility(View.VISIBLE);
		}
			
		
        tmp = (TextView) v.findViewById(R.id.rss_base_url);
		if (tmp.getText().equals(""))
			tmp.setVisibility(View.GONE);  
		else
			tmp.setVisibility(View.VISIBLE);
		
		// the url never needs to be shown, it only holds data to launch the article
		// in a browser
        tmp = (TextView) v.findViewById(R.id.rss_url);
		tmp.setVisibility(View.GONE);   
        
    	tmp = (TextView) v.findViewById(R.id.rss_published_date);
		if (tmp.getText().equals(""))
			tmp.setVisibility(View.GONE);
		else
			tmp.setVisibility(View.VISIBLE);

		return v;
	}
	
	public void setFeeds(List<String> urlList) {
		if (urlList != null)
			this.articleURLList = urlList;
	}

	@Override
	public boolean cabOnMenuItemClicked(ActionMode mode,
			MenuItem item) {
		switch (item.getItemId()) {
		
		    case R.id.action_select_all:
		    	selectAll();
		    	return true;
		    	
		    case R.id.action_deselect_all:
		    	deselectAll();
		    	return true;
		    	
		    case R.id.action_share_selected:
		    	this.cabMultiselectPrimaryAction();
	            mode.finish();
		    	return true;
		    	
		    default:
		        return false;
		}
	}

	@Override
	public int cabGetMenuLayoutId() {
		return R.menu.articles_cab;
	}
	
	public final List<String> getArticleURLList() {
		return this.articleURLList;
	}
	
	protected void onItemSwiped(List<Integer> removed) {
		int numRemoved = removed.size();
		MySimpleCursorAdapter adapt = (MySimpleCursorAdapter)getListAdapter();
		
		for (int i = 0; i < numRemoved; i++) {
			View v = adapt.getView(removed.get(i), null, null);
			v.setVisibility(View.GONE);
			String link = ((TextView)v.findViewById(R.id.rss_url)).getText().toString();
			ArticleOrm.deleteArticlesWhereArticleLinkIs(link, getActivity());
		}
		ListRefreshCallback callbackInterface = (ListRefreshCallback) getActivity();
		callbackInterface.refreshCurrentList(true);
	}
}
