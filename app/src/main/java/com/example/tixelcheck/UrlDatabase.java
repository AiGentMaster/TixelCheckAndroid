package com.example.tixelcheck;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class UrlDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "tixelcheck.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_URLS = "urls";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_URL = "url";
    private static final String COLUMN_FREQUENCY = "frequency";
    private static final String COLUMN_ACTIVE = "active";

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
                COLUMN_ACTIVE + " INTEGER)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_URLS);
        onCreate(db);
    }

    public void addUrl(MonitoredUrl url) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_URL, url.getUrl());
        values.put(COLUMN_FREQUENCY, url.getFrequency());
        values.put(COLUMN_ACTIVE, url.isActive() ? 1 : 0);
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
                urlList.add(new MonitoredUrl(id, url, frequency, active));
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
        db.update(TABLE_URLS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(url.getId())});
        db.close();
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
                urlList.add(new MonitoredUrl(id, url, frequency, true));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return urlList;
    }
}