package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

public class RecentNoteDateViewModel extends AndroidViewModel {

    private final DatabaseHelper db;
    private final MutableLiveData<String> recentDateLive = new MutableLiveData<>();

    public RecentNoteDateViewModel(@NonNull Application application) {
        super(application);
        db = new DatabaseHelper(application);

        // Whenever database changes, recalculate recent date
        db.setOnDatabaseChangedListener(this::calculateRecentDate);

        // Initial calculation
        calculateRecentDate();
    }

    public LiveData<String> getRecentDate() {
        return recentDateLive;
    }

    // Calculate the most recent date compared to today
    public void calculateRecentDate() {
        List<String> allDates = db.getAllNoteDates(); // returns all note dates as Strings

        if (allDates == null || allDates.isEmpty()) {
            recentDateLive.postValue("No notes yet");
            return;
        }

        String mostRecent = allDates.get(0);
        long todayMillis = System.currentTimeMillis();

        for (String dateStr : allDates) {
            long noteMillis = db.convertDateStringToMillis(dateStr);
            long mostRecentMillis = db.convertDateStringToMillis(mostRecent);

            if (noteMillis <= todayMillis && noteMillis > mostRecentMillis) {
                mostRecent = dateStr;
            }
        }

        recentDateLive.postValue(mostRecent != null ? mostRecent : "No notes yet");
    }
}
