package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "DebtsDB";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "Debts";

    // Column names
    public static final String COL_ID = "DebtID";
    public static final String COL_BORROWER = "Borrower";
    public static final String COL_AMOUNT_OWED = "AmountOwed";
    public static final String COL_AMOUNT_PAID = "AmountPaid";
    public static final String COL_REMAINING = "RemainingDebt";
    public static final String COL_REASON = "Reason";
    public static final String COL_DATE = "Date";

    // ✅ Income table columns
    public static final String INCOME_TABLE = "Income";
    public static final String INCOME_ID = "IncomeID";
    public static final String INCOME_AMOUNT = "Amount";
    public static final String INCOME_DATE = "Date";


    // ✅ Expense table columns
    public static final String EXPENSE_TABLE = "Expense";
    public static final String EXPENSE_ID = "ExpenseID";
    public static final String EXPENSE_REASON = "Reason";
    public static final String EXPENSE_AMOUNT = "Amount";
    public static final String EXPENSE_DATE = "Date";

    // ✅ Notes table columns
    public static final String NOTES_TABLE = "Notes";
    public static final String NOTE_DESCRIPTION = "Description";
    public static final String NOTE_AMOUNT = "Amount";
    public static final String NOTE_DATE = "Date";


    public interface OnDatabaseChangedListener {
        void onDatabaseChanged();
    }
    private OnDatabaseChangedListener listener;

    public void setOnDatabaseChangedListener(OnDatabaseChangedListener listener) {
        this.listener = listener;
    }

    private void notifyChange() {
        if (listener != null) listener.onDatabaseChanged();
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " TEXT PRIMARY KEY, " +
                COL_BORROWER + " TEXT, " +
                COL_AMOUNT_OWED + " REAL, " +
                COL_AMOUNT_PAID + " REAL, " +
                COL_REMAINING + " REAL, " +
                COL_REASON + " TEXT, " +
                COL_DATE + " TEXT)";
        db.execSQL(createTable);

        // RepaidDebts table with unique RepaidID
        db.execSQL("CREATE TABLE IF NOT EXISTS RepaidDebts (" +
                "RepaidID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "DebtID TEXT," +
                "Borrower TEXT," +
                "Amount REAL," +
                "RepaymentDate TEXT)");


        db.execSQL("CREATE TABLE IF NOT EXISTS Names (" +
                "NameID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "Name TEXT UNIQUE COLLATE NOCASE)");

        // ✅ Income table
        db.execSQL("CREATE TABLE IF NOT EXISTS " + INCOME_TABLE + " (" +
                INCOME_ID + " TEXT PRIMARY KEY, " +
                INCOME_AMOUNT + " REAL, " +
                INCOME_DATE + " TEXT)");


        db.execSQL("CREATE TABLE IF NOT EXISTS " + EXPENSE_TABLE + " (" +
                EXPENSE_ID + " TEXT PRIMARY KEY, " +
                EXPENSE_AMOUNT + " REAL, " +
                EXPENSE_REASON + " TEXT, " +
                EXPENSE_DATE + " TEXT, " +
                "Category TEXT)");


        db.execSQL("CREATE TABLE IF NOT EXISTS " + NOTES_TABLE + " (" +
                "NoteId INTEGER PRIMARY KEY AUTOINCREMENT," +
                NOTE_DESCRIPTION + " TEXT, " +
                NOTE_AMOUNT + " REAL, " +
                NOTE_DATE + " TEXT)");

    }

    // Inside DatabaseHelper class
    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_NAME, null, null);       // Debts
            db.delete("RepaidDebts", null, null);    // Repaid Debts
            db.delete("Names", null, null);          // Names
            db.delete(INCOME_TABLE, null, null);     // Income
            db.delete(EXPENSE_TABLE, null, null);    // Expenses
            db.delete(NOTES_TABLE, null, null);      // Notes

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        notifyChange(); // notify listeners so UI updates if needed
    }


    // ------------------ Notes-specific helper methods ------------------

    // 1️⃣ Get all unique dates from Notes table
    public List<String> getAllNoteDates() {
        List<String> dates = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT DISTINCT " + NOTE_DATE + " FROM " + NOTES_TABLE + " ORDER BY " + NOTE_DATE + " DESC",
                null
        );
        if (cursor.moveToFirst()) {
            do {
                dates.add(cursor.getString(cursor.getColumnIndexOrThrow(NOTE_DATE)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return dates;
    }

    // 2️⃣ Convert a note date string to milliseconds (for comparison)
    public long convertDateStringToMillis(String dateStr) {
        // Adjust the pattern to match your date format stored in DB, e.g., "yyyy-MM-dd"
        java.text.SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        sdf.setLenient(false);
        try {
            java.util.Date date = sdf.parse(dateStr);
            if (date != null) return date.getTime();
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }
        return 0; // fallback if parsing fails
    }



    // Delete note(s) by date
    public void deleteNoteByDate(String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(NOTES_TABLE, NOTE_DATE + " = ?", new String[]{date});
        if (rowsDeleted > 0) notifyChange();
    }



    // ✅ Insert new note entry
    public long insertNote(String description, double amount, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(NOTE_DESCRIPTION, description);
        cv.put(NOTE_AMOUNT, amount);
        cv.put(NOTE_DATE, date);
        long result = db.insert(NOTES_TABLE, null, cv);

        if (result != -1) notifyChange();
        return result;
    }


    public List<Note> getAllNotes() {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + NOTES_TABLE + " ORDER BY " + NOTE_DATE + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                String desc = cursor.getString(cursor.getColumnIndexOrThrow(NOTE_DESCRIPTION));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(NOTE_AMOUNT));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(NOTE_DATE));

                notes.add(new Note(desc, amount, date));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return notes;
    }



    // ✅ Insert new income entry
    public long insertIncome(String incomeId, double amount, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(INCOME_ID, incomeId);
        cv.put(INCOME_AMOUNT, amount);
        cv.put(INCOME_DATE, date);
        long result = db.insert(INCOME_TABLE, null, cv);

        if (result != -1) notifyChange();
        return result;
    }

    // ✅ Insert new income entry
    public long insertExpense(String expenseId, double amount, String reason, String date, String category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(EXPENSE_ID, expenseId);
        cv.put(EXPENSE_AMOUNT, amount);
        cv.put(EXPENSE_REASON, reason);
        cv.put(EXPENSE_DATE, date);
        cv.put("Category", category);
        long result = db.insert(EXPENSE_TABLE, null, cv);

        if (result != -1) notifyChange();
        return result;
    }

    // Get last N debts
    public List<Debt> getLastDebts(int limit) {
        List<Debt> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COL_DATE + " DESC LIMIT ?",
                new String[]{String.valueOf(limit)}
        );

        if (cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(COL_ID));
                String borrower = cursor.getString(cursor.getColumnIndexOrThrow(COL_BORROWER));
                double amountOwed = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT_OWED));
                double amountPaid = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT_PAID));
                double remainingDebt = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_REMAINING));
                String reason = cursor.getString(cursor.getColumnIndexOrThrow(COL_REASON));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE));

                list.add(new Debt(-1, id, borrower, amountOwed, amountPaid, remainingDebt, reason, date));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // Get last N expenses grouped by category
    public List<Expense> getLastExpensesByCategory(int limit) {
        List<Expense> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Group by category and take latest expenses
        String query = "SELECT * FROM " + EXPENSE_TABLE +
                " ORDER BY " + EXPENSE_DATE + " DESC LIMIT ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(limit)});

        if (cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(EXPENSE_ID));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(EXPENSE_AMOUNT));
                String reason = cursor.getString(cursor.getColumnIndexOrThrow(EXPENSE_REASON));
                String category = cursor.getString(cursor.getColumnIndexOrThrow("Category"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(EXPENSE_DATE));

                list.add(new Expense(id, amount, reason, date, category));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }




    // ------------------ New methods to add at the end of your DatabaseHelper ------------------

    // 1️⃣ Get all unique dates from Income and Expense tables
    public List<String> getAllDates() {
        List<String> dates = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT Date FROM " + INCOME_TABLE + " UNION SELECT Date FROM " + EXPENSE_TABLE + " ORDER BY Date DESC",
                null
        );
        if (cursor.moveToFirst()) {
            do {
                dates.add(cursor.getString(cursor.getColumnIndexOrThrow("Date")));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return dates;
    }

    // 2️⃣ Get all incomes for a specific date
    // Get all incomes for a specific date
    public List<Income> getIncomesByDate(String date) {
        List<Income> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + INCOME_TABLE + " WHERE Date = ?",
                new String[]{date}
        );
        if (cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(INCOME_ID));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(INCOME_AMOUNT));
                String d = cursor.getString(cursor.getColumnIndexOrThrow(INCOME_DATE));
                list.add(new Income(id, amount, d));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // Get all expenses for a specific date
    public List<Expense> getExpensesByDate(String date) {
        List<Expense> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + EXPENSE_TABLE + " WHERE Date = ?",
                new String[]{date}
        );
        if (cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(EXPENSE_ID));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(EXPENSE_AMOUNT));
                String reason = cursor.getString(cursor.getColumnIndexOrThrow(EXPENSE_REASON));
                String category = cursor.getString(cursor.getColumnIndexOrThrow("Category"));
                String d = cursor.getString(cursor.getColumnIndexOrThrow(EXPENSE_DATE));
                list.add(new Expense(id, amount, reason, d, category));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // Delete all incomes and expenses for a specific date
    public void deleteByDate(String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(INCOME_TABLE, "Date = ?", new String[]{date});
            db.delete(EXPENSE_TABLE, "Date = ?", new String[]{date});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        notifyChange();
    }

    // Get all incomes from the Income table
    public List<Income> getAllIncomes() {
        List<Income> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + INCOME_TABLE + " ORDER BY " + INCOME_DATE + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(INCOME_ID));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(INCOME_AMOUNT));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(INCOME_DATE));
                list.add(new Income(id, amount, date));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // ✅ Insert borrower name if not exists (case-insensitive)
    public void insertNameIfNotExists(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Check if name already exists (case-insensitive)
        Cursor cursor = db.rawQuery("SELECT Name FROM Names WHERE Name = ? COLLATE NOCASE", new String[]{name});
        if(!cursor.moveToFirst()) {
            ContentValues cv = new ContentValues();
            cv.put("Name", name);
            db.insert("Names", null, cv);
        }
        cursor.close();
    }

    public List<Debt> getFullyPaidDebts() {
        List<Debt> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT d.DebtID, d.Borrower, d.AmountOwed, d.AmountPaid, d.Reason, d.Date " +
                "FROM " + TABLE_NAME + " d " +
                "WHERE d.RemainingDebt = 0 " +
                "ORDER BY d.Date DESC";

        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow("DebtID"));
                String borrower = cursor.getString(cursor.getColumnIndexOrThrow("Borrower"));
                double amountOwed = cursor.getDouble(cursor.getColumnIndexOrThrow("AmountOwed"));
                double amountPaid = cursor.getDouble(cursor.getColumnIndexOrThrow("AmountPaid"));
                String reason = cursor.getString(cursor.getColumnIndexOrThrow("Reason"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("Date"));

                list.add(new Debt(-1, id, borrower, amountOwed, amountPaid, 0, reason, date));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return list;
    }

    public List<Debt> getPaidDebts() {
        List<Debt> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT r.RepaidID, r.DebtID, r.Borrower, r.Amount AS AmountPaid, r.RepaymentDate, " +
                "d.Reason, d.AmountOwed AS AmountOwed " +
                "FROM RepaidDebts r " +
                "LEFT JOIN Debts d ON r.DebtID = d.DebtID " +
                "ORDER BY r.RepaymentDate DESC";

        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                int repaidId = cursor.getInt(cursor.getColumnIndexOrThrow("RepaidID"));
                String debtId = cursor.getString(cursor.getColumnIndexOrThrow("DebtID"));
                String borrower = cursor.getString(cursor.getColumnIndexOrThrow("Borrower"));
                double amountPaid = cursor.getDouble(cursor.getColumnIndexOrThrow("AmountPaid"));
                double amountOwed = cursor.getDouble(cursor.getColumnIndexOrThrow("AmountOwed"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("RepaymentDate"));
                String reason = cursor.getString(cursor.getColumnIndexOrThrow("Reason"));

                list.add(new Debt(repaidId, debtId, borrower, amountOwed, amountPaid, 0, reason, date));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return list;
    }

    public List<Debt> getDebtsByBorrower(String borrower) {
        List<Debt> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_NAME + " WHERE Borrower LIKE ? COLLATE NOCASE AND RemainingDebt > 0",
                new String[]{borrower}
        );

        if (cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(COL_ID));
                String bName = cursor.getString(cursor.getColumnIndexOrThrow(COL_BORROWER));
                double amountOwed = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT_OWED));
                double amountPaid = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT_PAID));
                double remainingDebt = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_REMAINING));
                String reason = cursor.getString(cursor.getColumnIndexOrThrow(COL_REASON));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE));

                list.add(new Debt(-1, id, bName, amountOwed, amountPaid, remainingDebt, reason, date));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    /**
     * Add repayment and return status:
     *  1 = success
     * -1 = debt ID not found
     * -2 = repayment exceeds remaining debt
     */
    public int addRepayment(String debtId, double amount, String repaymentDate) {
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.rawQuery("SELECT Borrower, AmountPaid, RemainingDebt FROM Debts WHERE DebtID = ?", new String[]{debtId});
        if (!cursor.moveToFirst()) {
            cursor.close();
            return -1;
        }

        String borrower = cursor.getString(cursor.getColumnIndexOrThrow("Borrower"));
        double amountPaid = cursor.getDouble(cursor.getColumnIndexOrThrow("AmountPaid"));
        double remainingDebt = cursor.getDouble(cursor.getColumnIndexOrThrow("RemainingDebt"));
        cursor.close();

        if (amount > remainingDebt) return -2;

        // Update debts table
        amountPaid += amount;
        remainingDebt -= amount;
        ContentValues cv = new ContentValues();
        cv.put("AmountPaid", amountPaid);
        cv.put("RemainingDebt", remainingDebt);
        db.update("Debts", cv, "DebtID = ?", new String[]{debtId});

        // Insert into RepaidDebts
        ContentValues cv2 = new ContentValues();
        cv2.put("DebtID", debtId);
        cv2.put("Borrower", borrower);
        cv2.put("Amount", amount);
        cv2.put("RepaymentDate", repaymentDate);
        db.insert("RepaidDebts", null, cv2);

        notifyChange();
        return 1;
    }

    // Delete a specific repayment by RepaidID
    public int deleteRepaidDebtById(int repaidId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete("RepaidDebts", "RepaidID = ?", new String[]{String.valueOf(repaidId)});
        if (rowsDeleted > 0) notifyChange();
        return rowsDeleted;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS RepaidDebts");
        db.execSQL("DROP TABLE IF EXISTS Names");
        db.execSQL("DROP TABLE IF EXISTS " + INCOME_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + EXPENSE_TABLE);
        onCreate(db);
    }

    public long insertDebt(String debtID, String borrower, double amountOwed, double amountPaid, double remainingDebt, String reason, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_ID, debtID);
        cv.put(COL_BORROWER, borrower);
        cv.put(COL_AMOUNT_OWED, amountOwed);
        cv.put(COL_AMOUNT_PAID, amountPaid);
        cv.put(COL_REMAINING, remainingDebt);
        cv.put(COL_REASON, reason);
        cv.put(COL_DATE, date);
        long result = db.insert(TABLE_NAME, null, cv);

        notifyChange();
        return result;
    }

    public double getRemainingDebt(String debtId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT RemainingDebt FROM Debts WHERE DebtID = ?", new String[]{debtId});
        double remaining = 0;
        if (cursor.moveToFirst()) {
            remaining = cursor.getDouble(cursor.getColumnIndexOrThrow("RemainingDebt"));
        }
        cursor.close();
        return remaining;
    }

    /**
     * Deletes a fully paid debt from both Debts and RepaidDebts tables using DebtID.
     *
     * @param debtId The ID of the debt to delete
     */
    public void deleteFullyPaidDebtById(String debtId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        int rowsDeleted = 0;
        try {
            // Delete from RepaidDebts first
            db.delete("RepaidDebts", "DebtID = ?", new String[]{debtId});
            // Delete from Debts table
            rowsDeleted = db.delete(TABLE_NAME, "DebtID = ? AND RemainingDebt = 0", new String[]{debtId});

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (rowsDeleted > 0) notifyChange();
    }


    public List<Debt> getUnpaidDebts() {
        List<Debt> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM Debts WHERE RemainingDebt > 0", null);
        if (cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow("DebtID"));
                String borrower = cursor.getString(cursor.getColumnIndexOrThrow("Borrower"));
                double amountOwed = cursor.getDouble(cursor.getColumnIndexOrThrow("AmountOwed"));
                double amountPaid = cursor.getDouble(cursor.getColumnIndexOrThrow("AmountPaid"));
                double remainingDebt = cursor.getDouble(cursor.getColumnIndexOrThrow("RemainingDebt"));
                String reason = cursor.getString(cursor.getColumnIndexOrThrow("Reason"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("Date"));

                list.add(new Debt(-1, id, borrower, amountOwed, amountPaid, remainingDebt, reason, date));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public ArrayList<String> getBorrowerSuggestions(String query) {
        ArrayList<String> names = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // ✅ Fetch suggestions from Names table (case-insensitive)
        Cursor cursor = db.rawQuery(
                "SELECT Name FROM Names WHERE Name LIKE ? COLLATE NOCASE ORDER BY Name ASC",
                new String[]{"%" + query + "%"}
        );

        if (cursor.moveToFirst()) {
            do {
                names.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return names;
    }
    // Get last N expenses (latest first)
    public List<Expense> getLastExpenses(int limit) {
        List<Expense> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + EXPENSE_TABLE +
                " ORDER BY " + EXPENSE_DATE + " DESC LIMIT ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(limit)});
        if (cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(EXPENSE_ID));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(EXPENSE_AMOUNT));
                String reason = cursor.getString(cursor.getColumnIndexOrThrow(EXPENSE_REASON));
                String category = cursor.getString(cursor.getColumnIndexOrThrow("Category"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(EXPENSE_DATE));

                list.add(new Expense(id, amount, reason, date, category));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // Get all debts (no limit)
    public List<Debt> getAllDebts() {
        List<Debt> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_NAME + " WHERE " + COL_REMAINING + " > 0" + " ORDER BY " + COL_DATE + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(COL_ID));
                String borrower = cursor.getString(cursor.getColumnIndexOrThrow(COL_BORROWER));
                double amountOwed = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT_OWED));
                double amountPaid = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT_PAID));
                double remainingDebt = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_REMAINING));
                String reason = cursor.getString(cursor.getColumnIndexOrThrow(COL_REASON));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE));

                list.add(new Debt(-1, id, borrower, amountOwed, amountPaid, remainingDebt, reason, date));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // Get all expenses (no limit)
    public List<Expense> getAllExpenses() {
        List<Expense> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + EXPENSE_TABLE + " ORDER BY " + EXPENSE_DATE + " DESC",
                null
        );

        if (cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(EXPENSE_ID));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(EXPENSE_AMOUNT));
                String reason = cursor.getString(cursor.getColumnIndexOrThrow(EXPENSE_REASON));
                String category = cursor.getString(cursor.getColumnIndexOrThrow("Category"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(EXPENSE_DATE));

                list.add(new Expense(id, amount, reason, date, category));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

}
