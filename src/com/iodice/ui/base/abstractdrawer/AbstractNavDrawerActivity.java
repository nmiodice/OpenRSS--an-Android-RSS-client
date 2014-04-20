package com.iodice.ui.base.abstractdrawer;

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
    // used to keep track of the last selected menu item
    private int lastSelectedPosition = -1;
    
    private ListView mDrawerList;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private NavDrawerActivityConfiguration navConf;
    
    protected abstract NavDrawerActivityConfiguration getNavDrawerConfiguration();
    protected abstract void onNavItemSelected(int id );
    
    // some activities may want an 'up' button instead of the navigation drawer. For example,
    // any low level application component. This method returns true if the drawer icon should
    // be visible and false if not
    protected abstract boolean isActionBarNavDrawerIndicatorVisible();
    
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
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu();
            }
        };
        // some activities may want an 'up' button instead of the navigation drawer. For example,
        // any low level application component
        mDrawerToggle.setDrawerIndicatorEnabled(this.isActionBarNavDrawerIndicatorVisible());
        mDrawerLayout.setDrawerListener(mDrawerToggle);
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
            for( int iItem : navConf.getActionMenuItemsToHideWhenDrawerOpen()) {
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
            if ( this.mDrawerLayout.isDrawerOpen(this.mDrawerList)) {
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
        
        if (selectedItem.getType() == NavMenuItem.ITEM_TYPE) {
        	if (position != lastSelectedPosition) {
        		// set the view selected, incase the adapter needs to redraw it
        		NavMenuItem _sItem = (NavMenuItem)selectedItem;
        		_sItem.setIsSelected(true);
        		if (lastSelectedPosition != -1) {
        			// set the old view unslected, incase the adapter needs to redraw it
        			_sItem = (NavMenuItem)navConf.getNavItems()[lastSelectedPosition];
        			_sItem.setIsSelected(false);
        		}
        		lastSelectedPosition = position;
        	}
        }
        
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