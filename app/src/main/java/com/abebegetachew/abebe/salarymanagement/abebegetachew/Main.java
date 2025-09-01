package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.graphics.Outline;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class Main extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.view_pager);
        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setItemIconTintList(null);

        MainPagerAdapter adapter = new MainPagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(adapter.getItemCount());
        viewPager.setOverScrollMode(ViewPager2.OVER_SCROLL_NEVER);

        final float cornerRadiusPx = dpToPx(30);

        // Page transformer for scaling, alpha, and rounded corners
        CompositePageTransformer transformer = new CompositePageTransformer();
        transformer.addTransformer((page, position) -> {
            page.setCameraDistance(20000);
            if (position < -1f || position > 1f) {
                page.setAlpha(0f);
                page.setScaleX(0.85f);
                page.setScaleY(0.85f);
                removeRoundedCorners(page);
            } else {
                float scale = 0.85f + (1 - Math.abs(position)) * 0.15f;
                page.setScaleX(scale);
                page.setScaleY(scale);
                float alpha = 0.5f + (1 - Math.abs(position)) * 0.5f;
                page.setAlpha(alpha);

                if (Math.abs(position) > 0f) setRoundedCorners(page, cornerRadiusPx);
                else removeRoundedCorners(page);
            }
        });
        viewPager.setPageTransformer(transformer);

        // Initialize pages AFTER layout
        viewPager.post(() -> initializePages(viewPager, cornerRadiusPx));

        // Page change callbacks
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                bottomNav.setSelectedItemId(getMenuIdByPosition(position));
                animateMenuItem(getMenuIdByPosition(position));
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                animateMenuIconsDuringScroll(position, positionOffset);
            }
        });

        // Bottom nav click
        bottomNav.setOnItemSelectedListener(item -> {
            int pos = getPositionByMenuId(item.getItemId());
            if (pos != -1) {
                viewPager.setCurrentItem(pos, true);
                return true;
            }
            return false;
        });

        bottomNav.post(() -> animateMenuItem(bottomNav.getSelectedItemId()));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });
    }

    private void initializePages(ViewPager2 viewPager, float cornerRadiusPx) {
        ViewGroup recyclerView = (ViewGroup) viewPager.getChildAt(0);
        if (recyclerView == null) return;
        recyclerView.post(() -> {
            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                View page = recyclerView.getChildAt(i);
                if (i == viewPager.getCurrentItem()) {
                    page.setScaleX(1f);
                    page.setScaleY(1f);
                    page.setAlpha(1f);
                    removeRoundedCorners(page);
                } else {
                    page.setScaleX(0.85f);
                    page.setScaleY(0.85f);
                    page.setAlpha(0.5f);
                    setRoundedCorners(page, cornerRadiusPx);
                }
            }
        });
    }

    private int getMenuIdByPosition(int position) {
        switch (position) {
            case 0: return R.id.nav_home;
            case 1: return R.id.nav_debts;
            case 2: return R.id.nav_expenses;
            case 3: return R.id.nav_notes;
            case 4: return R.id.nav_settings;
            default: return R.id.nav_home;
        }
    }

    /** Smoothly sets current page programmatically */
    public void setCurrentPage(int position) {
        if (viewPager != null) {
            viewPager.setCurrentItem(position, true);
        }
    }

    private int getPositionByMenuId(int menuId) {
        if (menuId == R.id.nav_home) return 0;
        else if (menuId == R.id.nav_debts) return 1;
        else if (menuId == R.id.nav_expenses) return 2;
        else if (menuId == R.id.nav_notes) return 3;
        else if (menuId == R.id.nav_settings) return 4;
        else return -1;
    }


    private void animateMenuItem(int selectedItemId) {
        bottomNav.post(() -> {
            View menuView = bottomNav.getChildAt(0);
            if (!(menuView instanceof ViewGroup)) return;
            ViewGroup menuViewGroup = (ViewGroup) menuView;
            for (int i = 0; i < menuViewGroup.getChildCount(); i++) {
                View itemView = menuViewGroup.getChildAt(i);
                int itemId = bottomNav.getMenu().getItem(i).getItemId();
                itemView.setPivotX(itemView.getWidth() / 2f);
                itemView.setPivotY(itemView.getHeight() / 2f);
                if (itemId == selectedItemId) {
                    itemView.animate().scaleX(1.2f).scaleY(1.2f).alpha(1f).setDuration(250).start();
                } else {
                    itemView.animate().scaleX(1f).scaleY(1f).alpha(0.6f).setDuration(250).start();
                }
            }
        });
    }

    private void animateMenuIconsDuringScroll(int position, float positionOffset) {
        View menuView = bottomNav.getChildAt(0);
        if (!(menuView instanceof ViewGroup)) return;
        ViewGroup menuViewGroup = (ViewGroup) menuView;
        int itemCount = bottomNav.getMenu().size();
        if (position < 0 || position >= itemCount - 1) return;
        for (int i = 0; i < menuViewGroup.getChildCount(); i++) {
            View itemView = menuViewGroup.getChildAt(i);
            itemView.setPivotX(itemView.getWidth() / 2f);
            itemView.setPivotY(itemView.getHeight() / 2f);
            if (i == position) {
                float scale = 1.2f - 0.2f * positionOffset;
                float alpha = 1f - 0.4f * positionOffset;
                itemView.setScaleX(scale);
                itemView.setScaleY(scale);
                itemView.setAlpha(alpha);
            } else if (i == position + 1) {
                float scale = 1f + 0.2f * positionOffset;
                float alpha = 0.6f + 0.4f * positionOffset;
                itemView.setScaleX(scale);
                itemView.setScaleY(scale);
                itemView.setAlpha(alpha);
            } else {
                itemView.setScaleX(1f);
                itemView.setScaleY(1f);
                itemView.setAlpha(0.6f);
            }
        }
    }

    private float dpToPx(int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private void setRoundedCorners(View view, float radius) {
        view.setClipToOutline(true);
        view.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline outline) {
                outline.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
            }
        });
    }

    private void removeRoundedCorners(View view) {
        view.setClipToOutline(false);
        view.setOutlineProvider(null);
    }


}
