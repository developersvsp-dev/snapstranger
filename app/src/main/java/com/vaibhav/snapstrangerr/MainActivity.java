package com.vaibhav.snapstrangerr;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.QuerySnapshot;
import com.vaibhav.snapstrangerr.R;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etPassword, etConfirmPassword;
    private AutoCompleteTextView autoCompleteGender;
    private Button btnRegister;
    private TextView usernameStatus, tvGoToLogin;
    private FirebaseFirestore db;
    private CollectionReference usersCollection;
    private boolean isUsernameValid = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable checkRunnable = this::validateUsernameRealTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        usersCollection = db.collection("users");

        initViews();
        setupUI();
        setupGenderDropdown();
        setupRealTimeUsernameValidation();
        setupClickListeners();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkLoginSession();
    }

    private void checkLoginSession() {
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("IS_LOGGED_IN", false);

        if (isLoggedIn) {
            String username = prefs.getString("USERNAME", "");
            String gender = prefs.getString("GENDER", "");
            String documentId = prefs.getString("DOCUMENT_ID", "");  // 🔥 ADD THIS

            if (!username.isEmpty()) {
                Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                intent.putExtra("USERNAME", username);
                intent.putExtra("GENDER", gender);
                startActivity(intent);
                finish();
                return;
            }
        }
    }


    // 🔥 OLD (2 params) → NEW (3 params)
    private void saveLoginSession(String username, String gender, String documentId) {
        getSharedPreferences("user_session", MODE_PRIVATE)
                .edit()
                .putString("USERNAME", username)
                .putString("GENDER", gender)
                .putString("DOCUMENT_ID", documentId)  // 🔥 NEW PARAMETER
                .putBoolean("IS_LOGGED_IN", true)
                .apply();
    }


    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        autoCompleteGender = findViewById(R.id.spinnerGender);
        btnRegister = findViewById(R.id.btnRegister);
        usernameStatus = findViewById(R.id.usernameStatus);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);
    }

    private void setupUI() {
        if (usernameStatus != null) {
            usernameStatus.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        if (btnRegister != null) {
            btnRegister.setOnClickListener(v -> registerUser());
        }
        if (tvGoToLogin != null) {
            tvGoToLogin.setOnClickListener(v -> goToLogin());
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void setupGenderDropdown() {
        if (autoCompleteGender != null) {
            String[] genders = {"Male", "Female", "Other"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, genders);
            autoCompleteGender.setAdapter(adapter);
            autoCompleteGender.setThreshold(0);
            autoCompleteGender.setOnClickListener(v -> autoCompleteGender.showDropDown());
        }
    }

    private void setupRealTimeUsernameValidation() {
        if (etUsername != null) {
            etUsername.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    handler.removeCallbacks(checkRunnable);
                    String currentText = s.toString().trim();
                    if (currentText.length() >= 3) {
                        handler.postDelayed(checkRunnable, 400);
                    } else {
                        if (usernameStatus != null) {
                            usernameStatus.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void validateUsernameRealTime() {
        if (etUsername == null) return;
        String username = etUsername.getText().toString().trim();

        if (username.length() < 3) {
            showStatus("Username must be 3+ chars", android.R.color.holo_red_dark, false);
            return;
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            showStatus("Only letters, numbers, _ allowed", android.R.color.holo_red_dark, false);
            return;
        }

        showStatus("🔍 Checking...", android.R.color.holo_orange_dark, false);

        usersCollection.whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    try {
                        if (task.isSuccessful() && task.getResult() != null) {
                            QuerySnapshot snapshot = task.getResult();
                            if (snapshot.isEmpty()) {
                                showStatus("✅ " + username + " available!", android.R.color.holo_green_dark, true);
                            } else {
                                showStatus("❌ " + username + " taken", android.R.color.holo_red_dark, false);
                            }
                        } else {
                            showStatus("⚠️ Network error", android.R.color.holo_red_dark, false);
                        }
                    } catch (Exception e) {
                        Log.e("MainActivity", "Validation error: " + e.getMessage());
                        showStatus("⚠️ Error checking", android.R.color.holo_red_dark, false);
                    }
                });
    }

    private void showStatus(String message, int colorRes, boolean isValid) {
        if (usernameStatus != null) {
            usernameStatus.setText(message);
            usernameStatus.setTextColor(ContextCompat.getColor(this, colorRes));
            usernameStatus.setVisibility(View.VISIBLE);
            isUsernameValid = isValid;
        }
    }

    private void registerUser() {
        if (etUsername == null || etPassword == null || etConfirmPassword == null || autoCompleteGender == null) {
            Toast.makeText(this, "UI not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String gender = autoCompleteGender.getText().toString().trim();

        if (username.isEmpty() || !isUsernameValid) {
            showError(etUsername, "Choose available username");
            return;
        }
        if (password.isEmpty() || password.length() < 6) {
            showError(etPassword, "Password 6+ characters");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError(etConfirmPassword, "Passwords don't match");
            return;
        }
        if (gender.isEmpty() || (!gender.equals("Male") && !gender.equals("Female") && !gender.equals("Other"))) {
            showError(autoCompleteGender, "Select valid gender");
            return;
        }

        btnRegister.setText("Creating...");
        btnRegister.setEnabled(false);
        registerNewUser(username, password, gender);
    }

    private void registerNewUser(String username, String password, String gender) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("password", password);
        userData.put("gender", gender);
        userData.put("status", "offline");  // 🔥 Initial status

        usersCollection.add(userData)
                .addOnSuccessListener(documentReference -> {
                    String documentId = documentReference.getId();  // 🔥 Get document ID

                    // 🔥 Save with document ID
                    saveLoginSession(username, gender, documentId);

                    Toast.makeText(this, "✅ Welcome to SnapStrangerr, " + username + "!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                    intent.putExtra("USERNAME", username);
                    intent.putExtra("GENDER", gender);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE", "Registration failed: " + e.getMessage());
                    Toast.makeText(this, "❌ Registration failed", Toast.LENGTH_LONG).show();
                    btnRegister.setText("Create Account");
                    btnRegister.setEnabled(true);
                });
    }



    private void showError(View inputView, String errorMsg) {
        if (inputView == null) return;
        ViewParent parent = inputView.getParent();
        while (parent != null) {
            if (parent instanceof com.google.android.material.textfield.TextInputLayout) {
                ((com.google.android.material.textfield.TextInputLayout) parent).setError(errorMsg);
                inputView.requestFocus();
                return;
            }
            parent = parent.getParent();
        }
        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
    }

    private void clearError(View inputView) {
        if (inputView == null) return;
        ViewParent parent = inputView.getParent();
        while (parent != null) {
            if (parent instanceof com.google.android.material.textfield.TextInputLayout) {
                ((com.google.android.material.textfield.TextInputLayout) parent).setError(null);
                break;
            }
            parent = parent.getParent();
        }
    }

    private void clearAllErrors() {
        clearError(etUsername);
        clearError(etPassword);
        clearError(etConfirmPassword);
        clearError(autoCompleteGender);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(checkRunnable);
    }
}
