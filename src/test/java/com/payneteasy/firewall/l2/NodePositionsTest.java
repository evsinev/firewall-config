package com.payneteasy.firewall.l2;

import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TInterface;
import com.payneteasy.firewall.testing.TestFixtures;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.awt.Point;
import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * The node-position store behind the L2 diagram: a properties file of
 * "&lt;host&gt;.&lt;port&gt; = x, y" lines.
 */
public class NodePositionsTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void aMissingFileLoadsAsAnEmptyMap() {
        NodePositions positions = new NodePositions(new File(tmp.getRoot(), "absent.properties"));

        assertThat(positions.getMap().keySet(), empty());
    }

    @Test
    public void loadsEveryPositionFromTheFile() {
        NodePositions positions = load();

        assertThat(positions.getMap().keySet(), hasSize(5));
        assertThat(positions.getMap().get("sw-core-1.1"), is(new Point(120, 40)));
        assertThat(positions.getMap().get("fw-1.eth1"), is(new Point(370, 240)));
    }

    /** The port number wins over the interface name when the interface declares one. */
    @Test
    public void getPositionPrefersThePortNumberOverTheInterfaceName() {
        NodePositions positions = load();

        THost host = new THost();
        host.name = "sw-core-1";

        TInterface withPort = new TInterface();
        withPort.name = "ether1";
        withPort.port = "1";
        assertThat(positions.getPosition(host, withPort), is(new Point(120, 40)));

        TInterface withoutPort = new TInterface();
        withoutPort.name = "ether1";
        assertThat("no key sw-core-1.ether1", positions.getPosition(host, withoutPort), nullValue());
    }

    @Test
    public void addClearAndSaveRoundTrip() throws Exception {
        File file = tmp.newFile("positions.properties");
        NodePositions positions = new NodePositions(file);

        positions.add("fw-9.eth0", 11, 22);
        positions.add("fw-9.eth1", 33, 44);
        positions.save();

        assertThat(TestFixtures.readFile(file), is("fw-9.eth0 = 11, 22\nfw-9.eth1 = 33, 44\n"));
        assertThat(new NodePositions(file).getMap().get("fw-9.eth1"), is(new Point(33, 44)));

        positions.clear();
        assertThat(positions.getMap().keySet(), empty());
    }

    @Test
    public void aMalformedValueFailsWithTheFileName() {
        File file = TestFixtures.resource("/l2/broken-positions.properties");

        try {
            new NodePositions(file);
            throw new AssertionError("expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("Could not load"));
            assertThat(e.getMessage(), containsString("broken-positions.properties"));
        }
    }

    @Test
    public void savingToAnUnwritablePathFails() {
        NodePositions positions = new NodePositions(new File(tmp.getRoot(), "no/such/dir/p.properties"));
        positions.add("fw-9.eth0", 1, 2);

        try {
            positions.save();
            throw new AssertionError("expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("Could no save"));
        }
    }

    private NodePositions load() {
        return new NodePositions(TestFixtures.resource("/l2/l2positions.properties"));
    }
}
