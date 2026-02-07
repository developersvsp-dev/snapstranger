package com.vaibhav.snapstrangerr;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import com.vaibhav.snapstrangerr.R;

public class StrangerChatActivity extends AppCompatActivity {

    private TextView tvStatus;
    private ProgressBar progressSearch;
    private RecyclerView rvMessages;
    private TextInputEditText etMessage;
    private Button btnSend;
    private TextView btnBack;  // ✅ FIXED: TextView, not ImageView!
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stranger_chat);

        // Get search type from intent
        String searchType = getIntent().getStringExtra("SEARCH_TYPE");
        String username = getIntent().getStringExtra("USERNAME");

        initViews();
        setupStatus(searchType, username);
        setupClickListeners();

        // Simulate searching
        simulateSearching(searchType);
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tvStatus);
        progressSearch = findViewById(R.id.progressSearch);
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);  // ✅ FIXED: TextView!

        // Setup RecyclerView
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupStatus(String searchType, String username) {
        tvStatus.setText("🔍 Searching " + searchType + " stranger...");
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());  // ✅ Works with TextView
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void simulateSearching(String searchType) {
        progressSearch.setVisibility(View.VISIBLE);

        // Simulate 3-5 second search
        handler.postDelayed(() -> {
            progressSearch.setVisibility(View.GONE);
            tvStatus.setText("✅ Found " + searchType + " stranger! Say Hi 👋");

            // Show input area & messages
            findViewById(R.id.inputArea).setVisibility(View.VISIBLE);
            rvMessages.setVisibility(View.VISIBLE);

        }, 3000 + (int)(Math.random() * 2000));
    }

    private void sendMessage() {
        String message = etMessage.getText().toString().trim();
        if (!message.isEmpty()) {
            etMessage.setText("");
            // TODO: Add real chat logic here
        }
    }
}
