package com.payneteasy.firewall.l2.editor.model;

import com.payneteasy.firewall.l2.editor.actions.PointSaverProperties;
import com.payneteasy.firewall.testing.RecordingCanvas;
import org.junit.Test;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * The L2 diagram model is pure integer geometry over java.awt value types, so it is
 * testable headless. Drawing goes through a RecordingCanvas instead of Swing.
 *
 * Text metrics come from RecordingCanvas: width 20, height 10. A port that has not
 * been drawn yet reports the 30x30 default instead.
 */
public class L2ModelTest {

    @Test
    public void anUndrawnPortUsesTheThirtyByThirtyDefault() {
        Port port = port("ether1", 10, 20);

        assertThat(port.calculateWidth(), is(30));
        assertThat(port.calculateHeight(), is(30));
        assertThat(port.getEndX(), is(40));
        assertThat(port.getEndY(), is(50));
    }

    @Test
    public void drawingAPortCachesTheTextMetrics() {
        Port port = port("ether1", 10, 20);

        port.draw(new RecordingCanvas());

        assertThat(port.calculateWidth(), is(RecordingCanvas.TEXT_WIDTH + 10));
        assertThat(port.calculateHeight(), is(RecordingCanvas.TEXT_HEIGHT + 10));
    }

    /** The label drops the ether/bridge noise so the diagram stays readable. */
    @Test
    public void thePortLabelStripsEtherAndAbbreviatesBridge() {
        RecordingCanvas canvas = new RecordingCanvas();

        port("ether7", 0, 0).draw(canvas);
        port("bridge2", 0, 0).draw(canvas);

        assertThat(canvas.getCalls(), hasItem(endsWith(" 7")));
        assertThat(canvas.getCalls(), hasItem(endsWith(" br2")));
    }

    @Test
    public void aPortDrawsAFilledRectAnOutlineAndItsLabel() {
        RecordingCanvas canvas = new RecordingCanvas();

        port("ether1", 10, 20).draw(canvas);

        assertThat(canvas.count("fillRect"), is(1));
        assertThat(canvas.count("drawRect"), is(1));
        assertThat(canvas.count("drawText"), is(1));
    }

    @Test
    public void hasPointIsStrictlyInsideThePortRectangle() {
        Port port = port("ether1", 10, 20);

        assertThat(port.hasPoint(25, 35), is(true));
        assertThat(port.hasPoint(10, 20), is(false)); // on the top-left corner
        assertThat(port.hasPoint(40, 50), is(false)); // on the bottom-right corner
        assertThat(port.hasPoint(5, 35), is(false));
        assertThat(port.hasPoint(25, 5), is(false));
    }

    /** moveTo ignores non-positive coordinates, so a drag off-canvas cannot lose a port. */
    @Test
    public void movingAPortIgnoresNonPositiveCoordinates() {
        Port port = port("ether1", 10, 20);

        port.moveTo(0, -5);
        assertThat(port.createRectangle(), is(new Rectangle(10, 20, 30, 30)));

        port.moveTo(70, 80);
        assertThat(port.createRectangle(), is(new Rectangle(70, 80, 30, 30)));
    }

    @Test
    public void createOffsetIsTheVectorBackToTheNodeOrigin() {
        assertThat(port("ether1", 10, 20).createOffset(15, 25), is(new Point(-5, -5)));
    }

    @Test
    public void portsReportTheFarthestEdgeOfAnyPort() {
        Ports ports = new Ports(Arrays.asList(port("a", 10, 10), port("b", 100, 50)));

        assertThat(ports.getMaxX(), is(130));
        assertThat(ports.getMaxY(), is(80));
    }

    @Test
    public void portsFindsAPortByName() {
        Ports ports = new Ports(Arrays.asList(port("ether1", 0, 0), port("ether2", 40, 0)));

        assertThat(ports.findPort("ether2"), notNullValue());
    }

