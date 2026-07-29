package com.payneteasy.firewall.l2.editor.create;

import com.payneteasy.firewall.testing.TestFixtures;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.awt.Point;
import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * The three IPositionManager implementations and the miss-fallback chain
 * PropertiesPositionManager -&gt; PlainPositions -&gt; EmptyPositionManager.
 */
public class PositionManagersTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final File POSITIONS = TestFixtures.resource("/l2/l2positions.properties");

    /**
     * PlainPositions converts absolute port coordinates into a host origin plus
     * host-relative port offsets, then shifts every host so the top-left one sits at 20,20.
     *
     * sw-core-1 ports are at y=40 and x=120..220, so its origin is (120-5, 40-5) = (115, 35)
     * before the shift. fw-1 is the other host, at (315, 235). The minimum is (115, 35),
     * minus another 20, so everything shifts by (95, 15).
     */
    @Test
    public void plainPositionsMakesPortsRelativeAndShiftsHostsToTheOrigin() {
        PlainPositions positions = new PlainPositions(POSITIONS);

        assertThat(positions.getHostPosition("sw-core-1"), is(new Point(20, 20)));
        assertThat(positions.getHostPosition("fw-1"), is(new Point(220, 220)));
        assertThat(positions.getPortPosition("sw-core-1", "1"), is(new Point(5, 5)));
        assertThat(positions.getPortPosition("sw-core-1", "2"), is(new Point(55, 5)));
        assertThat(positions.getPortPosition("fw-1", "eth1"), is(new Point(55, 5)));
    }

    /** A port name is retried with the "ether" prefix stripped, matching the port-number keys. */
    @Test
    public void plainPositionsFallsBackToThePortNumberWhenTheNameMisses() {
        PlainPositions positions = new PlainPositions(POSITIONS);

        assertThat(positions.getPortPosition("sw-core-1", "ether2"), is(new Point(55, 5)));
    }

    @Test
    public void plainPositionsReturnsTheOriginForUnknownHostsAndPorts() {
        PlainPositions positions = new PlainPositions(POSITIONS);

        assertThat(positions.getHostPosition("nope"), is(new Point(0, 0)));
        assertThat(positions.getPortPosition("nope", "eth0"), is(new Point(0, 0)));
        assertThat(positions.getPortPosition("fw-1", "eth9"), is(new Point(0, 0)));
    }

    /**
     * The key is tokenised on ",. " so only the first two tokens are used: a key like
     * fw-1.eth0.202 yields the port "eth0", not "eth0.202". Pinned - it silently
     * collapses every vlan sub-interface of an interface onto one position.
     */
    @Test
    public void aDottedVlanSubInterfaceKeyCollapsesToTheBareInterface() throws Exception {
        File file = tmp.newFile("vlan.properties");
        java.nio.file.Files.write(file.toPath(), "fw-1.eth0.202 = 120, 40\n".getBytes("UTF-8"));

        PlainPositions positions = new PlainPositions(file);

        assertThat(positions.getPortPosition("fw-1", "eth0"), is(new Point(5, 5)));
        assertThat(positions.getPortPosition("fw-1", "eth0.202"), is(new Point(0, 0)));
    }

    @Test
    public void propertiesPositionManagerReadsPositionsFromItsOwnFile() throws Exception {
        File file = tmp.newFile("current-l2-positions.properties");
        java.nio.file.Files.write(file.toPath(),
                ("fw-1 = 300, 400\nfw-1.eth0 = 30x40\n").getBytes("UTF-8"));

        PropertiesPositionManager manager = new PropertiesPositionManager(file, new EmptyPositionManager());

        assertThat(manager.getHostPosition("fw-1"), is(new Point(300, 400)));
        // the value separator can be any of " ,;x"
        assertThat(manager.getPortPosition("fw-1", "eth0"), is(new Point(30, 40)));
    }

    @Test
    public void propertiesPositionManagerDelegatesMissesToTheFallback() {
        PropertiesPositionManager manager = new PropertiesPositionManager(
                new File(tmp.getRoot(), "absent.properties"), new PlainPositions(POSITIONS));

        assertThat(manager.getHostPosition("sw-core-1"), is(new Point(20, 20)));
        assertThat(manager.getPortPosition("sw-core-1", "1"), is(new Point(5, 5)));
    }

    /** EmptyPositionManager just lays hosts out in a column and ports in a row. */
    @Test
    public void emptyPositionManagerLaysOutHostsInAColumnAndPortsInARow() {
        EmptyPositionManager manager = new EmptyPositionManager();

        assertThat(manager.getHostPosition("a"), is(new Point(10, 30)));
        assertThat(manager.getHostPosition("b"), is(new Point(10, 100)));

        assertThat(manager.getPortPosition("a", "eth0"), is(new Point(10, 10)));
        assertThat(manager.getPortPosition("a", "eth1"), is(new Point(60, 10)));
        assertThat(manager.getPortPosition("b", "eth0"), is(new Point(10, 10)));
    }

    @Test
    public void linkHolderIdentityIgnoresTheLinkType() {
        LinkHolder common = new LinkHolder("sw-1", "ether1", "fw-1", "eth0",
                com.payneteasy.firewall.l2.editor.model.LinkType.COMMON);
        LinkHolder added = new LinkHolder("sw-1", "ether1", "fw-1", "eth0",
                com.payneteasy.firewall.l2.editor.model.LinkType.CUSTOM_ADDED);
        LinkHolder other = new LinkHolder("sw-1", "ether2", "fw-1", "eth0",
                com.payneteasy.firewall.l2.editor.model.LinkType.COMMON);

        // this is what makes removedLinks work: a removal never states the link type
        assertThat(common, is(added));
        assertThat(common.hashCode(), is(added.hashCode()));
        assertThat(common, is(not(other)));
        assertThat(common.equals(null), is(false));
        assertThat(common.equals("a string"), is(false));
        assertThat(common.compareTo(other), is(lessThan(0)));
        assertThat(common.toString(), is("LinkHolder{sw-1'/ether1  -> fw-1/eth0}"));
    }
}
