package com.example.team3Project.domain.settlement.util;

public final class CardNumberUtil {

    private CardNumberUtil() {
    }

    public static String last4(String cardNumber) {
        if (cardNumber == null || cardNumber.isBlank()) {
            return null;
        }

        String digits = cardNumber.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }

        if (digits.length() <= 4) {
            return digits;
        }

        return digits.substring(digits.length() - 4);
    }
}

