package com.payneteasy.firewall.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

public class Files {

    public static String readFile(File aFile) {
        try {
            StringBuilder sb = new StringBuilder();
            try (LineNumberReader in = new LineNumberReader(new FileReader(aFile))) {
                String line;
                while(  ( (line = in.readLine()) != null)) {
                    sb.append(line);
                    sb.append("\n");
                }
                return sb.toString();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't read file " + aFile, e);
        }

    }
}
