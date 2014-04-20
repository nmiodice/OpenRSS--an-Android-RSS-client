package com.iodice.ui.base;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.iodice.rssreader.R;
import com.iodice.ui.base.abstractdrawer.AbstractNavDrawerActivity;
import com.iodice.ui.base.abstractdrawer.NavDrawerActivityConfiguration;
import com.iodice.ui.base.abstractdrawer.NavDrawerAdapter;
import com.iodice.ui.base.abstractdrawer.NavDrawerItem;
import com.iodice.ui.base.abstractdrawer.NavMenuItem;
import com.iodice.ui.base.abstractdrawer.NavMenuSection;
import com.iodice.ui.rssgroups.FeedActivity;

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
    abstract public int[] getViewsToHidewOnNavigationBarOpen();
    
    private static final int RSS = 100;
    private static final int GROUPS = 100;
    private static final int TOPICS = 101;
    
    private static final int GENERAL = 200;
    private static final int SETTINGS = 202;
    private static final int ABOUT = 203;
    private static final int GITHUB = 204;
    private static final int EXIT = 205;

	@Override
	protected NavDrawerActivityConfiguration getNavDrawerConfiguration() {
		Log.i(TAG, "Getting nav drawer config");
        NavDrawerItem[] menu = new NavDrawerItem[] {
        		NavMenuSection.create(RSS, getString(R.string.drawer_rss_section)),
        		NavMenuItem.create(GROUPS, getString(R.string.drawer_groups), R.drawable.news, false, this),
        		NavMenuItem.create(TOPICS, getString(R.string.drawer_topics), R.drawable.topics, false, this),
        		
        		NavMenuSection.create(GENERAL, getString(R.string.drawer_general_section)),
        		NavMenuItem.create(SETTINGS, getString(R.string.drawer_settings), R.drawable.settings, false, this),
        		NavMenuItem.create(ABOUT, getString(R.string.drawer_about), R.drawable.info, false, this),
        		NavMenuItem.create(GITHUB, getString(R.string.drawer_github), R.drawable.github, false, this),
        		NavMenuItem.create(EXIT, getString(R.string.drawer_exit), R.drawable.exit, false, this),
        		};
        
        NavDrawerActivityConfiguration navDrawerActivityConfiguration = new NavDrawerActivityConfiguration();
        navDrawerActivityConfiguration.setMainLayout(getViewLayoutId());
        navDrawerActivityConfiguration.setDrawerLayoutId(R.id.drawer_layout);
        navDrawerActivityConfiguration.setLeftDrawerId(R.id.left_drawer);
        navDrawerActivityConfiguration.setNavItems(menu);
        navDrawerActivityConfiguration.setDrawerShadow(R.drawable.drawer_shadow);       
        navDrawerActivityConfiguration.setDrawerOpenDesc(R.string.drawer_open);
        navDrawerActivityConfiguration.setDrawerCloseDesc(R.string.drawer_close);
        navDrawerActivityConfiguration.setActionMenuItemsToHideWhenDrawerOpen(
        		this.getViewsToHidewOnNavigationBarOpen());
        navDrawerActivityConfiguration.setBaseAdapter(
        		new NavDrawerAdapter(this, R.layout.navdrawer_item, menu ));
        return navDrawerActivityConfiguration;
	}
	

	@Override
	protected void onNavItemSelected(int id) {
		Intent intent;
		
		switch (id) {
			case GROUPS:
				intent = new Intent(this, FeedActivity.class);
				startActivity(intent);		
				return;
				
			case TOPICS:
				return;
				
			case SETTINGS:
				return;
				
			case ABOUT:
				return;
				
			case GITHUB:
				Intent browserIntent;
				String feedURL = "https://www.google.com/";
				try {
					browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(feedURL));
					startActivity(browserIntent);
				} catch (ActivityNotFoundException e) {
					Toast.makeText(getApplicationContext(), R.string.no_browser,  Toast.LENGTH_LONG).show();
					e.printStackTrace();
				} catch (Exception e) {
					e.getMessage();
					e.printStackTrace();
				}
				return;
				
			case EXIT:
				intent = new Intent(Intent.ACTION_MAIN);
				intent.addCategory(Intent.CATEGORY_HOME);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
				return;
			default:
				throw new UnsupportedOperationException();
		}
		
	}
	
}
