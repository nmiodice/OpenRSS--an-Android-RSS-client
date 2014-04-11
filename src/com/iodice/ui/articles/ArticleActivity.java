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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.WindowManager;

import com.iodice.rssreader.R;
import com.iodice.services.ArticleUpdateService;
import com.iodice.utilities.Callback;



public class ArticleActivity extends Activity implements Callback {
	
	private static final String TAG = "Activity_Rss";
	private static final String LIST = "LIST";
	ArticleUpdateReceiver receiver; 

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.article_activity);
		
		// get list of feed URLs
		Intent intent = getIntent();
		List<String> urlList = intent.getStringArrayListExtra(getResources().getString(R.string.rss_url_intent));
		
		displayArticleList(urlList);
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.articles, menu);
        return true;
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
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_refresh:
        		FragmentManager fMan = getFragmentManager();
        		ArticleList articleList = (ArticleList) fMan.findFragmentByTag(ArticleActivity.LIST);
        		if (articleList != null)
        			this.queryWebForNewListData(articleList.getArticleURLList());
        		else
        			this.queryWebForNewListData(null);
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
			Log.i(TAG, "Adding fragment to feed_list_container");
			
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
			articleList.setUpAdapter();
			articleList.redrawListView();
		}
	}
	
	@Override
	/* n = 0: 
	 * 	Asks the article list to update. Principle caller is 
	 *  ArticleUpdateReceiver.onReceive()
	 */
	public void handleCallbackEvent(int n, Object obj) {
		switch (n) {
			case 0:
				this.redrawActiveArticleListWithCachedData();
				return;
			default:
				assert(true == false);
		}
	}
} // end rssActivity