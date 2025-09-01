package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.TransitionSet;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

public class DeveloperInfo extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);

        Slide slide = new Slide(Gravity.BOTTOM);
        slide.setDuration(400);

        Fade fade = new Fade();
        fade.setDuration(350);

        TransitionSet enter = new TransitionSet();
        enter.addTransition(slide);
        enter.addTransition(fade);
        enter.setOrdering(TransitionSet.ORDERING_TOGETHER);

        getWindow().setEnterTransition(enter);
        getWindow().setReturnTransition(new Fade().setDuration(250));

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_developer_info);

        // Fixed back button
        ImageButton back = findViewById(R.id.backButtonDev);
        back.setOnClickListener(v -> finish());

        // Animate all cards, texts, chips, CTA
        runStaggeredAnimations();

        // Animate chips
        animateChips();

        // Social icon click listeners
        findViewById(R.id.btnEmail).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:awashsimrgeta123@gmail.com"))));
        findViewById(R.id.btnLinkedIn).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://et.linkedin.com/in/simrgeta-awash-aba613201"))));
        findViewById(R.id.btnGitHub).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Simrgeta"))));

        // CTA button click
        MaterialButton cta = findViewById(R.id.btnPortfolio);
        cta.setOnClickListener(v ->{});
    }

    private void runStaggeredAnimations() {
        int[] ids = new int[]{
                R.id.devName,
                R.id.devName2,
                R.id.devTagline,
                R.id.devStoryHeader,
                R.id.devStoryBody,
                R.id.skillsHeader,
                R.id.chipSkills,
                R.id.contactHeader,
                R.id.socialIcons,
                R.id.btnPortfolio
        };

        int baseDelay = 120;
        int i = 0;

        for (int id : ids) {
            View v = findViewById(id);
            if (v == null) { i++; continue; }

            v.setTranslationY(60f);
            v.setAlpha(0f);
            v.setVisibility(View.VISIBLE);

            long delay = (long) baseDelay * i;
            v.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setStartDelay(delay)
                    .setDuration(400)
                    .start();
            i++;
        }

        // CTA button pop + continuous pulse after all animations
        final View cta = findViewById(R.id.btnPortfolio);
        if (cta != null) {
            long totalDelay = baseDelay * ids.length + 200;
            cta.postDelayed(() -> cta.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(150)
                    .withEndAction(() -> cta.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150)
                            .withEndAction(() -> startPulse(cta))
                            .start())
                    .start(), totalDelay);
        }
    }

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

    private void animateChips() {
        Chip[] chips = new Chip[]{
                findViewById(R.id.chipAndroid),
                findViewById(R.id.chipAspNet),
                findViewById(R.id.chipDjango),
                findViewById(R.id.chipUnity)
        };
        int baseDelay = 100;
        int i = 0;
        for (Chip chip : chips) {
            chip.setAlpha(0f);
            chip.setTranslationY(40f);
            chip.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay((long) baseDelay * i + 400) // after hero cards
                    .setDuration(350)
                    .start();
            i++;
        }
    }
}
