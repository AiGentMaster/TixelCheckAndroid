package com.example.tixelcheck;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
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
        holder.textUrl.setText(url.getUrl());
        holder.textFrequency.setText("Check every " + url.getFrequency() + " minutes");
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

        holder.buttonDelete.setOnClickListener(v -> {
            // Cancel alarm before deleting
            TicketCheckerAlarm.cancelAlarm(context, url.getId());
            
            // Delete from database and update UI
            UrlDatabase.getInstance(context).deleteUrl(url.getId());
            urlList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, urlList.size());
        });
    }

    @Override
    public int getItemCount() {
        return urlList.size();
    }

    static class UrlViewHolder extends RecyclerView.ViewHolder {
        TextView textUrl;
        TextView textFrequency;
        Switch switchActive;
        Button buttonDelete;

        public UrlViewHolder(@NonNull View itemView) {
            super(itemView);
            textUrl = itemView.findViewById(R.id.text_url);
            textFrequency = itemView.findViewById(R.id.text_frequency);
            switchActive = itemView.findViewById(R.id.switch_active);
            buttonDelete = itemView.findViewById(R.id.button_delete);
        }
    }
}