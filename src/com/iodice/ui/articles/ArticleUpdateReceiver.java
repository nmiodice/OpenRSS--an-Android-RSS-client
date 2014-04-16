package com.iodice.ui.articles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.iodice.utilities.Callback;

public class ArticleUpdateReceiver extends BroadcastReceiver {
	public static final String HANDLE_ARTICLE_REFRESH = "HANDLE_ARTICLE_REFRESH";
	public static final String ACTION_REFRESH_DATA = "ACTION_REFRESH_DATA";
    
	@Override
	public void onReceive(Context context, Intent intent) {
		Callback callbackInterface = (Callback) context;
		callbackInterface.handleCallbackEvent(ArticleActivity.CALLBACK_REDRAW_WITH_CACHED_DATA, null);
	}
}
