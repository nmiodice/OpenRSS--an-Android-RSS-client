package com.iodice.database;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.iodice.ui.home.Feed_Data;

@SuppressLint("DefaultLocale")
public class feedsOrm extends ormBase {
	private static final String TAG = "feedsOrm";
	private static final String TABLE_NAME = "feeds";
    private static final String COMMA_SEP = ", ";
    
    private static final String COLUMN_NAME_TYPE = "TEXT PRIMARY KEY NOT NULL";
    public static final String COLUMN_NAME = "name";
    
    private static final String COLUMN_URL_TYPE = "TEXT NOT NULL";
    public static final String COLUMN_URL = "url";

    /* 'group' not used because it is an sqLite reserved word */
    private static final String COLUMN_GROUP_TYPE = "TEXT";
    public static final String COLUMN_GROUP = "collection";
    private static final String INDEX_GROUP = "collection_idx";
    
    
    public static final String SQL_CREATE_TABLE =
    		"CREATE TABLE "  + TABLE_NAME + " (" +
        		COLUMN_GROUP + " " + COLUMN_GROUP_TYPE + COMMA_SEP +
    			COLUMN_URL   + " " + COLUMN_URL_TYPE   + COMMA_SEP +
    			COLUMN_NAME  + " " + COLUMN_NAME_TYPE  +
   			")";
    
    public static final String SQL_DROP_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;
    
    public static final String SQL_CREATE_GROUP_INDEX = 
    		"CREATE INDEX " + INDEX_GROUP + " ON " + TABLE_NAME + "(" + COLUMN_GROUP + ")";

    public static void insertFeed(Feed_Data feed, SQLiteDatabase database) throws SQLiteException {
        ContentValues values = rssToContentValues(feed);
        long feedId = database.insertOrThrow(feedsOrm.TABLE_NAME, "null", values);
        Log.i(TAG, "Inserted new Feed with ID: " + feedId);
        saveCategories(feed, feedId, database);
        Log.i(TAG, "Inserted categories for Feed w/ ID = " + feedId);
    }
    
    private static void saveCategories(Feed_Data feed, long feedId, SQLiteDatabase database) {
    	assert (feed != null);
    	
    	List<String> groups = feed.getGroups();
    	int size = groups.size();
    	int catId;
    	
    	/* this is a two step process because the feed and its categories are stored in two different
    	 * tables. The structure is:
    	 * feeds:
    	 *	URL primary key
    	 *
    	 * categories:
    	 * 	ID primary key
    	 * 	Category name
    	 * 
    	 * categoryFeedMap:
    	 * 	Category ID
    	 * 	Feed URL
    	 */
    	for (int i = 0; i < size; i++) {
    		catId = (int) categories.insertCategory(groups.get(i), database);
    		Log.i(categories.TAG, "catId = " + catId);
    		categoryFeedMap.insertCategoryFeedPair(catId, feed.getURL(), database);
    	}
    }
    
    
    private static ContentValues rssToContentValues(Feed_Data feed) {
        ContentValues values = new ContentValues();
        
        // putNull in the case of "" values because these properties obey "NOT NULL"
        // data types
        if (feed.getURL().equals(""))
        	values.putNull(feedsOrm.COLUMN_URL);
        else
        	values.put(feedsOrm.COLUMN_URL, feed.getURL());
        
        if (feed.getName() == null)
        	values.putNull(feedsOrm.COLUMN_NAME);
        else
        	values.put(feedsOrm.COLUMN_NAME, feed.getName());
        
        return values;
    }
    
    
    public static Cursor selectAll(Context context) {
		DatabaseWrapper databaseWrapper = new DatabaseWrapper(context);
	    SQLiteDatabase database = databaseWrapper.getReadableDatabase();

	    // the rowid selected as _id makes this query compatible with the SimpleCursorAdapter class
	    String sql = "SELECT rowid _id,* FROM " + feedsOrm.TABLE_NAME;
        Log.i(TAG, "Executing: " + sql);

	    Cursor cursor = database.rawQuery(sql, null);	
	    Log.i(TAG, "Loaded " + cursor.getCount() + " feed definitions...");
	
	    database.close();
	    return cursor;
    }
    
