package com.iodice.ui.articles;

import java.util.List;

import android.app.Activity;
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
import android.widget.EditText;

import com.iodice.rssreader.R;
import com.iodice.services.ArticleUpdateService;
import com.iodice.ui.ListBase.MySimpleCursorAdapter;
import com.iodice.utilities.Callback;



public class ArticleActivity extends Activity implements Callback {
	
	private static final String TAG = "Activity_Rss";
	private static final String LIST = "LIST";
	private static final String SEARCH_KEY = "SEARCH_KEY";
	private static final String SEARCH_TEXT_KEY = "SEARCH_TEXT_KEY";
	/* receives notice from the article update service and triggers a data refresh */
	ArticleUpdateReceiver receiver; 
	
	/* supported callback method identifiers */
	public static final int CALLBACK_REDRAW_WITH_CACHED_DATA = 0;
	public static final int CALLBACK_UPDATE_WITH_WEB_QUERY = 1;


	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.article_activity);
		
		// get list of feed URLs
		Intent intent = getIntent();
		List<String> urlList = intent.getStringArrayListExtra(getResources().getString(R.string.rss_url_intent));
		
		displayArticleList(urlList);
		setupSearchBar();
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.articles, menu);
        return true;
    }
    
    public void onStop() {
    	if (receiver != null)
    		this.unregisterReceiver(receiver);
    	
    	super.onStop();
    }
    
    @Override
    protected void onSaveInstanceState (Bundle outState) {
    	EditText txt = (EditText)findViewById(R.id.article_search_box_text);
		View v = this.findViewById(R.id.article_search_box_container);

    	if (v != null) {
	    	if (v.getVisibility() == View.VISIBLE)
	    		outState.putBoolean(ArticleActivity.SEARCH_KEY, true);
	    	if (txt != null)
	    		outState.putString(ArticleActivity.SEARCH_TEXT_KEY, txt.getText().toString());
    	}
    	super.onSaveInstanceState(outState);
    }
    @Override
    protected void onRestoreInstanceState (Bundle savedInstanceState) {
    	if (savedInstanceState != null) {
    		View v = this.findViewById(R.id.article_search_box_container);
    		if (v != null) {
	    		boolean wasSearchVisible = savedInstanceState.getBoolean(ArticleActivity.SEARCH_KEY);
	    		if (wasSearchVisible == true)
	    			v.setVisibility(View.VISIBLE);	
    		}
    		
    		EditText txt = (EditText) this.findViewById(R.id.article_search_box_text);
    		if (txt != null) {
	    		String oldSearchTerm = savedInstanceState.getString(ArticleActivity.SEARCH_TEXT_KEY);
	    		txt.setText(oldSearchTerm);
    		}
    	}
    }




    private TextWatcher searchBarListener = null;
    private void setupSearchBar() {
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
    
    public void clearSearchText(View v) {
        EditText t = (EditText)findViewById(R.id.article_search_box_text);
        t.setText("");
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
            	View v = findViewById(R.id.article_search_box_container);
            	if (v.getVisibility() == View.GONE)
            		v.setVisibility(View.VISIBLE);
            	else
            		v.setVisibility(View.GONE);
            	return true;
            	
            default:
                return super.onOptionsItemSelected(item);
        }
    }
	
	private void displayArticleList(List<String> urlList) {
		// add fragment to apropriate layout item
		FragmentTransaction fTrans;
		FragmentManager fMan = getFragmentManager();
				
		// fragContainer is null until something is added to it
		if (fMan.findFragmentByTag(ArticleActivity.LIST) == null) {			
			ArticleList list = new ArticleList();
			list.setFeeds(urlList);
			fTrans = fMan.beginTransaction();
			fTrans.add(R.id.rss_fragment_container, list, ArticleActivity.LIST);
			fTrans.commit();
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
        this.unregisterReceiver(receiver);
        this.receiver = null;
	}
	
	@Override
	/* n = 0: 
	 * 	Asks the article list to update. Principle caller is 
	 *  ArticleUpdateReceiver.onReceive()
	 */
	public void handleCallbackEvent(int n, Object obj) {
		switch (n) {
			case ArticleActivity.CALLBACK_REDRAW_WITH_CACHED_DATA:
				this.redrawActiveArticleListWithCachedData();
				this.setupSearchBar();
				return;
			case ArticleActivity.CALLBACK_UPDATE_WITH_WEB_QUERY:
				this.updateCurrentListWithWebQuery();
				return;
			default:
				throw new UnsupportedOperationException();
		}
	}
} // end rssActivity