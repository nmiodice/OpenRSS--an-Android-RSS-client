package com.iodice.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.preference.PreferenceManager;
import android.util.Log;

import com.iodice.database.ArticleData;
import com.iodice.database.FeedOrm;
import com.iodice.database.ArticleOrm;
import com.iodice.network.RssFeedWebQuery;
import com.iodice.rssreader.R;

public class RssFeedUpdateService extends IntentService {
	
	private final static String TAG = "Feed_Update_Service";

	/* a public callin that will handle setting up the general use case for this service, updating all
	 * RSS links in the database indefinitely
	 */
	public static void startUpdatingAllFeeds(Context context) {
		ArrayList<String> links = new ArrayList<String>();
		Intent intent = new Intent(context, RssFeedUpdateService.class);
		String key = context.getResources().getString(R.string.rss_url_intent);
	    
		// query for all URLs
		Cursor cursor = FeedOrm.selectAll(context);
	    cursor.moveToFirst();
		while(!cursor.isAfterLast()) {
			links.add(cursor.getString(cursor.getColumnIndex(FeedOrm.COLUMN_URL)));
			cursor.moveToNext();
		}
	    intent.putStringArrayListExtra(key, links);
		context.getApplicationContext().startService(intent);
	}
	
	/**
	 * A constructor is required, and must call the super IntentService(String)
	 * constructor with a name for the worker thread.
	 */
	public RssFeedUpdateService() {
		super("Feed_Update_Service");
	}
	
	/**
	 * The IntentService calls this method from the default worker thread with
	 * the intent that started the service. When this method returns, IntentService
	 * stops the service, as appropriate.
	 * 
	 * This service queries for new RSS articles on a cycle
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		List<String> links = intent.getStringArrayListExtra(getResources().getString(R.string.rss_url_intent));;
		Log.i(TAG,"service started, onhandleevent");
		do {
			List<ArticleData> articles = this.queryWebLinks(links);
			commitArticlesToDatabase(articles);
			setDatabaseHasCachePreference();

			// 60 min pause. TODO: change this to something more meaningful, based off a user preference
			try {
				Thread.sleep(1*1000*60*60);
			} catch (InterruptedException e) {
			}
		} while (true);
	}
	// update shared setting to indicate that the table is populated
	private void setDatabaseHasCachePreference() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		boolean defaultVal = true;
		boolean isDatabaseNeverUpdated = prefs.getBoolean(getString(R.string.prefs_article_table_not_yet_updated), defaultVal);
		
		if (isDatabaseNeverUpdated ==  true) {
			Log.i(TAG, "Articles table cached with all current feeds");
			// update that the db has been populated has been run
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean(getString(R.string.prefs_article_table_not_yet_updated), false);
			editor.commit();
		}
	}
	  
	private void commitArticlesToDatabase(List<ArticleData> articles) {
		if (articles == null)
			return;
		  
		ArticleData data = null;
		int listSize = articles.size();
		SQLiteDatabase db = ArticleOrm.getWritableDatabase(getApplication().getApplicationContext());


		for (int i = 0; i < listSize; i++) {
			data = articles.get(i);
			if (data == null)
				continue;
			  
			try {
				ArticleOrm.insertArticle(data, db);
			} catch (SQLiteException e) {
				if (!e.getMessage().contains("code 19"))
					Log.e(TAG, "Error commiting article data. SQLiteDatabase error: " + e.getMessage());
				}
			}
		db.close();
	}
	
	private List<ArticleData> queryWebLinks(List<String> links) {
		/* holds each web requests list of articles from that requests URL */
		List<List<ArticleData>> listOfArticleDataLists = threadedArticleRequest(links);
		/* holds the final list of data aggregated across feeds */
		ArrayList<ArticleData> articles = new ArrayList<ArticleData>();
		int numLists = listOfArticleDataLists.size();
		
		for (int i = 0; i < numLists; i++)
			articles.addAll(listOfArticleDataLists.get(i));
		
		
		return articles;
	}
	
	
	/* executes a web request for each URL and returns the result of all threads responses in a list. Each
	 * list element contains a list of articles from a specific URL
	 */
	private List<List<ArticleData>> threadedArticleRequest(List<String> links) {
		/* holds each web requests list of articles from that requests URL */
		ArrayList<List<ArticleData>> listOfArticleDataLists = new ArrayList<List<ArticleData>>();
		/* a worker thread that queries a URL for articles */
		Callable<List<ArticleData>> worker;
		int numLinks;
		
		numLinks = links.size();
		
		// the executor and pool are used so that a return value can be obtained from the thread.Call() method
		ExecutorService executor = Executors.newFixedThreadPool(numLinks);
		CompletionService<List<ArticleData>> pool = new ExecutorCompletionService<List<ArticleData>>(executor);
		
		for (int i = 0; i < numLinks; i++) {
			worker = new RssFeedWebQuery(links.get(i), getApplicationContext());
			pool.submit(worker);
		}
		// shut it down LEMON!
        executor.shutdown();
        
        // aggregate thread return lists and splice them together
        listOfArticleDataLists = new ArrayList<List<ArticleData>>();
        List<ArticleData> threadResult;
        try {
	        for (int i = 0; i < numLinks; i++) {
	        	threadResult = pool.take().get();
	        	if (threadResult != null)
	        		listOfArticleDataLists.add(threadResult);
	        }
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
        
        return listOfArticleDataLists;
	}
}