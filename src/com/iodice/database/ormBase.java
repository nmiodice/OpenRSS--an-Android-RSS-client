package com.iodice.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class ormBase {
    public static SQLiteDatabase getWritableDatabase(Context context) {
    	DatabaseWrapper databaseWrapper = new DatabaseWrapper(context);
    	SQLiteDatabase database = databaseWrapper.getWritableDatabase();
    	return database;
    }
    
    // should not be called directly
    public static Cursor selectAllOrderBy(Context context, String col) {
    	return null;
    }
}
