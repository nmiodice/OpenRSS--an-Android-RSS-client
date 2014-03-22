package com.iodice.database;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.iodice.ui.feed.Fragment_Rss;

public class rssOrm extends ormBase {

	private static final String TAG = "rssOrm";
	private static final String TABLE_NAME = "rss";
    private static final String COMMA_SEP = ", ";
    
    private static final String COLUMN_URL_TYPE = "TEXT PRIMARY KEY";
    private static final String COLUMN_URL = "url";
    
    private static final String COLUMN_PARENT_URL_TYPE = "TEXT";
    private static final String COLUMN_PARENT_URL = "parentUrl";
    private static final String INDEX_PARENT_URL = "parent_url_index";
    
    private static final String COLUMN_AUTHOR_TYPE = "TEXT";
    private static final String COLUMN_AUTHOR =  "author";
    
    private static final String COLUMN_DESCRIPTION_TYPE = "TEXT";
    private static final String COLUMN_DESCRIPTION = "description";
    
    private static final String COLUMN_TITLE_TYPE = "TEXT";
    private static final String COLUMN_TITLE = "title";
    
    private static final String COLUMN_PUBLISHED_DATE_TYPE = "TEXT";
    private static final String COLUMN_PUBLISHED_DATE = "publishedDate";
    
    private static final String COLUMN_IS_CACHED_TYPE = "BOOLEAN";
    private static final String COLUMN_IS_CACHED = "isCached";
    
    public static final String SQL_CREATE_TABLE =
    		"CREATE TABLE " + TABLE_NAME + " (" +
    			COLUMN_URL            + " " + COLUMN_URL_TYPE            + COMMA_SEP +
    			COLUMN_PARENT_URL     + " " + COLUMN_PARENT_URL_TYPE     + COMMA_SEP +
                COLUMN_AUTHOR         + " " + COLUMN_AUTHOR_TYPE         + COMMA_SEP +
                COLUMN_DESCRIPTION    + " " + COLUMN_DESCRIPTION_TYPE    + COMMA_SEP +
                COLUMN_TITLE          + " " + COLUMN_TITLE_TYPE          + COMMA_SEP +
                COLUMN_PUBLISHED_DATE + " " + COLUMN_PUBLISHED_DATE_TYPE + COMMA_SEP +
                COLUMN_IS_CACHED      + " " + COLUMN_IS_CACHED_TYPE +
   			")";
    
    public static final String SQL_DROP_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;
    
    public static final String SQL_CREATE_PARENT_URL_INDEX = 
    		"CREATE INDEX " + INDEX_PARENT_URL + " ON " + TABLE_NAME + "(" + COLUMN_PARENT_URL + ")";
    

    public static void insertRss(Fragment_Rss rss, SQLiteDatabase database) throws SQLiteException {
        ContentValues values = rssToContentValues(rss);
        long rssId = database.insertOrThrow(rssOrm.TABLE_NAME, "null", values);
        Log.i(TAG, "Inserted new Fragment_Rss with ID: " + rssId);
    }
    
    private static ContentValues rssToContentValues(Fragment_Rss rss) {
        ContentValues values = new ContentValues();
        values.put(rssOrm.COLUMN_URL, rss.getURL());
        values.put(rssOrm.COLUMN_PARENT_URL, rss.getParentURL());        
        values.put(rssOrm.COLUMN_AUTHOR, rss.getAuthor());
        values.put(rssOrm.COLUMN_DESCRIPTION, rss.getDescription());
        values.put(rssOrm.COLUMN_TITLE, rss.getTitle());
        values.put(rssOrm.COLUMN_PUBLISHED_DATE, rss.getPublishedDate());
        values.put(rssOrm.COLUMN_IS_CACHED, rss.getIsCached());
        
        Log.i(TAG, "parent " + rss.getParentURL());
        
        return values;
    }
    
    public static List<Fragment_Rss> selectWhere(Context context, String whereClause) {
		Fragment_Rss frag;
		List<Fragment_Rss> results = new ArrayList<Fragment_Rss>();
		DatabaseWrapper databaseWrapper = new DatabaseWrapper(context);
	    SQLiteDatabase database = databaseWrapper.getReadableDatabase();
	    	    
	    String sql = "SELECT * FROM " + rssOrm.TABLE_NAME + " WHERE " + whereClause;
        Log.i(TAG, "Executing: " + sql);

	    Cursor cursor = database.rawQuery(sql, null);	
	    Log.i(TAG, "Loaded " + cursor.getCount() + " cached rss entries...");
	
	    if(cursor.getCount() > 0) {
	        cursor.moveToFirst();
	        while (!cursor.isAfterLast()) {
	            frag = cursorToFragment(cursor);
	            results.add(frag);
	            cursor.moveToNext();
	        }
	        Log.i(TAG, "Rss entries loaded successfully from database");
	    }
	
	    database.close();
	    return results;
    }
    
    private static Fragment_Rss cursorToFragment(Cursor cursor) {
    	Fragment_Rss frag = new Fragment_Rss();
    	
    	frag.setURL(cursor.getString(cursor.getColumnIndex(COLUMN_URL)));
    	frag.setParentURL(cursor.getString(cursor.getColumnIndex(COLUMN_PARENT_URL)));
    	frag.setAuthor(cursor.getString(cursor.getColumnIndex(COLUMN_AUTHOR)));
    	frag.setDescription(cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION)));
    	frag.setTitle(cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)));
    	frag.setPublishedDate(cursor.getString(cursor.getColumnIndex(COLUMN_PUBLISHED_DATE)));
    	
    	frag.setIsCached(true);
    	return frag;
    }
}