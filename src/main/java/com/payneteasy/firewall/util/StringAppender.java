package com.payneteasy.firewall.util;

public class StringAppender {
    private final StringBuilder sb = new StringBuilder();
    private final String delimiter;

    public StringAppender(String aDelimiter) {
        delimiter = aDelimiter;
    }

    public StringAppender append(String aText) {
        if(sb.length() > 0) {
            sb.append(delimiter);
        }
        sb.append(aText);
        return this;
    }

    @Override
    public String toString() {
        return sb.toString();
    }

    public String toStringFailIfEmpty(String aErrorMessage) {
        String text = toString().trim();
        if(text.length() == 0 ) {
            throw new IllegalStateException(aErrorMessage);
        }
        return text;
    }
}
