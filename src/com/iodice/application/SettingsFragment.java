package com.iodice.application;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.iodice.rssreader.R;

public class SettingsFragment 
extends PreferenceFragment
implements OnSharedPreferenceChangeListener {
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        setupDefaultSummaryText();
    }
    
   
    /**
     * Update each preference summary here because otherwise there isnt one
     * until a preference is actually changed.
     */
    private void setupDefaultSummaryText() {
		Activity a = getActivity();
		SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(a);
		String updateFreq = a.getString(R.string.prefs_update_frequency);
		String articlesToLoad = a.getString(R.string.prefs_articles_to_load);
		String daysToKeep = a.getString(R.string.prefs_days_to_keep_articles);

		/* to update the text, we can use the existing logic in the settings handler */
		onSharedPreferenceChanged(preferences, updateFreq);
		onSharedPreferenceChanged(preferences, articlesToLoad);
		onSharedPreferenceChanged(preferences, daysToKeep);
    }
	
	@Override
	public void onPause() {
		unregisterPreferenceChangeListener();
		super.onPause();
	}
	@Override
	public void onResume() {
		super.onResume();
		registerPreferenceChangeListener();
	}
	
	/**
	 * Setup a preference change listener so the application can react to
	 * user preference modifications
	 */
	private void registerPreferenceChangeListener() {
		Activity a = getActivity();
		SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(a);
		preferences.registerOnSharedPreferenceChangeListener(this);
	}
	
	/**
	 * Unregister as a preference change listener
	 */
	private void unregisterPreferenceChangeListener() {
		Activity a = getActivity();
		SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(a);
		preferences.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	/**
	 * Updates the summary text so that it accurately reflects the new setting
	 */
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Activity a = getActivity();
		Preference userPref = findPreference(key);
		String settingText = "";
		int settingValInt;
		
		String updateFreq = a.getString(R.string.prefs_update_frequency);
		String articlesToLoad = a.getString(R.string.prefs_articles_to_load);
		String daysToKeep = a.getString(R.string.prefs_days_to_keep_articles);
		
		if (key.equals(updateFreq)) {
			settingText = a.getString(R.string.prefs_update_frequency_setting_text);
			settingValInt = SharedPrefsHelper.getArticleUpdateFrequency(a);
			// convert 'days' into 'day'
			if (settingValInt == 1) {
				settingText = settingText.substring(0, settingText.length() - 1);
			}
			
			settingText = String.format(settingText, settingValInt);
		
		} else if (key.equals(articlesToLoad)) {
			settingText = a.getString(R.string.prefs_articles_to_load_setting_text);
			settingValInt = SharedPrefsHelper.getNumArticlesToLoad(a);
			settingText = String.format(settingText, settingValInt);
			
		} else if (key.equals(daysToKeep)) {
			settingText = a.getString(R.string.prefs_days_to_keep_articles_text);
			settingValInt = SharedPrefsHelper.getDaysToKeepArticles(a);
			// convert 'days' into 'day'
			if (settingValInt == 1) {
				settingText = settingText.substring(0, settingText.length() - 1);
			}
			settingText = String.format(settingText, settingValInt);
		}
		
		if (settingText.equals(""))
			return;
		
		userPref.setSummary(settingText);
	}
}










