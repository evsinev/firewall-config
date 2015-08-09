package com.payneteasy.firewall.util;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class MustacheFilePrinter {

    Mustache mustache;
    Map<String, Object> scope = new HashMap<>();

    public MustacheFilePrinter(String aTemplate) {
        MustacheFactory mf = new DefaultMustacheFactory();
        mustache = mf.compile(aTemplate);
    }

    public void add(String aName, Object aValue) {
        scope.put(aName, aValue);
    }

    public void write(File aFile) {
        try {
            mustache.execute(new PrintWriter(aFile), scope).flush();
        } catch (IOException e) {
            throw new IllegalStateException("Could not write file " + aFile.getAbsolutePath(), e);
        }
    }


}