    public static Cursor selectAllOrderBy(Context context, String orderBy) {
		Cursor cursor = feedsOrm.selectAllWhereLinkIs(context, null, orderBy);
	    return cursor;
    }
    
    public static Cursor selectAllOrderByWhere(Context context, String orderBy, String category) {
		DatabaseWrapper databaseWrapper = new DatabaseWrapper(context);
	    SQLiteDatabase database = databaseWrapper.getReadableDatabase();

	    // 1. use an inner join in order to get links that are part of a specific category
	    String sql = "SELECT " + categoryFeedMap.COLUMN_URL + 
	    				" FROM " + categoryFeedMap.TABLE_NAME + 
	    					" INNER JOIN " + categories.TABLE_NAME +
	    					" ON " + categories.COLUMN_ID + "=" + categoryFeedMap.COLUMN_CATEGORY +
	    				" WHERE " + categories.COLUMN_CATEGORY + "=?";
	    
        Log.i(TAG, "Executing: " + sql);
	    Cursor cursor = database.rawQuery(sql, new String[] {category});	
	    Log.i(TAG, "Loaded " + cursor.getCount() + " links with the category " + category + "...");
	    database.close();
	    
	    if (cursor.getCount() == 0)
	    	return null;
	    
	    // 2. build a list of links and return a full table query on those links
	    ArrayList<String> linkList = new ArrayList<String>();
	    cursor.moveToFirst();
		while(!cursor.isAfterLast()) {
			linkList.add(cursor.getString(cursor.getColumnIndex(feedsOrm.COLUMN_URL)));
			cursor.moveToNext();
		}
	    cursor = selectAllWhereLinkIs(context, linkList, orderBy);
	    
	    return cursor;
    }
    
    // a select all query where a list of links can be used as a constraint
    private static Cursor selectAllWhereLinkIs(Context context, List<String> links, String orderBy) {
		DatabaseWrapper databaseWrapper = new DatabaseWrapper(context);
	    SQLiteDatabase database = databaseWrapper.getReadableDatabase();
	    String whereClause = "";
	    
	    // if there are where constraints, build up the were clause dynamically
	    if (links != null && links.size() != 0) {
		    int linkListSize = links.size();
	    	whereClause = " WHERE " + feedsOrm.COLUMN_URL + " IN (";
		    for (int i = 0; i < linkListSize; i++) {
		    	whereClause += "'" + links.get(i) + "',";
		    }
		    whereClause = whereClause.substring(0, whereClause.length() - 1);
		    whereClause += ")";
	    }
	    
	    // the rowid selected as _id makes this query compatible with the SimpleCursorAdapter class
	    String sql = "SELECT rowid _id,* FROM " + feedsOrm.TABLE_NAME + whereClause + " ORDER BY LOWER(" + orderBy + ")";
        Log.i(TAG, "Executing: " + sql);

	    Cursor cursor = database.rawQuery(sql, null);	
	    Log.i(TAG, "Loaded " + cursor.getCount() + " feed definitions...");
	
	    database.close();
	    return cursor;
    }
    
    public static Cursor selectAllCategories(Context context) {
		return categories.selectAll(context);
    }
    
    public static void deleteFeedWithLink(String url, Context context) {
    	DatabaseWrapper databaseWrapper = new DatabaseWrapper(context);
	    SQLiteDatabase database = databaseWrapper.getReadableDatabase();
	    	    
	    database.beginTransaction();
	    int numDeleted = database.delete(feedsOrm.TABLE_NAME, "url = ?", new String[] {url});
	    categoryFeedMap.deletePairsAssociatedWithUrl(url, database);
	    
	    database.setTransactionSuccessful();
	    database.endTransaction();
	  	database.close();
	    
	    Log.i(TAG, "Deleted " + numDeleted + " feed(s) from the database.");

    }
    
    
    public static String getCategoryTableCategoryKey() {
    	return categories.COLUMN_CATEGORY;
    }
    public static String getCategoryTableCreateStatement() {
    	return categories.SQL_CREATE_TABLE;
    }
    public static String getCategoryTableDropStatement() {
    	return categories.SQL_DROP_TABLE;
    }   
    
