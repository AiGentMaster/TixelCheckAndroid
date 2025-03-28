package com.example.tixelcheck;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for displaying ticket history for a URL
 */
public class HistoryDialog {
    private final Context context;
    private final MonitoredUrl url;
    private final UrlDatabase urlDatabase;
    private boolean showAllUrls = false;
    
    /**
     * Constructor for showing history for a specific URL
     */
    public HistoryDialog(Context context, MonitoredUrl url) {
        this.context = context;
        this.url = url;
        this.urlDatabase = UrlDatabase.getInstance(context);
        this.showAllUrls = false;
    }
    
    /**
     * Constructor for showing history for all URLs
     */
    public HistoryDialog(Context context, UrlDatabase urlDatabase) {
        this.context = context;
        this.url = null;
        this.urlDatabase = urlDatabase;
        this.showAllUrls = true;
    }
    
    public void show() {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_history, null);
        dialog.setContentView(view);
        
        // Initialize UI elements
        TextView titleText = view.findViewById(R.id.text_dialog_title);
        TextView eventNameText = view.findViewById(R.id.text_event_name);
        ListView historyList = view.findViewById(R.id.list_history);
        TextView emptyText = view.findViewById(R.id.text_empty_history);
        Button closeButton = view.findViewById(R.id.button_close);
        
        // Set dialog title and event name based on mode
        if (showAllUrls) {
            titleText.setText("All Ticket History");
            eventNameText.setVisibility(View.GONE);
        } else {
            titleText.setText("Ticket History");
            // Set event name
            if (url != null && url.hasEventDetails()) {
                eventNameText.setText(url.getEventName());
                eventNameText.setVisibility(View.VISIBLE);
            } else {
                eventNameText.setVisibility(View.GONE);
            }
        }
        
        // Load history entries
        List<TicketHistoryEntry> historyEntries;
        if (showAllUrls) {
            // Get history for all URLs
            historyEntries = new ArrayList<>();
            List<MonitoredUrl> allUrls = urlDatabase.getAllUrls();
            for (MonitoredUrl monitoredUrl : allUrls) {
                historyEntries.addAll(urlDatabase.getTicketHistory(monitoredUrl.getId()));
            }
            
            // Sort by timestamp (most recent first)
            historyEntries.sort((entry1, entry2) -> 
                    Long.compare(entry2.getTimestamp(), entry1.getTimestamp()));
        } else {
            // Get history for specific URL
            historyEntries = urlDatabase.getTicketHistory(url.getId());
        }
        
        if (historyEntries.isEmpty()) {
            historyList.setVisibility(View.GONE);
            emptyText.setVisibility(View.VISIBLE);
        } else {
            historyList.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);
            
            // Setup adapter
            HistoryAdapter adapter = new HistoryAdapter(context, historyEntries);
            historyList.setAdapter(adapter);
        }
        
        // Set up close button
        closeButton.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
}