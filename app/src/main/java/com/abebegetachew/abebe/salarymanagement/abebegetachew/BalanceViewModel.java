package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

public class BalanceViewModel extends AndroidViewModel {

    private final DebtsViewModel debtsViewModel;
    private final IncomeExpenseViewModel incomeExpenseViewModel;

    private final MediatorLiveData<Double> balanceLive = new MediatorLiveData<>();

    public BalanceViewModel(@NonNull Application application,
                            DebtsViewModel debtsViewModel,
                            IncomeExpenseViewModel incomeExpenseViewModel) {
        super(application);
        this.debtsViewModel = debtsViewModel;
        this.incomeExpenseViewModel = incomeExpenseViewModel;

        // Observe net income
        balanceLive.addSource(incomeExpenseViewModel.getNetIncome(), netIncome ->
                calculateBalance(netIncome, debtsViewModel.getTotalDebt().getValue())
        );

        // Observe total debt
        balanceLive.addSource(debtsViewModel.getTotalDebt(), totalDebt ->
                calculateBalance(incomeExpenseViewModel.getNetIncome().getValue(), totalDebt)
        );
    }

    public LiveData<Double> getBalance() {
        return balanceLive;
    }

    private void calculateBalance(Double netIncome, Double totalDebt) {
        if (netIncome == null) netIncome = 0.0;
        if (totalDebt == null) totalDebt = 0.0;

        double balance = netIncome - totalDebt;
        balanceLive.postValue(balance);
    }
}
