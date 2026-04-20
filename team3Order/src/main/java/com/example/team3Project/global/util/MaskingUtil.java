package com.example.team3Project.global.util;

public class MaskingUtil {

    public static String maskCardNumber(String cardNumber) {

        return cardNumber.substring(0,4) +
                "-****-****-" +
                cardNumber.substring(cardNumber.length()-4);
    }

    public static String maskCvc() {
        return "***";
    }

    public static String maskExpiry() {
        return "**/**";
    }
}
