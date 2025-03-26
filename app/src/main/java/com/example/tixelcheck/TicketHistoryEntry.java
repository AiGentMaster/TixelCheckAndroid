package com.example.tixelcheck;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Represents a historical record of when tickets were found
 */
public class TicketHistoryEntry {
    private long id;
    private long urlId;
    private long timestamp;
    private String note;
    
    public TicketHistoryEntry(long id, long urlId, long timestamp, String note) {
        this.id = id;
        this.urlId = urlId;
        this.timestamp = timestamp;
        this.note = note;
    }
    
    public long getId() {
        return id;
    }
    
    public long getUrlId() {
        return urlId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public String getNote() {
        return note;
    }
    
    /**
     * Gets formatted timestamp string in format "DD/MM/YYYY, HH:MMam/pm"
     */
    public String getFormattedTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy, hh:mma", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}