package com.payneteasy.firewall.service.impl;

import com.payneteasy.firewall.dao.ConfigDaoYaml;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.dao.model.ChainType;
import com.payneteasy.firewall.dao.model.TCustomRule;
import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TInterface;
import com.payneteasy.firewall.service.ConfigurationException;
import com.payneteasy.firewall.service.model.InputMssPacket;
import com.payneteasy.firewall.service.model.LinkedVrrpPacket;
import com.payneteasy.firewall.service.model.VrrpPacket;
import com.payneteasy.firewall.testing.TestFixtures;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.payneteasy.firewall.service.impl.PacketServiceImpl.findAddress;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * The derived-packet semantics. The generated rules themselves are covered by
 * IptablesGoldenTest; this class pins the source-address selection and the error
 * branches that no valid config reaches.
 */
public class PacketServiceImplTest {

    private PacketServiceImpl service;

    @Before
    public void setUp() throws Exception {
        IConfigDao dao = new ConfigDaoYaml(TestFixtures.demoNetworkDir());
        service = new PacketServiceImpl(dao);
    }

    // ------------------------------------------------------- findAddress

    @Test
    public void testFindAddress() throws ConfigurationException {
        THost host = new THost();
        host.interfaces = new ArrayList<TInterface>();
        add(host, "eth0", "10.0.1.1");
        add(host, "eth0", "10.0.2.1");
        add(host, "eth0", "10.0.3.1");
        add(host, "eth0", "10.0.4.1");
        add(host, "eth0", "10.0.5.1");

        assertThat(findAddress("10.0.4.110", host), is("10.0.4.1"));
        assertThat(findAddress("10.0.2.255", host), is("10.0.2.1"));

        // 10.0.102.255 is in none of the networks, and calcEqualsBits compares the
        // Integer.toBinaryString() *strings*, which drop leading zeros - so the
        // "longest prefix" is not a real prefix length and the first interface wins.
        // Pinned, not fixed: this is the findAddress trap named in CLAUDE.md.
        assertThat(findAddress("10.0.102.255", host), is("10.0.1.1"));
    }

    /** A single-interface host always answers with its default address. */
    @Test
    public void findAddressShortCircuitsForASingleInterface() throws Exception {
        THost host = new THost();
        host.interfaces = new ArrayList<>();
        add(host, "eth0", "10.0.1.1");

        assertThat(findAddress("192.0.2.1", host), is("10.0.1.1"));
    }

    /** A 0.0.0.0 destination means "any", so the default address is used. */
    @Test
    public void findAddressUsesTheDefaultIpForAWildcardDestination() throws Exception {
        THost host = new THost();
        host.interfaces = new ArrayList<>();
        add(host, "eth0", "10.0.1.1");
        add(host, "eth1", "10.0.2.1");

        assertThat(findAddress("0.0.0.0", host), is("10.0.1.1"));
    }

    @Test
    public void findAddressFailsWhenEveryInterfaceIsSkipped() {
        THost host = new THost();
        host.name = "ghost";
        host.interfaces = new ArrayList<>();
        add(host, "eth0", "skip");
        add(host, "eth1", "skip");

        try {
            findAddress("10.0.1.5", host);
            throw new AssertionError("expected a ConfigurationException");
        } catch (ConfigurationException e) {
            assertThat(e.getMessage(), is("Can't find address for 10.0.1.5 in ghost"));
        }
    }

