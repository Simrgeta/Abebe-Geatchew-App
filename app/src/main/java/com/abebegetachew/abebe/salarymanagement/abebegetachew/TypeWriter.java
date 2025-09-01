package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

public class TypeWriter extends AppCompatTextView {

    private CharSequence mText;
    private boolean isAnimating = false;
    private int mIndex;
    private long mDelay = 50;           // Delay per character
    private long mRestartDelay = 2000;  // Delay before restarting after marquee
    private float marqueeSpeed = 30f;   // pixels per second
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean marqueeRunning = false;

    public TypeWriter(Context context) { super(context); init(); }
    public TypeWriter(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        setSingleLine(true);
        setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);
        setMarqueeRepeatLimit(-1); // infinite loop
        setHorizontallyScrolling(true);
        setSelected(true);
    }

    private final Runnable typeRunner = new Runnable() {
        @Override
        public void run() {
            if (mText == null || marqueeRunning) {
                isAnimating = false;
                return;
            }

            if (mIndex <= mText.length()) {
                // Show next character
                CharSequence currentText = mText.subSequence(0, mIndex);
                setText(currentText);
                setSelected(true);

                // Measure text width
                float textWidth = getPaint().measureText(currentText.toString());

                // If text width exceeds view width → start marquee immediately
                if (textWidth >= getWidth()) {
                    setText(mText); // show full text
                    startMarquee();
                    return;
                }

                mIndex++;
                mHandler.postDelayed(this, mDelay);
            } else {
                // Text smaller than view → wait and restart
                mHandler.postDelayed(() -> {
                    mIndex = 0;
                    mHandler.post(typeRunner);
                }, mRestartDelay);
            }
        }
    };

    private void startMarquee() {
        marqueeRunning = true;

        // Ensure full text and marquee settings
        setText(mText);
        setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);
        setSelected(true);

        // Force layout so marquee recognizes width
        post(() -> {
            float textWidth = getPaint().measureText(mText.toString());
            long duration = (long) ((textWidth - getWidth()) / marqueeSpeed * 1000);
            if (duration < 0) duration = 0;

            // Restart typing after marquee finishes
            mHandler.postDelayed(() -> {
                marqueeRunning = false;
                mIndex = 0;
                setSelected(false); // stop marquee
                setText("");
                mHandler.postDelayed(typeRunner, mRestartDelay);
            }, duration + 500); // buffer to ensure scroll completes
        });
    }




    public void animateText(CharSequence text) {
        if (isAnimating && text.equals(mText)) return;

        stopAnimation();
        mText = text;
        mIndex = 0;
        setText("");
        marqueeRunning = false;
        isAnimating = true;
        mHandler.postDelayed(typeRunner, mDelay);
    }

    public void stopAnimation() {
        mHandler.removeCallbacksAndMessages(null);
        marqueeRunning = false;
        mIndex = 0;
        isAnimating = false;
        setText("");
    }

    // Optional setters
    public void setCharacterDelay(long millis) { mDelay = millis; }
    public void setRestartDelay(long millis) { mRestartDelay = millis; }
    public void setMarqueeSpeed(float speed) { marqueeSpeed = speed; }
}
