package com.payneteasy.firewall.util;

import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * The Velocity wrapper used to render iptables.vm.
 *
 * Rendering iptables.vm itself is covered end to end by IptablesGoldenTest; this class
 * only pins the wrapper's contract, including that an unresolved reference is emitted
 * verbatim rather than as an empty string - which is why renaming a snake_case field
 * breaks the template silently.
 */
public class VelocityBuilderTest {

    @Test
    public void substitutesTheValuesItWasGiven() throws IOException {
        StringWriter out = new StringWriter();

        new VelocityBuilder()
                .add("name", "world")
                .processTemplate(getClass().getResource("/velocity/simple.vm"), out);

        assertThat(out.toString(), containsString("Hello world!"));
    }

    /** An unknown reference is left as written - Velocity resolves names at render time. */
    @Test
    public void leavesAnUnresolvedReferenceVerbatim() throws IOException {
        StringWriter out = new StringWriter();

        new VelocityBuilder()
                .add("name", "world")
                .processTemplate(getClass().getResource("/velocity/simple.vm"), out);

        assertThat(out.toString(), containsString("Missing stays literal: $missing"));
    }

    @Test
    public void iteratesOverACollection() throws IOException {
        StringWriter out = new StringWriter();

        new VelocityBuilder()
                .add("name", "world")
                .add("items", Arrays.asList("one", "two"))
                .processTemplate(getClass().getResource("/velocity/simple.vm"), out);

        assertThat(out.toString(), containsString("- one"));
        assertThat(out.toString(), containsString("- two"));
    }

    @Test
    public void aMissingTemplateResourceFails() {
        try {
            new VelocityBuilder().processTemplate(getClass().getResource("/no/such/template.vm"),
                    new StringWriter());
            throw new AssertionError("expected an IllegalStateException");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("URL for resource is null"));
        }
    }
}