    @Test
    public void findAddressFailsWhenTheHostHasNoInterfaces() {
        THost host = new THost();
        host.name = "ghost";
        host.interfaces = new ArrayList<>();

        try {
            findAddress("10.0.1.5", host);
            throw new AssertionError("expected an IllegalStateException");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("No interfaces for host ghost"));
        }
    }

    @Test
    public void findAddressFailsOnAnUnparseableAddress() {
        THost host = new THost();
        host.name = "ghost";
        host.interfaces = new ArrayList<>();
        add(host, "eth0", "10.0.1.1");
        add(host, "eth1", "10.0.2.1");

        try {
            findAddress("not an address", host);
            throw new AssertionError("expected a ConfigurationException");
        } catch (ConfigurationException e) {
            assertThat(e.getMessage(), containsString("Can't parse ip address"));
        }
    }

    // -------------------------------------------------------------- VRRP

    @Test
    public void vrrpPacketsPairEachVirtualAddressWithTheOtherFirewall() {
        List<VrrpPacket> packets = service.getVrrpPackets("fw-1");

        assertThat(packets, not(empty()));
        for (VrrpPacket packet : packets) {
            assertThat(packet.remote_host, is("fw-2"));
            assertThat(packet.remote_address, notNullValue());
        }
    }

    @Test
    public void aHostWithoutVirtualAddressesHasNoVrrpPackets() {
        assertThat(service.getVrrpPackets("db-1"), empty());
    }

    /** An unpaired vip means the peer firewall is missing the same vip. */
    @Test
    public void anUnpairedVirtualAddressFails() throws Exception {
        IConfigDao dao = new ConfigDaoYaml(TestFixtures.demoNetworkDir());
        TInterface eth0 = dao.getHostByName("fw-1").interfaces.get(0);
        String saved = eth0.vip;
        eth0.vip = "10.99.99.1";
        try {
            new PacketServiceImpl(dao).getVrrpPackets("fw-1");
            throw new AssertionError("expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(),
                    is("There no any additional virtual interface with ip address 10.99.99.1"));
        } finally {
            eth0.vip = saved;
        }
    }

    /** A vip that collides with a real interface address is a configuration error. */
    @Test
    public void aVirtualAddressPairedWithABareInterfaceFails() throws Exception {
        IConfigDao dao = new ConfigDaoYaml(TestFixtures.demoNetworkDir());
        TInterface eth0 = dao.getHostByName("fw-1").interfaces.get(0);
        String saved = eth0.vip;
        eth0.vip = "10.20.20.21"; // web-1/eth0
        try {
            new PacketServiceImpl(dao).getVrrpPackets("fw-1");
            throw new AssertionError("expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("has pair with bare interface web-1"));
        } finally {
            eth0.vip = saved;
        }
    }

    /**
     * A linked VRRP packet needs the host on the far side of the local address to carry
     * a vip of its own, i.e. two VRRP routers cabled together. The demo network has no
     * such pair, so every host yields an empty list.
     */
    @Test
    public void theDemoNetworkHasNoLinkedVrrpPairs() {
        for (String host : new String[]{"fw-1", "fw-2", "web-1", "adm-1", "db-1", "proxy-1", "sw-core-1"}) {
            assertThat(host, service.getLinkedVrrpPackets(host), empty());
        }
    }

    @Test
    public void aLinkedVrrpPacketIsDerivedWhenTheDownstreamHostHasAVipToo() throws Exception {
        IConfigDao dao = new ConfigDaoYaml(TestFixtures.demoNetworkDir());
        // both hosts behind fw-1/eth0.202 get a vip, so the result does not depend on
        // the order in which the host files happen to be listed
        TInterface adm = dao.getHostByName("adm-1").interfaces.get(0);
        TInterface web = dao.getHostByName("web-1").interfaces.get(0);
        adm.vip = "10.20.20.98";
        web.vip = "10.20.20.99";
        try {
            List<LinkedVrrpPacket> packets = new PacketServiceImpl(dao).getLinkedVrrpPackets("fw-1");

            assertThat(packets, hasSize(1));
            assertThat(packets.get(0).local_interface, is("eth0.202"));
            assertThat(packets.get(0).remote_host, isOneOf("adm-1", "web-1"));
            assertThat(packets.get(0).virtual_address, isOneOf("10.20.20.98", "10.20.20.99"));
        } finally {
            adm.vip = null;
            web.vip = null;
        }
    }

    // --------------------------------------------------------------- MSS

    @Test
    public void mssPacketsComeFromTheSourceInterfaceThatDeclaresAnMss() throws Exception {
        Set<InputMssPacket> packets = service.getInputMssPackets("proxy-1");

        assertThat(packets, hasSize(1));
        InputMssPacket packet = packets.iterator().next();
        assertThat(packet.getMss(), is(1300));
        assertThat(packet.getSource_address(), is("198.51.100.50"));
        assertThat(packet.getSource_address_name(), is("partner-api.example.com"));
    }

    @Test
    public void aHostNoOneClampsForHasNoMssPackets() throws Exception {
        assertThat(service.getInputMssPackets("db-1"), empty());
    }

    /** InputMssPacket dedupes on the source address alone, which keeps the output stable. */
    @Test
    public void mssPacketsAreDedupedBySourceAddress() {
        InputMssPacket first = new InputMssPacket("10.0.0.1", 1300, "a", "ssh");
        InputMssPacket sameAddress = new InputMssPacket("10.0.0.1", 1400, "b", "http");
        InputMssPacket other = new InputMssPacket("10.0.0.2", 1300, "a", "ssh");

        assertThat(first, is(sameAddress));
        assertThat(first.hashCode(), is(sameAddress.hashCode()));
        assertThat(first, is(not(other)));
        assertThat(first.equals(null), is(false));
        assertThat(first.toString(), containsString("mss=1300"));
    }

    // ------------------------------------------------ blocked and custom

    @Test
    public void blockedAddressesAreReturnedForTheHostsThatDeclareThem() {
        assertThat(service.getBlockedIpAddresses("fw-1"), hasSize(2));
        assertThat(service.getBlockedIpAddresses("db-1"), empty());
    }

    @Test
    public void customRulesAreFilteredByChain() {
        assertThat(service.getCustomRules("fw-1", ChainType.INPUT), hasSize(1));
        assertThat(service.getCustomRules("fw-1", ChainType.POSTROUTING), hasSize(1));
        assertThat(service.getCustomRules("fw-1", ChainType.OUTPUT), empty());
        assertThat(service.getCustomRules("db-1", ChainType.INPUT), empty());
    }

    @Test
    public void aCustomRuleWithoutAChainFails() throws Exception {
        IConfigDao dao = new ConfigDaoYaml(TestFixtures.demoNetworkDir());
        THost host = dao.getHostByName("fw-1");
        TCustomRule broken = new TCustomRule();
        broken.rule = "-A INPUT -j ACCEPT";
        host.customRules.add(broken);
        try {
            new PacketServiceImpl(dao).getCustomRules("fw-1", ChainType.INPUT);
            throw new AssertionError("expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("No chain type for host fw-1"));
        } finally {
            host.customRules.remove(broken);
        }
    }

    // ------------------------------------------------------- access lists

    @Test
    public void anUnknownNameInAnAccessListFails() throws Exception {
        IConfigDao dao = new ConfigDaoYaml(TestFixtures.demoNetworkDir());
        List<String> access = dao.getHostByName("db-1").services.get(0).access;
        access.add("nope");
        try {
            new PacketServiceImpl(dao).getInputPackets("db-1");
            throw new AssertionError("expected a ConfigurationException");
        } catch (ConfigurationException e) {
            assertThat(rootCause(e).getMessage(),
                    containsString("Host or service 'nope' not found in access list for"));
        } finally {
            access.remove("nope");
        }
    }

    /**
     * A host whose every interface is skipped has no address to publish a service on,
     * and the failure surfaces from getDefaultIp before any rule is derived.
     */
    @Test
    public void aHostWithOnlySkippedInterfacesFails() throws Exception {
        IConfigDao dao = new ConfigDaoYaml(TestFixtures.demoNetworkDir());
        THost host = dao.getHostByName("db-1");
        String saved = host.interfaces.get(0).ip;
        host.interfaces.get(0).ip = "skip";
        try {
            new PacketServiceImpl(dao).getInputPackets("db-1");
            throw new AssertionError("expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("There no default ip address for host db-1"));
        } finally {
            host.interfaces.get(0).ip = saved;
        }
    }

    /** A service pinned to an address the host does not own cannot be placed on an interface. */
    @Test
    public void aServiceOnAnAddressTheHostDoesNotOwnFails() throws Exception {
        IConfigDao dao = new ConfigDaoYaml(TestFixtures.demoNetworkDir());
        com.payneteasy.firewall.dao.model.TService service = dao.getHostByName("db-1").services.get(0);
        String saved = service.url;
        service.url = "postgres://10.99.99.99";
        try {
            new PacketServiceImpl(dao).getInputPackets("db-1");
            throw new AssertionError("expected a ConfigurationException");
        } catch (ConfigurationException e) {
            assertThat(e.getMessage(), containsString("Can't find ip 10.99.99.99"));
        } finally {
            service.url = saved;
        }
    }

    @Test
    public void inputPacketsAreSortedBySourceNameThenProtocol() throws Exception {
        List<String> names = new ArrayList<>();
        for (com.payneteasy.firewall.service.model.InputPacket packet : service.getInputPackets("db-1")) {
            names.add(packet.source_address_name + " " + packet.app_protocol);
        }

        List<String> sorted = new ArrayList<>(names);
        java.util.Collections.sort(sorted);
        assertThat(names, is(sorted));
    }

    @Test
    public void outputPacketsAreSortedByDestinationNameThenProtocol() throws Exception {
        List<String> names = new ArrayList<>();
        for (com.payneteasy.firewall.service.model.OutputPacket packet : service.getOutputPackets("adm-1")) {
            names.add(packet.destination_address_name + " " + packet.app_protocol);
        }

        List<String> sorted = new ArrayList<>(names);
        java.util.Collections.sort(sorted);
        assertThat(names, is(sorted));
    }

    /** SNAT is decided from the destination address; the demo network SNATs to the internet. */
    @Test
    public void forwardPacketsCarrySnatForAPublicDestination() throws Exception {
        boolean found = false;
        for (com.payneteasy.firewall.service.model.Packet packet : service.getForwardPackets("fw-1")) {
            if ("SNAT".equals(packet.type)) {
                assertThat(packet.source_nat_address, is("198.51.100.10"));
                found = true;
            }
        }
        assertThat("an SNAT packet was derived", found, is(true));
    }

    /**
     * The nat: url of the service is the *public* address traffic arrives on, so
     * destination_nat_address is the gateway address; the real backend stays in
     * destination_address. iptables.vm renders them as "-d <nat> --to-destination <dest>".
     */
    @Test
    public void forwardPacketsCarryDnatForAPublicSource() throws Exception {
        boolean found = false;
        for (com.payneteasy.firewall.service.model.Packet packet : service.getForwardPackets("fw-1")) {
            if ("DNAT".equals(packet.type)) {
                assertThat(packet.destination_nat_address, is("198.51.100.10"));
                assertThat(packet.destination_nat_port, is(443));
                assertThat(packet.destination_address, is("10.20.20.21"));
                found = true;
            }
        }
        assertThat("a DNAT packet was derived", found, is(true));
    }

    /** A public source with no nat: on the service has no address to translate to. */
    @Test
    public void aPublicSourceWithoutANatAddressFails() throws Exception {
        IConfigDao dao = new ConfigDaoYaml(TestFixtures.demoNetworkDir());
        com.payneteasy.firewall.dao.model.TService webApp = null;
        for (com.payneteasy.firewall.dao.model.TService service : dao.getHostByName("web-1").services) {
            if ("web-app".equals(service.name)) {
                webApp = service;
            }
        }
        String saved = webApp.nat;
        webApp.nat = null;
        try {
            new PacketServiceImpl(dao).getForwardPackets("fw-1");
            throw new AssertionError("expected a ConfigurationException");
        } catch (ConfigurationException e) {
            assertThat(e.getMessage(), containsString("No nat for service https at host web-1"));
        } finally {
            webApp.nat = saved;
        }
    }

    @Test
    public void anUnknownHostFails() {
        try {
            service.getForwardPackets("nope");
            throw new AssertionError("expected an IllegalStateException");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Host nope not found"));
        }
    }

    private static Throwable rootCause(Throwable aThrowable) {
        Throwable cause = aThrowable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    private void add(THost aHost, String aEth, String aIp) {
        TInterface iface = new TInterface();
        iface.ip = aIp;
        iface.name = aEth;
        aHost.interfaces.add(iface);
    }
}