    public static String getCategoryFeedMapTableCreateStatement() {
    	return categoryFeedMap.SQL_CREATE_TABLE;
    }
    public static String getCategoryFeedMapTableDropStatement() {
    	return categoryFeedMap.SQL_DROP_TABLE;
    }
    
    /* holds a category and ID */
    private static class categories extends ormBase {
    	private static final String TAG = "categories";
    	public static final String TABLE_NAME = "feed_categories";
        private static final String COMMA_SEP = ", ";
        
        private static final String COLUMN_ID_TYPE = "INTEGER PRIMARY KEY";
        public static final String COLUMN_ID = "id";
        
        private static final String COLUMN_CATEGORY_TYPE = "TEXT UNIQUE";
        public static final String COLUMN_CATEGORY = "category";
        
        public static final String SQL_CREATE_TABLE =
        		"CREATE TABLE "  + TABLE_NAME + " (" +
        			COLUMN_ID + " " + COLUMN_ID_TYPE + COMMA_SEP +
        			COLUMN_CATEGORY   + " " + COLUMN_CATEGORY_TYPE +
       			")";
        
        public static final String SQL_DROP_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
        
        /* returns ID of inserted category, or the ID of the category if it already exists */
	    public static long insertCategory(String category, SQLiteDatabase database) throws SQLiteException {
	        ContentValues values = new ContentValues();
	        values.put(categories.COLUMN_CATEGORY, category);
	        
	        int id = -1;
	        
	        try {
	        	id = (int)database.insertOrThrow(categories.TABLE_NAME, "null", values);
	        	Log.i(categories.TAG, "Inserted new category with ID: " + id);
			} catch (SQLiteException e) {
				if (e.getMessage().contains("code 19")) {
					Log.i(categories.TAG, "Duplicate category = '" + category + "' found");
					id = getCategoryId(category, database);
				} else {
					Log.i(categories.TAG, "Failed to save category and category does not exist in DB. Category = " + category);
				}
	        }

	        return id;
	    }
	    
	    /* takes a parameter. client call may not require a DB object */
	    private static int getCategoryId(String category, SQLiteDatabase database) {
	    	int id = -1;
		    String sql = "SELECT " + categories.COLUMN_ID + 
		    		" FROM " + categories.TABLE_NAME + 
		    		" WHERE " + categories.COLUMN_CATEGORY + " =?" +
		    		" LIMIT 1";
	        Log.i(categories.TAG, "Executing: " + sql);

		    Cursor cursor = database.rawQuery(sql, new String[] {category});	
		    cursor.moveToFirst();
		    id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
		    cursor.close();
		    
		    Log.i(TAG, "Loaded category with ID = " + id);
		    return id;
	    }
	    
	    public static Cursor selectAll(Context context) {
	    	DatabaseWrapper databaseWrapper = new DatabaseWrapper(context);
		    SQLiteDatabase database = databaseWrapper.getReadableDatabase();
		    String sql = "SELECT rowid _id," + categories.COLUMN_CATEGORY + " FROM " + categories.TABLE_NAME;  
	        
		    Log.i(TAG, "Executing: " + sql);

		    Cursor cursor = database.rawQuery(sql, null);	
		    cursor.moveToFirst();
		    
		    Log.i(TAG, "Found " + cursor.getCount() + " categories");
		    
		    return cursor;
	    }
    }

    /* holds the mappings between a feed and its categories. A feed is defined
     * by its URL
     */
    private static class categoryFeedMap extends ormBase {
    	private static final String TAG = "categoryFeedMap";
    	public static final String TABLE_NAME = "feed_and_categories";
        private static final String COMMA_SEP = ", ";
        
