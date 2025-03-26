package com.example.tixelcheck;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Dialog for editing an existing monitored URL
 */
public class EditUrlDialog {
    private final Context context;
    private final MonitoredUrl url;
    private final EditUrlDialogListener listener;
    
    public interface EditUrlDialogListener {
        void onUrlEdited(MonitoredUrl url, String newUrl, int newFrequency, boolean isActive, String eventType);
    }
    
    public EditUrlDialog(Context context, MonitoredUrl url, EditUrlDialogListener listener) {
        this.context = context;
        this.url = url;
        this.listener = listener;
    }
    
    public void show() {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_url, null);
        dialog.setContentView(view);
        
        // Initialize UI elements
        final EditText editUrl = view.findViewById(R.id.edit_url);
        final TextView textFrequency = view.findViewById(R.id.text_frequency);
        final SeekBar seekFrequency = view.findViewById(R.id.seek_frequency);
        final Switch switchActive = view.findViewById(R.id.switch_active);
        final RadioGroup radioEventType = view.findViewById(R.id.radio_event_type);
        final RadioButton radioConcert = view.findViewById(R.id.radio_concert);
        final RadioButton radioSports = view.findViewById(R.id.radio_sports);
        final RadioButton radioTheater = view.findViewById(R.id.radio_theater);
        Button buttonSave = view.findViewById(R.id.button_save);
        Button buttonCancel = view.findViewById(R.id.button_cancel);
        
        // Set initial values
        editUrl.setText(url.getUrl());
        seekFrequency.setProgress(getSeekbarProgressFromFrequency(url.getFrequency()));
        textFrequency.setText("Check every " + url.getFrequency() + " minutes");
        switchActive.setChecked(url.isActive());
        
        // Set initial event type selection
        switch (url.getEventType()) {
            case MonitoredUrl.EVENT_TYPE_CONCERT:
                radioConcert.setChecked(true);
                break;
            case MonitoredUrl.EVENT_TYPE_SPORTS:
                radioSports.setChecked(true);
                break;
            case MonitoredUrl.EVENT_TYPE_THEATER:
                radioTheater.setChecked(true);
                break;
            default:
                // If none match, default to concert
                radioConcert.setChecked(true);
        }
        
        // Update the frequency text when seekbar changes
        seekFrequency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int frequency = getFrequencyFromSeekbarProgress(progress);
                textFrequency.setText("Check every " + frequency + " minutes");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Set up save button
        buttonSave.setOnClickListener(v -> {
            String newUrl = editUrl.getText().toString().trim();
            if (newUrl.isEmpty()) {
                Toast.makeText(context, "URL cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Get the selected frequency from the seekbar
            int frequency = getFrequencyFromSeekbarProgress(seekFrequency.getProgress());
            
            // Get active status
            boolean isActive = switchActive.isChecked();
            
            // Get selected event type
            String eventType = MonitoredUrl.EVENT_TYPE_OTHER;
            int selectedId = radioEventType.getCheckedRadioButtonId();
            if (selectedId == R.id.radio_concert) {
                eventType = MonitoredUrl.EVENT_TYPE_CONCERT;
            } else if (selectedId == R.id.radio_sports) {
                eventType = MonitoredUrl.EVENT_TYPE_SPORTS;
            } else if (selectedId == R.id.radio_theater) {
                eventType = MonitoredUrl.EVENT_TYPE_THEATER;
            }
            
            // Notify listener and close dialog
            if (listener != null) {
                listener.onUrlEdited(url, newUrl, frequency, isActive, eventType);
            }
            dialog.dismiss();
        });
        
        // Set up cancel button
        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    /**
     * Converts a frequency value to a seekbar progress value
     * Maps frequencies 1, 2, 5, 10, 15, 30, 60, 120, 180, 360 to progress 0-9
     */
    private int getSeekbarProgressFromFrequency(int frequency) {
        if (frequency <= 1) return 0;
        if (frequency <= 2) return 1;
        if (frequency <= 5) return 2;
        if (frequency <= 10) return 3;
        if (frequency <= 15) return 4;
        if (frequency <= 30) return 5;
        if (frequency <= 60) return 6;
        if (frequency <= 120) return 7;
        if (frequency <= 180) return 8;
        return 9;
    }
    
    /**
     * Converts a seekbar progress value to a frequency value
     */
    private int getFrequencyFromSeekbarProgress(int progress) {
        switch (progress) {
            case 0: return 1;  // 1 minute
            case 1: return 2;  // 2 minutes
            case 2: return 5;  // 5 minutes
            case 3: return 10; // 10 minutes
            case 4: return 15; // 15 minutes
            case 5: return 30; // 30 minutes
            case 6: return 60; // 1 hour
            case 7: return 120; // 2 hours
            case 8: return 180; // 3 hours
            case 9: return 360; // 6 hours
            default: return 15; // Default to 15 minutes
        }
    }
}