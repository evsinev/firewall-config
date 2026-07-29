package com.payneteasy.firewall.util;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URL;
import java.util.Properties;

/**
 *
 */
public class VelocityBuilder {

    public VelocityBuilder() {
        Properties p = new Properties();
        p.setProperty("resource.loaders", "class");
        p.setProperty("resource.loader.class.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        // iptables.vm reads $blocked-ip-addresses, $input-packets, $custom-input-rules ...
        // Velocity 2 rejects the hyphen in an identifier unless this is on, and it fails
        // by rendering the reference literally, not by throwing. See Main.
        p.setProperty("parser.allow_hyphen_in_identifiers", "true");
        // 2.x defaults to "lines"; "bc" is the 1.7 behaviour the golden files were made with
        p.setProperty("parser.space_gobbling", "bc");

        theEngine = new VelocityEngine(p);

    }

    public VelocityBuilder add(String aKey, Object aValue) {
        theContext.put(aKey, aValue);
        return this;
    }

    public void processTemplate(URL aUrl, Writer output) throws IOException {
        if(aUrl==null) throw new IllegalStateException("URL for resource is null");

        InputStreamReader in = new InputStreamReader(aUrl.openStream());
        try {
            theEngine.evaluate(theContext, output, "velocity", in);
        } finally {
            in.close();
        }
    }


    private final VelocityContext theContext = new VelocityContext();
    private final VelocityEngine theEngine;
}
