package com.example.tixelcheck;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.List;

public class UrlDatabase extends SQLiteOpenHelper {
    private static final String TAG = "UrlDatabase";
    private static final String DATABASE_NAME = "tixelcheck.db";
    private static final int DATABASE_VERSION = 3; // Incremented for schema update
    private static final String TABLE_URLS = "urls";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_URL = "url";
    private static final String COLUMN_FREQUENCY = "frequency";
    private static final String COLUMN_ACTIVE = "active";
    private static final String COLUMN_EVENT_NAME = "event_name";
    private static final String COLUMN_EVENT_DATE = "event_date";
    private static final String COLUMN_EVENT_TYPE = "event_type";
    private static final String COLUMN_LAST_CHECKED = "last_checked";
    private static final String COLUMN_CONSECUTIVE_ERRORS = "consecutive_errors";
    private static final String COLUMN_TICKETS_FOUND = "tickets_found";
    
    // Table for storing history of ticket finds
    private static final String TABLE_HISTORY = "ticket_history";
    private static final String COLUMN_HISTORY_ID = "id";
    private static final String COLUMN_HISTORY_URL_ID = "url_id";
    private static final String COLUMN_HISTORY_TIMESTAMP = "timestamp";
    private static final String COLUMN_HISTORY_MESSAGE = "message";

    private static UrlDatabase instance;
    private Context appContext;

