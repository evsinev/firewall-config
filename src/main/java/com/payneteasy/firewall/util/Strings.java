package com.payneteasy.firewall.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

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

    public static String first(String ... args) {
        if(args == null) return null;
        for (String arg : args) {
            if (hasText(arg)) {
                return arg;
            }
        }
        return null;
    }

    public static String[] split(String aText, char ... aDelimiters) {
        StringTokenizer st = new StringTokenizer(aText, new String(aDelimiters));
        List<String> list = new ArrayList<>();
        while(st.hasMoreTokens()) {
            list.add(st.nextToken());
        }
        return list.toArray(new String[0]);
    }
}
