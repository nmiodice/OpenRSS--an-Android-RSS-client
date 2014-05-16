package com.iodice.database;

import android.content.Context;
import android.content.SharedPreferences;

import com.iodice.rssreader.R;

public class SharedPrefsHelper {
	
	public static int getNumArticlesToLoad(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(
				context.getString(R.string.prefs), 
				Context.MODE_PRIVATE);

		int defToLoad = context.getResources().getInteger(
				R.integer.prefs_default_max_articles_to_load);
		
		int numToLoad = prefs.getInt(
							context.getString(R.string.prefs_update_interval), 
							defToLoad);
		return numToLoad;
	}
	
	public static boolean getIsFirstRun(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(
				context.getString(R.string.prefs), 
				Context.MODE_PRIVATE);
		
		boolean firstRun = prefs.getBoolean(
				context.getString(R.string.prefs_first_run), 
				true);
		
		return firstRun;
	}
	
	public static void setIsFirstRun(Context context, boolean fr) {
		SharedPreferences prefs = context.getSharedPreferences(
				context.getString(R.string.prefs), 
				Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(context.getString(R.string.prefs_first_run), fr);
		editor.commit();
	}

}
