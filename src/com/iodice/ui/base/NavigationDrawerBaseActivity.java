package com.iodice.ui.base;

import java.util.ArrayList;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.iodice.database.FeedOrm;
import com.iodice.rssreader.R;
import com.iodice.ui.articles.ArticleActivityByTopic;
import com.iodice.ui.articles.ArticleActivityByUrl;
import com.iodice.ui.base.abstractdrawer.AbstractNavDrawerActivity;
import com.iodice.ui.base.abstractdrawer.NavDrawerActivityConfiguration;
import com.iodice.ui.base.abstractdrawer.NavDrawerAdapter;
import com.iodice.ui.base.abstractdrawer.NavDrawerItem;
import com.iodice.ui.base.abstractdrawer.NavMenuItem;
import com.iodice.ui.base.abstractdrawer.NavMenuSection;
import com.iodice.ui.feeds.FeedActivity;

/**
 * @author Nicholas M. Iodice
 * 
 * This class provides universal navigation throughout the application. Any class that implements this
 * will have acess the drawer defined here
 */

public abstract class NavigationDrawerBaseActivity 
extends AbstractNavDrawerActivity {
	
	private static String TAG = "ApplicationDrawerBaseActivity";
    abstract public int getViewLayoutId();
    abstract public int[] getViewsToHidewOnDrawerOpen();
    
    /* Navigation drawer indices. Each entry must have an index that is sequential
     * and indicates its position in the navigation drawer list
     */
    private static final int RSS = 0;
    private static final int CATEGORIES = 1;
    private static final int SAVED_SEARCHES = 2;
    private static final int GENERAL = 3;
    private static final int SETTINGS = 4;
    private static final int ABOUT = 5;
    private static final int GITHUB = 6;
    private static final int EXIT = 7;
    
    private int DEFAULT_SELECTED_POSITION = 1;
    private int SELECTED_DRAWER_POSITION = -1;
    private static final String SELECTED_DRAWER_POSITION_KEY = "SELECTED_POSITION_KEY";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	Intent intent = this.getIntent();
    	
    	if (savedInstanceState != null) {
    		SELECTED_DRAWER_POSITION = savedInstanceState.getInt(SELECTED_DRAWER_POSITION_KEY);
    	} else if (intent != null) {
    		SELECTED_DRAWER_POSITION = intent.getIntExtra(SELECTED_DRAWER_POSITION_KEY, -1);
    	}
    	
    	if (SELECTED_DRAWER_POSITION == -1){
    		SELECTED_DRAWER_POSITION = DEFAULT_SELECTED_POSITION;
    	}
    	
        NavDrawerItem selectedItem = navConf.getNavItems()[SELECTED_DRAWER_POSITION];
        if (selectedItem.getType() == NavMenuItem.ITEM_TYPE) {
			// set the view selected, incase the adapter needs to redraw it
			NavMenuItem _sItem = (NavMenuItem)selectedItem;
			_sItem.setIsSelected(true);
	        mDrawerList.setItemChecked(SELECTED_DRAWER_POSITION, true);
        }
    }
    
    @Override
    protected void onSaveInstanceState (Bundle outState) {
    	outState.putInt(SELECTED_DRAWER_POSITION_KEY, SELECTED_DRAWER_POSITION);
    	super.onSaveInstanceState(outState);
    }

    /**
     * Get the universal configuration for the application nav drawer
     */
	@Override
	protected NavDrawerActivityConfiguration getNavDrawerConfiguration() {
		Log.i(TAG, "Getting nav drawer config");
        NavDrawerItem[] menu = new NavDrawerItem[] {
    		NavMenuSection.create(RSS, getString(R.string.drawer_rss_section)),
    		NavMenuItem.create(CATEGORIES, getString(R.string.categories), R.drawable.news, false, this),
    		NavMenuItem.create(SAVED_SEARCHES, getString(R.string.saved_searches), R.drawable.topics, false, this),
    		
    		NavMenuSection.create(GENERAL, getString(R.string.drawer_general_section)),
    		NavMenuItem.create(SETTINGS, getString(R.string.drawer_settings), R.drawable.settings, false, this),
    		NavMenuItem.create(ABOUT, getString(R.string.drawer_about), R.drawable.info, false, this),
    		NavMenuItem.create(GITHUB, getString(R.string.drawer_github), R.drawable.github, false, this),
    		NavMenuItem.create(EXIT, getString(R.string.drawer_exit), R.drawable.exit, false, this),
        };
        
        NavDrawerActivityConfiguration activityConfiguration = new NavDrawerActivityConfiguration();
        activityConfiguration.setMainLayout(getViewLayoutId());
        activityConfiguration.setDrawerLayoutId(R.id.drawer_layout);
        activityConfiguration.setLeftDrawerId(R.id.left_drawer);
        activityConfiguration.setNavItems(menu);
        activityConfiguration.setDrawerShadow(R.drawable.drawer_shadow);       
        activityConfiguration.setDrawerOpenDesc(R.string.drawer_open);
        activityConfiguration.setDrawerCloseDesc(R.string.drawer_close);
        
        activityConfiguration.setActionMenuItemsToHideWhenDrawerOpen(
        		this.getViewsToHidewOnDrawerOpen());
        activityConfiguration.setBaseAdapter(
        		new NavDrawerAdapter(this, R.layout.navdrawer_item, menu));
        return activityConfiguration;
	}
	
	/**
	 * A visual trick. Here, we set the list item that corresponds to the current
	 * activity so that it appears that only one navigation drawer extends all 
	 * activies
	 */
	@Override
    public void selectItem(int position) {
		super.selectItem(position);
        mDrawerList.setItemChecked(position, false);
        mDrawerList.setItemChecked(SELECTED_DRAWER_POSITION, true);
    }
	
	/**
	 * Send an intent, with error handeling!
	 * @param intent
	 */
	private void sendIntent(Intent intent) {
		if (intent != null) {
			
			try {
				startActivity(intent);
			} catch (ActivityNotFoundException e) {
				Toast.makeText(getApplicationContext(), R.string.no_browser,  Toast.LENGTH_LONG).show();
				e.printStackTrace();
			} catch (Exception e) {
				e.getMessage();
				e.printStackTrace();
			}
		} else 
			throw new NullPointerException();
	}

	/**
	 * Handles list item clicks. Not all list items need to launch a new intent
	 */
	@Override
	protected void onNavItemSelected(int id) {
		Intent intent = null;
		if (id == SELECTED_DRAWER_POSITION)
			return;
		
		switch (id) {
			case CATEGORIES:
				intent = new Intent(this, FeedActivity.class);
				intent.putExtra(SELECTED_DRAWER_POSITION_KEY, id);
				//intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
				break;
				
			case SAVED_SEARCHES:
				// start topics activity in an intent because it requires
				// a DB query for all RSS feeds
				StartTopicsActivity asyncTask = new StartTopicsActivity(this, id);
				asyncTask.execute();
				return;
				
			case SETTINGS:
				return;
				
			case ABOUT:
				return;
				
			case GITHUB:
				String feedURL = getString(R.string.git_url);
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse(feedURL));
				break;
				
			case EXIT:
				intent = new Intent(Intent.ACTION_MAIN);
				intent.addCategory(Intent.CATEGORY_HOME);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				break;
				
			default:
				throw new UnsupportedOperationException();
				
		}
		sendIntent(intent);
	}

	
	/**
	 * This activity is started in the background because it a query on the DB is
	 * needed to load the list of all RSS feeds, which may or may not be avaliable
	 * in the currently activity 
	 * 
	 * @author Nicholas M. Iodice
	 *
	 */
	private class StartTopicsActivity extends AsyncTask<Void, Void, Intent> {
		
		private Context context = null;
		private int id = -1;
		
		public StartTopicsActivity(Context context, int id) {
			this.context = context;
			this.id = id;
		}
		
		// context must be set via constructor
		protected void onPreExecute() {
			if (this.context == null)
				throw new NullPointerException();
			if (this.id == -1)
				throw new UnsupportedOperationException();
		}
		
		// process db request in background thread
		protected Intent doInBackground(Void... arg0) {
			String urlListKey = ArticleActivityByUrl.INTENT_EXTRA_URL_LIST;
			Intent intent = new Intent(context, ArticleActivityByTopic.class);
			ArrayList<String> urlList = new ArrayList<String>();
			Cursor cursor = FeedOrm.selectAll(context);
			
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				urlList.add(cursor.getString(
						cursor.getColumnIndex(
								FeedOrm.COLUMN_URL)));
				cursor.moveToNext();
			}
		    intent.putStringArrayListExtra(urlListKey, urlList);
		    intent.putExtra(SELECTED_DRAWER_POSITION_KEY, this.id);

			//intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		    return intent;
		}
		
		protected void onPostExecute(Intent intent) {
			sendIntent(intent);
		}
	}

	

}
