package com.example.tixelcheck;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
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
import androidx.core.content.ContextCompat;
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
    
    /**
     * Updates the URL list and refreshes the adapter
     * 
     * @param newUrlList The new list of URLs to display
     */
    public void updateUrls(List<MonitoredUrl> newUrlList) {
        this.urlList.clear();
        this.urlList.addAll(newUrlList);
        notifyDataSetChanged();
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
        CardView cardView = (CardView) holder.itemView;
        Resources resources = context.getResources();
        
        // Set the URL text
        holder.textUrl.setText(url.getUrl());
        
        // Set frequency text
        holder.textFrequency.setText("Check every " + url.getFrequency() + " minutes");
        
        // Set last checked text
        holder.textLastChecked.setText("Last checked: " + url.getFormattedLastChecked());
        
        // Set event type icon
        switch (url.getEventType()) {
            case MonitoredUrl.EVENT_TYPE_CONCERT:
                holder.imageEventType.setImageResource(R.drawable.ic_concert);
                break;
            case MonitoredUrl.EVENT_TYPE_SPORTS:
                holder.imageEventType.setImageResource(R.drawable.ic_sports);
                break;
            case MonitoredUrl.EVENT_TYPE_THEATER:
                holder.imageEventType.setImageResource(R.drawable.ic_theater);
                break;
            default:
                holder.imageEventType.setImageResource(R.drawable.ic_other);
        }
        
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
        
        // Set card color based on status
        int textColor;
        if (url.hasTicketsFound()) {
            // Green for tickets found
            cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.ticketsFoundGreen));
            textColor = ContextCompat.getColor(context, android.R.color.white);
        } else if (url.isActive()) {
            // Blue for active
            cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.activeBlue));
            textColor = ContextCompat.getColor(context, android.R.color.white);
        } else {
            // Grey for inactive
            cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.inactiveGrey));
            textColor = ContextCompat.getColor(context, android.R.color.white);
        }
        
        // Apply text color
        holder.textUrl.setTextColor(textColor);
        holder.textFrequency.setTextColor(textColor);
        holder.textLastChecked.setTextColor(textColor);
        holder.textEventDetails.setTextColor(textColor);
        
        // Set up the active switch
        holder.switchActive.setChecked(url.isActive());
        holder.switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            url.setActive(isChecked);
            UrlDatabase.getInstance(context).updateUrl(url);
            
            // Update the card color
            notifyItemChanged(position);
            
            // Update alarm based on active state
            if (isChecked) {
                TicketCheckerAlarm.setAlarm(context, url);
                Toast.makeText(context, "Monitoring enabled", Toast.LENGTH_SHORT).show();
            } else {
                TicketCheckerAlarm.cancelAlarm(context, url.getId());
                Toast.makeText(context, "Monitoring disabled", Toast.LENGTH_SHORT).show();
            }
            
            // Broadcast URL update using the constant from MainActivity
            Intent updateIntent = new Intent(MainActivity.ACTION_DATABASE_UPDATED);
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
            
            // Broadcast URL update using the constant from MainActivity
            Intent updateIntent = new Intent(MainActivity.ACTION_DATABASE_UPDATED);
            LocalBroadcastManager.getInstance(context).sendBroadcast(updateIntent);
        });
        
        // Set up URL editing with normal click (opens EditUrlDialog)
        holder.itemView.setOnClickListener(v -> {
            showEditUrlDialog(position);
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
        
        // Set up history button
        holder.buttonHistory.setOnClickListener(v -> {
            showHistoryDialog(position);
        });
    }
    
    /**
     * Shows the ticket history dialog
     */
    private void showHistoryDialog(int position) {
        MonitoredUrl url = urlList.get(position);
        TicketHistoryDialog dialog = new TicketHistoryDialog(context, url);
        dialog.show();
    }
    
    /**
     * Shows a dialog to edit URL settings
     */
    private void showEditUrlDialog(int position) {
        MonitoredUrl url = urlList.get(position);
        
        EditUrlDialog dialog = new EditUrlDialog(context, url, (originalUrl, newUrl, newFrequency, isActive, eventType) -> {
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
                eventType,
                originalUrl.getLastCheckedTimestamp(),
                originalUrl.hasTicketsFound()
            );
            
            // Check if anything actually changed
            boolean changed = !originalUrl.getUrl().equals(newUrl) ||
                             originalUrl.getFrequency() != newFrequency ||
                             originalUrl.isActive() != isActive ||
                             !originalUrl.getEventType().equals(eventType);
            
            if (changed) {
                // Update database and UI
                UrlDatabase.getInstance(context).updateUrl(updatedUrl);
                urlList.set(position, updatedUrl);
                notifyItemChanged(position);
                
                // Set new alarm if active
                if (isActive) {
                    TicketCheckerAlarm.setAlarm(context, updatedUrl);
                }
                
                // Broadcast URL update using the constant from MainActivity
                Intent updateIntent = new Intent(MainActivity.ACTION_DATABASE_UPDATED);
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
            url.detectEventType(); // Detect event type from new details
            
            // Update the database
            UrlDatabase.getInstance(context).updateEventDetails(url.getId(), eventName, eventDate);
            
            // Send broadcast to notify MainActivity to refresh the URL list
            Intent refreshIntent = new Intent(MainActivity.ACTION_EVENT_DETAILS_UPDATED);
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
        TextView textUrl;
        TextView textFrequency;
        TextView textEventDetails;
        TextView textLastChecked;
        ImageView imageEventType;
        Switch switchActive;
        Button buttonDelete;
        Button buttonHistory;

        public UrlViewHolder(@NonNull View itemView) {
            super(itemView);
            textUrl = itemView.findViewById(R.id.text_url);
            textFrequency = itemView.findViewById(R.id.text_frequency);
            textEventDetails = itemView.findViewById(R.id.text_event_details);
            textLastChecked = itemView.findViewById(R.id.text_last_checked);
            imageEventType = itemView.findViewById(R.id.image_event_type);
            switchActive = itemView.findViewById(R.id.switch_active);
            buttonDelete = itemView.findViewById(R.id.button_delete);
            buttonHistory = itemView.findViewById(R.id.button_history);
        }
    }
}