package com.iodice.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class OrmBase {
    public static SQLiteDatabase getWritableDatabase(Context context) {
    	DatabaseWrapper databaseWrapper = DatabaseWrapper.getInstance(context);
    	SQLiteDatabase database = databaseWrapper.getWritableDatabase();
    	return database;
    }
}
