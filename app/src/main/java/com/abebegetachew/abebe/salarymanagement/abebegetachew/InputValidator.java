package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import android.util.Patterns;

public class InputValidator {

    public static boolean isValidUsername(String name) {
        return name.length()>=6;
    }
    public static boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPhone(String phone) {
        // Accepts formats like +2519XXXXXXXX or 09XXXXXXXX
        return phone != null && phone.matches("^(\\+2519\\d{8}|09\\d{8})$");
    }

    public static boolean isValidPassword(String password) {
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,32}$");
    }

    public static boolean isPasswordEmpty(String password) {
        return password.isEmpty();
    }

    public static boolean isEmailEmpty(String email) {
        return email.isEmpty();
    }
}

