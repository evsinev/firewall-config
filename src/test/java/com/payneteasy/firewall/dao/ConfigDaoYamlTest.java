package com.payneteasy.firewall.dao;

import com.payneteasy.firewall.dao.model.HostInterface;
import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TInterface;
import com.payneteasy.firewall.dao.model.TService;
import com.payneteasy.firewall.service.ConfigurationException;
import com.payneteasy.firewall.testing.TestFixtures;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * The loader and its lookups.
 *
 * A fresh instance per test: listHosts() hands back the live internal list and
 * processServicesLinks appends shared TService instances, so a mutating test must not
 * leak into the next one.
 *
 * Every validation rule fires from the constructor, so each one needs its own
 * deliberately broken fixture directory under src/test/resources/config.
 */
public class ConfigDaoYamlTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private ConfigDaoYaml dao;

    @Before
    public void setUp() throws Exception {
        dao = new ConfigDaoYaml(TestFixtures.demoNetworkDir());
    }

    // ---------------------------------------------------------------- loading

    @Test
    public void loadsEveryHostFile() {
        assertThat(dao.listHosts(), hasSize(8));
    }

    /** name and group come from the filesystem, not from the yml body. */
    @Test
    public void theHostNameIsTheFileNameAndTheGroupIsTheDirectory() {
        THost host = dao.getHostByName("fw-1");

        assertThat(host.name, is("fw-1"));
        assertThat(host.group, is("internal"));
        assertThat(dao.getHostByName("sw-core-1").group, is("ipmi"));
        // dots in the file name survive - only the .yml suffix is stripped
        assertThat(dao.getHostByName("partner-api.example.com").group, is("external"));
    }

    /**
     * loadHost strips the suffix with replaceAll(".yml", ""), and "." is a regex
     * metacharacter, so the pattern also matches "xyml" inside the name: axyml.yml
     * loads as the host "a". Pinned, not fixed - renaming a host changes its rules.
     */
    @Test
    public void theYmlSuffixIsStrippedWithARegexSoAnyCharPlusYmlMatches() throws Exception {
        ConfigDaoYaml weird = new ConfigDaoYaml(TestFixtures.configDir("weird-filename"));

        assertThat(weird.listHosts().get(0).name, is("a"));
    }

    @Test
    public void listsTheHostDirectoriesAsGroups() {
        assertThat(dao.listGroups(), containsInAnyOrder("internal", "external", "ipmi"));
    }

    @Test
    public void aMissingConfigDirFails() {
        try {
            new ConfigDaoYaml(new File(tmp.getRoot(), "nope"));
            throw new AssertionError("expected an IllegalStateException");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("is not exists"));
        }
    }

    // ------------------------------------------------------- services_links

    /**
     * A linked service is the very same object on every host that links it - which is
     * why no test may mutate a TService it got from the dao.
     */
    @Test
    public void aLinkedServiceIsTheSameInstanceOnEveryHostThatLinksIt() {
        TService onAdm = serviceNamed(dao.getHostByName("adm-1"), "ssh");
        TService onDb = serviceNamed(dao.getHostByName("db-1"), "ssh");
        TService onFw = serviceNamed(dao.getHostByName("fw-1"), "ssh");

        assertThat(onDb, sameInstance(onAdm));
        assertThat(onFw, sameInstance(onAdm));
    }

    @Test
    public void twoHostsNamingTheSameServiceFail() {
        assertConstructorFails("broken-duplicate-service", "Service 'ssh' already exists");
    }

    @Test
    public void linkingAServiceNoHostDeclaresFails() {
        assertConstructorFails("broken-unknown-services-link",
                "There are no service 'nope' in any host");
    }

    @Test
    public void linkingAServiceTheHostAlreadyOwnsFails() {
        assertConstructorFails("broken-service-already-in-host",
                "Service 'ssh' is already in the host");
    }

    @Test
    public void linkingAServiceWhoseUrlTheHostAlreadyUsesFails() {
        assertConstructorFails("broken-duplicate-url", "Service url 'ssh' is already in the host");
    }

    // ----------------------------------------------------------- protocols

    @Test
    public void findsAProtocolByName() {
        assertThat(dao.findProtocol("https").port, is(443));
        assertThat(dao.findProtocol("https").protocol, is("tcp"));
        assertThat(dao.listProtocols().protocols, hasSize(9));
    }

    @Test
    public void anUnknownProtocolFails() {
        try {
            dao.findProtocol("gopher");
            throw new AssertionError("expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("Protocol gopher not found in protocols.yml"));
        }
    }

    @Test
    public void aProtocolWithoutATransportProtocolFails() {
        assertConstructorFails("broken-protocol-null", "protocol is null for ssh");
    }

    @Test
    public void aProtocolWithoutAPortFails() {
        assertConstructorFails("broken-protocol-zero-port", "Port is empty for ssh");
    }

    // -------------------------------------------------------------- lookups

    @Test
    public void findsHostsByGroupAndByGroups() {
        assertThat(dao.findHostsByGroup("internal"), hasSize(6));
        assertThat(dao.findHostsByGroup("nope"), empty());
        assertThat(dao.findHostsByGroups("external", "ipmi"), hasSize(2));
    }

    @Test
    public void anUnknownHostFails() {
        try {
            dao.getHostByName("nope");
            throw new AssertionError("expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("Host nope not found"));
        }
    }

    @Test
    public void isHostExistDoesNotThrow() {
        assertThat(dao.isHostExist("fw-1"), is(true));
        assertThat(dao.isHostExist("nope"), is(false));
    }

    /** A "-*" pattern is a prefix match with the suffix removed. */
    @Test
    public void findsHostsByPattern() {
        assertThat(names(dao.getHostByPattern("fw-*")), containsInAnyOrder("fw-1", "fw-2"));
        assertThat(names(dao.getHostByPattern("adm-*")), contains("adm-1"));
        assertThat(dao.getHostByPattern("nope-*"), empty());
    }

    /**
     * listHostsByFilter applies every argument as a group and then again as a pattern,
     * so an argument that is both duplicates its hosts. Pinned - the L3 and L2 commands
     * rely on this being a plain concatenation.
     */
    @Test
    public void listHostsByFilterAppliesEachArgumentAsAGroupAndAsAPattern() {
        assertThat(dao.listHostsByFilter("internal", "ipmi"), hasSize(7));
        assertThat(dao.listHostsByFilter("nope"), empty());
    }

    @Test
    public void resolvesADnsNameFromAVirtualAddress() throws Exception {
        assertThat(dao.resolveDns("gw.demo.example.com"), is("198.51.100.10"));
    }

    @Test
    public void anUnknownDnsNameFails() {
        try {
            dao.resolveDns("nope.example.com");
            throw new AssertionError("expected a ConfigurationException");
        } catch (ConfigurationException e) {
            assertThat(e.getMessage(), is("DNS name 'nope.example.com' not found"));
        }
    }

    @Test
    public void findsTheHostsWhoseGatewayIsOneOfTheGivenInterfaces() {
        THost fw1 = dao.getHostByName("fw-1");

        assertThat(names(dao.findHostByGw(fw1.interfaces)),
                hasItems("adm-1", "db-1", "proxy-1", "web-1", "sw-core-1"));
        assertThat(dao.findHostInterfacesByGw(fw1.interfaces), not(empty()));
    }

    @Test
    public void findsTheInterfaceOnTheFarEndOfACable() {
        THost switchHost = dao.getHostByName("sw-core-1");
        TInterface ether5 = interfaceNamed(switchHost, "ether5");

        // sw-core-1/ether5 declares link: web-1/eth0, so the far end is found from web-1
        HostInterface linked = dao.findLinkedInterface(dao.getHostByName("web-1"),
                interfaceNamed(dao.getHostByName("web-1"), "eth0"));

        assertThat(linked, notNullValue());
        assertThat(linked.host.name, is("sw-core-1"));
        assertThat(linked.iface.name, is("ether5"));

        // the switch port itself is not the far end of anything
        assertThat(dao.findLinkedInterface(switchHost, ether5), nullValue());
    }

    @Test
    public void readsTheNetworkNames() {
        assertThat(dao.listNetworksNames().keySet(), hasSize(5));
        assertThat(dao.listNetworksNames().get("10.20.20.0"), is("app"));
    }

    @Test
    public void aMissingNetworksYmlFailsWithAnExampleFile() throws Exception {
        ConfigDaoYaml withoutNetworks = new ConfigDaoYaml(TestFixtures.configDir("no-networks-yml"));

        try {
            withoutNetworks.listNetworksNames();
            throw new AssertionError("expected an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("Couldn't load"));
            assertThat(e.getMessage(), containsString("Example network 1"));
        }
    }

    // --------------------------------------------------------- page history

    @Test
    public void findPageHistoryCreatesAnEntryOnAMiss() {
        assertThat(dao.findPageHistory("brand-new_details").pageName, is("brand-new_details"));
        assertThat(dao.findPageHistory("brand-new_details").pageHash, is(0L));
    }

    @Test
    public void aZeroPageHashInTheHistoryFileFails() {
        assertConstructorFails("broken-page-hash", "page hash is empty for srv-a_details");
    }

    @Test
    public void persistsThePageHistoryIntoTheConfigDir() throws Exception {
        File configDir = tmp.newFolder("persist");
        TestFixtures.copyDemoNetworkInputs(configDir);
        ConfigDaoYaml writable = new ConfigDaoYaml(configDir);
        writable.findPageHistory("some_page").pageHash = 42L;

        writable.persistPagesHistory();

        File written = new File(configDir, "pages_history.yml");
        assertThat(written.isFile(), is(true));
        assertThat(new ConfigDaoYaml(configDir).thePagesHistoryMap.get("some_page").pageHash, is(42L));
    }

    // ------------------------------------------------------------- helpers

    private void assertConstructorFails(String aFixture, String aExpectedMessage) {
        try {
            new ConfigDaoYaml(TestFixtures.configDir(aFixture));
            throw new AssertionError("expected the constructor of " + aFixture + " to fail");
        } catch (IOException | IllegalStateException e) {
            assertThat(aFixture, e.getMessage(), containsString(aExpectedMessage));
        }
    }

    private static TService serviceNamed(THost aHost, String aName) {
        for (TService service : aHost.services) {
            if (aName.equals(service.name)) {
                return service;
            }
        }
        throw new AssertionError("No service " + aName + " on " + aHost.name);
    }

    private static TInterface interfaceNamed(THost aHost, String aName) {
        for (TInterface iface : aHost.interfaces) {
            if (aName.equals(iface.name)) {
                return iface;
            }
        }
        throw new AssertionError("No interface " + aName + " on " + aHost.name);
    }

    private static java.util.List<String> names(java.util.Collection<? extends THost> aHosts) {
        java.util.List<String> names = new java.util.ArrayList<>();
        for (THost host : aHosts) {
            names.add(host.name);
        }
        return names;
    }
}
