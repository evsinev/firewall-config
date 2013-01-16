package com.payneteasy.firewall.yaml;

import com.payneteasy.firewall.util.VelocityBuilder;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Collections;
import java.util.Date;

/**
 *
 */
public class VelocityBuilderTest {

    @Test
    public void test() throws IOException {
        PrintWriter out = new PrintWriter(System.out);
        try {
            VelocityBuilder velocity = new VelocityBuilder();
            velocity.add("generated-date", new Date());
            velocity.processTemplate(getClass().getResource("/iptables.vm"), out);
        } finally {
            out.flush();
        }
    }
}
