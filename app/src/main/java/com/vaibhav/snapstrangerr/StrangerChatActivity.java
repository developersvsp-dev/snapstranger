package com.vaibhav.snapstrangerr;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vaibhav.snapstrangerr.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StrangerChatActivity extends AppCompatActivity {

    private TextView tvStatus, tvStrangerName;
    private ProgressBar progressSearch;
    private RecyclerView rvMessages;
    private TextInputEditText etMessage;
    private Button btnSend;
    private TextView btnBack;

    private FirebaseFirestore db;
    private String matchId;
    private String myUsername;
    private String myDocumentId;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private static final String TAG = "StrangerChatActivity";
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stranger_chat);

        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        myDocumentId = prefs.getString("DOCUMENT_ID", "");
        if (!myDocumentId.isEmpty()) {
            db = FirebaseFirestore.getInstance();
            db.collection("users").document(myDocumentId)
                    .update("status", "in_chat")
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Status: in_chat"))
                    .addOnFailureListener(e -> Log.e(TAG, "❌ Status update failed", e));
        }

        matchId = getIntent().getStringExtra("MATCH_ID");
        String initialStrangerName = getIntent().getStringExtra("STRANGER_NAME");

        initViews();
        setupMyInfo();
        setupRecyclerView();
        setupClickListeners();

        if (matchId != null) {
            setupRealTimeChat(initialStrangerName);
        } else {
            tvStatus.setText("❌ No match found");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause - Keeping in_chat status");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        endMatch();
        if (!myDocumentId.isEmpty() && db != null) {
            db.collection("users").document(myDocumentId)
                    .update("status", "offline")
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Status: offline"))
                    .addOnFailureListener(e -> Log.e(TAG, "❌ Status update failed", e));
        }
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tvStatus);
        tvStrangerName = findViewById(R.id.tvStrangerName);
        progressSearch = findViewById(R.id.progressSearch);
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);

        findViewById(R.id.inputArea).setVisibility(View.GONE);
        rvMessages.setVisibility(View.GONE);
        progressSearch.setVisibility(View.VISIBLE);
        tvStatus.setText("🔗 Connecting to chat...");
    }

    private void setupMyInfo() {
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        myUsername = prefs.getString("USERNAME", "Me");
        myDocumentId = prefs.getString("DOCUMENT_ID", "");
        Log.d(TAG, "My username: " + myUsername);
    }

    // 🔥 PERFECT WHATSAPP RecyclerView Setup
    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, myUsername);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(messageAdapter);

        // 🔥 WHATSAPP STYLE - New messages appear at bottom
        LinearLayoutManager layoutManager = (LinearLayoutManager) rvMessages.getLayoutManager();
        layoutManager.setStackFromEnd(true);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            endMatch();
            finish();
        });
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void setupRealTimeChat(String initialStrangerName) {
        // 🔥 1. MATCH STATUS LISTENER
        db.collection("matches").document(matchId)
                .addSnapshotListener((matchSnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Match listener error", error);
                        return;
                    }
                    if (matchSnapshot != null && matchSnapshot.exists()) {
                        String status = matchSnapshot.getString("status");
                        String user1Name = matchSnapshot.getString("user1_name");
                        String user2Name = matchSnapshot.getString("user2_name");
                        String strangerName = getStrangerName(user1Name, user2Name);

                        tvStrangerName.setText("Chatting with: " + strangerName);

                        if ("active".equals(status) || "waiting".equals(status)) {
                            showChatUI();
                        } else if ("ended".equals(status)) {
                            tvStatus.setText("👋 Stranger left the chat");
                        }
                    }
                });

        // 🔥 2. PERFECT MESSAGES LISTENER - ALL MESSAGES STACK + SCROLL
        db.collection("matches")
                .document(matchId)
                .collection("messages")
                .orderBy("timestamp")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Messages listener error", error);
                        return;
                    }

                    if (snapshot != null) {
                        Log.d(TAG, "📨 Snapshot received: " + snapshot.size() + " messages");

                        // 🔥 CRITICAL FIX: Full reload every time (SIMPLE + RELIABLE)
                        messageList.clear();

                        // 🔥 Load ALL messages in timestamp order
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Message message = doc.toObject(Message.class);
                            if (message != null) {
                                messageList.add(message);
                            }
                        }

                        Log.d(TAG, "📨 Display list: " + messageList.size() + " messages");
                        messageAdapter.notifyDataSetChanged();

                        // 🔥 PERFECT WHATSAPP SCROLL TO BOTTOM
                        rvMessages.post(() -> {
                            if (!messageList.isEmpty()) {
                                int lastPosition = messageList.size() - 1;
                                rvMessages.scrollToPosition(lastPosition);
                                Log.d(TAG, "📱 Scrolled to bottom: position " + lastPosition);
                            }
                        });
                    }
                });
    }

    private String getStrangerName(String user1Name, String user2Name) {
        if (user1Name == null || user2Name == null) return "Stranger";
        return !user1Name.equals(myUsername) ? user1Name : user2Name;
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        if (messageText.isEmpty()) return;

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("text", messageText);
        messageData.put("sender", myUsername);
        messageData.put("timestamp", System.currentTimeMillis());

        db.collection("matches")
                .document(matchId)
                .collection("messages")
                .add(messageData)
                .addOnSuccessListener(documentReference -> {
                    etMessage.setText("");
                    Log.d(TAG, "✅ Message sent: " + messageText);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Send failed", e);
                    tvStatus.setText("❌ Failed to send");
                });
    }

    private void showChatUI() {
        progressSearch.setVisibility(View.GONE);
        findViewById(R.id.inputArea).setVisibility(View.VISIBLE);
        rvMessages.setVisibility(View.VISIBLE);
        tvStatus.setText("🟢 Live chat - Type away!");
    }

    private void endMatch() {
        if (matchId != null && db != null) {
            db.collection("matches").document(matchId)
                    .update("status", "ended")
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to end match", e));
        }
    }
}
