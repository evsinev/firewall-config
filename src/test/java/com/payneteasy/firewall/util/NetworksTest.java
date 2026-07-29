package com.payneteasy.firewall.util;

import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TInterface;
import com.payneteasy.firewall.dao.model.TVirtualIpAddress;
import com.payneteasy.firewall.service.model.ServiceInfo;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * The network predicates behind the FORWARD suppression filters.
 *
 * Two of them are documented traps and are pinned here rather than fixed: isIpAddress
 * is not a validity check, and isInNetwork hard-codes /24.
 */
public class NetworksTest {

    /**
     * isIpAddress only means "usable as an address here": not blank and not the literal
     * "skip". A DNS name passes, which is exactly how UrlInfo decides to call resolveDns.
     */
    @Test
    public void isIpAddressIsNotAValidityCheck() {
        assertThat(Networks.isIpAddress("10.0.0.1"), is(true));
        assertThat(Networks.isIpAddress("host.example.com"), is(true));
        assertThat(Networks.isIpAddress("nonsense"), is(true));

        assertThat(Networks.isIpAddress(null), is(false));
        assertThat(Networks.isIpAddress(""), is(false));
        assertThat(Networks.isIpAddress("   "), is(false));
        assertThat(Networks.isIpAddress("skip"), is(false));
    }

    /**
     * isInNetwork compares everything before the last dot, i.e. it assumes /24 for both
     * sides. A pair that shares a /16 but not a /24 is reported as different networks.
     */
    @Test
    public void isInNetworkComparesTheTextBeforeTheLastDotOnly() {
        assertThat(Networks.isInNetwork("10.20.20.11", "10.20.20.99"), is(true));
        assertThat(Networks.isInNetwork("10.20.20.11", "10.20.21.11"), is(false));
        // same /16, different /24 - the netmask of the interface is never consulted
        assertThat(Networks.isInNetwork("10.20.0.1", "10.20.1.1"), is(false));
    }

    @Test
    public void isInNetworkIsFalseWhenEitherSideIsUnusable() {
        assertThat(Networks.isInNetwork("skip", "10.20.20.11"), is(false));
        assertThat(Networks.isInNetwork("10.20.20.11", "skip"), is(false));
        assertThat(Networks.isInNetwork(null, "10.20.20.11"), is(false));
    }

    @Test
    public void isInNetworkAlsoAcceptsInterfaces() {
        assertThat(Networks.isInNetwork(iface("eth0", "10.20.20.11"), iface("eth0", "10.20.20.21")), is(true));
        assertThat(Networks.isInNetwork(iface("eth0", "10.20.20.11"), iface("eth0", "10.20.2.21")), is(false));
    }

    @Test
    public void reversesTheNetworkForAPtrZoneName() {
        assertThat(Networks.get24NetworkReverse("10.20.20.31"), is("20.20.10"));
        assertThat(Networks.get24NetworkReverse("198.51.100.10"), is("100.51.198"));
    }

    @Test
    public void extractsTheHostPartOfTheAddress() {
        assertThat(Networks.get24MaskAddress("10.20.20.31"), is("31"));
        assertThat(Networks.get24MaskAddress("198.51.100.10"), is("10"));
    }

    @Test
    public void aSourceHostIsInTheSameNetworkAsAServiceAddress() {
        THost source = host("src", "10.20.20.1", iface("eth0", "10.20.20.31"));
        ServiceInfo service = new ServiceInfo();

        service.address = "10.20.20.21";
        assertThat(Networks.isInSameNetwork(source, service), is(true));

        service.address = "10.20.22.21";
        assertThat(Networks.isInSameNetwork(source, service), is(false));
    }

    /** Virtual addresses count too, so a vip in the peer network suppresses the rule. */
    @Test
    public void virtualAddressesParticipateInTheSameNetworkCheck() {
        TInterface withVip = iface("eth0", "10.20.20.31");
        withVip.vip = "10.20.99.1";
        THost source = host("src", "10.20.20.1", withVip);

        ServiceInfo service = new ServiceInfo();
        service.address = "10.20.99.5";

        assertThat(Networks.isInSameNetwork(source, service), is(true));
    }

    @Test
    public void vipsListEntriesAlsoParticipate() {
        TInterface withVips = iface("eth0", "10.20.20.31");
        TVirtualIpAddress vip = new TVirtualIpAddress();
        vip.ip = "10.20.98.1";
        withVips.vips = Arrays.asList(vip);

        ServiceInfo service = new ServiceInfo();
        service.address = "10.20.98.7";

        assertThat(Networks.isInSameNetwork(host("src", "10.20.20.1", withVips), service), is(true));
    }

    @Test
    public void twoHostsAreInTheSameNetworkWhenAnyAddressPairMatches() {
        THost left = host("left", "10.20.20.1", iface("eth0", "10.20.20.31"), iface("ipmi", "skip"));
        THost right = host("right", "10.20.20.1", iface("eth0", "10.20.20.21"));
        THost elsewhere = host("far", "10.20.22.1", iface("eth0", "10.20.22.21"));

        assertThat(Networks.isInSameNetwork(left, right), is(true));
        assertThat(Networks.isInSameNetwork(left, elsewhere), is(false));
    }

    /** Symmetric: it is enough that either host uses the other as its gateway. */
    @Test
    public void hasCommonGatewayWorksInBothDirections() {
        THost gateway = host("gw", "198.51.100.1", iface("eth0", "10.20.20.1"));
        THost behind = host("behind", "10.20.20.1", iface("eth0", "10.20.20.31"));
        THost unrelated = host("other", "10.20.22.1", iface("eth0", "10.20.22.21"));

        assertThat(Networks.hasCommonGateway(behind, gateway), is(true));
        assertThat(Networks.hasCommonGateway(gateway, behind), is(true));
        assertThat(Networks.hasCommonGateway(behind, unrelated), is(false));
    }

    private static TInterface iface(String aName, String aIp) {
        TInterface iface = new TInterface();
        iface.name = aName;
        iface.ip = aIp;
        return iface;
    }

    private static THost host(String aName, String aGateway, TInterface... aInterfaces) {
        THost host = new THost();
        host.name = aName;
        host.gw = aGateway;
        host.interfaces = new ArrayList<>(Arrays.asList(aInterfaces));
        return host;
    }
}
