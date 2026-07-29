package com.payneteasy.firewall;

import com.payneteasy.firewall.testing.TestFixtures;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Golden test for the keepalived config. MainKeepalived prints the whole shell
 * wrapper to stdout instead of writing a file, so the output is captured.
 */
public class MainKeepalivedTest {

    @Test
    public void fw1MatchesGolden() {
        assertThat(generate("fw-1"), is(TestFixtures.golden("keepalived-fw-1.txt")));
    }

    @Test
    public void wrapsTheConfigInACpAndTeeScript() {
        String out = generate("fw-1");
        assertThat(out, startsWith("cp /etc/keepalived/keepalived.conf /tmp/keepalived.conf-$(date +%s)\n"));
        assertThat(out, containsString("tee /etc/keepalived/keepalived.conf <<EOF"));
        assertThat(out.trim(), endsWith("EOF"));
    }

    /** The vrrp_instance name is the vlan suffix of the interface name. */
    @Test
    public void namesOneVrrpInstancePerVirtualInterface() {
        String out = generate("fw-1");
        assertThat(out, containsString("vrrp_instance V_202 {"));
        assertThat(out, containsString("vrrp_instance V_201 {"));
        assertThat(out, containsString("vrrp_instance V_203 {"));
        assertThat(out, containsString("vrrp_instance V_206 {"));
        assertThat(out, containsString("vrrp_instance V_100 {"));
    }

    /**
     * keepalived.mustache contains a literal {{vrrp_priority}} that nothing in the
     * scope resolves, so the placeholder reaches the generated config verbatim.
     * Pinned because it looks like a template bug and is easy to "fix" by accident.
     */
    @Test
    public void leavesTheVrrpPriorityPlaceholderUnresolved() {
        assertThat(generate("fw-1"), containsString("priority          {{vrrp_priority}}"));
    }

    /** A host without any vip renders the wrapper and an empty instance list. */
    @Test
    public void hostWithoutVirtualAddressesRendersNoInstance() {
        String out = generate("db-1");
        assertThat(out, containsString("tee /etc/keepalived/keepalived.conf <<EOF"));
        assertThat(out, not(containsString("vrrp_instance")));
    }

    @Test
    public void failsForAnUnknownHost() {
        try {
            generate("no-such-host");
            throw new AssertionError("expected an IllegalStateException");
        } catch (Exception e) {
            assertThat(e.getCause().getMessage(), containsString("Host no-such-host not found"));
        }
    }

    private String generate(String aHostname) {
        return TestFixtures.captureStdout(() ->
                MainKeepalived.main(new String[]{TestFixtures.demoNetworkDir().getPath(), aHostname}));
    }
}
