package com.iodice.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.widget.Toast;

import com.iodice.rssreader.R;

public class SearchesOrm extends BaseOrm {

	private static final String TAG = "SearchesOrm";
	
	private static final String TABLE_NAME = "savedsearches";
    
    private static final String COLUMN_SEARCH_TERM_TYPE = "TEXT PRIMARY KEY";
    public static final String COLUMN_SEARCH_TERM = "search_term";
    
    
    public static final String SQL_CREATE_TABLE =
    		"CREATE TABLE " + TABLE_NAME + " (" +
    			COLUMN_SEARCH_TERM + " " + COLUMN_SEARCH_TERM_TYPE +
   			")";
    
    public static final String SQL_DROP_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    public static void insertSearch(SearchData search, Context context) throws SQLiteException {
    	SQLiteDatabase database = BaseOrm.getWritableDatabase(context);
    	
    	if (search.getSearchTerm().isEmpty()) {
	        CharSequence text = context.getText(R.string.saved_search_is_empty);
			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(context, text, duration);
			toast.show();
			return;
    	}
    	
    	ContentValues values = searchToContentValues(search);
    	
		try {
			WriteLockManager.beginWriteTransaction(database);
	        database.insertOrThrow(SearchesOrm.TABLE_NAME, "null", values);
	        WriteLockManager.setWriteTransactionSuccessfull(database);
	        WriteLockManager.endWriteTransaction(database);
		
	        CharSequence text = context.getText(R.string.saved_search_success);
			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(context, text, duration);
			toast.show();
		} catch (Exception e) {
			if (e.getMessage().contains("code 19")) {
				CharSequence text = context.getText(R.string.saved_search_already_exists);
				int duration = Toast.LENGTH_SHORT;

				Toast toast = Toast.makeText(context, text, duration);
				toast.show();
			} else {
				Log.e(TAG, "Error saving feed. SQLiteDatabase error: " + e.getMessage());
			}
			WriteLockManager.endWriteTransaction(database);
		}
    }
    
    private static ContentValues searchToContentValues(SearchData search) {
        ContentValues values = new ContentValues();
        values.put(SearchesOrm.COLUMN_SEARCH_TERM, search.getSearchTerm());
        return values;
    }
    
    public static Cursor selectAll(Context context) {
    	SQLiteDatabase database = BaseOrm.getReadableDatabase(context);
	    String sql = "SELECT rowid _id,* FROM " + SearchesOrm.TABLE_NAME;

	    sql += " ORDER BY " + SearchesOrm.COLUMN_SEARCH_TERM;	    
	    Cursor cursor = database.rawQuery(sql, null);
	    return cursor;
    }
    
    public static void deleteSearchesWhereNameIs(String searchTerm, Context context) {
	    SQLiteDatabase database = BaseOrm.getWritableDatabase(context);
	    	    
		WriteLockManager.beginWriteTransaction(database);
		try {
			int id = database.delete(SearchesOrm.TABLE_NAME, SearchesOrm.COLUMN_SEARCH_TERM + " = ?", new String[]{searchTerm});
			Log.i(TAG, "num deleted = " + id + " :: searchTerm = " + searchTerm);
			WriteLockManager.setWriteTransactionSuccessfull(database);
			WriteLockManager.endWriteTransaction(database);
		} catch (Exception e) {
			WriteLockManager.endWriteTransaction(database);
		}
    }
}