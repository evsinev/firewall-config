package com.payneteasy.firewall.util;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.payneteasy.firewall.MainKeepalived;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShellFilePrinter {

    public ShellFilePrinter(String aFilename) {
        out("cp %s /tmp/%s-$(date +%%s)", aFilename, getFilenameOnly(aFilename));
        out("tee " + aFilename + " <<EOF");
    }

    private String getFilenameOnly(String aFilename) {
        return new File(aFilename).getName();
    }

    public void out() {
        System.out.println();
    }

    public void out(String aText) {
        System.out.println(aText);
    }

    public void out(String aFormat, Object... args) {
        out(String.format(aFormat, args));
    }

    public void close() {
        out("EOF");
    }

    public void mustache(String aTemplate, String aName, Object aValue) {
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile(aTemplate);

            Map<String, Object> scope = new HashMap<>();
            scope.put(aName, aValue);

        try {
            mustache.execute(new PrintWriter(System.out), scope).flush();
        } catch (IOException e) {
            throw new IllegalStateException("Could not process template "+aTemplate, e);
        }

    }
}
