package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MainPagerAdapter extends FragmentStateAdapter {

    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new HomeFragment();
            case 1: return new DebtsFragment();
            case 2: return new ExpensesFragment();
            case 3: return new NotesFragment();
            case 4: return new SettingsFragment();
            default: return new HomeFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 5; // number of tabs
    }
}
