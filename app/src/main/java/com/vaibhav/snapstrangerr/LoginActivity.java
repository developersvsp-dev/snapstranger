package com.vaibhav.snapstrangerr;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.QuerySnapshot;
import com.vaibhav.snapstrangerr.R;

import java.util.HashMap;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvGoToRegister;
    private FirebaseFirestore db;
    private CollectionReference usersCollection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        db = FirebaseFirestore.getInstance();
        usersCollection = db.collection("users");

        initViews();
        setupClickListeners();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkLoginSession();  // 🔥 AUTO-LOGIN CHECK
    }

    // 🔥 CRITICAL: Check existing session on app launch
    private void checkLoginSession() {
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("IS_LOGGED_IN", false);

        if (isLoggedIn) {
            String username = prefs.getString("USERNAME", "");
            String gender = prefs.getString("GENDER", "");

            if (!username.isEmpty()) {
                // ✅ DIRECT TO DASHBOARD - NO LOGIN NEEDED
                Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                intent.putExtra("USERNAME", username);
                intent.putExtra("GENDER", gender);
                startActivity(intent);
                finish();
                return;
            }
        }
    }

    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> loginUser());
        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        });
    }

    private void loginUser() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError(etUsername, "Enter username & password");
            return;
        }

        btnLogin.setText("Logging in...");
        btnLogin.setEnabled(false);

        usersCollection.whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        QuerySnapshot snapshot = task.getResult();
                        if (!snapshot.isEmpty() && snapshot.getDocuments().size() == 1) {
                            User user = snapshot.getDocuments().get(0).toObject(User.class);
                            if (user != null && user.getPassword().equals(password)) {

                                // 🔥 CRITICAL: Get Firestore document ID
                                String documentId = snapshot.getDocuments().get(0).getId();

                                // 🔥 Ensure status field exists
                                ensureUserStatus(documentId, "offline");

                                // 🔥 SAVE DOCUMENT ID in SharedPreferences
                                saveLoginSession(username, user.getGender(), documentId);

                                Toast.makeText(this, "✅ Welcome back, " + username + "!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                                intent.putExtra("USERNAME", username);
                                intent.putExtra("GENDER", user.getGender());
                                startActivity(intent);
                                finish();
                            } else {
                                showError(etUsername, "❌ Wrong password");
                            }
                        } else {
                            showError(etUsername, "❌ User not found");
                        }
                    } else {
                        showError(etUsername, "⚠️ Network error");
                    }
                    resetLoginButton();
                });
    }

    // 🔥 NEW: Save document ID
    private void saveLoginSession(String username, String gender, String documentId) {
        getSharedPreferences("user_session", MODE_PRIVATE)
                .edit()
                .putString("USERNAME", username)
                .putString("GENDER", gender)
                .putString("DOCUMENT_ID", documentId)  // 🔥 FIRESTORE DOC ID
                .putBoolean("IS_LOGGED_IN", true)
                .apply();
    }


    // 🔥 NEW: Helper method to ensure status field exists
    private void ensureUserStatus(String uid, String status) {
        db.collection("users").document(uid)
                .update("status", status)
                .addOnFailureListener(e -> {
                    // If update fails (field doesn't exist), create it
                    db.collection("users").document(uid)
                            .set(new HashMap<String, Object>() {{ put("status", status); }},
                                    com.google.firebase.firestore.SetOptions.merge())
                            .addOnFailureListener(ex -> Log.e("Login", "Failed to set status", ex));
                });
    }


    private void saveLoginSession(String username, String gender) {
        getSharedPreferences("user_session", MODE_PRIVATE)
                .edit()
                .putString("USERNAME", username)
                .putString("GENDER", gender)
                .putBoolean("IS_LOGGED_IN", true)
                .apply();
    }

    private void resetLoginButton() {
        btnLogin.setText("Login");
        btnLogin.setEnabled(true);
    }

    private void showError(android.view.View inputView, String errorMsg) {
        android.view.ViewParent parent = inputView.getParent();
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
}
