package com.payneteasy.firewall.l3;

import com.payneteasy.firewall.MainL3Diagram;
import com.payneteasy.firewall.dao.ConfigDaoYaml;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.testing.TestFixtures;
import org.junit.BeforeClass;
import org.junit.Test;
import picocli.CommandLine;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Golden test for the nwdiag input.
 *
 * CreateL3Diagram writes the CWD-relative target/network.diag, which lands in the
 * build directory because surefire keeps the module root as the working directory.
 * nwdiag itself is never run - runNwdiag is always false, otherwise the test would
 * need the nwdiag binary on PATH.
 */
public class CreateL3DiagramTest {

    private static final File OUTPUT = new File("target/network.diag");

    @BeforeClass
    public static void ensureOutputDirExists() {
        OUTPUT.getParentFile().mkdirs();
    }

    @Test
    public void matchesGolden() throws Exception {
        create(TestFixtures.demoNetworkDir(), "internal", "ipmi");

        assertThat(TestFixtures.readFile(OUTPUT), is(TestFixtures.golden("l3/network.diag")));
    }

    /** nwdiag-custom.diag is inlined verbatim; here it is the fw-1/fw-2 colour group. */
    @Test
    public void inlinesTheCustomDiagBlock() throws Exception {
        create(TestFixtures.demoNetworkDir(), "internal", "ipmi");

        assertThat(TestFixtures.readFile(OUTPUT), containsString("color = \"#FFCDD2\""));
    }

    /** The internet network is forced to the top; the rest are sorted by name. */
    @Test
    public void putsInternetFirstAndSortsTheRestByName() throws Exception {
        String diag = TestFixtures.captureStdout(() -> create(TestFixtures.demoNetworkDir(), "internal", "ipmi"));

        assertThat(diag, stringContainsInOrder(java.util.Arrays.asList(
                "network \"internet\"", "network \"app\"", "network \"db\"",
                "network \"dmz\"", "network \"ipmi\"")));
    }

    @Test
    public void failsWhenANetworkIsMissingFromNetworksYml() throws Exception {
        IConfigDao dao = new ConfigDaoYaml(TestFixtures.configDir("minimal"));
        // 10.30.1.0 is named in networks.yml, so add a host in an unnamed network
        dao.getHostByName("srv-a").interfaces.get(1).ip = "10.30.9.9";

        try {
            new CreateL3Diagram(TestFixtures.configDir("minimal"), false, new String[]{"internal"}).create(dao);
            throw new AssertionError("expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("has no name, please add it to networks.yml"));
        } finally {
            dao.getHostByName("srv-a").interfaces.get(1).ip = "skip";
        }
    }

    /** The picocli command is reachable without main(), which would System.exit. */
    @Test
    public void thePicocliCommandRunsAndReturnsZero() {
        int exitCode = new CommandLine(new MainL3Diagram()).execute(
                TestFixtures.demoNetworkDir().getPath(), "current",
                "--run-nwdiag=false", "--filter", "internal,ipmi");

        assertThat(exitCode, is(0));
    }

    /** The prefix parameter is positionally required even though this command ignores it. */
    @Test
    public void thePicocliCommandRequiresThePrefixArgument() {
        int exitCode = new CommandLine(new MainL3Diagram())
                .setErr(new java.io.PrintWriter(new java.io.StringWriter()))
                .execute(TestFixtures.demoNetworkDir().getPath(), "--run-nwdiag=false");

        assertThat(exitCode, is(not(0)));
    }

    private void create(File aConfigDir, String... aFilter) throws Exception {
        new CreateL3Diagram(aConfigDir, false, aFilter).create(new ConfigDaoYaml(aConfigDir));
    }
}
