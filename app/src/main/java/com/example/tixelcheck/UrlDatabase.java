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
    private static final String COLUMN_TICKETS_FOUND = "tickets_found";
    
    // For ticket found history
    private static final String TABLE_HISTORY = "ticket_history";
    private static final String COLUMN_HISTORY_ID = "id";
    private static final String COLUMN_HISTORY_URL_ID = "url_id";
    private static final String COLUMN_HISTORY_TIMESTAMP = "timestamp";
    private static final String COLUMN_HISTORY_NOTE = "note";

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
        // Create main URLs table
        String createTable = "CREATE TABLE " + TABLE_URLS + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_URL + " TEXT, " +
                COLUMN_FREQUENCY + " INTEGER, " +
                COLUMN_ACTIVE + " INTEGER, " +
                COLUMN_EVENT_NAME + " TEXT, " +
                COLUMN_EVENT_DATE + " TEXT, " +
                COLUMN_EVENT_TYPE + " TEXT, " +
                COLUMN_LAST_CHECKED + " INTEGER, " +
                COLUMN_TICKETS_FOUND + " INTEGER)";
        db.execSQL(createTable);
        
        // Create history table
        String createHistoryTable = "CREATE TABLE " + TABLE_HISTORY + "(" +
                COLUMN_HISTORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_HISTORY_URL_ID + " INTEGER, " +
                COLUMN_HISTORY_TIMESTAMP + " INTEGER, " +
                COLUMN_HISTORY_NOTE + " TEXT)";
        db.execSQL(createHistoryTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add columns for event details (v1 -> v2)
            try {
                db.execSQL("ALTER TABLE " + TABLE_URLS + " ADD COLUMN " + COLUMN_EVENT_NAME + " TEXT DEFAULT ''");
                db.execSQL("ALTER TABLE " + TABLE_URLS + " ADD COLUMN " + COLUMN_EVENT_DATE + " TEXT DEFAULT ''");
                Log.d(TAG, "Database upgraded from version 1 to 2");
            } catch (Exception e) {
                Log.e(TAG, "Error upgrading database from v1 to v2", e);
            }
        }
        
        if (oldVersion < 3) {
            // Add new columns for v3 (event type, last checked, tickets found)
            try {
                db.execSQL("ALTER TABLE " + TABLE_URLS + " ADD COLUMN " + COLUMN_EVENT_TYPE + " TEXT DEFAULT 'other'");
                db.execSQL("ALTER TABLE " + TABLE_URLS + " ADD COLUMN " + COLUMN_LAST_CHECKED + " INTEGER DEFAULT 0");
                db.execSQL("ALTER TABLE " + TABLE_URLS + " ADD COLUMN " + COLUMN_TICKETS_FOUND + " INTEGER DEFAULT 0");
                
                // Create history table
                String createHistoryTable = "CREATE TABLE " + TABLE_HISTORY + "(" +
                        COLUMN_HISTORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_HISTORY_URL_ID + " INTEGER, " +
                        COLUMN_HISTORY_TIMESTAMP + " INTEGER, " +
                        COLUMN_HISTORY_NOTE + " TEXT)";
                db.execSQL(createHistoryTable);
                
                Log.d(TAG, "Database upgraded from version 2 to 3");
            } catch (Exception e) {
                Log.e(TAG, "Error upgrading database from v2 to v3", e);
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
        values.put(COLUMN_LAST_CHECKED, url.getLastCheckedTimestamp());
        values.put(COLUMN_TICKETS_FOUND, url.hasTicketsFound() ? 1 : 0);
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
                
                // Get event details
                String eventName = "";
                String eventDate = "";
                String eventType = MonitoredUrl.EVENT_TYPE_OTHER;
                long lastChecked = 0;
                boolean ticketsFound = false;
                
                try {
                    eventName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_NAME));
                    eventDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_DATE));
                    
                    // Try to get new columns
                    try {
                        eventType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_TYPE));
                        lastChecked = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_CHECKED));
                        ticketsFound = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TICKETS_FOUND)) == 1;
                    } catch (Exception e) {
                        // Use default values if columns don't exist yet
                        Log.w(TAG, "Could not read new columns, using defaults", e);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Could not read event details columns", e);
                }
                
                // Auto-detect event type if not set
                if (eventType == null || eventType.isEmpty()) {
                    MonitoredUrl tempUrl = new MonitoredUrl(id, url, frequency, active, eventName, eventDate);
                    tempUrl.detectEventType();
                    eventType = tempUrl.getEventType();
                }
                
                urlList.add(new MonitoredUrl(id, url, frequency, active, 
                    eventName, eventDate, eventType, lastChecked, ticketsFound));
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
        values.put(COLUMN_LAST_CHECKED, url.getLastCheckedTimestamp());
        values.put(COLUMN_TICKETS_FOUND, url.hasTicketsFound() ? 1 : 0);
        db.update(TABLE_URLS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(url.getId())});
        db.close();
    }
    
    public void updateEventDetails(long urlId, String eventName, String eventDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EVENT_NAME, eventName);
        values.put(COLUMN_EVENT_DATE, eventDate);
        
        // Detect and update event type
        MonitoredUrl tempUrl = new MonitoredUrl(urlId, "", 0, false, eventName, eventDate);
        tempUrl.detectEventType();
        values.put(COLUMN_EVENT_TYPE, tempUrl.getEventType());
        
        db.update(TABLE_URLS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(urlId)});
        db.close();
        Log.d(TAG, "Updated event details for URL ID " + urlId + ": " + eventName + ", " + eventDate);
    }
    
    public void updateLastChecked(long urlId, long timestamp, boolean ticketsFound) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LAST_CHECKED, timestamp);
        values.put(COLUMN_TICKETS_FOUND, ticketsFound ? 1 : 0);
        db.update(TABLE_URLS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(urlId)});
        db.close();
        Log.d(TAG, "Updated last checked for URL ID " + urlId + ": " + timestamp + ", tickets found: " + ticketsFound);
    }
    
    public void addTicketHistory(long urlId, long timestamp, String note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_HISTORY_URL_ID, urlId);
        values.put(COLUMN_HISTORY_TIMESTAMP, timestamp);
        values.put(COLUMN_HISTORY_NOTE, note);
        db.insert(TABLE_HISTORY, null, values);
        db.close();
        Log.d(TAG, "Added ticket history for URL ID " + urlId + ": " + note);
    }
    
    public List<TicketHistoryEntry> getTicketHistory(long urlId) {
        List<TicketHistoryEntry> historyList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_HISTORY + 
                         " WHERE " + COLUMN_HISTORY_URL_ID + " = ?" +
                         " ORDER BY " + COLUMN_HISTORY_TIMESTAMP + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(urlId)});

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_HISTORY_ID));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_HISTORY_TIMESTAMP));
                String note = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HISTORY_NOTE));
                
                historyList.add(new TicketHistoryEntry(id, urlId, timestamp, note));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return historyList;
    }

    public void deleteUrl(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_URLS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        
        // Also delete history entries for this URL
        db.delete(TABLE_HISTORY, COLUMN_HISTORY_URL_ID + " = ?", new String[]{String.valueOf(id)});
        
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
                
                // Get event details
                String eventName = "";
                String eventDate = "";
                String eventType = MonitoredUrl.EVENT_TYPE_OTHER;
                long lastChecked = 0;
                boolean ticketsFound = false;
                
                try {
                    eventName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_NAME));
                    eventDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_DATE));
                    
                    // Try to get new columns
                    try {
                        eventType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_TYPE));
                        lastChecked = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_CHECKED));
                        ticketsFound = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TICKETS_FOUND)) == 1;
                    } catch (Exception e) {
                        // Use default values if columns don't exist yet
                        Log.w(TAG, "Could not read new columns for active URLs, using defaults");
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Could not read event details columns", e);
                }
                
                urlList.add(new MonitoredUrl(id, url, frequency, true, 
                    eventName, eventDate, eventType, lastChecked, ticketsFound));
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
            String eventType = MonitoredUrl.EVENT_TYPE_OTHER;
            long lastChecked = 0;
            boolean ticketsFound = false;
            
            try {
                eventName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_NAME));
                eventDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_DATE));
                
                // Try to get new columns
                try {
                    eventType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_TYPE));
                    lastChecked = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_CHECKED));
                    ticketsFound = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TICKETS_FOUND)) == 1;
                } catch (Exception e) {
                    // Use default values if columns don't exist yet
                    Log.w(TAG, "Could not read new columns for URL ID " + id + ", using defaults");
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not read event details columns", e);
            }
            
            url = new MonitoredUrl(id, urlStr, frequency, active, 
                eventName, eventDate, eventType, lastChecked, ticketsFound);
        }
        cursor.close();
        db.close();
        return url;
    }
}