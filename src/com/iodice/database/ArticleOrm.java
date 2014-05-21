package com.iodice.database;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.iodice.utilities.Text;

public class ArticleOrm extends BaseOrm {

	private static final String TAG = "rssOrm";
	private static final String TABLE_NAME = "articles";
    
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
    
    private static final String COLUMN_IS_READ = "isRead";
    private static final String COLUMN_IS_READ_TYPE = "BOOLEAN NOT NULL CHECK (" + COLUMN_IS_READ + " IN (0,1))";
    
    public static final String SQL_CREATE_TABLE =
    		"CREATE TABLE " + TABLE_NAME + " (" +
    			COLUMN_URL            + " " + COLUMN_URL_TYPE            + COMMA_SEP +
    			COLUMN_PARENT_URL     + " " + COLUMN_PARENT_URL_TYPE     + COMMA_SEP +
                COLUMN_AUTHOR         + " " + COLUMN_AUTHOR_TYPE         + COMMA_SEP +
                COLUMN_DESCRIPTION    + " " + COLUMN_DESCRIPTION_TYPE    + COMMA_SEP +
                COLUMN_TITLE          + " " + COLUMN_TITLE_TYPE          + COMMA_SEP +
                COLUMN_PUBLISHED_DATE + " " + COLUMN_PUBLISHED_DATE_TYPE + COMMA_SEP +
                COLUMN_IS_CACHED      + " " + COLUMN_IS_CACHED_TYPE      + COMMA_SEP + 
                COLUMN_IS_READ         + " " + COLUMN_IS_READ_TYPE +
   			")";
    
    public static final String SQL_DROP_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;
    
    public static final String SQL_CREATE_PARENT_URL_INDEX = 
    		"CREATE INDEX " + INDEX_PARENT_URL + " ON " + TABLE_NAME + "(" + COLUMN_PARENT_URL + ")";
    

    public static void insertArticle(Context context, ArticleData article) throws SQLiteException {
	    SQLiteDatabase database = BaseOrm.getWritableDatabase(context);
        ContentValues values = articleToContentValues(article);
		WriteLockManager.beginWriteTransaction(database);
        insertArticleWithDB(values, database);
		WriteLockManager.setWriteTransactionSuccessfull(database);
		WriteLockManager.endWriteTransaction(database);
    }
    
    // leave caller to handle transaction handeling because callers who insert large chunks in one
    // transaction may want to use them less frequently than single updaters (for performance reasons)
    private static void insertArticleWithDB(ContentValues values, SQLiteDatabase db) {
    	try {
    		long rssId = db.insertOrThrow(ArticleOrm.TABLE_NAME, "null", values);
    		Log.i(TAG, "Inserted new Article_Data with ID: " + rssId);
    	} catch (SQLiteException e) {
    		if (!e.getMessage().contains("code 19"))
				throw e;
    	}
    }
    
    public static void insertArticles(Context context, List<ArticleData> articles) throws SQLiteException {
    	SQLiteDatabase database = BaseOrm.getWritableDatabase(context);
		ArrayList<ContentValues> valueList = new ArrayList<ContentValues>();
		int numArticles = articles.size();
		int i;
		
		// batch convert --> shorter in transaction time
		for (i = 0; i < numArticles; i++)
			valueList.add(articleToContentValues(articles.get(i)));
		
		// nest calls in transaction to boost write time
		WriteLockManager.beginWriteTransaction(database);
		for (i = 0; i < numArticles; i++) {
			insertArticleWithDB(valueList.get(i), database);
		}
		WriteLockManager.setWriteTransactionSuccessfull(database);
		WriteLockManager.endWriteTransaction(database);
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
        values.put(ArticleOrm.COLUMN_IS_READ, article.getIsRead());
        return values;
    }
    
    public static Cursor selectWhereParentLinkIs(Context context, 
    		List<String> links, 
    		int max, 
    		boolean getUnreadOnly) {
	    SQLiteDatabase database = BaseOrm.getReadableDatabase(context);
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
	    // get all or get unread only
	    if (getUnreadOnly)
	    	sql += " AND " + COLUMN_IS_READ + " = 0";

	    // always order by date
	    sql += " ORDER BY DATETIME(" + ArticleOrm.COLUMN_PUBLISHED_DATE + ") DESC";	  
	    sql += " LIMIT " + max;
	    Cursor cursor = database.rawQuery(sql, null);
	    return cursor;
    }
    
