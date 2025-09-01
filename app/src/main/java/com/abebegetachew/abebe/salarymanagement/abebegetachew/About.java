package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.os.Bundle;
import android.os.Handler;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.TransitionSet;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class About extends AppCompatActivity {

    private final Handler handler = new Handler();
    ImageButton backbtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // enable window content transitions before setContentView
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);

        // build a nice Slide + Fade enter transition set
        Slide slide = new Slide(Gravity.BOTTOM);
        slide.setDuration(420);
        slide.setStartDelay(80);

        Fade fade = new Fade();
        fade.setDuration(360);
        fade.setStartDelay(0);

        TransitionSet enter = new TransitionSet();
        enter.addTransition(slide);
        enter.addTransition(fade);
        enter.setOrdering(TransitionSet.ORDERING_TOGETHER);

        getWindow().setEnterTransition(enter);

        // a subtle return transition
        Fade exitFade = new Fade();
        exitFade.setDuration(250);
        getWindow().setReturnTransition(exitFade);

        super.onCreate(savedInstanceState);

        // edge-to-edge (your helper)
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_about);

        backbtn = findViewById(R.id.backButton);

        backbtn.setOnClickListener(v -> finish());

        // Staggered animations for the visible parts
        runStaggeredAnimations();
    }

    private void runStaggeredAnimations() {
        int[] ids = new int[]{
                R.id.appTitle,
                R.id.appTitle2,
                R.id.trailerLine,
                R.id.intro,
                R.id.heroPunch,
                R.id.superpowersHeader,
                R.id.superpowersBody1,
                R.id.superpowersBody2,
                R.id.superpowersBody3,
                R.id.superpowersBody4,
                R.id.superpowersBody5,
                R.id.superpowersBody6,
                R.id.audienceHeader,
                R.id.audienceBody,
                R.id.whyHeader,
                R.id.whyBody1,
                R.id.whyBody2,
                R.id.experienceHeader,
                R.id.experienceBody1,
                R.id.experienceBody2,
                R.id.finalWordHeader,
                R.id.finalWordBody,
                R.id.btnGetStarted,
                R.id.chipImport,
                R.id.chipSecure
        };

        int baseDelay = 120;
        int i = 0;

        for (int id : ids) {
            final View v = findViewById(id);
            if (v == null) { i++; continue; }

            v.setTranslationY(40f);
            v.setAlpha(0f);
            v.setVisibility(View.VISIBLE);

            long delay = (long) baseDelay * i;

            v.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setStartDelay(delay)
                    .setDuration(300)
                    .start();

            i++;
        }

        // CTA button pop + continuous pulse
        final View cta = findViewById(R.id.btnGetStarted);
        if (cta != null) {
            long totalDelay = baseDelay * ids.length;
            cta.postDelayed(() -> {
                // Initial pop
                cta.animate()
                        .scaleX(1.05f)
                        .scaleY(1.05f)
                        .setDuration(150)
                        .withEndAction(() -> cta.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(150)
                                .withEndAction(() -> startPulse(cta))
                                .start())
                        .start();
            }, totalDelay);
        }
    }

    // Helper method for infinite subtle pulse
    private void startPulse(View view) {
        view.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(600)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(600)
                        .withEndAction(() -> startPulse(view))
                        .start())
                .start();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
