package com.payneteasy.firewall.l2.labels.wire;

import java.io.*;

public class L2WireSvgFile {

    private FileWriter out;

    public void openOutputFile(File aOutputFile) throws IOException {
        out = new FileWriter(aOutputFile);
    }

    public void addHeader(ResourcePath aResource) throws IOException {
        try (Reader in = aResource.createReader()) {
            char[] buf = new char[1024];
            int count;
            while( ( count = in.read(buf)) >= 0 ) {
                out.write(buf, 0, count);
            }
        }
    }

    public void addText(String aText) throws IOException {
        out.write(aText);
    }

    public void close() throws IOException {
        out.write("\n</svg>");
        out.close();
    }

}
