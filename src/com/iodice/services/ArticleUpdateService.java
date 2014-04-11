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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.iodice.database.ArticleData;
import com.iodice.database.ArticleOrm;
import com.iodice.database.FeedOrm;
import com.iodice.network.RssFeedWebQuery;
import com.iodice.ui.articles.ArticleUpdateReceiver;

public class ArticleUpdateService extends IntentService {
	
	private final static String TAG = "Feed_Update_Service";
	private final static String URL_LIST = "URL_LIST";

	/* a public callin that will handle setting up the general use case for this service, updating all
	 * RSS links in the database indefinitely. A second parameter, which can optionally be null, corresponds
	 * to a function number handled by the Callback interface. If this value is not null, a broadcast
	 * intent will be made and it will include the callbackFunctionNumber, which can be called'
	 * by the BroadcastReceiver as defined by the user
	 */
	public static void startUpdatingAllFeeds(Context context, List<String> urlList, Integer callbackFunctionNumber) {
		Intent intent = new Intent(context, ArticleUpdateService.class);
		if (intent != null)
			intent.putExtra(ArticleUpdateReceiver.HANDLE_ARTICLE_REFRESH, callbackFunctionNumber);

		if (urlList != null)
			intent.putStringArrayListExtra(ArticleUpdateService.URL_LIST, (ArrayList<String>)urlList);
		
		context.getApplicationContext().startService(intent);
	}
	
	/**
	 * A constructor is required, and must call the super IntentService(String)
	 * constructor with a name for the worker thread.
	 */
	public ArticleUpdateService() {
		super("Feed_Update_Service");
	}
	
	/**
	 * The IntentService calls this method from the default worker thread with
	 * the intent that started the service. When this method returns, IntentService
	 * stops the service, as appropriate.
	 * 
	 * This service queries for new RSS articles on a cycle. The intent can hold optional
	 * data:
	 * 	Identifer = ArticleUpdateReceiver.HANDLE_ARTICLE_REFRESH --> A callback function number,
	 * 		presumabley for use as a Callback interface function identifier. If not present,
	 * 		it is not included in the broadcast intent
	 * 
	 * 	Identifier = ArticleUpdateService.URL_LIST --> a list of URLs to query new data for. If
	 * 		it is not included, all URLs in the database are queried for
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		try {
			List<String> links = intent.getStringArrayListExtra(ArticleUpdateService.URL_LIST);
			
			if (links == null)
				links = queryDatabaseForFeedLinks();
			
			List<ArticleData> articles = queryWebForArticles(links);
			commitArticlesToDatabase(articles);
			broadcastFinishedStatus(intent);
		} catch (Exception e) {}
	}
	
	private void broadcastFinishedStatus(Intent callingIntent) {
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(ArticleUpdateReceiver.ACTION_REFRESH_DATA);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		
		int callbackFunctionNumber = callingIntent.getIntExtra(ArticleUpdateReceiver.HANDLE_ARTICLE_REFRESH, -1);
		if (callbackFunctionNumber != -1) {
			broadcastIntent.putExtra(ArticleUpdateReceiver.HANDLE_ARTICLE_REFRESH, callbackFunctionNumber);
		}
		
		sendBroadcast(broadcastIntent);
	}
	
	private List<String> queryDatabaseForFeedLinks() {
		ArrayList<String> links = new ArrayList<String>();
	    
		// query for all URLs
		Cursor cursor = FeedOrm.selectAll(getApplicationContext());
	    cursor.moveToFirst();
		while(!cursor.isAfterLast()) {
			links.add(cursor.getString(cursor.getColumnIndex(FeedOrm.COLUMN_URL)));
			cursor.moveToNext();
		}
		return links;
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
	
	private List<ArticleData> queryWebForArticles(List<String> links) {
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