    // the 'inclusive' parameter determines whether or not a result is included
    // if one of the terms matches (inclusive = true) or all of the terms match (inclusive = false)
    public static Cursor selectWhereParentLinkIsAndContains(Context context, 
													    		List<String> links, 
													    		List<String> filterTerms, 
													    		List<String> columnsToFilterOn,
													    		boolean inclusive,
													    		int max,
													    		boolean getUnreadOnly) {
	    SQLiteDatabase database = BaseOrm.getReadableDatabase(context);
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
		// depending on the query type (inclusive vs not inclusive), build the relevant
		// statements
		int numCols = columnsToFilterOn.size();
		int numFilterTerms = filterTerms.size();
		if (numFilterTerms != 0 && numCols != 0) {
			sql += " AND (";
			for (int filtTerm = 0; filtTerm < numFilterTerms; filtTerm++) {
				if (filtTerm != 0) {
					if (inclusive == true)
						sql += " OR";
					else
						sql += " AND";
				}
				sql +=  "(";
				for (int col = 0; col < numCols; col++) {
					if (col != 0)
						sql += " OR";
					 sql += " LOWER(" + columnsToFilterOn.get(col) + ")" +
							" LIKE '%" + filterTerms.get(filtTerm).toLowerCase(Locale.US) + "%'";
				}
				sql += ")";
			}
			sql += " )";
		}
	    // get all or get unread only
	    if (getUnreadOnly)
	    	sql += " AND " + COLUMN_IS_READ + " = 0";
	    
		// always order by date
		sql += " ORDER BY DATETIME(" + ArticleOrm.COLUMN_PUBLISHED_DATE + ") DESC";
		sql += " LIMIT " + max;
		Log.i(TAG, "Ecexuting sql: " + sql);
		Cursor cursor = database.rawQuery(sql, null);
		return cursor;
    }
    
    /**
     * Update an article's read state. Usually, if an article is read, it wont be included
     * with a query, though this isnt always true.
     * 
     * @param url
     * @param isRead
     * @param context
     */
    public static void setArticleReadState(List<String> urls, boolean isRead, Context context) {
    	int numUrls = urls.size();
	    SQLiteDatabase database = BaseOrm.getReadableDatabase(context);
	    String sql = "UPDATE " + ArticleOrm.TABLE_NAME + 
	    		" SET " + ArticleOrm.COLUMN_IS_READ + "=";
	    
	    if (isRead)
	    	sql += "1";
	    else
	    	sql += "0";
	    sql += " WHERE " + ArticleOrm.COLUMN_URL + "='";
    	
    	WriteLockManager.beginWriteTransaction(database);
    	for (int i = 0; i < numUrls; i++)
    		database.execSQL(sql + urls.get(i) + "'");
    	WriteLockManager.setWriteTransactionSuccessfull(database);
    	WriteLockManager.endWriteTransaction(database);
    }
    
    public static void deleteArticlesWhereParentLinkIs(String url, Context context) {
    	deleteArticlesWhere(context, ArticleOrm.COLUMN_PARENT_URL, url);
    }
    
    public static void deleteArticlesWhereArticleLinkIs(String url, Context context) {
    	deleteArticlesWhere(context, ArticleOrm.COLUMN_URL, url);
    }
    
    private static void deleteArticlesWhere(Context context, String col, String val) {
    	SQLiteDatabase database = BaseOrm.getReadableDatabase(context);
    	WriteLockManager.beginWriteTransaction(database);

	    int id = database.delete(ArticleOrm.TABLE_NAME, col + " = ?", new String[] {val});
	    Log.i(TAG, "i = " + id + " :: value = " + val);
    	WriteLockManager.setWriteTransactionSuccessfull(database);
    	WriteLockManager.endWriteTransaction(database);
    }


    public static void deleteArticlesOlderThan(Context context, int daysOld) {
    	SQLiteDatabase database = BaseOrm.getReadableDatabase(context);
    	WriteLockManager.beginWriteTransaction(database);
    	
    	String sql = "DELETE FROM " + ArticleOrm.TABLE_NAME + 
    			" WHERE DATETIME(" + ArticleOrm.COLUMN_PUBLISHED_DATE + ")" +
    			" < date('now','-" + daysOld + " day')";	
    	database.execSQL(sql);
    	Log.i(TAG,  "Deleted articles older than " + daysOld + " days");
    	
    	WriteLockManager.setWriteTransactionSuccessfull(database);
    	WriteLockManager.endWriteTransaction(database);
    }
}