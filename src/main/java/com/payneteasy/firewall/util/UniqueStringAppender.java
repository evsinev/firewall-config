package com.payneteasy.firewall.util;

import java.util.HashSet;

public class UniqueStringAppender extends StringAppender {

    HashSet<String> uniques = new HashSet<>();

    public UniqueStringAppender(String aDelimiter) {
        super(aDelimiter);
    }

    @Override
    public StringAppender append(String aText) {
        if(uniques.contains(aText)) {
            throw new IllegalStateException("String "+aText+" already added");
        }
        uniques.add(aText);
        return super.append(aText);
    }
}
