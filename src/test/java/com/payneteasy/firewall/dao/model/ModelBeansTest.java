package com.payneteasy.firewall.dao.model;

import com.payneteasy.firewall.service.model.Access;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * The derivation getters on the yaml-mapped beans, including two documented traps:
 * getLongNetmask supports only /24 and /16, and it throws for the literal "24".
 */
public class ModelBeansTest {

    // ----------------------------------------------------------- TInterface

    /**
     * Only an absent netmask means /24. Writing "netmask: 24" explicitly - the value a
     * reader would expect to be the supported one - throws. Pinned, not fixed.
     */
    @Test
    public void onlyAnAbsentNetmaskMeansSlash24() {
        assertThat(iface("eth0", "10.0.0.1").getLongNetmask(), is("255.255.255.0"));

        TInterface sixteen = iface("eth0", "10.0.0.1");
        sixteen.netmask = "16";
        assertThat(sixteen.getLongNetmask(), is("255.255.0.0"));
    }

    @Test
    public void anExplicitSlash24NetmaskThrows() {
        TInterface explicit = iface("eth0", "10.0.0.1");
        explicit.netmask = "24";

        try {
            explicit.getLongNetmask();
            throw new AssertionError("expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("Netmask not supported yet: 24"));
        }
    }

    @Test
    public void anyOtherNetmaskThrows() {
        TInterface eight = iface("eth0", "10.0.0.1");
        eight.netmask = "8";

        try {
            eight.getLongNetmask();
            throw new AssertionError("expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("Netmask not supported yet: 8"));
        }
    }

    @Test
    public void theVrrpPriorityDefaultsToOneHundred() {
        assertThat(iface("eth0", "10.0.0.1").getVrrpPriority(), is("100"));

        TInterface explicit = iface("eth0", "10.0.0.1");
        explicit.vrrpPriority = "150";
        assertThat(explicit.getVrrpPriority(), is("150"));
    }

    @Test
    public void skipIpAddressDetectsTheSkipLiteral() {
        assertThat(iface("ipmi", "skip").skipIpAddress(), is(true));
        assertThat(iface("eth0", "10.0.0.1").skipIpAddress(), is(false));
        assertThat(iface("eth0", null).skipIpAddress(), is(false));
    }

    @Test
    public void getAllIpAddressesCollectsTheRealTheVipAndTheVipsList() {
        TInterface iface = iface("eth0", "10.0.0.1");
        iface.vip = "10.0.0.254";
        TVirtualIpAddress extra = new TVirtualIpAddress();
        extra.ip = "10.0.0.253";
        extra.names = "gw.example.test";
        iface.vips = Arrays.asList(extra);

        assertThat(iface.getAllIpAddresses(), contains("10.0.0.1", "10.0.0.254", "10.0.0.253"));
    }

    @Test
    public void getAllIpAddressesDropsSkippedAddresses() {
        assertThat(iface("ipmi", "skip").getAllIpAddresses(), empty());
    }

    @Test
    public void theInterfaceToStringNamesItsFields() {
        assertThat(iface("eth0", "10.0.0.1").toString(), allOf(
                containsString("name='eth0'"), containsString("ip='10.0.0.1'")));
    }

    // ---------------------------------------------------------------- THost

    @Test
    public void theDefaultIpIsTheFirstInterfaceAddress() {
        assertThat(host("h", iface("eth0", "10.0.0.1"), iface("eth1", "10.0.1.1")).getDefaultIp(),
                is("10.0.0.1"));
    }

    /** A leading skip is stepped over rather than being reported as the default. */
    @Test
    public void theDefaultIpSkipsALeadingSkippedInterface() {
        assertThat(host("h", iface("ipmi", "skip"), iface("eth0", "10.0.0.1")).getDefaultIp(),
                is("10.0.0.1"));
    }

    @Test
    public void aHostWithOnlySkippedInterfacesHasNoDefaultIp() {
        try {
            host("ghost", iface("ipmi", "skip")).getDefaultIp();
            throw new AssertionError("expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("There no default ip address for host ghost"));
        }
    }

    @Test
    public void getAllIpAddressesFlattensEveryInterface() {
        THost host = host("h", iface("eth0", "10.0.0.1"), iface("ipmi", "skip"), iface("eth1", "10.0.1.1"));

        assertThat(host.getAllIpAddresses(), contains("10.0.0.1", "10.0.1.1"));
    }

    /** Bonded members are not drawn on the L2 diagram; the bond itself is. */
    @Test
    public void l2InterfacesDropBondMembers() {
        THost host = host("h", iface("eth0", "10.0.0.1"), iface("bond0", "10.0.2.1"),
                iface("bond0.100", "10.0.3.1"));

        assertThat(host.getL2Interfaces(), hasSize(1));
        assertThat(host.getL2Interfaces().get(0).name, is("eth0"));
    }

    @Test
    public void theHostToStringNamesItsFields() {
        assertThat(host("h", iface("eth0", "10.0.0.1")).toString(), containsString("name='h'"));
    }

    // -------------------------------------------------------- HostInterface

    @Test
    public void aHostInterfaceIsIdentifiedByItsHostSlashInterfaceLink() {
        THost host = host("fw-1", iface("eth0", "10.0.0.1"));
        HostInterface left = new HostInterface(host, host.interfaces.get(0));
        HostInterface same = new HostInterface(host, host.interfaces.get(0));
        HostInterface other = new HostInterface(host, iface("eth1", "10.0.1.1"));

        assertThat(left.link, is("fw-1/eth0"));
        assertThat(left.toString(), is("fw-1/eth0"));
        assertThat(left, is(same));
        assertThat(left.hashCode(), is(same.hashCode()));
        assertThat(left, is(not(other)));
        assertThat(left.equals(null), is(false));
        assertThat(left.equals("fw-1/eth0"), is(false));
    }

    // --------------------------------------------------------------- Access

    @Test
    public void accessSortsByHostNameAndPrintsTheServiceInBrackets() {
        Access adm = new Access(host("adm-1", iface("eth0", "10.0.0.1")));
        Access web = new Access(host("web-1", iface("eth0", "10.0.0.2")), "web-app");

        assertThat(adm.compareTo(web), is(lessThan(0)));
        assertThat(web.compareTo(adm), is(greaterThan(0)));
        assertThat(adm.toString(), is("adm-1"));
        assertThat(web.toString(), is("web-1[web-app]"));
    }

    /** A hostless Access compares equal to everything rather than throwing. */
    @Test
    public void accessComparisonToleratesAMissingHost() {
        Access empty = new Access();
        Access adm = new Access(host("adm-1", iface("eth0", "10.0.0.1")));

        assertThat(empty.compareTo(adm), is(0));
        assertThat(adm.compareTo(new Access()), is(0));
        assertThat(adm.compareTo(null), is(0));
    }

    private static TInterface iface(String aName, String aIp) {
        TInterface iface = new TInterface();
        iface.name = aName;
        iface.ip = aIp;
        return iface;
    }

    private static THost host(String aName, TInterface... aInterfaces) {
        THost host = new THost();
        host.name = aName;
        host.gw = "10.0.0.254";
        host.interfaces = new ArrayList<>(Arrays.asList(aInterfaces));
        return host;
    }
}
