package com.iodice.ui.articles;

import java.util.List;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.EditText;
import android.widget.ListView;

import com.iodice.database.SearchData;
import com.iodice.database.SearchesOrm;
import com.iodice.rssreader.R;
import com.iodice.services.ArticleUpdateService;
import com.iodice.ui.base.CabMultiselectList.MySimpleCursorAdapter;
import com.iodice.ui.base.NavigationDrawerWithSpinner;
import com.iodice.ui.topics.TopicsActivity;
import com.iodice.utilities.ListRefreshCallback;



public class ArticleActivity extends NavigationDrawerWithSpinner implements ListRefreshCallback {
	private static final String TAG = "ArticleActivity";
	protected static final String LIST = "LIST";
	private static final String SEARCH_KEY = "SEARCH_KEY";
	private static final String SEARCH_TEXT_KEY = "SEARCH_TEXT_KEY";
	/* receives notice from the article update service and triggers a data refresh */
	ArticleUpdateReceiver receiver; 
	/* controlls the behavior of the filter */
	protected boolean filterListInclusive = false;
	/* used to trigger list updates when the search bar text changes */
    protected TextWatcher searchBarListener = null;
    protected boolean showSearchBar = true;

	
	/* supported callback method identifiers */
	public static final int CALLBACK_REDRAW_WITH_CACHED_DATA = 0;
	public static final int CALLBACK_UPDATE_WITH_WEB_QUERY = 1;


	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// these URLs correspond to the parent source URL (as opposed to 
		// URLs to individual articles
		Intent intent = getIntent();
		List<String> urlList = intent.getStringArrayListExtra(
				getResources().getString(R.string.rss_url_intent));
		
