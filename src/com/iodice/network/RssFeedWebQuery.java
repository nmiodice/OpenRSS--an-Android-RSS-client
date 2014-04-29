package com.iodice.network;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import android.content.Context;
import android.util.Log;

import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndEntry;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndFeed;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.SyndFeedInput;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.XmlReader;
import com.iodice.database.ArticleData;
import com.iodice.utilities.Sys;
import com.iodice.utilities.Text;


public class RssFeedWebQuery implements Callable<List<ArticleData>> {
	private static final String TAG = "RssConnection";
	private String url = null;
	private Context context;	/* used to check if the device is online */
	
	public RssFeedWebQuery(String url, Context context) {
		assert(url != null);
		this.url = url;
		this.context = context;
	}
	
	
	@Override
	public List<ArticleData> call() {
		List<ArticleData> results;
		
		if (Sys.isOnline(this.context) == false)
			results = null;
		else
			results = queryWeb(this.url, true);

		return results;
	}


	// queries for web content, but optionally queries cache in a failure case
	private List<ArticleData> queryWeb(String url, boolean queryCacheInFailCase) {
		List<ArticleData> results = null;
		SyndFeedInput input;
		URLConnection urlConnection;
		
		System.out.println("Attempting to read data from " + url);
		try {
			// step 1. create connection
			URL urlObj = new URL(url);
			urlConnection = urlObj.openConnection();
			
			urlConnection.setConnectTimeout(10);
			urlConnection.setReadTimeout(10);
			
			// step 2. set up call to Rome API
			XmlReader xmlRdr = new XmlReader(urlObj);
			input = new SyndFeedInput();
			SyndFeed syndFeed = input.build(xmlRdr);

			// step 3. convert results into appropriate list form. pass false to indicate
			// that the feeds are not cached results
			results = syndFeedToArticleData(syndFeed, false);
			Log.i(TAG,"Successfully read data from " + url);
			
		} catch (Exception e) {
			Log.e(TAG,e.getMessage());
			e.printStackTrace();
			Log.e(TAG,"Failed to acquire web data for " + this.url);
			// in a total failure case, we can just return an null list instead of a potentially
			// degraded list
			results = null;
		}
		
		return results;
	}
	
	
	private List<ArticleData> syndFeedToArticleData(SyndFeed feed, boolean isCached) {
		ArticleData tmpArticle;
		List<ArticleData> results = new ArrayList<ArticleData>();
		
		assert(feed != null);
		
		@SuppressWarnings("unchecked")
		List<SyndEntry> entries = feed.getEntries();
		int numEntries = entries.size();
		//TODO: somehow load the feed image --> Log.i(TAG, "link = " + feed.getImage().getUrl());
		
		for (int i = 0; i < numEntries; i++) {
			tmpArticle = syndEntryToArticleData(entries.get(i));
			results.add(tmpArticle);
		}
		
		return results;
	}
	
	private ArticleData syndEntryToArticleData(SyndEntry entry) {
		ArticleData article = new ArticleData();
		String tmpStr;
		
		if (entry.getTitle() != null) {
			tmpStr = Text.removeHTMLAndStrip(entry.getTitle());
			article.setTitle(tmpStr);
		}
		
		if (entry.getAuthor() != null) {
			tmpStr = Text.removeHTMLAndStrip(entry.getAuthor());
			article.setAuthor(tmpStr);
		}
	    
		if (entry.getDescription() != null) {
			tmpStr = Text.removeHTMLAndStrip(entry.getDescription().getValue());
			article.setDescription(tmpStr);
		}
	    
	    if (entry.getPublishedDate() != null) {
	    	tmpStr = Text.removeHTMLAndStrip(entry.getPublishedDate().toString());
	    	article.setPublishedDate(tmpStr);
	    }
	    
	    if (entry.getLink() != null) {
	    	tmpStr = Text.removeHTMLAndStrip(entry.getLink());
	    	article.setURL(tmpStr);
	    }
	    
	    article.setIsCached(false);
	    article.setParentURL(this.url);
	    	    
	    return article;
	}
	
} // end RssConnection