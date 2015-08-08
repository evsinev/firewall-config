package com.payneteasy.firewall.util;

import java.io.File;

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

}
