package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import static com.abebegetachew.abebe.salarymanagement.abebegetachew.NetworkUtil.isConnected;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import androidx.activity.EdgeToEdge;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class NoInternet extends BaseActivity {

    private boolean firstLaunchOffline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_no_internet);

        // Check if this NoInternet screen is from the app being launched offline
        firstLaunchOffline = getIntent().getBooleanExtra("firstLaunchOffline", false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isConnected(this)) {
            // Connection is back — always return to Splash
            ensureAnonymousUserThenProceed();
        } else {
            // Keep checking every second
            checkConnectionRepeatedly();
        }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void ensureAnonymousUserThenProceed() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        Runnable goNext = () -> {
            Intent intent = new Intent(NoInternet.this, Splash.class);

            // Keep the flag if this was a first-launch offline case
            intent.putExtra("firstLaunchOffline", firstLaunchOffline);

            startActivity(intent);
            finish();

            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        };

        if (currentUser == null) {
            auth.signInAnonymously().addOnCompleteListener(task -> goNext.run());
        } else if (!currentUser.isAnonymous()) {
            auth.signOut();
            auth.signInAnonymously().addOnCompleteListener(task -> goNext.run());
        } else {
            goNext.run();
        }
    }

    private void checkConnectionRepeatedly() {
        final android.os.Handler handler = new android.os.Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isConnected(NoInternet.this)) {
                    ensureAnonymousUserThenProceed();
                } else {
                    handler.postDelayed(this, 1000); // check again after 1 second
                }
            }
        }, 1000);
    }

    public void open_settings(View view) {
        Intent intent = new Intent(Settings.ACTION_SETTINGS);
        startActivity(intent);
    }
}
