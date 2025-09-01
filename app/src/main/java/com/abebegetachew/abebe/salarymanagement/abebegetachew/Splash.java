package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class Splash extends BaseActivity {

    ImageView center, text1, text2;
    SessionManager sessionManager;

    LinearLayout pleaseWaitContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        if (!isConnected(this)) {
            Intent intent = new Intent(this, NoInternet.class);
            intent.putExtra("firstLaunchOffline", true);
            startActivity(intent);
            finish();
        }

        center = findViewById(R.id.imageView5);
        text1 = findViewById(R.id.imageView7);
        text2 = findViewById(R.id.imageView8);

        pleaseWaitContainer = findViewById(R.id.please_wait_container);

        sessionManager = new SessionManager(this);

        Animation fade_in_out = AnimationUtils.loadAnimation(this, R.anim.fade_in_out);

        fade_in_out.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // Nothing here
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Hide splash images
                center.setVisibility(View.GONE);
                text1.setVisibility(View.GONE);
                text2.setVisibility(View.GONE);

                // Show "Please wait..." UI with animation
                Animation fadeIn = AnimationUtils.loadAnimation(Splash.this, R.anim.fade_in);
                pleaseWaitContainer.startAnimation(fadeIn);
                pleaseWaitContainer.setVisibility(View.VISIBLE);


                // Start verification
                FirebaseAuth auth = FirebaseAuth.getInstance();
                if (auth.getCurrentUser() == null) {
                    auth.signInAnonymously().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            sessionManager.checkSession();
                        } else {
                            finish();
                        }
                    });
                } else {
                    sessionManager.checkSession();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // Nothing here
            }
        });

        center.startAnimation(fade_in_out);
        text1.startAnimation(fade_in_out);
        text2.startAnimation(fade_in_out);
    }



}
