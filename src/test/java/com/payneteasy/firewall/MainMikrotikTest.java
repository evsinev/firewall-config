package com.payneteasy.firewall;

import com.payneteasy.firewall.testing.TestFixtures;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Golden test for the RouterOS switch config. MainMikrotik prints to stdout.
 */
public class MainMikrotikTest {

    @Test
    public void swCore1MatchesGolden() {
        assertThat(generate("sw-core-1"), is(TestFixtures.golden("mikrotik-sw-core-1.txt")));
    }

    /**
     * One block per non-trunk vlan, in the order the interfaces are declared in the
     * host yml - MainMikrotik iterates host.interfaces directly, it does not sort.
     */
    @Test
    public void emitsOneBlockPerVlanInInterfaceDeclarationOrder() {
        String out = generate("sw-core-1");
        assertThat(out, stringContainsInOrder(asList(
                "# sw-core-1 201", "# sw-core-1 206", "# sw-core-1 202", "# sw-core-1 203")));
        assertThat(out, not(containsString("# sw-core-1 trunk")));
    }

    /** ether1 is the trunk, so it is the tagged port of every vlan. */
    @Test
    public void usesTheTrunkPortAsTheTaggedPort() {
        assertThat(generate("sw-core-1"), containsString("add tagged-ports=ether1 vlan-id=201"));
    }

    /** Ports with no vlan of their own inherit it from the switch port they are linked to. */
    @Test
    public void collectsPortsOfASingleVlanIncludingLinkedOnes() {
        assertThat(generate("sw-core-1"),
                containsString("add new-customer-vid=206 ports=ether4,ether6,ether8,ether10 sa-learning=yes"));
    }

    @Test
    public void aSingleVlanCanBeRequestedExplicitly() {
        String out = TestFixtures.captureStdout(() -> MainMikrotik.main(
                new String[]{TestFixtures.demoNetworkDir().getPath(), "sw-core-1", "202"}));
        assertThat(out, startsWith("# sw-core-1 202\n"));
        assertThat(out, not(containsString("# sw-core-1 201")));
    }

    @Test
    public void failsWhenTheHostHasNoTrunkPort() {
        try {
            TestFixtures.captureStdout(() -> MainMikrotik.main(
                    new String[]{TestFixtures.configDir("no-trunk").getPath(), "sw-1"}));
            throw new AssertionError("expected an IllegalStateException");
        } catch (Exception e) {
            assertThat(rootCause(e).getMessage(), containsString("trunk port not found at sw-1"));
        }
    }

    private static Throwable rootCause(Throwable aThrowable) {
        Throwable cause = aThrowable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    private String generate(String aHostname) {
        return TestFixtures.captureStdout(() ->
                MainMikrotik.main(new String[]{TestFixtures.demoNetworkDir().getPath(), aHostname}));
    }
}
