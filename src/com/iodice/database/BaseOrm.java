package com.iodice.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class BaseOrm {
	private static SQLiteDatabase db = null;
	
	
    public static SQLiteDatabase getWritableDatabase(Context context) {
		if (BaseOrm.db == null) {
			DatabaseWrapper databaseWrapper = DatabaseWrapper.getInstance(context);
			BaseOrm.db = databaseWrapper.getWritableDatabase();
			
			// simultaneous read/write access to the DB uses more memory, but is
			// critical for this application to work as it is designed. For example,
			// this allows background updates to occur while the user access content
			// in a read only manner
			BaseOrm.db.enableWriteAheadLogging();
		}
		return BaseOrm.db;
    }
    
    public static SQLiteDatabase getReadableDatabase(Context context) {
    	return getWritableDatabase(context);
    }
    
    
    /**
     * This is a simple write lock manager that database writers are strongly encouraged to 
     * utilize, but are not forced to. If all updaters that write (SELECT, UPDATE, DELETE)
     * utilize this locking mechanism, it will ensure that no two processes write to the 
     * database at the same time and therefore no write will fail due to a failure to acquire
     * a database lock. Because reading from the database will not impact this (see the use of
     * enableWriteAheadLogging in BaseOrm), only writers should use this class.
     * 
     * A logical lock is taken out on the database when a write transaction is started and it
     * is released when the write transaction ends. Other callers will wait for the logical
     * lock to be released, which  may take some time. 
     * 
     * There are some best practices to follow:
     * 	1. Use asynchronous DB access so the wait for a lock does not cause an ANR message
     * 	2. Do not call any transaction methods from the database, the helper methods here
     * 		will make all necessary calls
     * 	3. Do not acquire a lock for an extended period of time and do not ever fail to
     * 		release a lock under any circumstances
     * 
     * @author Nicholas M. Iodice
     *
     */
    public static class WriteLockManager {
    	private static final String TAG = "WriteLockManager";
    	static boolean isInWriteMode = false;
    	
    	public static void beginWriteTransaction(SQLiteDatabase db) {
    		long milliWait = 0;
    		while (isInWriteMode == true) {
				try {
					milliWait += 100;
					Thread.sleep(100);
				} catch (InterruptedException e) {}
    		}
    		isInWriteMode = true;
    		if (milliWait > 0)
    			Log.w(TAG,  "DB write lock time = " + milliWait + " milliseconds");
    		db.beginTransaction();
    	}
    	
    	public static void setWriteTransactionSuccessfull(SQLiteDatabase db) {
    		db.setTransactionSuccessful();
    	}
    	
    	public static void endWriteTransaction(SQLiteDatabase db) {
    		db.endTransaction();
    		isInWriteMode = false;
    	}
    	
    	
    	
    }
}
