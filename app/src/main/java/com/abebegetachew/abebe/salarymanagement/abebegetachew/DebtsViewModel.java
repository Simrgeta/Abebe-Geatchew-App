package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

public class DebtsViewModel extends AndroidViewModel {
    private final DatabaseHelper db;
    private final MutableLiveData<Double> totalDebtLive = new MutableLiveData<>();

    public DebtsViewModel(@NonNull Application application) {
        super(application);
        db = new DatabaseHelper(application);

        db.setOnDatabaseChangedListener(this::calculateTotalDebt);

        calculateTotalDebt();
    }

    public LiveData<Double> getTotalDebt() {
        return totalDebtLive;
    }

    // Refresh whenever debts change
    public void calculateTotalDebt() {
        List<Debt> unpaidDebts = db.getUnpaidDebts();
        double totalDebt = 0;
        for (Debt debt : unpaidDebts) {
            totalDebt += debt.getRemainingDebt();
        }
        totalDebtLive.postValue(totalDebt);
    }
}
