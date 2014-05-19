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

import com.iodice.database.ArticleData;
import com.iodice.database.ArticleOrm;
import com.iodice.database.FeedOrm;

public class ArticleUpdateService extends IntentService {
	
	public static final String HANDLE_ARTICLE_REFRESH = "HANDLE_ARTICLE_REFRESH";
	public static final String ACTION_REFRESH_DATA = "ACTION_REFRESH_DATA";
	public static final String MAX_WORKER_THREADS = "MAX_WORKER_THREADS";
	public static final String UPDATE_ARTICLES_ACTION = "UPDATE_ARTICLES";	
	private final static String URL_LIST = "URL_LIST";
	

	/**
	 * A public callin that will handle setting up the general use case for this service, updating all
	 * RSS links in the database indefinitely
	 * 
	 * @param context
	 * @param urlList The list of URLs to update, or null to update all lists
	 * @param callbackFunctionNumber A callback function number included with the resulting braodcast
	 * intent, or null if no such number should be included
	 * @param maxWorkerThreads The maximum number of worker threads to use, or null to use one worker
	 * for each URL. Limiting the number of worker threads can reduce system load.
	 */
	public static void startUpdatingAllFeeds(Context context, 
			List<String> urlList, 
			Integer callbackFunctionNumber,
			Integer maxWorkerThreads) {
		Intent intent = new Intent(context, ArticleUpdateService.class);
		if (callbackFunctionNumber != null)
			intent.putExtra(ArticleUpdateService.HANDLE_ARTICLE_REFRESH, callbackFunctionNumber);

		if (urlList != null)
			intent.putStringArrayListExtra(ArticleUpdateService.URL_LIST, (ArrayList<String>)urlList);
		
		if (maxWorkerThreads != null)
			intent.putExtra(ArticleUpdateService.MAX_WORKER_THREADS, maxWorkerThreads);
		
		context.getApplicationContext().startService(intent);
	}
	
	/**
	 * A constructor is required, and must call the super IntentService(String)
	 * constructor with a name for the worker thread.
	 */
	public ArticleUpdateService() {
		super("ArticleUpdateService");
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
	 * 
	 * 	Identifier = ArticleUpdateService.MAX_WORKER_THREADS --> an integer limit used as a maximum
	 * 		number of worker jobs used to query web data. This can be done in order 
	 * 		to limit system load
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		try {
			List<String> links = intent.getStringArrayListExtra(ArticleUpdateService.URL_LIST);
			int maxWorkerThreads = intent.getIntExtra(ArticleUpdateService.MAX_WORKER_THREADS, -1);
			
			if (links == null)
				links = queryDatabaseForFeedLinks();
			
			queryWebForArticles(links, maxWorkerThreads);
			broadcastFinishedStatus(intent);
		} catch (Exception e) {}
	}
	
	private void broadcastFinishedStatus(Intent callingIntent) {
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(ArticleUpdateService.ACTION_REFRESH_DATA);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		
		int callbackFunctionNumber = callingIntent.getIntExtra(ArticleUpdateService.HANDLE_ARTICLE_REFRESH, -1);
		if (callbackFunctionNumber != -1) {
			broadcastIntent.putExtra(ArticleUpdateService.HANDLE_ARTICLE_REFRESH, callbackFunctionNumber);
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
		ArticleOrm.insertArticles(getApplication().getApplicationContext(), articles);
	}
	
	private void queryWebForArticles(List<String> links, int maxWorkerThreads) {
		threadedArticleRequest(links, maxWorkerThreads);
	}
	
	
	/* executes a web request for each URL and saves the content to the database
	 */
	private void threadedArticleRequest(List<String> links, int maxWorkerThreads) {
		/* a worker thread that queries a URL for articles */
		Callable<List<ArticleData>> worker;
		int numLinks = links.size();
		
		if (maxWorkerThreads <= 0)
			maxWorkerThreads = numLinks;
		
		// the executor and pool are used so that a return value can be obtained from the thread.Call() method
		ExecutorService executor = Executors.newFixedThreadPool(maxWorkerThreads);
		CompletionService<List<ArticleData>> pool = new ExecutorCompletionService<List<ArticleData>>(executor);
		
		for (int i = 0; i < numLinks; i++) {
			worker = new RssFeedWebQuery(links.get(i), getApplicationContext());
			pool.submit(worker);
		}
		// shut it down LEMON!
        executor.shutdown();
        
        // as the data comes in save the article data to the database.
        List<ArticleData> threadResult;
        try {
	        for (int i = 0; i < numLinks; i++) {
	        	threadResult = pool.take().get();
	        	if (threadResult != null)
	        		this.commitArticlesToDatabase(threadResult);
	        }
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
    }
}
