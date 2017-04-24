package com.payneteasy.firewall.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

public class CommandProcess {


    private final Process process;
    private final String name;

    public CommandProcess(String aName, String aCommand) throws IOException {
        name = aName;
        process = Runtime.getRuntime().exec("nwdiag -a --no-transparency target/network.diag");
    }

    public int waitFor() throws InterruptedException {
        Thread stdThread = new Thread(new StreamReader(process.getInputStream(), System.out, name));
        Thread errThread = new Thread(new StreamReader(process.getErrorStream(), System.err, name));
        stdThread.start();
        errThread.start();
        try {
            return process.waitFor();
        } finally {
            stdThread.interrupt();
            errThread.interrupt();
        }
    }

    public void waitSuccess() throws InterruptedException {
        final int code = waitFor();
        if(code !=0) {
            throw new RuntimeException("Command "+name+ " returned error exit code: " + code);
        }
    }

    private static class StreamReader implements Runnable {

        private final InputStream in;
        private final PrintStream out;
        private final String name;

        public StreamReader(InputStream in, PrintStream aOut, String aName) {
            this.in = in;
            out = aOut;
            name = aName;
        }

        @Override
        public void run() {
            byte[] buf = new byte[1024];
            int count;
            try {
                try {
                    while ( (count = in.read(buf)) >= 0 ) {
                        out.println(name + " : " + new String(buf, 0, count));
                    }
                } finally {
                    in.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