		displayArticleList(urlList);
		setupSearchBar();
	}
	
	@Override
	public int getViewLayoutId() {
		return R.layout.article_activity;
	}
	
	@Override
	protected boolean isActionBarDrawerIndicatorVisible() {
		// instead of the menu to pull up the nav drawer, show an "up" indicator
		return false;
	}
	
	@Override
	public int[] getViewsToHidewOnDrawerOpen() {
		if (showSearchBar) {
			return new int[] {
					R.id.action_article_search,
					R.id.action_refresh,
			};
		} else {
			return new int[] {
					R.id.action_refresh,
			};			
		}
	}
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (!showSearchBar) {
            getMenuInflater().inflate(R.menu.articles_without_search, menu);
        } else {
            getMenuInflater().inflate(R.menu.articles_with_search, menu);
        }
        return true;
    }
    
    public void onStop() {
    	// this prevents an article update from notifying an activity/list
    	// that no longer exists (i.e., prevents a crash)!
    	if (receiver != null)
    		this.unregisterReceiver(receiver);
    	
    	super.onStop();
    }
    
    @Override
    protected void onSaveInstanceState (Bundle outState) {
    	EditText sText = (EditText)findViewById(R.id.article_search_box_text);
		View sContainer = this.findViewById(R.id.article_search_box_container);

    	if (sContainer != null) {
	    	if (sContainer.getVisibility() == View.VISIBLE)
	    		outState.putBoolean(ArticleActivity.SEARCH_KEY, true);
	    	if (sText != null)
	    		outState.putString(ArticleActivity.SEARCH_TEXT_KEY, sText.getText().toString());
    	}
    	super.onSaveInstanceState(outState);
    }
    @Override
    protected void onRestoreInstanceState (Bundle savedInstanceState) {
    	if (savedInstanceState != null) {
    		View sContainer = this.findViewById(R.id.article_search_box_container);
    		if (sContainer != null) {
	    		boolean wasSearchVisible = savedInstanceState.getBoolean(ArticleActivity.SEARCH_KEY);
	    		if (wasSearchVisible == true)
	    			sContainer.setVisibility(View.VISIBLE);	
    		}
    		
    		EditText sText = (EditText) this.findViewById(R.id.article_search_box_text);
    		if (sText != null) {
	    		String oldSearchTerm = savedInstanceState.getString(ArticleActivity.SEARCH_TEXT_KEY);
	    		sText.setText(oldSearchTerm);
    		}
    	}
    }


    /**
     * Configures the current search bar to listen for text changes & trigger a
     * query on the current list of articles
     */
    protected void addSearchBarListener() {
    	if (!showSearchBar)
    		return;
    	
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
    				adapt.getFilter().filter(cs);
    			}
    	    }
    	    @Override
    	    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
    	    @Override
    	    public void afterTextChanged(Editable arg0) {}
    	};
    	txtBox.addTextChangedListener(searchBarListener);
    }
    
    /**
     * TODO: Finish! Fix! Something, this crap is broken
     * TODO: comment effectively
     */
    private void addSearchBarAnimation() {
    	if (!showSearchBar)
    		return;
    	FragmentManager fMan = getFragmentManager();
		ArticleList articleList = (ArticleList) fMan.findFragmentByTag(ArticleActivity.LIST);
		if (articleList == null)
			return;
		ListView lv = articleList.getListView();
		if (lv == null)
			return;
		
		lv.setOnScrollListener(new OnScrollListener() {
			private int yPositionAtLastStop = 0;
			private boolean animatedSinceLastStop = false;
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
		    	if(scrollState == OnScrollListener.SCROLL_STATE_IDLE && view != null) {
		    		// a listview has no notion of the total list size, and therefore 
		    		// no scroll position. Therefore, we use the position of the top child
		    		View topChild = view.getChildAt(0);
		    		if (topChild != null)
		    			yPositionAtLastStop = topChild.getTop();
		    	}
		    	animatedSinceLastStop = false;
			}
			@Override
			/**
			 * Trigger an animation only if the change in scroll value is sufficiently
			 * large & the animation is necessary. For example, a searchbar will be
			 * animated up if the scroll is down and it is already visible
			 */
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				if (animatedSinceLastStop == true || view == null)
					return;

				// a listview has no notion of the total list size, and therefore 
	    		// no scroll position. Therefore, we use the position of the top child
	    		View topChild = view.getChildAt(0);
	    		if (topChild == null)
	    			return;
	    		
	    		int currScrollY = topChild.getTop();
				int deltaScroll = currScrollY - yPositionAtLastStop;
				
				// this is an arbitrary threshold at which I want the animation to start
				if (Math.abs(deltaScroll) < 50)
					return;
				
				if (deltaScroll < 0) {
					Log.i(TAG,  "TRIGGER ANIMATION");
					animatedSinceLastStop = true;
				}
			}
		});
		
    }
    
    private void setupSearchBar() {
    	if (showSearchBar)
    		addSearchBarListener();
    }
    
    public void clearSearchText(View v) {
    	if (!showSearchBar)
    		return;
        EditText t = (EditText)findViewById(R.id.article_search_box_text);
        t.setText("");
    }
    
    public void saveSearchText(View v) {
    	if (!showSearchBar)
    		return;
        EditText t = (EditText)findViewById(R.id.article_search_box_text);
        String tText = t.getText().toString();
        
        SearchData sd = new SearchData();
        sd.setSearchTerm(tText);
        sd.setName("name");
        SearchesOrm.insertSearch(sd, this);
    }
    
	public void queryWebForNewListData(List<String> urlList) {
		// sets up the intent filter to catch the refresh initiated by the caller
        IntentFilter filter = new IntentFilter(ArticleUpdateReceiver.ACTION_REFRESH_DATA);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        receiver = new ArticleUpdateReceiver();
        this.registerReceiver(receiver, filter);
        
        // calls the service with relevant parameter information
        ArticleUpdateService.startUpdatingAllFeeds(this, urlList, 0);
	}
	
	private void updateCurrentListWithWebQuery() {
		FragmentManager fMan = getFragmentManager();
		ArticleList articleList = (ArticleList) fMan.findFragmentByTag(ArticleActivity.LIST);
		if (articleList != null)
			articleList.setLoadState(true);
		this.queryWebForNewListData(articleList.getArticleURLList());
	}
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_refresh:
            	this.updateCurrentListWithWebQuery();
                return true;
                
            case R.id.action_article_search:
            	if (!showSearchBar)
            		return false;
            	addSearchBarAnimation();
            	View v = findViewById(R.id.article_search_box_container);
            	if (v.getVisibility() == View.GONE) {
            		v.setVisibility(View.VISIBLE);
            		Log.i(TAG, "search clicked!");
            	}
            	else
            		v.setVisibility(View.GONE);
            	return true;
            	
            default:
                return super.onOptionsItemSelected(item);
        }
    }
	
	private void displayArticleList(List<String> urlList) {
		if (urlList == null) {
			throw new NullPointerException();
		}
		// add fragment to apropriate layout item
		FragmentTransaction fTrans;
		FragmentManager fMan = getFragmentManager();
				
		// fragContainer is null until something is added to it
		if (fMan.findFragmentByTag(ArticleActivity.LIST) == null) {			
			ArticleList list = new ArticleList();
			list.setFilterInclusive(filterListInclusive);
			list.setFeeds(urlList);
			fTrans = fMan.beginTransaction();
			fTrans.add(R.id.rss_fragment_container, list, ArticleActivity.LIST);
			fTrans.commit();
			//fMan.executePendingTransactions();
		}
	}

	// helper method to lock screen orientation. Should call unlockScreenOrientation
	// shortly after making this to avoid a UI lock in one orientation.
	public void lockScreenOrientation() {
	    int orientation = getRequestedOrientation();
	    int rotation = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
	    switch (rotation) {
	    case Surface.ROTATION_0:
	        orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
	        break;
	    case Surface.ROTATION_90:
	        orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
	        break;
	    case Surface.ROTATION_180:
	        orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
	        break;
	    default:
	        orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
	        break;
	    }
	    setRequestedOrientation(orientation);
	    Log.i(TAG, "Screen orientation locked");
	}
	 
	public void unlockScreenOrientation() {
	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
	    Log.i(TAG, "Screen orientation unlocked");
	}
	
	private void redrawActiveArticleListWithCachedData() {
		FragmentManager fMan = getFragmentManager();
		ArticleList articleList = (ArticleList) fMan.findFragmentByTag(ArticleActivity.LIST);
		
		// fragContainer is null until something is added to it
		if (articleList != null) {
			articleList.setLoadState(false);

			articleList.setUpAdapter();
			articleList.redrawListView();
			// load state may have been set to true if the list was requested to update
			// its data from the web
		}
		if (this.receiver == null)
			return;
        this.unregisterReceiver(receiver);
        this.receiver = null;
	}
	
	/**
	 * Resets search bar with the currently active filter. Assumes that the active list
	 * is already set as a listener to the search bar text
	 */
	protected void refilterArticles() {
		if (showSearchBar) {
	    	EditText searchText = (EditText)findViewById(R.id.article_search_box_text);
	    	if (searchText != null) {
	    		searchText.setText(searchText.getText().toString());
	    	}
		}
	}
	
	@Override
	/**
	 * Handle a callback event.
	 */
	public void handleCallbackEvent(int n, Object obj) {
		switch (n) {
			case ArticleActivity.CALLBACK_REDRAW_WITH_CACHED_DATA:
				this.redrawActiveArticleListWithCachedData();
				this.refilterArticles();
				return;
			case ArticleActivity.CALLBACK_UPDATE_WITH_WEB_QUERY:
				this.updateCurrentListWithWebQuery();
				return;
			default:
				throw new UnsupportedOperationException();
		}
	}
	

	@Override
	public void refreshCurrentList() {
		this.handleCallbackEvent(TopicsActivity.CALLBACK_REDRAW_WITH_CACHED_DATA, null);
	}

	
	
	/* this is essentially a standard article list but without any spinner
	 * to go along with it. All of the methods here must override the ones
	 * related to spinners so that one is not added.
	 */
	@Override
	public void setupCategorySpinner() {}
	@Override
	public void setupCategorySpinnerWithSelection(String selection) {}
	@Override
	public boolean onSpinnerItemClick(int position, long id) {
		return false;
	}
	@Override
	public List<String> getSpinnerListPrimaryKeys() {
		return null;
	}

	@Override
	public String getSpinnerTitleText() {
		return null;
	}
}