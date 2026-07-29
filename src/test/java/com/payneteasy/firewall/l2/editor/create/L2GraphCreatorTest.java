package com.payneteasy.firewall.l2.editor.create;

import com.payneteasy.firewall.dao.ConfigDaoYaml;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.l2.editor.model.Hosts;
import com.payneteasy.firewall.l2.editor.model.Link;
import com.payneteasy.firewall.l2.editor.model.LinkType;
import com.payneteasy.firewall.l2.editor.model.Links;
import com.payneteasy.firewall.service.model.LinkInfo;
import com.payneteasy.firewall.testing.RecordingCanvas;
import com.payneteasy.firewall.testing.TestFixtures;
import org.junit.Before;
import org.junit.Test;

import java.awt.Color;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * The L2 derivation: cabling declared in hosts/ plus the &lt;prefix&gt;-l2-additions.yml
 * overrides become a host/port/link graph.
 *
 * Only sw-core-1 describes cables in the demo network (10 of them), and each cable is
 * declared on one side only, so the expected link count is exactly 10.
 */
public class L2GraphCreatorTest {

    private IConfigDao dao;

    @Before
    public void setUp() throws Exception {
        dao = new ConfigDaoYaml(TestFixtures.demoNetworkDir());
    }

    @Test
    public void derivesEveryCableDeclaredOnTheSwitch() {
        L2GraphCreator creator = create();
        creator.getHosts();

        assertThat(creator.getLinks().getLinks(), hasSize(10));
    }

