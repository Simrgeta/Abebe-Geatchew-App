package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import static android.graphics.Typeface.BOLD;
import static android.graphics.Typeface.ITALIC;

public class Help extends BaseActivity {

    private LinearLayout faqContent;
    private ImageView ivFAQArrow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_help);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        faqContent = findViewById(R.id.faqContent);
        ivFAQArrow = findViewById(R.id.ivFAQArrow);
        LinearLayout faqHeader = findViewById(R.id.faqHeader);
        faqHeader.setOnClickListener(v -> toggleFAQ());

        setupRichFAQ();
        animateCards();
    }

    // 🌟 Toggle FAQ section
    private void toggleFAQ() {
        if (faqContent.getVisibility() == View.GONE) {
            faqContent.setVisibility(View.VISIBLE);
            ObjectAnimator.ofFloat(ivFAQArrow, "rotation", 0f, 180f).setDuration(300).start();
            fadeIn(faqContent);
        } else {
            ObjectAnimator.ofFloat(ivFAQArrow, "rotation", 180f, 0f).setDuration(300).start();
            fadeOut(faqContent);
        }
    }

    private void fadeIn(View view) {
        view.setAlpha(0f);
        view.animate()
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(null);
    }

    private void fadeOut(final View view) {
        view.animate()
                .alpha(0f)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(View.GONE);
                    }
                });
    }

    // 🌟 Animate cards with scale + translation
    private void animateCards() {
        ScrollView scrollView = findViewById(R.id.scrollHelp);
        LinearLayout container = (LinearLayout) scrollView.getChildAt(0);

        for (int i = 0; i < container.getChildCount(); i++) {
            View card = container.getChildAt(i);
            card.setTranslationY(200f);
            card.setAlpha(0f);
            card.setScaleX(0.95f);
            card.setScaleY(0.95f);

            card.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setStartDelay(i * 150L)
                    .setDuration(500)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }
    }

    // 🌟 Setup bold/italic/colors in FAQ
    private void setupRichFAQ() {
        TextView q1 = (TextView) faqContent.getChildAt(0);
        q1.setText(formatFAQ("Q1: Why can’t I log in?", "Check your internet connection and try again."));

        TextView q2 = (TextView) faqContent.getChildAt(1);
        q2.setText(formatFAQ("Q2: How do I reset my password?", "Use the 'Forgot Password' option on login screen."));

        TextView q3 = (TextView) faqContent.getChildAt(2);
        q3.setText(formatFAQ("Q3: Can I use the app offline?", "Yes, core features work offline but cloud sync requires internet."));
    }

    private SpannableString formatFAQ(String question, String answer) {
        String full = question + "\n" + answer;
        SpannableString spannable = new SpannableString(full);

        // Bold question
        spannable.setSpan(new StyleSpan(BOLD), 0, question.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        // Italic answer
        spannable.setSpan(new StyleSpan(ITALIC), question.length() + 1, full.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        // Color the question
        spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#09DCE3")), 0, question.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannable;
    }
}
