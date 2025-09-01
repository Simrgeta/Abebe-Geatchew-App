package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

public class IncomeExpenseViewModel extends AndroidViewModel {

    private final DatabaseHelper db;
    private final MutableLiveData<Double> totalIncomeLive = new MutableLiveData<>();
    private final MutableLiveData<Double> totalExpenseLive = new MutableLiveData<>();
    private final MutableLiveData<Double> netIncomeLive = new MutableLiveData<>();

    public IncomeExpenseViewModel(@NonNull Application application) {
        super(application);
        db = new DatabaseHelper(application);

        // Whenever database changes, recalculate totals
        db.setOnDatabaseChangedListener(this::calculateTotals);

        // Initial calculation
        calculateTotals();
    }

    public LiveData<Double> getTotalIncome() {
        return totalIncomeLive;
    }

    public LiveData<Double> getTotalExpense() {
        return totalExpenseLive;
    }

    public LiveData<Double> getNetIncome() {
        return netIncomeLive;
    }

    // Calculate totals for incomes, expenses, and net income
    public void calculateTotals() {
        List<Income> incomes = db.getAllIncomes();
        List<Expense> expenses = db.getAllExpenses();

        double totalIncome = 0;
        for (Income income : incomes) {
            totalIncome += income.getAmount();
        }

        double totalExpense = 0;
        for (Expense expense : expenses) {
            totalExpense += expense.getAmount();
        }

        double netIncome = totalIncome - totalExpense;

        totalIncomeLive.postValue(totalIncome);
        totalExpenseLive.postValue(totalExpense);
        netIncomeLive.postValue(netIncome);
    }
}
