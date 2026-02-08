package com.vaibhav.snapstrangerr;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.vaibhav.snapstrangerr.R;
import java.util.HashMap;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvUsername, tvGender;
    private Button btnLogout, btnSearchStranger;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String selectedStrangerType = "";
    private Handler handler = new Handler(Looper.getMainLooper());
    private static final String TAG = "DashboardActivity";
    private boolean isSearching = false;
    private boolean hasMatch = false;
    private String myGender = "";
    private String myDesiredGender = ""; // 🔥 NEW: Track what user wants

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupUserData();
        setupMatchListener();

        setUserStatus("active");
        showActiveBadge();
        setupButtons();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.dashboard_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        isSearching = false;
        hasMatch = false;
        setUserStatus("active");
        showActiveBadge();
        Log.d(TAG, "Dashboard onResume - ACTIVE");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isSearching && !hasMatch) {
            setUserStatus("offline");
            leaveAllNeedCollections();
        }
        Log.d(TAG, "onPause - searching=" + isSearching + " matched=" + hasMatch);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!hasMatch) {
            setUserStatus("inactive");
        }
        Log.d(TAG, "onStop - hasMatch=" + hasMatch);
    }

    private void setupMatchListener() {
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        final String myDocId = prefs.getString("DOCUMENT_ID", "");
        Log.d(TAG, "🔍 SETUP LISTENER for myDocId=" + myDocId);

        if (myDocId.isEmpty()) {
            Log.e(TAG, "❌ NO DOCUMENT_ID!");
            return;
        }

        db.collection("matches")
                .whereEqualTo("user1_id", myDocId)
                .addSnapshotListener((snapshot, error) -> {
                    Log.d(TAG, "👤 USER1 LISTENER: " + (snapshot != null ? snapshot.size() : 0) + " docs");
                    handleMatchSnapshot(snapshot, error, myDocId);
                });

        db.collection("matches")
                .whereEqualTo("user2_id", myDocId)
                .addSnapshotListener((snapshot, error) -> {
                    Log.d(TAG, "👤 USER2 LISTENER: " + (snapshot != null ? snapshot.size() : 0) + " docs");
                    handleMatchSnapshot(snapshot, error, myDocId);
                });

        Log.d(TAG, "✅ MATCH LISTENERS ACTIVE for " + myDocId);
    }

    private void handleMatchSnapshot(QuerySnapshot snapshot, FirebaseFirestoreException error, String myDocId) {
        Log.d(TAG, "🔍 LISTENER FIRED! snapshot=" + (snapshot != null) + " size=" + (snapshot != null ? snapshot.size() : 0));

        if (error != null) {
            Log.e(TAG, "❌ LISTENER ERROR: " + error.getMessage());
            return;
        }

        if (snapshot != null && !snapshot.isEmpty() && !hasMatch) {
            for (DocumentSnapshot matchDoc : snapshot.getDocuments()) {
                String matchId = matchDoc.getId();
                String status = matchDoc.getString("status");
                Log.d(TAG, "📄 Match doc: " + matchId + " status=" + status);

                if ("active".equals(status)) {
                    hasMatch = true;
                    isSearching = false;
                    Log.d(TAG, "🎯 MATCH DETECTED! Going to chat: " + matchId);
                    goToChatScreen(matchId);
                    return;
                }
            }
        }
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvUsername = findViewById(R.id.tvUsername);
        tvGender = findViewById(R.id.tvGender);
        btnLogout = findViewById(R.id.btnLogout);
        btnSearchStranger = findViewById(R.id.btnSearchStranger);
    }

    private void setupUserData() {
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        String username = prefs.getString("USERNAME", "User");
        myGender = prefs.getString("GENDER", "Not set");

        tvUsername.setText("Username: " + username);
        tvGender.setText("Gender: " + myGender);
        Log.d(TAG, "👤 My Gender: " + myGender);
    }

    private void showActiveBadge() {
        String username = tvUsername.getText().toString().replace("Username: ", "");
        tvWelcome.setText("🟢 Active - Welcome to SnapStrangerr, " + username + "!");
    }

    private void setUserStatus(String status) {
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        String documentId = prefs.getString("DOCUMENT_ID", null);

        if (documentId == null) {
            Log.e(TAG, "No document ID found");
            return;
        }

        db.collection("users").document(documentId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Status: " + status))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Status failed", e));
    }

    private void leaveAllNeedCollections() {
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        String myDocId = prefs.getString("DOCUMENT_ID", "");

        if (myDocId.isEmpty()) return;

        String[] collections = {"need_male", "need_female", "need_random"};
        for (String collection : collections) {
            db.collection(collection).document(myDocId).delete()
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Cleaned " + collection));
        }
    }

    private void setupButtons() {
        btnSearchStranger.setOnClickListener(v -> showStrangerSearchDialog());
        btnLogout.setOnClickListener(v -> {
            setUserStatus("offline");
            leaveAllNeedCollections();
            getSharedPreferences("user_session", MODE_PRIVATE).edit().clear().apply();
            Toast.makeText(this, "👋 See you soon!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void showStrangerSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_stranger_search, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        TextView iconMale = dialogView.findViewById(R.id.iconMale);
        TextView iconFemale = dialogView.findViewById(R.id.iconFemale);
        TextView iconRandom = dialogView.findViewById(R.id.iconRandom);
        LinearLayout optionMale = dialogView.findViewById(R.id.optionMale);
        LinearLayout optionFemale = dialogView.findViewById(R.id.optionFemale);
        LinearLayout optionRandom = dialogView.findViewById(R.id.optionRandom);
        Button btnSearch = dialogView.findViewById(R.id.btnSearch);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        TextView tvHint = dialogView.findViewById(R.id.tvHint);

        if ("Male".equalsIgnoreCase(myGender)) {
            tvHint.setText("🔥 What type of stranger do you want to chat with?");
        } else if ("Female".equalsIgnoreCase(myGender)) {
            tvHint.setText("💃 What type of stranger do you want to chat with?");
        } else {
            tvHint.setText("🎲 Choose your chat partner!");
        }

        btnSearch.setEnabled(false);
        btnSearch.setAlpha(0.6f);

        View.OnClickListener optionClick = v -> {
            resetAllIcons(iconMale, iconFemale, iconRandom);
            if (v.getId() == R.id.optionMale) {
                iconMale.setVisibility(View.VISIBLE);
                selectedStrangerType = "🔥 Male";
                myDesiredGender = "Male";
                Log.d(TAG, "👨 Selected: Male");
            } else if (v.getId() == R.id.optionFemale) {
                iconFemale.setVisibility(View.VISIBLE);
                selectedStrangerType = "💃 Female";
                myDesiredGender = "Female";
                Log.d(TAG, "👩 Selected: Female");
            } else if (v.getId() == R.id.optionRandom) {
                iconRandom.setVisibility(View.VISIBLE);
                selectedStrangerType = "🎲 Random";
                myDesiredGender = "Random";
                Log.d(TAG, "🎲 Selected: Random");
            }
            btnSearch.setEnabled(true);
            btnSearch.setAlpha(1.0f);
        };

        optionMale.setOnClickListener(optionClick);
        optionFemale.setOnClickListener(optionClick);
        optionRandom.setOnClickListener(optionClick);

        btnSearch.setOnClickListener(v -> {
            if (!selectedStrangerType.isEmpty()) {
                dialog.dismiss();
                joinNeedCollection(selectedStrangerType);
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void joinNeedCollection(String searchType) {
        if (isSearching) {
            Toast.makeText(this, "⏳ Already searching...", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        String myDocId = prefs.getString("DOCUMENT_ID", "");
        String myUsername = prefs.getString("USERNAME", "");

        if (myDocId.isEmpty()) {
            Toast.makeText(this, "❌ Login error", Toast.LENGTH_SHORT).show();
            return;
        }

        isSearching = true;
        Toast.makeText(this, "🔍 Searching " + searchType + "...", Toast.LENGTH_SHORT).show();

        String needCollection = getNeedCollection(searchType);
        Map<String, Object> searchData = new HashMap<>();
        searchData.put("user_id", myDocId);
        searchData.put("username", myUsername);
        searchData.put("gender", myGender);
        searchData.put("desired_gender", myDesiredGender); // 🔥 FIXED: Store desired gender
        searchData.put("search_type", searchType);
        searchData.put("timestamp", System.currentTimeMillis());

        db.collection(needCollection).document(myDocId).set(searchData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Joined " + needCollection + " (Gender: " + myGender + ", Wants: " + myDesiredGender + ")");
                    findAndMatchImmediately(needCollection, myDocId, myUsername);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Join failed", e);
                    isSearching = false;
                    Toast.makeText(this, "❌ Network error", Toast.LENGTH_SHORT).show();
                });
    }

    private void findAndMatchImmediately(String myNeedCollection, String myDocId, String myUsername) {
        String[] priorityPools = getPrioritySearchPools(myNeedCollection, myGender, myDesiredGender);

        for (String pool : priorityPools) {
            searchSinglePool(pool, myNeedCollection, myDocId, myUsername);
        }

        handler.postDelayed(() -> {
            if (isSearching) {
                Log.d(TAG, "🔄 Fallback: Searching own pool " + myNeedCollection);
                searchSinglePool(myNeedCollection, myNeedCollection, myDocId, myUsername);
            }
        }, 500);

        handler.postDelayed(() -> {
            if (isSearching && !hasMatch) {
                isSearching = false;
                leaveNeedCollection(myNeedCollection, myDocId);
                Log.d(TAG, "⏰ Search timeout");
                runOnUiThread(() ->
                        Toast.makeText(this, "😔 No match found. Try again!", Toast.LENGTH_LONG).show()
                );
            }
        }, 20000);
    }

    // 🔥 FIXED: Perfect gender-based search logic
    private String[] getPrioritySearchPools(String myCollection, String myGender, String desiredGender) {
        Log.d(TAG, "🎯 Priority pools for: " + myGender + " → wants " + desiredGender + " in " + myCollection);

        if ("Random".equals(desiredGender)) {
            return new String[]{"need_random", "need_male", "need_female"};
        }

        // Male wants Male
        if ("Male".equalsIgnoreCase(myGender) && "Male".equalsIgnoreCase(desiredGender)) {
            return new String[]{"need_male"};
        }
        // Male wants Female
        if ("Male".equalsIgnoreCase(myGender) && "Female".equalsIgnoreCase(desiredGender)) {
            // Search females who want males OR females OR anyone
            return new String[]{"need_male", "need_female", "need_random"};
        }
        // Female wants Female
        if ("Female".equalsIgnoreCase(myGender) && "Female".equalsIgnoreCase(desiredGender)) {
            return new String[]{"need_female"};
        }
        // Female wants Male
        if ("Female".equalsIgnoreCase(myGender) && "Male".equalsIgnoreCase(desiredGender)) {
            // Search males who want females OR males OR anyone
            return new String[]{"need_female", "need_male", "need_random"};
        }

        return new String[]{myCollection};
    }

    // 🔥 FIXED: Gender filtering + validation
    private void searchSinglePool(String targetPool, String myPool, String myDocId, String myUsername) {
        if (!isSearching || hasMatch) return;

        Log.d(TAG, "🎯 [" + myGender + "→" + myDesiredGender + "] Searching " + targetPool + " (from " + myPool + ")");

        Query query = db.collection(targetPool).limit(1);

        // 🔥 GENDER FILTERING - Only search for desired gender
        if (!"Random".equals(myDesiredGender)) {
            query = query.whereEqualTo("gender", myDesiredGender);
        }

        query.get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty() && isSearching && !hasMatch) {
                        DocumentSnapshot strangerDoc = snapshot.getDocuments().get(0);
                        String strangerId = strangerDoc.getId();
                        String strangerName = strangerDoc.getString("username");
                        String strangerGender = strangerDoc.getString("gender");
                        String strangerDesired = strangerDoc.getString("desired_gender");

                        Log.d(TAG, "🔍 Found: " + strangerName + " (" + strangerGender + "→" + strangerDesired + ")");

                        // 🔥 FIXED: Multiple validation checks
                        if (!strangerId.equals(myDocId) && // Not myself
                                isValidGenderMatch(strangerGender, strangerDesired)) { // Gender compatibility

                            Log.d(TAG, "✅ PERFECT MATCH: " + strangerName + " (" + strangerGender + ") from " + targetPool);
                            createMatchAndCleanup(myDocId, myUsername, strangerId, strangerName, myPool, targetPool);
                            return;
                        } else {
                            Log.d(TAG, "❌ Rejected: " + strangerName +
                                    " (self=" + strangerId.equals(myDocId) +
                                    ", gender_ok=" + isValidGenderMatch(strangerGender, strangerDesired) + ")");
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Search failed", e));
    }

    // 🔥 NEW: Validate gender compatibility
    private boolean isValidGenderMatch(String strangerGender, String strangerDesired) {
        if ("Random".equals(myDesiredGender)) return true;
        if (myDesiredGender.equalsIgnoreCase(strangerGender)) return true;

        // Cross-gender check: They want me back OR they're random
        if (("Male".equals(myGender) && "Female".equals(strangerGender)) ||
                ("Female".equals(myGender) && "Male".equals(strangerGender))) {
            return "Male".equals(strangerDesired) || "Female".equals(strangerDesired) || "Random".equals(strangerDesired);
        }

        return false;
    }

    private void createMatchAndCleanup(String myId, String myName, String strangerId, String strangerName,
                                       String myCollection, String strangerCollection) {
        hasMatch = true;
        isSearching = false;

        String matchId = "match_" + System.currentTimeMillis();
        String displayName = strangerName != null ? strangerName : "Stranger";

        Map<String, Object> matchData = new HashMap<>();
        matchData.put("user1_id", myId);
        matchData.put("user2_id", strangerId);
        matchData.put("user1_name", myName);
        matchData.put("user2_name", displayName);
        matchData.put("user1_gender", myGender);
        matchData.put("user2_gender", myDesiredGender);
        matchData.put("status", "active");
        matchData.put("created_by", myId);
        matchData.put("created_at", System.currentTimeMillis());

        db.collection("matches").document(matchId).set(matchData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ MATCH CREATED: " + matchId + " (" + myGender + " ↔ " + myDesiredGender + ")");

                    handler.postDelayed(() -> {
                        removeBothUsers(myId, strangerId, myCollection, strangerCollection);
                    }, 3000);

                    goToChatScreen(matchId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Match creation failed", e);
                    isSearching = false;
                    hasMatch = false;
                });
    }

    private void goToChatScreen(String matchId) {
        Log.d(TAG, "🚀 NAVIGATING TO CHAT: " + matchId);
        setUserStatus("in_chat");
        Intent intent = new Intent(this, StrangerChatActivity.class);
        intent.putExtra("MATCH_ID", matchId);
        intent.putExtra("MY_GENDER", myGender);
        intent.putExtra("MY_ROLE", "matched");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void removeBothUsers(String myId, String strangerId, String myCollection, String strangerCollection) {
        db.collection(myCollection).document(myId).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Cleanup self"));
        db.collection(strangerCollection).document(strangerId).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Cleanup stranger"));
    }

    private String getNeedCollection(String searchType) {
        if (searchType.contains("Male")) return "need_male";
        if (searchType.contains("Female")) return "need_female";
        return "need_random";
    }

    private void leaveNeedCollection(String collection, String myDocId) {
        db.collection(collection).document(myDocId).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Left " + collection));
    }

    private void resetAllIcons(TextView iconMale, TextView iconFemale, TextView iconRandom) {
        if (iconMale != null) iconMale.setVisibility(View.GONE);
        if (iconFemale != null) iconFemale.setVisibility(View.GONE);
        if (iconRandom != null) iconRandom.setVisibility(View.GONE);
        selectedStrangerType = "";
        myDesiredGender = "";
    }
}
