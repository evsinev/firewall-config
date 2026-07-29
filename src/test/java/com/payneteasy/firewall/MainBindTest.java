package com.payneteasy.firewall;

import com.payneteasy.firewall.testing.TestFixtures;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Golden test for the bind zones.
 *
 * Only the record lines are compared. MainBind.addHeader puts a yyMMddHHmm serial,
 * a new Date(), the local hostname and user.name into every zone header, so a whole
 * file comparison would fail a minute later or on another machine. zones.conf has no
 * header variables and is compared in full.
 */
public class MainBindTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File outputDir;

    @Before
    public void setUp() throws Exception {
        outputDir = tmp.newFolder("bind-out");
        MainBind.main(new String[]{TestFixtures.demoNetworkDir().getPath(), "demo.example.com", outputDir.getPath()});
    }

    @Test
    public void zonesConfMatchesGolden() {
        assertThat(TestFixtures.readFile(new File(outputDir, "zones.conf")),
                is(TestFixtures.golden("bind/zones.conf")));
    }

    @Test
    public void writesOneForwardZoneOneServiceZoneAndOneReverseZonePerNetwork() {
        assertThat(outputDir.list(), arrayContainingInAnyOrder(
                "demo.example.com.zone",
                "static.zone",
                "100.51.198.zone",
                "2.20.10.zone",
                "20.20.10.zone",
                "22.20.10.zone",
                "6.20.10.zone",
                "zones.conf"));
    }

    @Test
    public void forwardZoneRecordsMatchGolden() {
        assertRecordsMatchGolden("demo.example.com");
    }

    /**
     * static.zone is the one zone whose records are not sorted: createServiceZone collects
     * them into an ArrayList while walking findHostsByGroup("internal"), i.e. File.listFiles()
     * order, so the record order differs between APFS and ext4. Compare them as a set.
     */
    @Test
    public void serviceZoneRecordsMatchGoldenRegardlessOfOrder() {
        String actual = TestFixtures.grepLines(new File(outputDir, "static.zone"), RECORD_LINE);

        assertThat(TestFixtures.sortedLines(actual),
                is(TestFixtures.sortedLines(TestFixtures.golden("bind/static.records"))));
    }

    /** One A record per named service per host, so a shared service appears several times. */
    @Test
    public void theServiceZoneRepeatsASharedServiceForEveryHost() {
        String records = TestFixtures.grepLines(new File(outputDir, "static.zone"), RECORD_LINE);

        assertThat(records.split("\n").length, is(17));
        assertThat(records, containsString("ssh       IN A 10.20.20.11"));
        assertThat(records, containsString("ssh       IN A 10.20.2.21"));
        assertThat(records, containsString("main-db       IN A 10.20.22.21"));
    }

    @Test
    public void reverseZoneRecordsMatchGolden() {
        assertRecordsMatchGolden("100.51.198");
        assertRecordsMatchGolden("2.20.10");
        assertRecordsMatchGolden("20.20.10");
        assertRecordsMatchGolden("22.20.10");
        assertRecordsMatchGolden("6.20.10");
    }

    /** The header is the nondeterministic part; assert its shape rather than its value. */
    @Test
    public void zoneHeaderCarriesATenDigitSerial() {
        String zone = TestFixtures.readFile(new File(outputDir, "demo.example.com.zone"));
        assertThat(zone, zone.matches("(?s).*\\n    \\d{10}   ; serial\\n.*"), is(true));
    }

    /**
     * Byte-exact: the forward zone goes through a TreeSet and each reverse zone through a
     * TreeMap of TreeSets, so their record order is stable everywhere.
     */
    private void assertRecordsMatchGolden(String aZoneName) {
        String actual = TestFixtures.grepLines(new File(outputDir, aZoneName + ".zone"), RECORD_LINE);
        assertThat(aZoneName, actual, is(TestFixtures.golden("bind/" + aZoneName + ".records")));
    }

    private static final String RECORD_LINE = ".*\\bIN\\s+(A|PTR)\\b.*";
}
