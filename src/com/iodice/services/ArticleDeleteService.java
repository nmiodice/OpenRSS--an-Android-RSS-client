package com.iodice.services;

import com.iodice.application.SharedPrefsHelper;
import com.iodice.database.ArticleOrm;

import android.app.IntentService;
import android.content.Intent;

public class ArticleDeleteService extends IntentService {

	
	public ArticleDeleteService() {
		super("CacheMaintenenceService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		int daysToKeep = SharedPrefsHelper.getDaysToKeepArticles(this);
		ArticleOrm.deleteArticlesOlderThan(this, daysToKeep);
	}

}
