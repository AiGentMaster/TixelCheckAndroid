package com.example.tixelcheck;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Represents a single history entry for when tickets were found
 */
public class TicketHistoryEntry {
    private long id;
    private long urlId;
    private long timestamp;
    private String message;
    
    public TicketHistoryEntry(long id, long urlId, long timestamp, String message) {
        this.id = id;
        this.urlId = urlId;
        this.timestamp = timestamp;
        this.message = message;
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
    
    public String getMessage() {
        return message;
    }
    
    /**
     * Returns a formatted date and time string
     */
    public String getFormattedDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy, h:mm a", Locale.getDefault());
        return dateFormat.format(new Date(timestamp));
    }
}