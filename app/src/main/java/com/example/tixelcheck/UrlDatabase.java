package com.example.tixelcheck;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class UrlDatabase extends SQLiteOpenHelper {
    private static final String TAG = "UrlDatabase";
    private static final String DATABASE_NAME = "tixelcheck.db";
    private static final int DATABASE_VERSION = 2; // Incremented for schema update
    private static final String TABLE_URLS = "urls";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_URL = "url";
    private static final String COLUMN_FREQUENCY = "frequency";
    private static final String COLUMN_ACTIVE = "active";
    private static final String COLUMN_EVENT_NAME = "event_name";
    private static final String COLUMN_EVENT_DATE = "event_date";

    private static UrlDatabase instance;

    public static synchronized UrlDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new UrlDatabase(context.getApplicationContext());
        }
        return instance;
    }

    private UrlDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_URLS + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_URL + " TEXT, " +
                COLUMN_FREQUENCY + " INTEGER, " +
                COLUMN_ACTIVE + " INTEGER, " +
                COLUMN_EVENT_NAME + " TEXT, " +
                COLUMN_EVENT_DATE + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) {
            // Add the new columns for event details
            try {
                db.execSQL("ALTER TABLE " + TABLE_URLS + " ADD COLUMN " + COLUMN_EVENT_NAME + " TEXT DEFAULT ''");
                db.execSQL("ALTER TABLE " + TABLE_URLS + " ADD COLUMN " + COLUMN_EVENT_DATE + " TEXT DEFAULT ''");
                Log.d(TAG, "Database upgraded successfully from version " + oldVersion + " to " + newVersion);
            } catch (Exception e) {
                Log.e(TAG, "Error upgrading database", e);
            }
        } else {
            // Fallback to recreating the database
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_URLS);
            onCreate(db);
        }
    }

    public void addUrl(MonitoredUrl url) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_URL, url.getUrl());
        values.put(COLUMN_FREQUENCY, url.getFrequency());
        values.put(COLUMN_ACTIVE, url.isActive() ? 1 : 0);
        values.put(COLUMN_EVENT_NAME, url.getEventName());
        values.put(COLUMN_EVENT_DATE, url.getEventDate());
        db.insert(TABLE_URLS, null, values);
        db.close();
    }

    public List<MonitoredUrl> getAllUrls() {
        List<MonitoredUrl> urlList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_URLS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String url = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_URL));
                int frequency = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FREQUENCY));
                boolean active = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACTIVE)) == 1;
                
                // Get event details (with fallbacks for older database versions)
                String eventName = "";
                String eventDate = "";
                try {
                    eventName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_NAME));
                    eventDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_DATE));
                } catch (Exception e) {
                    Log.w(TAG, "Could not read event details columns", e);
                }
                
                urlList.add(new MonitoredUrl(id, url, frequency, active, eventName, eventDate));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return urlList;
    }

    public void updateUrl(MonitoredUrl url) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_URL, url.getUrl());
        values.put(COLUMN_FREQUENCY, url.getFrequency());
        values.put(COLUMN_ACTIVE, url.isActive() ? 1 : 0);
        values.put(COLUMN_EVENT_NAME, url.getEventName());
        values.put(COLUMN_EVENT_DATE, url.getEventDate());
        db.update(TABLE_URLS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(url.getId())});
        db.close();
    }
    
    public void updateEventDetails(long urlId, String eventName, String eventDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EVENT_NAME, eventName);
        values.put(COLUMN_EVENT_DATE, eventDate);
        db.update(TABLE_URLS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(urlId)});
        db.close();
        Log.d(TAG, "Updated event details for URL ID " + urlId + ": " + eventName + ", " + eventDate);
    }

    public void deleteUrl(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_URLS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public List<MonitoredUrl> getActiveUrls() {
        List<MonitoredUrl> urlList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_URLS + " WHERE " + COLUMN_ACTIVE + " = 1";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String url = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_URL));
                int frequency = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FREQUENCY));
                
                // Get event details (with fallbacks for older database versions)
                String eventName = "";
                String eventDate = "";
                try {
                    eventName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_NAME));
                    eventDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_DATE));
                } catch (Exception e) {
                    Log.w(TAG, "Could not read event details columns", e);
                }
                
                urlList.add(new MonitoredUrl(id, url, frequency, true, eventName, eventDate));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return urlList;
    }
    
    public MonitoredUrl getUrlById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_URLS, null, COLUMN_ID + "=?", 
                new String[]{String.valueOf(id)}, null, null, null);
        
        MonitoredUrl url = null;
        if (cursor.moveToFirst()) {
            String urlStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_URL));
            int frequency = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FREQUENCY));
            boolean active = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACTIVE)) == 1;
            
            // Get event details
            String eventName = "";
            String eventDate = "";
            try {
                eventName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_NAME));
                eventDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_DATE));
            } catch (Exception e) {
                Log.w(TAG, "Could not read event details columns", e);
            }
            
            url = new MonitoredUrl(id, urlStr, frequency, active, eventName, eventDate);
        }
        cursor.close();
        db.close();
        return url;
    }
}