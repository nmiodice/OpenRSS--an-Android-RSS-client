package com.iodice.application;

import java.util.Locale;

import android.app.Activity;
import android.os.Bundle;

import com.iodice.rssreader.R;

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
    
    /**
     * Identifies the page as the preference panel, just to give the user
     * a bit of context
     */
    private void setupActionBar() {
    	String appName = this.getString(R.string.app_name);
    	String pageName = this.getString(R.string.drawer_settings);
    	pageName = pageName.toLowerCase(Locale.US); 
    	String abText = appName + " " + pageName;
    	this.getActionBar().setTitle(abText);
    }
}