    @Test
    public void lookingUpAnUnknownPortFails() {
        Ports ports = new Ports(Arrays.asList(port("ether1", 0, 0)));

        try {
            ports.findPort("nope");
            throw new AssertionError("expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("Port not found nope"));
        }
    }

    @Test
    public void anEmptyPortsCollectionHasZeroExtent() {
        Ports ports = new Ports(new ArrayList<>());

        assertThat(ports.getMaxX(), is(0));
        assertThat(ports.getMaxY(), is(0));
        assertThat(ports.findNode(5, 5), nullValue());
    }

    /** The host box wraps its ports plus PADDING on the right and bottom. */
    @Test
    public void theHostBoxIsItsPortsPlusPadding() {
        Host host = new Host("fw-1", 100, 200, new Ports(Arrays.asList(port("ether1", 10, 10))),
                new Color(0xf8ecc9));

        assertThat(host.getMaxWidth(), is(100 + 40 + Host.PADDING));
        assertThat(host.getMaxHeight(), is(200 + 40 + Host.PADDING));
        assertThat(host.getPoint(), is(new Point(100, 200)));
        assertThat(host.hasName("fw-1"), is(true));
        assertThat(host.hasName("fw-2"), is(false));
    }

    /** Ports are hit-tested in host-relative coordinates and win over the host itself. */
    @Test
    public void findNodeReturnsThePortInsideItTheHostAroundItAndNullOutside() {
        Host host = new Host("fw-1", 100, 200, new Ports(Arrays.asList(port("ether1", 10, 10))),
                Color.WHITE);

        assertThat(host.findNode(125, 225), instanceOf(Port.class));
        assertThat(host.findNode(105, 205), sameInstance((Object) host));
        assertThat(host.findNode(10, 10), nullValue());
    }

    @Test
    public void aHostDrawsItsBoxItsNameAndDelegatesToItsPorts() {
        RecordingCanvas canvas = new RecordingCanvas();
        Host host = new Host("fw-1", 100, 200, new Ports(Arrays.asList(port("ether1", 10, 10))),
                Color.WHITE);

        host.draw(canvas);

        assertThat(canvas.getCalls(), hasItem("drawText 100,195 fw-1"));
        // the port is drawn on a child canvas offset by the host origin
        assertThat(canvas.getCalls(), hasItem(startsWith("drawText 115,")));
        assertThat(canvas.count("fillRect"), is(2));
    }

    @Test
    public void shiftMovesAHostByAnOffset() {
        Host host = new Host("fw-1", 100, 200, new Ports(new ArrayList<>()), Color.WHITE);

        host.shift(10, -20);

        assertThat(host.getPoint(), is(new Point(110, 180)));
    }

    @Test
    public void hostsDrawsEveryHostAndOneLegendEntryPerVlan() {
        RecordingCanvas canvas = new RecordingCanvas();

        hosts().draw(canvas);

        // 2 hosts + 1 port each = 4 fillRect, plus 1 legend swatch
        assertThat(canvas.count("fillRect"), is(5));
        assertThat(canvas.getCalls(), hasItem(endsWith(" 202")));
    }

    @Test
    public void pickThenMoveMovesTheNodeUnderTheCursor() {
        Hosts hosts = hosts();

        assertThat(hosts.pick(105, 205), notNullValue());
        hosts.movePicked(300, 400);
        // the offset keeps the grab point, so the host lands at 300,400
        assertThat(hosts.getMaxWidth(), is(300 + 40 + Host.PADDING - 5));
    }

    @Test
    public void movingAfterUnpickIsANoOp() {
        Hosts hosts = hosts();
        int widthBefore = hosts.getMaxWidth();

        hosts.pick(105, 205);
        hosts.unpick();
        hosts.movePicked(900, 900);

        assertThat(hosts.getMaxWidth(), is(widthBefore));
    }

    @Test
    public void pickingEmptySpaceReturnsNull() {
        assertThat(hosts().pick(5, 5), nullValue());
    }

    @Test
    public void createLinkResolvesBothEndsByName() {
        Link link = hosts().createLink("fw-1", "ether1", "fw-2", "ether2", LinkType.COMMON);

        assertThat(link.createLinkInfo().getLeftAddress(), is("fw-1/1"));
        assertThat(link.createLinkInfo().getRightAddress(), is("fw-2/2"));
    }

    @Test
    public void createLinkToAnUnknownHostFails() {
        try {
            hosts().createLink("fw-1", "ether1", "nope", "ether2", LinkType.COMMON);
            throw new AssertionError("expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("Host not found nope"));
        }
    }

    /** A custom-added cable is drawn red and twice as thick. */
    @Test
    public void customAddedLinksAreDrawnRedAndThicker() {
        RecordingCanvas canvas = new RecordingCanvas();
        Hosts hosts = hosts();

        hosts.createLink("fw-1", "ether1", "fw-2", "ether2", LinkType.CUSTOM_ADDED).draw(canvas);
        hosts.createLink("fw-1", "ether1", "fw-2", "ether2", LinkType.COMMON).draw(canvas);

        assertThat(canvas.getCalls().get(0), containsString("w=2.0"));
        assertThat(canvas.getCalls().get(1), containsString("w=1.0"));
    }

    /** The cable colour is taken from whichever end is not plain white. */
    @Test
    public void theLinkColourComesFromTheNonWhiteEnd() {
        List<Port> left = Arrays.asList(new Port("ether1", 10, 10, Color.WHITE));
        List<Port> right = Arrays.asList(new Port("ether2", 10, 10, new Color(0xA5D6A7)));
        Hosts hosts = new Hosts(Arrays.asList(
                new Host("fw-1", 0, 0, new Ports(left), Color.WHITE),
                new Host("fw-2", 0, 0, new Ports(right), Color.WHITE)),
                new HashMap<>());

        Link link = hosts.createLink("fw-1", "ether1", "fw-2", "ether2", LinkType.COMMON);

        assertThat(link.createLinkInfo().getColorHex(), is("a5d6a7"));
    }

    @Test
    public void linksDrawEveryCable() {
        RecordingCanvas canvas = new RecordingCanvas();
        Hosts hosts = hosts();
        Links links = new Links(Arrays.asList(
                hosts.createLink("fw-1", "ether1", "fw-2", "ether2", LinkType.COMMON),
                hosts.createLink("fw-2", "ether2", "fw-1", "ether1", LinkType.COMMON)));

        links.draw(canvas);

        assertThat(canvas.count("drawLine"), is(2));
        assertThat(links.getLinks(), hasSize(2));
    }

    /** Positions are persisted as "host = x, y" and "host.port = x, y". */
    @Test
    public void saveWritesAHostKeyAndADottedPortKey() {
        Properties properties = new Properties();

        hosts().save(new PointSaverProperties(properties));

        assertThat(properties.getProperty("fw-1"), is("100, 200"));
        assertThat(properties.getProperty("fw-1.ether1"), is("10, 10"));
        assertThat(properties.getProperty("fw-2.ether2"), is("10, 10"));
    }

    @Test
    public void moveHostsShiftsEveryHost() {
        Hosts hosts = hosts();
        int widthBefore = hosts.getMaxWidth();

        hosts.moveHosts(15, 15);

        assertThat(hosts.getMaxWidth(), is(widthBefore + 15));
    }

    @Test
    public void anEmptyDiagramHasZeroExtent() {
        Hosts hosts = new Hosts(new ArrayList<>(), new HashMap<>());

        assertThat(hosts.getMaxWidth(), is(0));
        assertThat(hosts.getMaxHeight(), is(0));
    }

    private static Port port(String aName, int aX, int aY) {
        return new Port(aName, aX, aY, Color.WHITE);
    }

    private static Hosts hosts() {
        Map<String, Color> vlanColors = new HashMap<>();
        vlanColors.put("202", new Color(0xA5D6A7));
        return new Hosts(Arrays.asList(
                new Host("fw-1", 100, 200, new Ports(Arrays.asList(port("ether1", 10, 10))), Color.WHITE),
                new Host("fw-2", 100, 400, new Ports(Arrays.asList(port("ether2", 10, 10))), Color.WHITE)),
                vlanColors);
    }
}
