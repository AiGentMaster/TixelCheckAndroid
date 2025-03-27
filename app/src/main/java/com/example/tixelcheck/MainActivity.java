package com.example.tixelcheck;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements AddUrlDialog.UrlDialogListener {

    private RecyclerView rvUrlList;
    private UrlAdapter urlAdapter;
    private UrlDatabase urlDatabase;
    private TextView emptyStateTextView;
    private FloatingActionButton fabAddUrl;
    
    // Action for database updates - matching the string used in UrlAdapter
    public static final String ACTION_DATABASE_UPDATED = "com.example.tixelcheck.URL_UPDATED";
    public static final String ACTION_EVENT_DETAILS_UPDATED = "com.example.tixelcheck.EVENT_DETAILS_UPDATED";

    // BroadcastReceiver to handle database updates
    private BroadcastReceiver urlUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshUrlList();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        initializeUI();
        
        // Initialize database - using getInstance() instead of constructor
        urlDatabase = UrlDatabase.getInstance(this);
        
        // Setup the URL list recycler view
        setupRecyclerView();
        
        // Initial refresh
        refreshUrlList();
        
        // Register for database updates
        LocalBroadcastManager.getInstance(this).registerReceiver(
                urlUpdateReceiver,
                new IntentFilter(ACTION_DATABASE_UPDATED)
        );
        
        // Also register for event details updates
        LocalBroadcastManager.getInstance(this).registerReceiver(
                urlUpdateReceiver,
                new IntentFilter(ACTION_EVENT_DETAILS_UPDATED)
        );
    }

    private void initializeUI() {
        rvUrlList = findViewById(R.id.rv_url_list);
        emptyStateTextView = findViewById(R.id.tv_empty_state);
        fabAddUrl = findViewById(R.id.fab_add_url);
        
        fabAddUrl.setOnClickListener(v -> showAddUrlDialog());
    }

    private void setupRecyclerView() {
        urlAdapter = new UrlAdapter(new ArrayList<>(), this);
        rvUrlList.setLayoutManager(new LinearLayoutManager(this));
        rvUrlList.setAdapter(urlAdapter);
    }

    private void refreshUrlList() {
        ArrayList<MonitoredUrl> urls = (ArrayList<MonitoredUrl>) urlDatabase.getAllUrls();
        urlAdapter.updateUrls(urls);
        
        // Show/hide empty state
        if (urls.isEmpty()) {
            emptyStateTextView.setVisibility(View.VISIBLE);
            rvUrlList.setVisibility(View.GONE);
        } else {
            emptyStateTextView.setVisibility(View.GONE);
            rvUrlList.setVisibility(View.VISIBLE);
        }
    }

    private void showAddUrlDialog() {
        AddUrlDialog dialog = new AddUrlDialog(this, this);
        dialog.show();
    }
    
    @Override
    public void onUrlAdded(MonitoredUrl url) {
        // Add to database
        urlDatabase.addUrl(url);
        
        // Refresh the list
        refreshUrlList();
        
        Toast.makeText(this, "URL added successfully", Toast.LENGTH_SHORT).show();
        
        // Broadcast URL update
        Intent updateIntent = new Intent(ACTION_DATABASE_UPDATED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(updateIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_check_now) {
            // Get all active URLs and check them
            ArrayList<MonitoredUrl> activeUrls = (ArrayList<MonitoredUrl>) urlDatabase.getActiveUrls();
            if (activeUrls.isEmpty()) {
                Toast.makeText(this, "No active URLs to check", Toast.LENGTH_SHORT).show();
            } else {
                for (MonitoredUrl url : activeUrls) {
                    // Start manual check for each URL
                    Intent checkIntent = new Intent(this, TicketMonitorService.class);
                    checkIntent.putExtra("url_id", url.getId());
                    startService(checkIntent);
                }
                Toast.makeText(this, "Checking all active URLs...", Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (id == R.id.menu_history) {
            HistoryDialog dialog = new HistoryDialog(this, urlDatabase);
            dialog.show();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        // Unregister broadcast receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(urlUpdateReceiver);
        super.onDestroy();
    }
}