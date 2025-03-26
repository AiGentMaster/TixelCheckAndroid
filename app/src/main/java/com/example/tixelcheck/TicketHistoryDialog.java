package com.example.tixelcheck;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Dialog to show ticket availability history for a URL
 */
public class TicketHistoryDialog {
    private final Context context;
    private final MonitoredUrl url;
    
    public TicketHistoryDialog(Context context, MonitoredUrl url) {
        this.context = context;
        this.url = url;
    }
    
    public void show() {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_ticket_history, null);
        dialog.setContentView(view);
        
        // Initialize UI elements
        TextView titleText = view.findViewById(R.id.text_dialog_title);
        TextView eventDetailsText = view.findViewById(R.id.text_event_details);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_history);
        Button closeButton = view.findViewById(R.id.button_close);
        
        // Set dialog title
        titleText.setText("Ticket History for " + url.getUrl());
        
        // Set event details
        if (url.hasEventDetails()) {
            String eventDetails = url.getEventName();
            if (!url.getEventDate().isEmpty()) {
                eventDetails += " on " + url.getEventDate();
            }
            eventDetailsText.setText(eventDetails);
            eventDetailsText.setVisibility(View.VISIBLE);
        } else {
            eventDetailsText.setVisibility(View.GONE);
        }
        
        // Get history entries from database
        List<TicketHistoryEntry> historyEntries = UrlDatabase.getInstance(context).getTicketHistory(url.getId());
        
        // Set up recycler view
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        
        if (historyEntries.isEmpty()) {
            // Show message if no history entries
            TextView emptyText = new TextView(context);
            emptyText.setText("No ticket history found. Tickets haven't been detected yet.");
            emptyText.setPadding(16, 16, 16, 16);
            ((ViewGroup) recyclerView.getParent()).addView(emptyText);
            recyclerView.setVisibility(View.GONE);
        } else {
            // Set adapter with history entries
            HistoryAdapter adapter = new HistoryAdapter(historyEntries);
            recyclerView.setAdapter(adapter);
        }
        
        // Set up close button
        closeButton.setOnClickListener(v -> dialog.dismiss());
        
        // Show dialog
        dialog.show();
    }
    
    /**
     * Adapter for history entries
     */
    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private final List<TicketHistoryEntry> historyEntries;
        
        public HistoryAdapter(List<TicketHistoryEntry> historyEntries) {
            this.historyEntries = historyEntries;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history_entry, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TicketHistoryEntry entry = historyEntries.get(position);
            holder.timestampText.setText(entry.getFormattedTimestamp());
            holder.noteText.setText(entry.getNote());
        }
        
        @Override
        public int getItemCount() {
            return historyEntries.size();
        }
        
        public class ViewHolder extends RecyclerView.ViewHolder {
            public final TextView timestampText;
            public final TextView noteText;
            
            public ViewHolder(View view) {
                super(view);
                timestampText = view.findViewById(R.id.text_timestamp);
                noteText = view.findViewById(R.id.text_note);
            }
        }
    }
}