        private static final String COLUMN_URL_TYPE = "TEXT";
        public static final String COLUMN_URL = "url";
        
        private static final String COLUMN_CATEGORY_TYPE = "INT";
        public static final String COLUMN_CATEGORY = "categoryID";
        
        public static final String SQL_CREATE_TABLE =
        		"CREATE TABLE "  + TABLE_NAME + " (" +
        			COLUMN_URL + " " + COLUMN_URL_TYPE + COMMA_SEP +
        			COLUMN_CATEGORY   + " " + COLUMN_CATEGORY_TYPE + COMMA_SEP +
        			"UNIQUE(" + COLUMN_URL + COMMA_SEP + COLUMN_CATEGORY + ") " +
       			")";
        
        public static final String SQL_DROP_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
        
	    public static void insertCategoryFeedPair(int categoryID, 
	    		String url, 
	    		SQLiteDatabase database) 
	    				throws SQLiteException {
	        ContentValues values = new ContentValues();
	        
	        values.put(categoryFeedMap.COLUMN_URL, url);
	        values.put(categoryFeedMap.COLUMN_CATEGORY, categoryID);
	        
	        long id = database.insertOrThrow(categoryFeedMap.TABLE_NAME, "null", values);
	        Log.i(categoryFeedMap.TAG, "Inserted category ID/feed URL pair (id " + id + "): " + categoryID + ", " + url);
	    }
	    
	   
	    // 1. select the categoryIDs associated with the specified link
	    // 2. delete all cateogry/link pairs with the specified link
	    // 3. for each categoryID from (1), if there are no more categoryID/link pairs with that categoryID, delete
	    //		the category with that ID.
	    public static void deletePairsAssociatedWithUrl(String url, SQLiteDatabase database) {
	    	
	    	// 1. select the categoryIDs associated with the specified link
		    String sql = "SELECT " + categoryFeedMap.COLUMN_CATEGORY + 
		    				" FROM " + categoryFeedMap.TABLE_NAME +
		    				" WHERE " + categoryFeedMap.COLUMN_URL + "=?";
		    Log.i(TAG, "Executing: " + sql);
		    Cursor cursor = database.rawQuery(sql, new String[] {url});
		    
		    // build up list of categoryIDs
		    cursor.moveToFirst();
		    ArrayList<Integer> catIDList = new ArrayList<Integer>();
			while(!cursor.isAfterLast()) {
				catIDList.add(cursor.getInt(cursor.getColumnIndex(categoryFeedMap.COLUMN_CATEGORY)));
				cursor.moveToNext();
			}
		    
		    // 2. delete all cateogry/link pairs with the specified link
	    	int numDeleted = database.delete(categoryFeedMap.TABLE_NAME, 
	    			categoryFeedMap.COLUMN_URL + " =?", 
	    			new String[] {url});
	    	Log.i(categoryFeedMap.TAG, "Deleted " + numDeleted + " category/url pair(s)");
	    	
	    	// 3. check to see if we need to delete any category definitions
	    	int numCats = catIDList.size();
	    	for (int i = 0; i < numCats; i++) {
	    		cursor.close();
		    	sql = "SELECT * FROM " + categoryFeedMap.TABLE_NAME + 
		    			" WHERE " + categoryFeedMap.COLUMN_CATEGORY + "=?";
	    		cursor = database.rawQuery(sql, new String[] {catIDList.get(i).toString()});
	    		// only delete the category if no more feeds are classified under that category
	    		if (cursor.getCount() != 0)
	    			continue;
	    		database.delete(categories.TABLE_NAME, 
	    							categories.COLUMN_ID + "=?", 
	    							new String[] {catIDList.get(i).toString()});
		    	Log.i(categoryFeedMap.TAG, "No more feeds under category with ID " + catIDList.get(i) + ". Category deleted");
	    	}
	    }
    }

}
