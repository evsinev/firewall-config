package com.payneteasy.firewall.util;

public class Printer {
    public static void out() {
        System.out.println();
    }

    public static void out(String aText) {
        System.out.println(aText);
    }

    public static void out(String aFormat, Object... args) {
        System.out.println(String.format(aFormat, args));
    }
}
