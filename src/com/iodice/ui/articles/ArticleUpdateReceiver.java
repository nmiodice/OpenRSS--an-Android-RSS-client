package com.iodice.ui.articles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.iodice.utilities.Callback;
/**
 * A small class that acts as a receiver for re-query events
 * 
 * @author Nicholas M. Iodice
 *
 */
public class ArticleUpdateReceiver extends BroadcastReceiver {
    
	@Override
	public void onReceive(Context context, Intent intent) {
		Callback callbackInterface = (Callback) context;
		callbackInterface.handleCallbackEvent(ArticleActivityByUrl.CALLBACK_REDRAW_WITH_CACHED_DATA, null);
	}
}