    /**
     * getHosts() must be called before getLinks(): createHosts() is what applies the
     * removed links and builds the node map the links are resolved against.
     */
    @Test
    public void getLinksBeforeGetHostsFails() {
        try {
            create().getLinks();
            throw new AssertionError("expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("Hosts is null"));
        }
    }

    @Test
    public void everyCabledHostBecomesANode() {
        Hosts hosts = create().getHosts();
        RecordingCanvas canvas = new RecordingCanvas();

        hosts.draw(canvas);

        // one host label per cabled host: the switch plus the five hosts it connects
        for (String host : asList("sw-core-1", "fw-1", "fw-2", "proxy-1", "web-1", "adm-1", "db-1")) {
            assertThat(host, hasHostLabel(canvas, host), is(true));
        }
    }

    /**
     * The legend: one filled swatch, one outline and one label per configured vlan colour.
     * There are six colours in current-l2-additions.yml.
     */
    @Test
    public void drawsALegendEntryPerVlanColour() {
        Hosts hosts = create().getHosts();
        RecordingCanvas canvas = new RecordingCanvas();

        hosts.draw(canvas);

        for (String vlan : asList("trunk", "100", "201", "202", "203", "206")) {
            assertThat("legend " + vlan, canvas.getCalls(), hasItem(containsString(" " + vlan)));
        }
    }

    /** The cable colour comes from the non-white end - here the switch port's vlan. */
    @Test
    public void linkInfoCarriesBothEndsAndTheVlanColour() {
        L2GraphCreator creator = create();
        creator.getHosts();

        Map<String, LinkInfo> byLeft = new HashMap<>();
        for (Link link : creator.getLinks().getLinks()) {
            LinkInfo info = link.createLinkInfo();
            byLeft.put(info.getLeftAddress(), info);
        }

        // Port.displayName strips the "ether" prefix, so ether1 shows as 1.
        // ether1 is the trunk port of sw-core-1: 0xEF9A9A
        LinkInfo trunk = byLeft.get("sw-core-1/1");
        assertThat(trunk, notNullValue());
        assertThat(trunk.getRightAddress(), is("fw-1/eth0"));
        assertThat(trunk.getColorHex(), is("ef9a9a"));

        // ether5 carries vlan 202: 0xA5D6A7
        LinkInfo vlan202 = byLeft.get("sw-core-1/5");
        assertThat(vlan202.getRightAddress(), is("web-1/eth0"));
        assertThat(vlan202.getColorHex(), is("a5d6a7"));
    }

    /**
     * snakeyaml 1.11 applies the field's generic type arguments when building map keys,
     * so the numeric vlan ids in current-l2-additions.yml arrive as Strings. The whole
     * vlan-colour lookup depends on that - a Map<Integer, ...> would throw "No color for vlan".
     */
    @Test
    public void vlanColourKeysAreStringsNotIntegers() {
        L2CustomParameters params = L2CustomParameters.load(
                new File(TestFixtures.demoNetworkDir(), "current-l2-additions.yml"));

        Map<String, Color> colors = params.getVlanColors();

        assertThat(colors.keySet(), hasItems("trunk", "100", "201", "202", "203", "206"));
        assertThat(colors.get("202"), is(new Color(0xA5D6A7)));
    }

    @Test
    public void loadFailsWithTheFileNameWhenTheAdditionsFileIsMissing() {
        try {
            L2CustomParameters.load(new File(TestFixtures.demoNetworkDir(), "no-such-l2-additions.yml"));
            throw new AssertionError("expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("no-such-l2-additions.yml"));
        }
    }

    @Test
    public void addedLinksInjectHostsAndPortsThatNoHostFileDescribes() {
        L2CustomParameters params = new L2CustomParameters();
        params.vlanColors.put("vlan_inside", "0xF78181");
        params.addedLinks = asList("sw-9/ether1 (vlan_inside) >>>> host-9/eth0 (vlan_inside)");

        assertThat(params.getLinks(), hasSize(1));
        LinkHolder holder = params.getLinks().iterator().next();
        assertThat(holder.leftHost, is("sw-9"));
        assertThat(holder.leftPort, is("ether1"));
        assertThat(holder.rightHost, is("host-9"));
        assertThat(holder.rightPort, is("eth0"));
        assertThat(holder.linkType, is(LinkType.CUSTOM_ADDED));
    }

    /** The vlan part of each side is optional in the >>>> grammar. */
    @Test
    public void theLinkGrammarAcceptsSidesWithoutAVlan() {
        L2CustomParameters params = new L2CustomParameters();
        params.addedLinks = asList("sw-9/ether1 >>>> host-9/eth0");

        LinkHolder holder = params.getLinks().iterator().next();
        assertThat(holder.leftHost, is("sw-9"));
        assertThat(holder.rightHost, is("host-9"));
        assertThat(holder.rightPort, is("eth0"));
    }

    @Test
    public void removedLinksDropACableFromTheGraph() throws Exception {
        L2CustomParameters params = new L2CustomParameters();
        params.vlanColors = colorsOfTheDemoNetwork();
        params.removedLinks = asList("sw-core-1/ether5 >>>> web-1/eth0");

        HostAndLinkBuilder builder = build(params);
        builder.createHosts();

        assertThat(builder.createLinks().getLinks(), hasSize(9));
    }

    @Test
    public void removingACableThatDoesNotExistFails() throws Exception {
        L2CustomParameters params = new L2CustomParameters();
        params.vlanColors = colorsOfTheDemoNetwork();
        params.removedLinks = asList("sw-core-1/ether5 >>>> no-such-host/eth0");

        try {
            build(params).createHosts();
            throw new AssertionError("expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("Couldn't find"));
            assertThat(e.getMessage(), containsString("to delete"));
        }
    }

    @Test
    public void aVlanWithoutAConfiguredColourFails() throws Exception {
        L2CustomParameters params = new L2CustomParameters();
        params.vlanColors.put("trunk", "0xEF9A9A"); // 201/202/203/206/100 deliberately missing

        try {
            build(params).createHosts();
            throw new AssertionError("expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("No color for vlan"));
        }
    }

    @Test
    public void aLinkToAnUnknownHostFails() throws Exception {
        HostAndLinkBuilder builder = new HostAndLinkBuilder(
                new EmptyPositionManager(), new L2CustomParameters(), dao);
        builder.addHost("fw-1");
        builder.addPort("fw-1", "eth0", "trunk");

        try {
            builder.addLink("fw-1", "eth9", "sw-core-1/ether1");
            throw new AssertionError("expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("Couldn't add link from fw-1/eth9"));
            assertThat(e.getCause().getMessage(), containsString("does not have port eth9"));
        }
    }

    @Test
    public void addingAPortToAnUnknownHostFails() {
        HostAndLinkBuilder builder = new HostAndLinkBuilder(
                new EmptyPositionManager(), new L2CustomParameters(), dao);

        try {
            builder.addPort("nope", "eth0", "trunk");
            throw new AssertionError("expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("Host nope not found"));
        }
    }

    @Test
    public void createLinksBeforeCreateHostsFails() {
        HostAndLinkBuilder builder = new HostAndLinkBuilder(
                new EmptyPositionManager(), new L2CustomParameters(), dao);

        try {
            builder.createLinks();
            throw new AssertionError("expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("Hosts is null"));
        }
    }

    /** Positions fall back through PropertiesPositionManager to EmptyPositionManager. */
    @Test
    public void hostsGetLaidOutEvenWithoutAPositionsFile() {
        Hosts hosts = create().getHosts();

        assertThat(hosts.getMaxWidth(), greaterThan(0));
        assertThat(hosts.getMaxHeight(), greaterThan(0));
    }

    private static Map<String, String> colorsOfTheDemoNetwork() {
        Map<String, String> colors = new HashMap<>();
        colors.put("trunk", "0xEF9A9A");
        colors.put("100", "0xFFCC80");
        colors.put("201", "0xFFF59D");
        colors.put("202", "0xA5D6A7");
        colors.put("203", "0x90CAF9");
        colors.put("206", "0xE1BEE7");
        return colors;
    }

    private static boolean hasHostLabel(RecordingCanvas aCanvas, String aHostname) {
        for (String call : aCanvas.getCalls()) {
            if (call.startsWith("drawText ") && call.endsWith(" " + aHostname)) {
                return true;
            }
        }
        return false;
    }

    private L2GraphCreator create() {
        L2GraphCreator creator = new L2GraphCreator(dao, TestFixtures.demoNetworkDir(), "current");
        creator.create(new String[]{"internal", "ipmi"});
        return creator;
    }

    /**
     * Same derivation as L2GraphCreator but with hand-built parameters, so a test can
     * vary the additions file without writing one.
     */
    private HostAndLinkBuilder build(L2CustomParameters aParams) throws Exception {
        HostAndLinkBuilder builder = new HostAndLinkBuilder(new EmptyPositionManager(), aParams, dao);
        for (com.payneteasy.firewall.dao.model.THost host : dao.listHostsByFilter("internal", "ipmi")) {
            builder.addHost(host.name);
            for (com.payneteasy.firewall.dao.model.TInterface iface : host.getL2Interfaces()) {
                builder.addPort(host.name, iface.name, iface.vlan);
            }
        }
        for (com.payneteasy.firewall.dao.model.THost host : dao.listHostsByFilter("internal", "ipmi")) {
            for (com.payneteasy.firewall.dao.model.TInterface iface : host.getL2Interfaces()) {
                if (iface.link != null) {
                    builder.addLink(host.name, iface.name, iface.link);
                }
            }
        }
        return builder;
    }
}
