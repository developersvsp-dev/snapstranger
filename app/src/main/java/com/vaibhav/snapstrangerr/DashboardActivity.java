package com.vaibhav.snapstrangerr;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import com.vaibhav.snapstrangerr.R;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvUsername, tvGender;
    private Button btnLogout, btnSearchStranger;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String selectedStrangerType = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupUserData();
        setupButtons();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.dashboard_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvUsername = findViewById(R.id.tvUsername);
        tvGender = findViewById(R.id.tvGender);
        btnLogout = findViewById(R.id.btnLogout);
        btnSearchStranger = findViewById(R.id.btnSearchStranger);
    }

    private void setupUserData() {
        String username = getIntent().getStringExtra("USERNAME");
        String gender = getIntent().getStringExtra("GENDER");

        if (username == null) {
            SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
            username = prefs.getString("USERNAME", "User");
        }
        if (gender == null) {
            SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
            gender = prefs.getString("GENDER", "Not set");
        }

        tvWelcome.setText("Welcome to SnapStrangerr, " + username + "!");
        tvUsername.setText("Username: " + username);
        tvGender.setText("Gender: " + gender);
    }

    private void setupButtons() {
        btnSearchStranger.setOnClickListener(v -> showStrangerSearchDialog());

        btnLogout.setOnClickListener(v -> {
            getSharedPreferences("user_session", MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();
            Toast.makeText(this, "👋 See you soon!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    // 🔥 PERFECTLY FIXED DIALOG - NO CRASH!
    private void showStrangerSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_stranger_search, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // ✅ FIXED: TextView (NOT ImageView!)
        TextView iconMale = dialogView.findViewById(R.id.iconMale);
        TextView iconFemale = dialogView.findViewById(R.id.iconFemale);
        TextView iconRandom = dialogView.findViewById(R.id.iconRandom);

        LinearLayout optionMale = dialogView.findViewById(R.id.optionMale);
        LinearLayout optionFemale = dialogView.findViewById(R.id.optionFemale);
        LinearLayout optionRandom = dialogView.findViewById(R.id.optionRandom);

        Button btnSearch = dialogView.findViewById(R.id.btnSearch);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        // Initially disable search button
        btnSearch.setEnabled(false);
        btnSearch.setAlpha(0.6f);

        // Selection logic
        View.OnClickListener optionClick = v -> {
            resetAllIcons(iconMale, iconFemale, iconRandom);

            if (v.getId() == R.id.optionMale) {
                iconMale.setVisibility(View.VISIBLE);
                selectedStrangerType = "🔥 Male";
            } else if (v.getId() == R.id.optionFemale) {
                iconFemale.setVisibility(View.VISIBLE);
                selectedStrangerType = "💃 Female";
            } else if (v.getId() == R.id.optionRandom) {
                iconRandom.setVisibility(View.VISIBLE);
                selectedStrangerType = "🎲 Random";
            }

            // Enable search button
            btnSearch.setEnabled(true);
            btnSearch.setAlpha(1.0f);
        };

        optionMale.setOnClickListener(optionClick);
        optionFemale.setOnClickListener(optionClick);
        optionRandom.setOnClickListener(optionClick);

        // Search button
        btnSearch.setOnClickListener(v -> {
            if (!selectedStrangerType.isEmpty()) {
                Toast.makeText(this, "🔍 Searching for " + selectedStrangerType + "...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(DashboardActivity.this, StrangerChatActivity.class);
                intent.putExtra("SEARCH_TYPE", selectedStrangerType);
                intent.putExtra("USERNAME", getIntent().getStringExtra("USERNAME"));
                intent.putExtra("GENDER", getIntent().getStringExtra("GENDER"));
                startActivity(intent);
                dialog.dismiss();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // ✅ FIXED: Takes TextView parameters
    private void resetAllIcons(TextView iconMale, TextView iconFemale, TextView iconRandom) {
        if (iconMale != null) iconMale.setVisibility(View.GONE);
        if (iconFemale != null) iconFemale.setVisibility(View.GONE);
        if (iconRandom != null) iconRandom.setVisibility(View.GONE);
        selectedStrangerType = "";
    }
}
