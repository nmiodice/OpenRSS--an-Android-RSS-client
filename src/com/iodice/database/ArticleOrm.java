package com.iodice.database;

import java.util.List;

import com.iodice.utilities.Text;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

public class ArticleOrm extends OrmBase {

	private static final String TAG = "rssOrm";
	private static final String TABLE_NAME = "articles";
    private static final String COMMA_SEP = ", ";
    
    private static final String COLUMN_URL_TYPE = "TEXT PRIMARY KEY";
    public static final String COLUMN_URL = "url";
    
    private static final String COLUMN_PARENT_URL_TYPE = "TEXT";
    public static final String COLUMN_PARENT_URL = "parentUrl";
    private static final String INDEX_PARENT_URL = "parent_url_index";
    
    private static final String COLUMN_AUTHOR_TYPE = "TEXT";
    public static final String COLUMN_AUTHOR =  "author";
    
    private static final String COLUMN_DESCRIPTION_TYPE = "TEXT";
    public static final String COLUMN_DESCRIPTION = "description";
    
    private static final String COLUMN_TITLE_TYPE = "TEXT";
    public static final String COLUMN_TITLE = "title";
    
    private static final String COLUMN_PUBLISHED_DATE_TYPE = "DATETIME";
    public static final String COLUMN_PUBLISHED_DATE = "publishedDate";
    
    private static final String COLUMN_IS_CACHED_TYPE = "BOOLEAN";
    public static final String COLUMN_IS_CACHED = "isCached";
    
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
    

    public static void insertArticle(ArticleData article, SQLiteDatabase database) throws SQLiteException {
        ContentValues values = articleToContentValues(article);
        long rssId = database.insertOrThrow(ArticleOrm.TABLE_NAME, "null", values);
        Log.i(TAG, "Inserted new Article_Data with ID: " + rssId);
    }
    
    private static ContentValues articleToContentValues(ArticleData article) {
        ContentValues values = new ContentValues();
        values.put(ArticleOrm.COLUMN_URL, article.getURL());
        values.put(ArticleOrm.COLUMN_PARENT_URL, article.getParentURL());        
        values.put(ArticleOrm.COLUMN_AUTHOR, article.getAuthor());
        values.put(ArticleOrm.COLUMN_DESCRIPTION, article.getDescription());
        values.put(ArticleOrm.COLUMN_TITLE, article.getTitle());
        values.put(ArticleOrm.COLUMN_PUBLISHED_DATE, Text.datetimeToSQLDateString(article.getPublishedDate()));
        values.put(ArticleOrm.COLUMN_IS_CACHED, article.getIsCached());        
        return values;
    }
    
    public static Cursor selectWhereParentLinkIs(Context context, List<String> links) {
		DatabaseWrapper databaseWrapper = new DatabaseWrapper(context);
	    SQLiteDatabase database = databaseWrapper.getReadableDatabase();
	    String sql = "SELECT rowid _id,* FROM " + ArticleOrm.TABLE_NAME + 
	    				" WHERE " + ArticleOrm.COLUMN_PARENT_URL + " IN(";
	    int numLinks = links.size();
	    for (int i = 0; i < numLinks; i++) {
	    	sql += "'";
	    	sql += links.get(i);
	    	sql += "'";
	    	
	    	if (i < numLinks -1)
	    		sql += ",";
	    	else
	    		sql += ")";
	    }
	    // always order by date
	    sql += " ORDER BY DATETIME(" + ArticleOrm.COLUMN_PUBLISHED_DATE + ") DESC";
	    
	    Log.i(TAG,  "Executing SQL: " + sql);
	    Cursor cursor = database.rawQuery(sql, null);
	    return cursor;
    }

}