package com.payneteasy.firewall.l2.labels.wire;

import java.io.*;

public class ResourcePath {

    private final String resourcePath;

    public ResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public Reader createReader() throws IOException {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        if(in == null) {
            throw new IOException("Resource " + resourcePath + " not found");
        }
        return new InputStreamReader(in);
    }

    public String getText() throws IOException {
        StringBuilder sb = new StringBuilder();
        try(LineNumberReader in = new LineNumberReader(createReader())) {
            String line;
            while ( (line = in.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
}
