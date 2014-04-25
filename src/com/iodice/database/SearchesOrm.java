package com.iodice.database;

import com.iodice.rssreader.R;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.widget.Toast;

public class SearchesOrm extends OrmBase {

	private static final String TAG = "SavedSearchesOrm";
	
	private static final String TABLE_NAME = "savedsearches";
    private static final String COMMA_SEP = ", ";
    
    private static final String COLUMN_SEARCH_TERM_TYPE = "TEXT PRIMARY KEY";
    public static final String COLUMN_SEARCH_TERM = "search_term";
    
    private static final String COLUMN_NAME_TYPE = "TEXT";
    public static final String COLUMN_NAME = "name";
    
    
    public static final String SQL_CREATE_TABLE =
    		"CREATE TABLE " + TABLE_NAME + " (" +
    			COLUMN_SEARCH_TERM + " " + COLUMN_SEARCH_TERM_TYPE + COMMA_SEP +
    			COLUMN_NAME + " " + COLUMN_NAME_TYPE +
   			")";
    
    public static final String SQL_DROP_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    public static void insertSearch(SearchData search, Context context) throws SQLiteException {
    	SQLiteDatabase database = getWritableDatabase(context);
    	
    	ContentValues values = searchToContentValues(search);
        
		try {
	    	database.beginTransaction();
	        long id = database.insertOrThrow(SearchesOrm.TABLE_NAME, "null", values);
	        database.setTransactionSuccessful();
	        database.endTransaction();
	        database.close();
	        Log.i(TAG, "Inserted new SearchData with ID: " + id);
		} catch (Exception e) {
			if (e.getMessage().contains("code 19")) {
				CharSequence text = context.getText(R.string.saved_search_already_exists);
				int duration = Toast.LENGTH_SHORT;

				Toast toast = Toast.makeText(context, text, duration);
				toast.show();
			} else {
				Log.e(TAG, "Error saving feed. SQLiteDatabase error: " + e.getMessage());
			}
		}
    }
    
    private static ContentValues searchToContentValues(SearchData search) {
        ContentValues values = new ContentValues();
        values.put(SearchesOrm.COLUMN_NAME, search.getName());
        values.put(SearchesOrm.COLUMN_SEARCH_TERM, search.getSearchTerm());
        return values;
    }
    
    public static Cursor selectAll(Context context) {
    	DatabaseWrapper databaseWrapper = new DatabaseWrapper(context);
	    SQLiteDatabase database = databaseWrapper.getReadableDatabase();
	    String sql = "SELECT rowid _id,* FROM " + SearchesOrm.TABLE_NAME;

	    // always order by date
	    sql += " ORDER BY " + SearchesOrm.COLUMN_NAME;	    
	    Cursor cursor = database.rawQuery(sql, null);
	    return cursor;
    }
    
    public static void deleteArticlesWhereLinkIs(String name, Context context) {
    	DatabaseWrapper databaseWrapper = new DatabaseWrapper(context);
	    SQLiteDatabase database = databaseWrapper.getReadableDatabase();
	    	    
	    database.beginTransaction();
	    Log.i(TAG, "DELETING!");
	    int id = database.delete(SearchesOrm.TABLE_NAME, SearchesOrm.COLUMN_NAME + " = ?", new String[] {name});
	    Log.i(TAG, "i = " + id + " :: name = " + name);
	    database.setTransactionSuccessful();
	    database.endTransaction();
	  	database.close();
    }
}