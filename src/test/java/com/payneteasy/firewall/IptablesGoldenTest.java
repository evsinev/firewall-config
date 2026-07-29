package com.payneteasy.firewall;

import com.payneteasy.firewall.testing.TestFixtures;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Compares the generated iptables-save files against the expected output committed
 * under src/test/resources/golden/iptables.
 *
 * This is the regression net for {@link com.payneteasy.firewall.service.impl.PacketServiceImpl}
 * and iptables.vm: a silently changed rule fails here. After a deliberate change,
 * regenerate the golden files (see CLAUDE.md) and read the diff.
 *
 * The output is byte-deterministic: iptables.vm never references the generated-date
 * Main puts in the context, every packet list ends in a Collections.sort, access lists
 * go through a TreeSet and InputMssPacket hashes on its source address.
 */
public class IptablesGoldenTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void adm1MatchesGolden() throws Exception {
        assertHostMatchesGolden("adm-1");
    }

    @Test
    public void db1MatchesGolden() throws Exception {
        assertHostMatchesGolden("db-1");
    }

    /** The richest host: SNAT, DNAT, blocked addresses, custom rules in two chains, VRRP. */
    @Test
    public void fw1MatchesGolden() throws Exception {
        assertHostMatchesGolden("fw-1");
    }

    @Test
    public void fw2MatchesGolden() throws Exception {
        assertHostMatchesGolden("fw-2");
    }

    /** Carries the only mss: clamping in the demo network. */
    @Test
    public void proxy1MatchesGolden() throws Exception {
        assertHostMatchesGolden("proxy-1");
    }

    @Test
    public void web1MatchesGolden() throws Exception {
        assertHostMatchesGolden("web-1");
    }

    @Test
    public void swCore1MatchesGolden() throws Exception {
        assertHostMatchesGolden("sw-core-1");
    }

    @Test
    public void proxy1ClampsMss() throws Exception {
        assertThat(generate("proxy-1"), containsString(
                "-A INPUT -s 198.51.100.50 -p tcp -m tcp --tcp-flags SYN,RST SYN -j TCPMSS --set-mss 1300"));
    }

    /**
     * VRRP packets are computed but iptables.vm ignores them, so no rule permits VRRP
     * advertisements. Pinned so the trap documented in CLAUDE.md cannot regress silently.
     */
    @Test
    public void doesNotEmitVrrpRules() throws Exception {
        String config = generate("fw-1");
        assertThat(config, not(containsString("224.0.0.18")));
        assertThat(config, not(containsString("vrrp")));
    }

    @Test
    public void groupModeGeneratesEveryHostOfTheGroup() throws Exception {
        File out = tmp.newFolder("group-internal");

        Main.main(new String[]{TestFixtures.demoNetworkDir().getPath(), "group-internal", out.getPath()});

        assertThat(out.list(), arrayContainingInAnyOrder(
                "adm-1", "db-1", "fw-1", "fw-2", "proxy-1", "web-1"));
        for (File generated : out.listFiles()) {
            assertThat(generated.getName(), TestFixtures.readFile(generated),
                    is(TestFixtures.golden("iptables/" + generated.getName())));
        }
    }

    @Test
    public void requiresExactlyThreeArguments() {
        try {
            Main.main(new String[]{"one", "two"});
            throw new AssertionError("expected an IllegalStateException");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("usage: firewall-config.sh config-dir host output-dir"));
        }
    }

    @Test
    public void failsOnAMissingConfigDir() {
        try {
            Main.main(new String[]{"no/such/dir", "fw-1", tmp.getRoot().getPath()});
            throw new AssertionError("expected an IllegalStateException");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("is not exists"));
        }
    }

    private void assertHostMatchesGolden(String aHostname) throws Exception {
        assertThat(generate(aHostname), is(TestFixtures.golden("iptables/" + aHostname)));
    }

    private String generate(String aHostname) throws Exception {
        File out = tmp.newFolder(aHostname);
        Main.main(new String[]{TestFixtures.demoNetworkDir().getPath(), aHostname, out.getPath()});
        return TestFixtures.readFile(new File(out, aHostname));
    }
}
