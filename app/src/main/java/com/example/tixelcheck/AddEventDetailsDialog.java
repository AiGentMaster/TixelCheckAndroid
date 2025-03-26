package com.example.tixelcheck;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class AddEventDetailsDialog {
    private final Context context;
    private final MonitoredUrl url;
    private final EventDetailsListener listener;
    
    public interface EventDetailsListener {
        void onEventDetailsUpdated(String eventName, String eventDate);
    }
    
    public AddEventDetailsDialog(Context context, MonitoredUrl url, EventDetailsListener listener) {
        this.context = context;
        this.url = url;
        this.listener = listener;
    }
    
    public void show() {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // Inflate the dialog layout
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_event_details, null);
        dialog.setContentView(view);
        
        // Initialize UI elements
        EditText editEventName = view.findViewById(R.id.edit_event_name);
        EditText editEventDate = view.findViewById(R.id.edit_event_date);
        Button buttonSave = view.findViewById(R.id.button_save);
        Button buttonCancel = view.findViewById(R.id.button_cancel);
        TextView textTitle = view.findViewById(R.id.text_dialog_title);
        
        // Set dialog title
        textTitle.setText("Event Details for " + trimUrl(url.getUrl()));
        
        // Pre-fill with existing details if available
        if (url.hasEventDetails()) {
            editEventName.setText(url.getEventName());
            editEventDate.setText(url.getEventDate());
        }
        
        // Set up the save button
        buttonSave.setOnClickListener(v -> {
            String eventName = editEventName.getText().toString().trim();
            String eventDate = editEventDate.getText().toString().trim();
            
            // Notify the listener
            if (listener != null) {
                listener.onEventDetailsUpdated(eventName, eventDate);
            }
            
            dialog.dismiss();
        });
        
        // Set up the cancel button
        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        
        // Show the dialog
        dialog.show();
    }
    
    private String trimUrl(String url) {
        // Trim URL for display purposes
        String trimmed = url;
        if (trimmed.startsWith("https://")) {
            trimmed = trimmed.substring(8);
        } else if (trimmed.startsWith("http://")) {
            trimmed = trimmed.substring(7);
        }
        
        // Limit the length
        if (trimmed.length() > 30) {
            trimmed = trimmed.substring(0, 27) + "...";
        }
        
        return trimmed;
    }
}