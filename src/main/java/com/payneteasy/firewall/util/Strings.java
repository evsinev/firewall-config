package com.payneteasy.firewall.util;

public class Strings {

    public static boolean hasText(String aString) {
        if(aString == null || aString.length() == 0) {
            return false;
        }

        return aString.trim().length() != 0;
    }

    public static boolean isEmpty(String aString) {
        return !hasText(aString);
    }
}
