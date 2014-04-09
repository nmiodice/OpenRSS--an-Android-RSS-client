package com.iodice.ui.articles;

import java.util.List;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.WindowManager;

import com.iodice.rssreader.R;



public class ArticleActivity extends Activity {
	
	private static final String TAG = "Activity_Rss";
	private static final String LIST = "LIST";

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.article_activity);
		
		// get list of feed URLs
		Intent intent = getIntent();
		List<String> urlList = intent.getStringArrayListExtra(getResources().getString(R.string.rss_url_intent));
		
		displayListView(urlList);
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.articles, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_refresh:
            	Log.e(TAG, "IMPLEMENT!");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
	
	private void displayListView(List<String> urlList) {
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

/*
	public void openLinkInBrowser(View v) {
		Intent browserIntent;
		String feedURL = (String) v.getTag();

		Log.i(TAG, "Opening feed: " + feedURL);
		
		try {
			browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(feedURL));
			startActivity(browserIntent);
			} catch (ActivityNotFoundException e) {
			  Toast.makeText(getApplicationContext(), "No application can handle this request,"
			    + " Please install a webbrowser",  Toast.LENGTH_LONG).show();
			  e.printStackTrace();
			} catch (Exception e) {
				e.getMessage();
				e.printStackTrace();
			}
		}
*/
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
} // end rssActivity