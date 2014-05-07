package com.iodice.ui.base.abstractdrawer;

import android.app.ActionBar;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.iodice.rssreader.R;

public abstract class AbstractNavDrawerActivity extends FragmentActivity {
	
	private static final String TAG = "AbstractNavDrawerActivity";

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    
    protected ListView mDrawerList;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    protected NavDrawerActivityConfiguration navConf;
    
    private int ORIGINAL_NAVIGATION_MODE = -1;
    private String ORIGINAL_NAVIGATION_MODE_KEY = "ORIGINAL_NAVIGATION_MODE_KEY";
    private String IS_DRAWER_OPEN_KEY = "IS_DRAWER_OPEN_KEY";
    
    protected abstract NavDrawerActivityConfiguration getNavDrawerConfiguration();
    protected abstract void onNavItemSelected(int id );
    
    // some activities may want an 'up' button instead of the navigation drawer. For example,
    // any low level application component. This method returns true if the drawer icon should
    // be visible and false if not
    protected abstract boolean isActionBarDrawerIndicatorVisible();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	Log.i(TAG, "creating nav drawer activity");
        super.onCreate(savedInstanceState);
        
        navConf = getNavDrawerConfiguration();
        
        setContentView(navConf.getMainLayout()); 
        
        mTitle = mDrawerTitle = getTitle();
        
        mDrawerLayout = (DrawerLayout) findViewById(navConf.getDrawerLayoutId());
        mDrawerList = (ListView) findViewById(navConf.getLeftDrawerId());
        mDrawerList.setAdapter(navConf.getBaseAdapter());
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        
        this.initDrawerShadow();
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
       	getActionBar().setHomeButtonEnabled(true);

        
        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                getDrawerIcon(),
                navConf.getDrawerOpenDesc(),
                navConf.getDrawerCloseDesc()
                ) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu();
                getActionBar().setNavigationMode(ORIGINAL_NAVIGATION_MODE);
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu();
                ORIGINAL_NAVIGATION_MODE = getActionBar().getNavigationMode();
    			getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            }
        };
        // some activities may want an 'up' button instead of the navigation drawer. For example,
        // any low level application component
        mDrawerToggle.setDrawerIndicatorEnabled(this.isActionBarDrawerIndicatorVisible());
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        
        if (savedInstanceState != null) {
        	// without resetting the original navigation mode, any custom navigation may not be
        	// drawn properly
        	ORIGINAL_NAVIGATION_MODE = savedInstanceState.getInt(ORIGINAL_NAVIGATION_MODE_KEY);
	        boolean isDrawerOpen = savedInstanceState.getBoolean(IS_DRAWER_OPEN_KEY, false);
	        if(isDrawerOpen)
	        	getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
	        else
	        	getActionBar().setNavigationMode(ORIGINAL_NAVIGATION_MODE);
        }
    }
    
    @Override
    protected void onSaveInstanceState (Bundle outState) {
    	if (ORIGINAL_NAVIGATION_MODE == -1)
            ORIGINAL_NAVIGATION_MODE = getActionBar().getNavigationMode();
    	outState.putInt(ORIGINAL_NAVIGATION_MODE_KEY, ORIGINAL_NAVIGATION_MODE);
    	
    	boolean isDrawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
    	outState.putBoolean(IS_DRAWER_OPEN_KEY, isDrawerOpen);
    	
    	super.onSaveInstanceState(outState);
    }
    
    protected void initDrawerShadow() {
        mDrawerLayout.setDrawerShadow(navConf.getDrawerShadow(), GravityCompat.START);
    }
    
    protected int getDrawerIcon() {
        return R.drawable.ic_drawer;
    }
    
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if ( navConf.getActionMenuItemsToHideWhenDrawerOpen() != null ) {
            boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
            for(int iItem : navConf.getActionMenuItemsToHideWhenDrawerOpen()) {
                menu.findItem(iItem).setVisible(!drawerOpen);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        else {
            return false;
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ( keyCode == KeyEvent.KEYCODE_MENU ) {
            if (this.mDrawerLayout.isDrawerOpen(this.mDrawerList)) {
                this.mDrawerLayout.closeDrawer(this.mDrawerList);
            }
            else {
                this.mDrawerLayout.openDrawer(this.mDrawerList);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    protected DrawerLayout getDrawerLayout() {
        return mDrawerLayout;
    }

    protected ActionBarDrawerToggle getDrawerToggle() {
        return mDrawerToggle;
    }
   
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }
    
    public void selectItem(int position) {
        NavDrawerItem selectedItem = navConf.getNavItems()[position];
        
        this.onNavItemSelected(selectedItem.getId());
        mDrawerList.setItemChecked(position, true);
        
        if ( selectedItem.updateActionBarTitle()) {
            setTitle(selectedItem.getLabel());
        }
        
        if ( this.mDrawerLayout.isDrawerOpen(this.mDrawerList)) {
            mDrawerLayout.closeDrawer(mDrawerList);
        }
    }
    
    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getActionBar().setTitle(mTitle);
    }
}