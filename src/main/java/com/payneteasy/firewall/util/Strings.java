package com.payneteasy.firewall.util;

import com.payneteasy.firewall.dao.model.THost;

import java.util.Collection;

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

    public static <T> int maxLength(Collection<T> aList, IGetText<T> aText) {
        int max = 0;
        for (T obj : aList) {
            String text = aText.getText(obj);
            if(text.length() > max) {
                max = text.length();
            }
        }
        return max;
    }

    public static String padRight(String aText, int aLength) {
        if(aText.length() == aLength) {
            return aText;
        }
        StringBuilder sb = new StringBuilder(aLength);
        sb.append(aText);
        while(sb.length() < aLength) {
            sb.append(' ');
        }
        return sb.toString();
    }

    public interface IGetText<T> {
        String getText(T aObj);
    }
}