    public static synchronized UrlDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new UrlDatabase(context.getApplicationContext());
        }
        return instance;
    }

    private UrlDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.appContext = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create URLs table
        String createUrlsTable = "CREATE TABLE " + TABLE_URLS + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_URL + " TEXT, " +
                COLUMN_FREQUENCY + " INTEGER, " +
                COLUMN_ACTIVE + " INTEGER, " +
                COLUMN_EVENT_NAME + " TEXT, " +
                COLUMN_EVENT_DATE + " TEXT, " +
                COLUMN_EVENT_TYPE + " TEXT, " +
                COLUMN_LAST_CHECKED + " INTEGER, " +
                COLUMN_CONSECUTIVE_ERRORS + " INTEGER, " +
                COLUMN_TICKETS_FOUND + " INTEGER)";
        db.execSQL(createUrlsTable);
        
        // Create history table
        String createHistoryTable = "CREATE TABLE " + TABLE_HISTORY + "(" +
                COLUMN_HISTORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_HISTORY_URL_ID + " INTEGER, " +
                COLUMN_HISTORY_TIMESTAMP + " INTEGER, " +
                COLUMN_HISTORY_MESSAGE + " TEXT, " +
                "FOREIGN KEY(" + COLUMN_HISTORY_URL_ID + ") REFERENCES " + 
                TABLE_URLS + "(" + COLUMN_ID + "))";
        db.execSQL(createHistoryTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add event name and date columns from version 1 to 2
            try {
                db.execSQL("ALTER TABLE " + TABLE_URLS + " ADD COLUMN " + COLUMN_EVENT_NAME + " TEXT DEFAULT ''");
                db.execSQL("ALTER TABLE " + TABLE_URLS + " ADD COLUMN " + COLUMN_EVENT_DATE + " TEXT DEFAULT ''");
                Log.d(TAG, "Database upgraded from version 1 to 2");
            } catch (Exception e) {
                Log.e(TAG, "Error upgrading database from version 1 to 2", e);
            }
        }
        
        if (oldVersion < 3) {
            // Add new columns for version 3
            try {
                db.execSQL("ALTER TABLE " + TABLE_URLS + " ADD COLUMN " + COLUMN_EVENT_TYPE + " TEXT DEFAULT 'other'");
                db.execSQL("ALTER TABLE " + TABLE_URLS + " ADD COLUMN " + COLUMN_LAST_CHECKED + " INTEGER DEFAULT 0");
                db.execSQL("ALTER TABLE " + TABLE_URLS + " ADD COLUMN " + COLUMN_CONSECUTIVE_ERRORS + " INTEGER DEFAULT 0");
                db.execSQL("ALTER TABLE " + TABLE_URLS + " ADD COLUMN " + COLUMN_TICKETS_FOUND + " INTEGER DEFAULT 0");
                
                // Create history table
                String createHistoryTable = "CREATE TABLE " + TABLE_HISTORY + "(" +
                        COLUMN_HISTORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_HISTORY_URL_ID + " INTEGER, " +
                        COLUMN_HISTORY_TIMESTAMP + " INTEGER, " +
                        COLUMN_HISTORY_MESSAGE + " TEXT, " +
                        "FOREIGN KEY(" + COLUMN_HISTORY_URL_ID + ") REFERENCES " + 
                        TABLE_URLS + "(" + COLUMN_ID + "))";
                db.execSQL(createHistoryTable);
                
                Log.d(TAG, "Database upgraded from version 2 to 3");
            } catch (Exception e) {
                Log.e(TAG, "Error upgrading database from version 2 to 3", e);
            }
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
        values.put(COLUMN_EVENT_TYPE, url.getEventType());
        values.put(COLUMN_LAST_CHECKED, url.getLastChecked());
        values.put(COLUMN_CONSECUTIVE_ERRORS, url.getConsecutiveErrors());
        values.put(COLUMN_TICKETS_FOUND, url.isTicketsFound() ? 1 : 0);
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
                
                // Get event details with fallbacks for older database versions
                String eventName = "";
                String eventDate = "";
                String eventType = "other";
                long lastChecked = 0;
                int consecutiveErrors = 0;
                boolean ticketsFound = false;
                
                try {
                    eventName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_NAME));
                    eventDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_DATE));
                    eventType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_TYPE));
                    lastChecked = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_CHECKED));
                    consecutiveErrors = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CONSECUTIVE_ERRORS));
                    ticketsFound = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TICKETS_FOUND)) == 1;
                } catch (Exception e) {
                    Log.w(TAG, "Could not read all columns, may be using older database version", e);
                }
                
                MonitoredUrl monitoredUrl = new MonitoredUrl(id, url, frequency, active, 
                                                      eventName, eventDate, eventType,
                                                      lastChecked, consecutiveErrors, ticketsFound);
                urlList.add(monitoredUrl);
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
        values.put(COLUMN_EVENT_TYPE, url.getEventType());
        values.put(COLUMN_LAST_CHECKED, url.getLastChecked());
        values.put(COLUMN_CONSECUTIVE_ERRORS, url.getConsecutiveErrors());
        values.put(COLUMN_TICKETS_FOUND, url.isTicketsFound() ? 1 : 0);
        db.update(TABLE_URLS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(url.getId())});
        db.close();
    }
    
    public void updateEventDetails(long urlId, String eventName, String eventDate) {
        MonitoredUrl url = getUrlById(urlId);
        if (url != null) {
            String eventType = url.determineEventType(eventName);
            
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_EVENT_NAME, eventName);
            values.put(COLUMN_EVENT_DATE, eventDate);
            values.put(COLUMN_EVENT_TYPE, eventType);
            db.update(TABLE_URLS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(urlId)});
            db.close();
            Log.d(TAG, "Updated event details for URL ID " + urlId + ": " + eventName + ", " + eventDate + ", " + eventType);
        }
    }
    
    public void updateLastChecked(long urlId, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LAST_CHECKED, timestamp);
        db.update(TABLE_URLS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(urlId)});
        db.close();
        
        // Broadcast an update
        Intent updateIntent = new Intent("com.example.tixelcheck.URL_UPDATED");
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(updateIntent);
    }
    
    public void updateConsecutiveErrors(long urlId, int errors) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CONSECUTIVE_ERRORS, errors);
        db.update(TABLE_URLS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(urlId)});
        db.close();
    }
    
    public void updateTicketsFound(long urlId, boolean found) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TICKETS_FOUND, found ? 1 : 0);
        db.update(TABLE_URLS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(urlId)});
        db.close();
        
        // If tickets are found, add to history
        if (found) {
            addHistoryEntry(urlId, System.currentTimeMillis(), "Tickets found!");
        }
        
        // Broadcast an update
        Intent updateIntent = new Intent("com.example.tixelcheck.URL_UPDATED");
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(updateIntent);
    }
    
    public void addHistoryEntry(long urlId, long timestamp, String message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_HISTORY_URL_ID, urlId);
        values.put(COLUMN_HISTORY_TIMESTAMP, timestamp);
        values.put(COLUMN_HISTORY_MESSAGE, message);
        db.insert(TABLE_HISTORY, null, values);
        db.close();
    }
    
    public List<TicketHistoryEntry> getHistoryForUrl(long urlId) {
        List<TicketHistoryEntry> historyList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_HISTORY, null, 
                               COLUMN_HISTORY_URL_ID + "=?", 
                               new String[]{String.valueOf(urlId)}, 
                               null, null, 
                               COLUMN_HISTORY_TIMESTAMP + " DESC");
        
        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_HISTORY_ID));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_HISTORY_TIMESTAMP));
                String message = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HISTORY_MESSAGE));
                
                historyList.add(new TicketHistoryEntry(id, urlId, timestamp, message));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return historyList;
    }

    public void deleteUrl(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        // First delete any history entries for this URL
        db.delete(TABLE_HISTORY, COLUMN_HISTORY_URL_ID + " = ?", new String[]{String.valueOf(id)});
        
        // Then delete the URL itself
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
                
                // Get event details with fallbacks for older database versions
                String eventName = "";
                String eventDate = "";
                String eventType = "other";
                long lastChecked = 0;
                int consecutiveErrors = 0;
                boolean ticketsFound = false;
                
                try {
                    eventName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_NAME));
                    eventDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_DATE));
                    eventType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_TYPE));
                    lastChecked = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_CHECKED));
                    consecutiveErrors = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CONSECUTIVE_ERRORS));
                    ticketsFound = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TICKETS_FOUND)) == 1;
                } catch (Exception e) {
                    Log.w(TAG, "Could not read all columns, may be using older database version", e);
                }
                
                MonitoredUrl monitoredUrl = new MonitoredUrl(id, url, frequency, true, 
                                                      eventName, eventDate, eventType,
                                                      lastChecked, consecutiveErrors, ticketsFound);
                urlList.add(monitoredUrl);
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
            String eventType = "other";
            long lastChecked = 0;
            int consecutiveErrors = 0;
            boolean ticketsFound = false;
            
            try {
                eventName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_NAME));
                eventDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_DATE));
                eventType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_TYPE));
                lastChecked = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_CHECKED));
                consecutiveErrors = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CONSECUTIVE_ERRORS));
                ticketsFound = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TICKETS_FOUND)) == 1;
            } catch (Exception e) {
                Log.w(TAG, "Could not read all columns, may be using older database version", e);
            }
            
            url = new MonitoredUrl(id, urlStr, frequency, active, 
                               eventName, eventDate, eventType,
                               lastChecked, consecutiveErrors, ticketsFound);
        }
        cursor.close();
        db.close();
        return url;
    }
}