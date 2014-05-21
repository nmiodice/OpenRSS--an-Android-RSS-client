package com.iodice.application;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.iodice.rssreader.R;

public class SharedPrefsHelper {
	
	private static SharedPreferences getPrefs(Context context) {
		/*
		return context.getSharedPreferences(
				context.getString(R.string.prefs), 
				Context.MODE_PRIVATE);
				*/
		return PreferenceManager.getDefaultSharedPreferences(context);
	}
	
	public static int getNumArticlesToLoad(Context context) {
		SharedPreferences prefs = getPrefs(context);
		String defToLoadAsString = context.getResources().
				getString(R.string.prefs_default_articles_to_load);
		String toLoadAsString = prefs.getString(context.
				getString(R.string.prefs_articles_to_load), defToLoadAsString);	
		return Integer.valueOf(toLoadAsString);
	}
	
	public static boolean getIsFirstRun(Context context) {
		SharedPreferences prefs = getPrefs(context);
		
		boolean firstRun = prefs.getBoolean(context.
				getString(R.string.prefs_first_run), true);
		
		return firstRun;
	}
	
	public static boolean getEnableAnimations(Context context) {
		SharedPreferences prefs = getPrefs(context);
		
		boolean animate = prefs.getBoolean(context.
				getString(R.string.prefs_enable_animation), true);
		
		return animate;
	}
	
	public static void setIsFirstRun(Context context, boolean fr) {
		SharedPreferences prefs = getPrefs(context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(context.getString(R.string.prefs_first_run), fr);
		editor.commit();
	}
	
	public static boolean getUpdateInBackground(Context context) {
		SharedPreferences prefs = getPrefs(context);
		
		boolean autoUpdate = prefs.getBoolean(context.
				getString(R.string.prefs_update_in_background), true);
		
		return autoUpdate;
	}
	
	public static int getArticleUpdateFrequency(Context context) {
		SharedPreferences prefs = getPrefs(context);
		
		String defaultAsString = context.getString(R.string.prefs_default_update_frequency);
		String timeAsString = prefs.getString(context.
				getString(R.string.prefs_update_frequency), defaultAsString);
		return Integer.parseInt(timeAsString);
	}
	
	public static int getDaysToKeepArticles(Context context) {
		SharedPreferences prefs = getPrefs(context);
		
		String defaultAsString = context.getString(R.string.prefs_default_days_to_keep_articles);
		String daysAsString = prefs.getString(context.
				getString(R.string.prefs_days_to_keep_articles), defaultAsString);
		return Integer.parseInt(daysAsString);
	}
	
	public static boolean getHideArticlesAfterClick(Context context) {
		SharedPreferences prefs = getPrefs(context);
		
		boolean hideOnClick = prefs.getBoolean(context.
				getString(R.string.prefs_hide_articles_after_click), true);
		return hideOnClick;
	}

}
