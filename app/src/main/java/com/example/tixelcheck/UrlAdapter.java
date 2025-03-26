package com.example.tixelcheck;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class UrlAdapter extends RecyclerView.Adapter<UrlAdapter.UrlViewHolder> {
    private List<MonitoredUrl> urlList;
    private Context context;

    public UrlAdapter(List<MonitoredUrl> urlList, Context context) {
        this.urlList = urlList;
        this.context = context;
    }

    @NonNull
    @Override
    public UrlViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.url_item, parent, false);
        return new UrlViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UrlViewHolder holder, int position) {
        MonitoredUrl url = urlList.get(position);
        
        // Set the URL text
        holder.textUrl.setText(url.getUrl());
        
        // Set frequency text
        holder.textFrequency.setText("Check every " + url.getFrequency() + " minutes");
        
        // Set event details if available
        if (url.hasEventDetails()) {
            holder.textEventDetails.setVisibility(View.VISIBLE);
            String eventText = url.getEventName();
            if (!url.getEventDate().isEmpty()) {
                eventText += " on " + url.getEventDate();
            }
            holder.textEventDetails.setText(eventText);
        } else {
            holder.textEventDetails.setVisibility(View.GONE);
        }
        
        // Set up the active switch
        holder.switchActive.setChecked(url.isActive());
        holder.switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            url.setActive(isChecked);
            UrlDatabase.getInstance(context).updateUrl(url);
            
            // Update alarm based on active state
            if (isChecked) {
                TicketCheckerAlarm.setAlarm(context, url);
            } else {
                TicketCheckerAlarm.cancelAlarm(context, url.getId());
            }
        });

        // Set up delete button
        holder.buttonDelete.setOnClickListener(v -> {
            // Cancel alarm before deleting
            TicketCheckerAlarm.cancelAlarm(context, url.getId());
            
            // Delete from database and update UI
            UrlDatabase.getInstance(context).deleteUrl(url.getId());
            urlList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, urlList.size());
        });
        
        // Make the URL clickable to open in a browser
        holder.itemView.setOnClickListener(v -> {
            try {
                String urlStr = url.getUrl();
                if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
                    urlStr = "https://" + urlStr;
                }
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlStr));
                context.startActivity(browserIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
        // Add long press to edit event details
        holder.itemView.setOnLongClickListener(v -> {
            showEventDetailsDialog(position);
            return true;
        });
        
        // Make event details text clickable to edit
        holder.textEventDetails.setOnClickListener(v -> {
            showEventDetailsDialog(position);
        });
    }

    /**
     * Shows a dialog to edit event details
     */
    private void showEventDetailsDialog(int position) {
        MonitoredUrl url = urlList.get(position);
        
        AddEventDetailsDialog dialog = new AddEventDetailsDialog(context, url, (eventName, eventDate) -> {
            // Update the URL with new event details
            url.setEventName(eventName);
            url.setEventDate(eventDate);
            
            // Update the database
            UrlDatabase.getInstance(context).updateEventDetails(url.getId(), eventName, eventDate);
            
            // Update the UI
            notifyItemChanged(position);
            
            Toast.makeText(context, "Event details updated", Toast.LENGTH_SHORT).show();
        });
        
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return urlList.size();
    }

    static class UrlViewHolder extends RecyclerView.ViewHolder {
        TextView textUrl;
        TextView textFrequency;
        TextView textEventDetails;
        Switch switchActive;
        Button buttonDelete;

        public UrlViewHolder(@NonNull View itemView) {
            super(itemView);
            textUrl = itemView.findViewById(R.id.text_url);
            textFrequency = itemView.findViewById(R.id.text_frequency);
            textEventDetails = itemView.findViewById(R.id.text_event_details);
            switchActive = itemView.findViewById(R.id.switch_active);
            buttonDelete = itemView.findViewById(R.id.button_delete);
        }
    }
}