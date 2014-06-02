package com.iodice.ui.articles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FilterQueryProvider;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.iodice.application.SharedPrefsHelper;
import com.iodice.database.ArticleOrm;
import com.iodice.rssreader.R;
import com.iodice.ui.base.AnimatedEntryList;
import com.iodice.utilities.Callback;
import com.iodice.utilities.ConfirmationDialog;
import com.iodice.utilities.ListRefreshCallback;
import com.iodice.utilities.Text;

public class ArticleList extends AnimatedEntryList implements Callback {
	
	private static final String TAG = "ArticleList";
	private List<String> articleURLList;
	private Typeface headline_font = null;
	private int cabMarkReadIconResourceId = -1;
	
	// used primarly to avoid an infinite loop that can be caused if the feed has no
	// data, tries to re-query the web, and then fails again. Without keeping track
	// of the first failure, the loop wont exit
	private int loadFailCount = 0;
	private List<String> filterTerms = new ArrayList<String>();
	private List<String> columnsToFilterOn = Arrays.asList(new String[] {
			ArticleOrm.COLUMN_TITLE,
			ArticleOrm.COLUMN_DESCRIPTION,
			});
	
	// If true, filtering will be based on 'include if any term matches.' If false,
	// filtering will be more strict and require each term to appear
	private boolean filterInclusive = false;
	private static final String ARTICLE_LIST_TAG = "ARTICLE_LIST_TAG";
	private boolean showUnreadOnly = true;
	public static final int CALLBACK_MARK_SELECTED_AS_READ = 30;
	
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
			return;
		} catch (Exception e) {
			e.getMessage();
			e.printStackTrace();
			return;
		}
		
		/* if the system is configured to hide articles after reading them, do it here */
		boolean hideOnClick = SharedPrefsHelper.getHideArticlesAfterClick(getActivity());
		if (hideOnClick) {
			markArticleRead(view, feedURL);
		}
	}
	
	private void markArticleRead(View articleView, String feedURL) {
		ArrayList<String> linkList = new ArrayList<String>();
		linkList.add(feedURL);
		ArticleOrm.setArticleReadState(linkList, true, getActivity());
		
		/* need the animation if unread articles only are showing */ 
		if (showUnreadOnly) {
			Animation fadeOut = new AlphaAnimation(1, 0);
		    fadeOut.setDuration(500);
		    
		    fadeOut.setAnimationListener(new Animation.AnimationListener(){
			    @Override
			    public void onAnimationStart(Animation arg0) {}           
			    @Override
			    public void onAnimationRepeat(Animation arg0) {}           
			    @Override
			    public void onAnimationEnd(Animation arg0) {
					ListRefreshCallback callbackInterface = (ListRefreshCallback) getActivity();
					callbackInterface.refreshCurrentList(true);
			    }
			});
		    articleView.setAnimation(fadeOut);
		} else {
			ListRefreshCallback callbackInterface = (ListRefreshCallback) getActivity();
			callbackInterface.refreshCurrentList(true);			
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
			ArticleOrm.COLUMN_PUBLISHED_DATE,
			ArticleOrm.COLUMN_IS_READ,
		};
		
		to = new int[] { 
		    R.id.rss_title,
		    R.id.rss_url,
		    R.id.rss_base_url,
		    R.id.rss_author,
		    R.id.rss_description,
		    R.id.rss_published_date,
		    R.id.rss_is_read,
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
				maxArticles,
				showUnreadOnly);
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
						maxArticles,
						showUnreadOnly);
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
        	tmp.setVisibility(View.VISIBLE);
        	int maxLen = getResources().getInteger(R.integer.article_description_max_length);
        	if (desc.length() > maxLen) {
        		desc = Text.limitTextCharacters(desc, maxLen);
        		desc += "...";
        	}
        	tmp.setText(desc);
        } else
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

		// finally, if this article is read, modify the background color
		tmp = (TextView) v.findViewById(R.id.rss_is_read);
		tmp.setVisibility(View.GONE);
		int targetColor;
		Drawable tileBackground = v.findViewById(R.id.tile_drawable).getBackground();
		if (tmp.getText().equals("0")) {
			targetColor = getResources().getColor(R.color.tile);
		} else {
			targetColor = getResources().getColor(R.color.tile_dark);
		}
		tileBackground.setColorFilter(targetColor, PorterDuff.Mode.MULTIPLY);
		return v;
	}
	
	@Override
	/**
	 * If this is the first item selected, choose whether to show the
	 * 'mark read' or 'mark unread' icon
	 * @param position
	 */
	protected void cabOnItemPress(int position) {
		if (selectedListItems.size() != 1 || cabMenu == null)
			return;
		Log.i(TAG,  "Potentially modifying icon");
		/* use the selected list item because we dont know whether or
		 * not the item that was just pressed is checked or unchecked
		 */
		View v = getViewAtPosition(selectedListItems.get(0));
		TextView isRead = (TextView)v.findViewById(R.id.rss_is_read);
		String isReadAsString = isRead.getText().toString();
		MenuItem readOrUnread = cabMenu.findItem(R.id.action_mark_as_read);
		Drawable icon;

		
		/* this is populated via a boolean value, and as such will 
		 * be either 0 or 1 
		 */
		if (isReadAsString.equals("0"))
			cabMarkReadIconResourceId = R.drawable.ic_action_read;
		else
			cabMarkReadIconResourceId = R.drawable.ic_action_unread;
		icon = getResources().getDrawable(cabMarkReadIconResourceId);
		readOrUnread.setIcon(icon);
	}
	
	public void setFeeds(List<String> urlList) {
		if (urlList != null)
			this.articleURLList = urlList;
	}
	
	/**
	 * Change the read/unread status of the selected articles based on
	 * which icon is showing for the 'mark read' or 'mark unread'
	 * menu icon
	 */
	public void cabChangeSelectedReadStatus() {
		MultiArticleMarkUnreadTask asyncTask = new MultiArticleMarkUnreadTask();
		/* set the selected items here because the async task cannot accuratley
		 * obtain a list of items in the case that items become unselected
		 * before the task makes a copy of them
		 */
		asyncTask.setSelectedItems(selectedListItems);
		asyncTask.execute();
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
		    	
		    case R.id.action_mark_as_read:
		    	String markReadOrUnread;
		    	if (cabMarkReadIconResourceId == R.drawable.ic_action_read)
		    		markReadOrUnread = getActivity().getString(R.string.confirm_mark_as_read);
		    	else 
		    		markReadOrUnread = getActivity().getString(R.string.confirm_mark_as_unread);
				
		    	AlertDialog alertDialog = ConfirmationDialog.getCustomDialog(getActivity(), 
    					this,
    					ArticleList.CALLBACK_MARK_SELECTED_AS_READ, 
    					mode,
    					markReadOrUnread);
		    	alertDialog.show();
				
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
	
	/**
	 * Show an undo dialog and optionally mark items as unread
	 * @param linkList the list of items to optionally mark as read
	 */
	protected void showUndoMarkUnreadPrompt(List<String> linkList) {
		Log.i(TAG, "Ask if mark unread");
	}
	
	protected void onItemSwiped(List<Integer> removed) {
		int numRemoved = removed.size();
		MySimpleCursorAdapter adapt = (MySimpleCursorAdapter)getListAdapter();
		ArrayList<String> linkList = new ArrayList<String>();

		for (int i = 0; i < numRemoved; i++) {
			View v = adapt.getView(removed.get(i), null, null);
			v.setVisibility(View.GONE);
			String link = ((TextView)v.findViewById(R.id.rss_url)).getText().toString();			
			linkList.add(link);
		}
		
		ArticleOrm.setArticleReadState(linkList, true, getActivity());
		ListRefreshCallback callbackInterface = (ListRefreshCallback) getActivity();
		callbackInterface.refreshCurrentList(true);
		showUndoMarkUnreadPrompt(linkList);
	}

	@Override
	public void handleCallbackEvent(int n, Object obj)
			throws UnsupportedOperationException {
		switch (n) {
			case ArticleList.CALLBACK_MARK_SELECTED_AS_READ:
		    	cabChangeSelectedReadStatus();
				
				/* obj is not null if the callback was 
				 * called while in contextual action mode 
				 */
				if (obj != null) {
					ActionMode mode = (ActionMode)obj;
					mode.finish();
				}
				break;
		}
		
	}
	
	public boolean getShowUnreadOnly() {
		return showUnreadOnly;
	}
	/**
	 * Toggle the list to show or hide read articles. The method call
	 * will auto-update the current list elements
	 * @param b
	 */
	public void setShowUnreadOnly(boolean b) {
		this.showUnreadOnly = b;
		ListRefreshCallback callbackInterface = (ListRefreshCallback) getActivity();
		callbackInterface.refreshCurrentList(true);
	}
	
	
	private class MultiArticleMarkUnreadTask extends 
	AsyncTask<Void, Void, Void> {
		
		ArrayList<String> toChangeReadStatus = new ArrayList<String>();
		List<Integer> selectedItems = null;
		Context context = null;
		boolean markRead;
		
		public void setSelectedItems(ArrayList<Integer> items) {
			/* obtain a copy, otherwise if the source list changes, the task
			 * doesnt always operate on the correct list elements
			 */
			this.selectedItems = new ArrayList<Integer>(items);
		}
		
		/**
		 * Gets a list of URLs for which the read state should be modified.
		 * Getting views from the adapter requires recreating the view
		 * (in this case) so ensure this occurs outside of the UI thread. 
		 */
		private void populateChangeList() {
			
			int selectedPos;
			LayoutInflater inflater;
			View article;
			TextView txt;

			if (context == null)
				return;
			
			inflater = LayoutInflater.from(context);
			article = inflater.inflate(getListItemLayoutID(), null);
			
			/* call setSelectedItems prior to invoking the async task */
			if (selectedItems == null)
				throw new NullPointerException();
			
			ListAdapter adapt = getListAdapter();
			int numSelected = selectedItems.size();
			
			for (int i = 0; i < numSelected; i++) {
				selectedPos = selectedItems.get(i);
				Log.i(TAG, "POS = " + selectedPos);
				if (adapt == null)
					return;
				article = adapt.getView(selectedPos, article, null);
				txt = (TextView)article.findViewById(R.id.rss_url);
				toChangeReadStatus.add(txt.getText().toString());
				Log.i(TAG, "pos " + i + " = " + txt.getText().toString());
			}
		}
		
		/**
		 * Checks the resource ID to determine whether or not the action
		 * is to mark articles as 'read' or 'unread'
		 */
		private void setMarkRead() {
			if (cabMarkReadIconResourceId == R.drawable.ic_action_unread)
				markRead = false;
			else
				markRead = true;
		}
		
		/**
		 * Setup a context
		 */
		protected void onPreExecute() {
			context = getActivity();
		}
		
		@Override
		/**
		 * Establishes a list of URLs to work on, and then updates the 
		 * database accordingly
		 */
		protected Void doInBackground(Void... params) {
			populateChangeList();
			setMarkRead();
			if (context != null)
				ArticleOrm.setArticleReadState(toChangeReadStatus, markRead, context);
			return null;
		}
		
		/**
		 * Updates the list view, if necessary
		 */
		protected void onPostExecute(Void params) {
			if (context == null)
				return;
			ListRefreshCallback callbackInterface = (ListRefreshCallback) context;
			if (callbackInterface != null)
				callbackInterface.refreshCurrentList(true);
		}
	}
	
}


