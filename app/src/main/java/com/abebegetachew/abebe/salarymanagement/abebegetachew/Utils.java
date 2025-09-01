package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import java.util.Random;

public class Utils {
    // Generate unique 8-digit ID
    public static String generateUniqueId() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < 8; i++) {
            sb.append(random.nextInt(10)); // 0-9
        }
        return sb.toString();
    }
}

