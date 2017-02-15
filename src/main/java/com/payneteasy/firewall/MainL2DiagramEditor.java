package com.payneteasy.firewall;

import com.payneteasy.firewall.l2.editor.L2Editor;

import java.io.File;
import java.io.IOException;

public class MainL2DiagramEditor {

    public static void main(String[] args) throws IOException {
        new L2Editor().show(new File(args[0]));
    }
}
