/* *
 * sets up initial data and starts a background thread whose only job is to call the
 * article update service periodically, allowing for freshly cached results
 */

package com.iodice.application;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.util.Log;

import com.iodice.database.FeedData;
import com.iodice.database.FeedOrm;
import com.iodice.rssreader.R;
import com.iodice.services.ArticleUpdateService;
import com.iodice.ui.base.AnimatedEntryList;

public class MyApplication 
extends Application
implements OnSharedPreferenceChangeListener {
	private static String TAG = "ApplicationSuperclass";
	
	@Override
	public void onCreate() {
		super.onCreate();
		initializeApplication(getApplicationContext());
	}
	
	/**
	 * Setup a preference change listener so the application can react to
	 * user preference modifications
	 */
	private void registerPreferenceChangeListener() {
		SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(this);
		preferences.registerOnSharedPreferenceChangeListener(this);
	}
	
	/**
	 * Runs startup logic, such as starting services and populating default data
	 * @param context
	 */
	private void initializeApplication(Context context) {
		boolean firstRun = SharedPrefsHelper.getIsFirstRun(this);
		
		/* or else our poor user wont have any fun test data to look at! */
		if (firstRun == true) {
			initDefaultFeeds(context);
			SharedPrefsHelper.setIsFirstRun(this, false);
		}
		startArticleUpdateService(context);
		registerPreferenceChangeListener();
	}
	
	/**
	 * Returns an intent that can be run via an alarm schedule
	 * @param context
	 * @return
	 */
	private PendingIntent getArticleUpdateServicePendingIntent(Context context) {
		Intent intent = new Intent(this, ArticleUpdateService.class);
		intent.putExtra(ArticleUpdateService.MAX_WORKER_THREADS, 2);
		/* set the action, otherwise the extras will not be sent when
		 * the intent is wrapper inside the pending intent
		 */
		intent.setAction(ArticleUpdateService.UPDATE_ARTICLES_ACTION);
		PendingIntent pintent = PendingIntent.getService(this, 0, intent, 0);
		return pintent;
	}
	
	/**
	 * Begin background updates according to the current user preferences
	 * @param context
	 */
	private void startArticleUpdateService(Context context) {
		boolean autoUpdate = SharedPrefsHelper.getUpdateInBackground(context);
		if (!autoUpdate)
			return;
		
		int hoursToWait = SharedPrefsHelper.getArticleUpdateFrequency(context);
		Calendar cal = Calendar.getInstance();
		PendingIntent pIntent = getArticleUpdateServicePendingIntent(context);
		
		// setup timer to run update schedule
		AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), hoursToWait*60*60*1000, pIntent);
		
		Log.i(TAG, "started article update service w/ update frequency = " + hoursToWait + " hours");
	}
	/**
	 * This is useful to invoke if the preference driving the update frequency is modified
	 * @param context
	 */
	private void cancelArticleUpdateService(Context context) {
		PendingIntent pIntent = getArticleUpdateServicePendingIntent(context);
		AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		alarm.cancel(pIntent);
	}
	
	// TODO: after testing, make this private. Being used to add a repopulate menu item for testing
	public void initDefaultFeeds(Context context) {
		Log.i(TAG, "Initializing default RSS feeds in database");
		List<FeedData> rssFeeds = new ArrayList<FeedData>();
		
		ArrayList<String> tech = new ArrayList<String>();
		ArrayList<String> news = new ArrayList<String>();
		ArrayList<String> reddit = new ArrayList<String>();
		ArrayList<String> sports = new ArrayList<String>();
		ArrayList<String> food = new ArrayList<String>();
		ArrayList<String> comboA = new ArrayList<String>();
		ArrayList<String> comboB = new ArrayList<String>();
		
		tech.add("Technology");
		news.add("News");
		reddit.add("Reddit");
		sports.add("Sports");
		food.add("Recipes & Food");
		
		rssFeeds.add(new FeedData("Apple", tech, "ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/limit=10/xml"));
		rssFeeds.add(new FeedData("Wired", tech, "http://feeds.wired.com/wired/index"));
		rssFeeds.add(new FeedData("BBC world", news, "http://feeds.bbci.co.uk/news/world/rss.xml"));
		rssFeeds.add(new FeedData("CNN", news, "http://rss.cnn.com/rss/cnn_topstories.rss"));
		rssFeeds.add(new FeedData("New York Times", news, "http://feeds.nytimes.com/nyt/rss/HomePage"));
		rssFeeds.add(new FeedData("USA Today", news, "http://rssfeeds.usatoday.com/usatoday-NewsTopStories"));
		rssFeeds.add(new FeedData("NPR", news, "http://www.npr.org/rss/rss.php?id=1001"));
		rssFeeds.add(new FeedData("Reuters", news, "http://feeds.reuters.com/reuters/topNews"));
		rssFeeds.add(new FeedData("BBC America", news, "http://newsrss.bbc.co.uk/rss/newsonline_world_edition/americas/rss.xml"));
		
		comboA.addAll(reddit);
		comboA.addAll(tech);
		rssFeeds.add(new FeedData("/r/androiddev", comboA, "http://www.reddit.com/r/androiddev/.rss"));
		rssFeeds.add(new FeedData("/r/programming", comboA, "http://www.reddit.com/r/programming/.rss"));
		rssFeeds.add(new FeedData("/r/android", comboA, "http://www.reddit.com/r/Android/.rss"));
		rssFeeds.add(new FeedData("/r/engineering", comboA, "http://www.reddit.com/r/engineering/.rss"));
		rssFeeds.add(new FeedData("/r/math", reddit, "http://www.reddit.com/r/math/.rss"));
		rssFeeds.add(new FeedData("/r/gradschool", reddit, "http://www.reddit.com/r/GradSchool/.rss"));
		rssFeeds.add(new FeedData("Yahoo Skiing", sports, "http://sports.yahoo.com/ski/rss.xml"));
		rssFeeds.add(new FeedData("Y.Combinator", tech, "https://news.ycombinator.com/rss"));
		rssFeeds.add(new FeedData("ESPN", sports, "http://sports.espn.go.com/espn/rss/news"));
		
		comboB.addAll(reddit);
		comboB.addAll(sports);
		rssFeeds.add(new FeedData("/r/skiing", comboB, "http://www.reddit.com/r/skiing/.rss"));
		rssFeeds.add(new FeedData("Food.com", food, "http://www.food.com/rss?"));
		rssFeeds.add(new FeedData("All Recipes", food, "http://rss.allrecipes.com/daily.aspx?hubID=84"));
		rssFeeds.add(new FeedData("TechCrunch", tech, "http://feeds.feedburner.com/TechCrunch/"));
		
		FeedOrm.saveFeeds(rssFeeds, context);
	}

	@Override
	/**
	 * Reacts to changes in user preferences
	 */
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		/* the article update service is not reactive to this change,
		 * so we can just manually restart it
		 */
		if (key.equals(getString(R.string.prefs_update_frequency)) ||
				key.equals(getString(R.string.prefs_update_in_background))) {
			cancelArticleUpdateService(this);
			startArticleUpdateService(this);
		} else if (key.equals(getString(R.string.prefs_enable_animation))) {
			boolean animate = SharedPrefsHelper.getEnableAnimations(this);
			AnimatedEntryList.setAnimationEnabled(animate);
		}
		
	}
}
