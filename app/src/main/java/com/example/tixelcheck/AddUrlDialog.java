package com.example.tixelcheck;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

public class AddUrlDialog extends Dialog {
    private EditText editTextUrl;
    private RadioGroup radioGroupFrequency;
    private Button buttonAdd;
    private UrlDialogListener listener;

    public AddUrlDialog(Context context, UrlDialogListener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_add_url);
        setTitle("Enter a new Tixel URL to Monitor");

        editTextUrl = findViewById(R.id.edit_text_url);
        radioGroupFrequency = findViewById(R.id.radio_group_frequency);
        buttonAdd = findViewById(R.id.button_add);

        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = editTextUrl.getText().toString().trim();
                if (url.isEmpty()) {
                    Toast.makeText(getContext(), "Please enter a URL", Toast.LENGTH_SHORT).show();
                    return;
                }

                int frequencyMinutes;
                int selectedId = radioGroupFrequency.getCheckedRadioButtonId();
                if (selectedId == R.id.radio_1min) {
                    frequencyMinutes = 1;
                } else if (selectedId == R.id.radio_2min) {
                    frequencyMinutes = 2;
                } else if (selectedId == R.id.radio_5min) {
                    frequencyMinutes = 5;
                } else if (selectedId == R.id.radio_10min) {
                    frequencyMinutes = 10;
                } else if (selectedId == R.id.radio_30min) {
                    frequencyMinutes = 30;
                } else if (selectedId == R.id.radio_60min) {
                    frequencyMinutes = 60;
                } else {
                    frequencyMinutes = 2; // Default to 2 minutes
                }

                MonitoredUrl monitoredUrl = new MonitoredUrl(0, url, frequencyMinutes, true);
                listener.onUrlAdded(monitoredUrl);
                
                // Immediately set alarm for this URL
                TicketCheckerAlarm.setAlarm(getContext(), monitoredUrl);
                
                dismiss();
            }
        });
    }

    public interface UrlDialogListener {
        void onUrlAdded(MonitoredUrl url);
    }
}