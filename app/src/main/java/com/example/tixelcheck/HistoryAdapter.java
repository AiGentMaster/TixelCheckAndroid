package com.example.tixelcheck;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Adapter for displaying ticket history entries in a ListView
 */
public class HistoryAdapter extends ArrayAdapter<TicketHistoryEntry> {
    private final Context context;
    private final List<TicketHistoryEntry> entries;
    
    public HistoryAdapter(Context context, List<TicketHistoryEntry> entries) {
        super(context, R.layout.item_history, entries);
        this.context = context;
        this.entries = entries;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        }
        
        TicketHistoryEntry entry = entries.get(position);
        
        TextView timeText = convertView.findViewById(R.id.text_time);
        TextView messageText = convertView.findViewById(R.id.text_message);
        
        timeText.setText(entry.getFormattedTimestamp());
        messageText.setText(entry.getNote());
        
        return convertView;
    }
}