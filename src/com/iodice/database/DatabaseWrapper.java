package com.iodice.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseWrapper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseWrapper";
    private static DatabaseWrapper sInstance;

    private static final String DATABASE_NAME = "RSSReader.db";
    private static final int DATABASE_VERSION = 21;

    public DatabaseWrapper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    // follow a singleton pattern
    public static DatabaseWrapper getInstance(Context context) {
        // Use the application context, which will ensure that you 
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
          sInstance = new DatabaseWrapper(context.getApplicationContext());
        }
        return sInstance;
      }
    
    

    /**
     * Called if the database named DATABASE_NAME doesn't exist in order to create it.
     */
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Log.i(TAG, "Creating database [" + DATABASE_NAME + " v." + DATABASE_VERSION + "]...");
        sqLiteDatabase.execSQL(ArticleOrm.SQL_CREATE_TABLE);
        sqLiteDatabase.execSQL(ArticleOrm.SQL_CREATE_PARENT_URL_INDEX);
        
        sqLiteDatabase.execSQL(FeedOrm.SQL_CREATE_TABLE);
        sqLiteDatabase.execSQL(FeedOrm.SQL_CREATE_GROUP_INDEX); 
        
        sqLiteDatabase.execSQL(FeedOrm.getCategoryTableCreateStatement());
        sqLiteDatabase.execSQL(FeedOrm.getCategoryTableCreateIndexStatement());
        
        sqLiteDatabase.execSQL(FeedOrm.getCategoryFeedMapTableCreateStatement());
        sqLiteDatabase.execSQL(FeedOrm.getCategoryFeedMapTableCreateIndexStatement());
        
    }

    /**
     * Called when the DATABASE_VERSION is increased.
     */
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        Log.i(TAG, "Upgrading database ["+DATABASE_NAME+" v." + oldVersion+"] to ["+DATABASE_NAME+" v." + newVersion+"]...");
        sqLiteDatabase.execSQL(ArticleOrm.SQL_DROP_TABLE);
        sqLiteDatabase.execSQL(FeedOrm.SQL_DROP_TABLE);
        sqLiteDatabase.execSQL(FeedOrm.getCategoryTableDropStatement());
        sqLiteDatabase.execSQL(FeedOrm.getCategoryFeedMapTableDropStatement());
        
        onCreate(sqLiteDatabase);
    }
}