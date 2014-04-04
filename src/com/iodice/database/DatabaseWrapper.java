package com.iodice.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseWrapper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseWrapper";

    private static final String DATABASE_NAME = "RSSReader.db";
    private static final int DATABASE_VERSION = 20;

    public DatabaseWrapper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Called if the database named DATABASE_NAME doesn't exist in order to create it.
     */
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Log.i(TAG, "Creating database [" + DATABASE_NAME + " v." + DATABASE_VERSION + "]...");
        sqLiteDatabase.execSQL(rssOrm.SQL_CREATE_TABLE);
        sqLiteDatabase.execSQL(rssOrm.SQL_CREATE_PARENT_URL_INDEX);
        
        sqLiteDatabase.execSQL(feedsOrm.SQL_CREATE_TABLE);
        sqLiteDatabase.execSQL(feedsOrm.SQL_CREATE_GROUP_INDEX); 
        
        sqLiteDatabase.execSQL(feedsOrm.getCategoryTableCreateStatement());
        sqLiteDatabase.execSQL(feedsOrm.getCategoryFeedMapTableCreateStatement());
        
    }

    /**
     * Called when the DATABASE_VERSION is increased.
     */
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        Log.i(TAG, "Upgrading database ["+DATABASE_NAME+" v." + oldVersion+"] to ["+DATABASE_NAME+" v." + newVersion+"]...");
        sqLiteDatabase.execSQL(rssOrm.SQL_DROP_TABLE);
        sqLiteDatabase.execSQL(feedsOrm.SQL_DROP_TABLE);
        sqLiteDatabase.execSQL(feedsOrm.getCategoryTableDropStatement());
        sqLiteDatabase.execSQL(feedsOrm.getCategoryFeedMapTableDropStatement());
        
        onCreate(sqLiteDatabase);
    }
}