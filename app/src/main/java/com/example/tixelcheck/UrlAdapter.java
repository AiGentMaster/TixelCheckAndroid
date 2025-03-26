package com.example.tixelcheck;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
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
        
        // Set last checked info
        holder.textLastChecked.setText(url.getLastCheckedFormatted());
        
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
        
        // Set event type icon
        switch (url.getEventType()) {
            case "concert":
                holder.imageEventType.setImageResource(R.drawable.ic_concert);
                break;
            case "sports":
                holder.imageEventType.setImageResource(R.drawable.ic_sports);
                break;
            case "theater":
                holder.imageEventType.setImageResource(R.drawable.ic_theater);
                break;
            default:
                holder.imageEventType.setImageResource(R.drawable.ic_other);
                break;
        }
        
        // Set status icon and card color
        if (url.isTicketsFound()) {
            // Green for tickets found
            holder.imageStatus.setImageResource(R.drawable.ic_tickets_found);
            setCardColor(holder.cardView, "#4CAF50", 0.2f);
        } else if (url.isActive()) {
            // Blue for active
            holder.imageStatus.setImageResource(R.drawable.ic_active);
            setCardColor(holder.cardView, "#2196F3", 0.1f);
        } else {
            // Grey for inactive
            holder.imageStatus.setImageResource(R.drawable.ic_inactive);
            setCardColor(holder.cardView, "#9E9E9E", 0.1f);
        }
        
        // Set up the active switch
        holder.switchActive.setChecked(url.isActive());
        holder.switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            url.setActive(isChecked);
            UrlDatabase.getInstance(context).updateUrl(url);
            
            // Update alarm based on active state
            if (isChecked) {
                TicketCheckerAlarm.setAlarm(context, url);
                Toast.makeText(context, "Monitoring enabled", Toast.LENGTH_SHORT).show();
            } else {
                TicketCheckerAlarm.cancelAlarm(context, url.getId());
                Toast.makeText(context, "Monitoring disabled", Toast.LENGTH_SHORT).show();
            }
            
            // Update UI for status change
            notifyItemChanged(position);
            
            // Broadcast URL update
            Intent updateIntent = new Intent("com.example.tixelcheck.URL_UPDATED");
            LocalBroadcastManager.getInstance(context).sendBroadcast(updateIntent);
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
            
            Toast.makeText(context, "URL removed", Toast.LENGTH_SHORT).show();
            
            // Broadcast URL update
            Intent updateIntent = new Intent("com.example.tixelcheck.URL_UPDATED");
            LocalBroadcastManager.getInstance(context).sendBroadcast(updateIntent);
        });
        
        // Set up URL editing with normal click (opens EditUrlDialog)
        holder.cardView.setOnClickListener(v -> {
            showEditUrlDialog(position);
        });
        
        // Add long press to edit event details
        holder.cardView.setOnLongClickListener(v -> {
            showEventDetailsDialog(position);
            return true;
        });
        
        // Make event details text clickable to edit
        holder.textEventDetails.setOnClickListener(v -> {
            showEventDetailsDialog(position);
        });
        
        // Set up history button
        holder.buttonHistory.setOnClickListener(v -> {
            showHistoryDialog(position);
        });
    }
    
    /**
     * Set card background color based on status with dark mode support
     */
    private void setCardColor(CardView cardView, String colorHex, float alpha) {
        int nightMode = context.getResources().getConfiguration().uiMode & 
                        Configuration.UI_MODE_NIGHT_MASK;
        
        if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
            // Dark mode is active - use darker colors
            int color = Color.parseColor(colorHex);
            // Darken the color for dark mode
            int r = Math.max(0, Color.red(color) - 40);
            int g = Math.max(0, Color.green(color) - 40);
            int b = Math.max(0, Color.blue(color) - 40);
            color = Color.argb((int)(alpha * 255), r, g, b);
            cardView.setCardBackgroundColor(color);
        } else {
            // Light mode - use regular colors
            int color = Color.parseColor(colorHex);
            color = Color.argb((int)(alpha * 255), Color.red(color), Color.green(color), Color.blue(color));
            cardView.setCardBackgroundColor(color);
        }
    }
    
    /**
     * Shows a dialog with ticket find history
     */
    private void showHistoryDialog(int position) {
        MonitoredUrl url = urlList.get(position);
        HistoryDialog dialog = new HistoryDialog(context, url);
        dialog.show();
    }
    
    /**
     * Shows a dialog to edit URL settings
     */
    private void showEditUrlDialog(int position) {
        MonitoredUrl url = urlList.get(position);
        
        EditUrlDialog dialog = new EditUrlDialog(context, url, (originalUrl, newUrl, newFrequency, isActive) -> {
            // Cancel old alarm if URL or frequency changed
            if (!originalUrl.getUrl().equals(newUrl) || originalUrl.getFrequency() != newFrequency || !isActive) {
                TicketCheckerAlarm.cancelAlarm(context, originalUrl.getId());
            }
            
            // Update URL properties
            MonitoredUrl updatedUrl = new MonitoredUrl(
                originalUrl.getId(),
                newUrl,
                newFrequency,
                isActive,
                originalUrl.getEventName(),
                originalUrl.getEventDate(),
                originalUrl.getEventType(),
                originalUrl.getLastChecked(),
                originalUrl.getConsecutiveErrors(),
                originalUrl.isTicketsFound()
            );
            
            // Check if anything actually changed
            boolean changed = !originalUrl.getUrl().equals(newUrl) ||
                             originalUrl.getFrequency() != newFrequency ||
                             originalUrl.isActive() != isActive;
            
            if (changed) {
                // Update database and UI
                UrlDatabase.getInstance(context).updateUrl(updatedUrl);
                urlList.set(position, updatedUrl);
                notifyItemChanged(position);
                
                // Set new alarm if active
                if (isActive) {
                    TicketCheckerAlarm.setAlarm(context, updatedUrl);
                }
                
                // Broadcast URL update
                Intent updateIntent = new Intent("com.example.tixelcheck.URL_UPDATED");
                LocalBroadcastManager.getInstance(context).sendBroadcast(updateIntent);
                
                Toast.makeText(context, "URL settings updated", Toast.LENGTH_SHORT).show();
            }
        });
        
        dialog.show();
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
            
            // Send broadcast to notify MainActivity to refresh the URL list
            Intent refreshIntent = new Intent("com.example.tixelcheck.EVENT_DETAILS_UPDATED");
            LocalBroadcastManager.getInstance(context).sendBroadcast(refreshIntent);
            
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
        CardView cardView;
        TextView textUrl;
        TextView textFrequency;
        TextView textEventDetails;
        TextView textLastChecked;
        ImageView imageEventType;
        ImageView imageStatus;
        Switch switchActive;
        Button buttonDelete;
        Button buttonHistory;

        public UrlViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            textUrl = itemView.findViewById(R.id.text_url);
            textFrequency = itemView.findViewById(R.id.text_frequency);
            textEventDetails = itemView.findViewById(R.id.text_event_details);
            textLastChecked = itemView.findViewById(R.id.text_last_checked);
            imageEventType = itemView.findViewById(R.id.image_event_type);
            imageStatus = itemView.findViewById(R.id.image_status);
            switchActive = itemView.findViewById(R.id.switch_active);
            buttonDelete = itemView.findViewById(R.id.button_delete);
            buttonHistory = itemView.findViewById(R.id.button_history);
        }
    }
}