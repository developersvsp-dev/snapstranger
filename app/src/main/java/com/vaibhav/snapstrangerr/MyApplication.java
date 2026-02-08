package com.vaibhav.snapstrangerr;

import android.app.Application;
import android.util.Log;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // Observe app lifecycle globally
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onStart(LifecycleOwner owner) {
                // App to foreground -> set active
                setUserStatus("active");
                Log.d(TAG, "App foreground - status: active");
            }

            @Override
            public void onStop(LifecycleOwner owner) {
                // App to background -> set offline
                setUserStatus("offline");
                Log.d(TAG, "App background - status: offline");
            }
        });
    }

    private void setUserStatus(String status) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("status", status);
    }
}
