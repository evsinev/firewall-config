package com.payneteasy.firewall.dao.model;

import com.payneteasy.firewall.util.Yamls;
import org.junit.Test;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;

import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * snakeyaml round-trips the plain public-field beans in dao/model. This is the reason
 * those beans carry no lombok: snakeyaml constructs them field by field.
 */
public class ThostYamlRoundTripTest {

    @Test
    public void aHostSurvivesADumpAndLoad() {
        Yaml yaml = yaml();

        THost loaded = yaml.loadAs(yaml.dump(host()), THost.class);

        assertThat(loaded.gw, is("10.0.0.1"));
        assertThat(loaded.description, is("host description"));
        assertThat(loaded.interfaces, hasSize(2));
        assertThat(loaded.interfaces.get(0).name, is("eth0"));
        assertThat(loaded.interfaces.get(0).ip, is("10.0.0.2"));
        assertThat(loaded.interfaces.get(1).name, is("eth0:1"));
        assertThat(loaded.services, hasSize(1));
        assertThat(loaded.services.get(0).program, is("nginx"));
        assertThat(loaded.services.get(0).access, contains("internet", "remote"));
    }

    /** dumpAs with Tag.MAP writes plain yaml, without the !!com.payneteasy class tag. */
    @Test
    public void dumpAsMapEmitsNoClassTag() {
        Yaml yaml = yaml();

        String text = yaml.dumpAs(host(), Tag.MAP, null);

        assertThat(text, not(containsString("!!com.payneteasy")));
        assertThat(text, containsString("gw: 10.0.0.1"));
    }

    /** A dump without Tag.MAP does carry the tag, which is what loadAs needs. */
    @Test
    public void aPlainDumpCarriesTheClassTag() {
        assertThat(yaml().dump(host()), containsString("!!com.payneteasy.firewall.dao.model.THost"));
    }

    /** Yamls, not new Yaml(): snakeyaml 2.x refuses the !!com.payneteasy tag on load. */
    private static Yaml yaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.AUTO);
        return Yamls.newYaml(options);
    }

    private static THost host() {
        TService service = new TService();
        service.nat = "http://gate.example.test";
        service.program = "nginx";
        service.access = Arrays.asList("internet", "remote");

        TInterface eth0 = new TInterface();
        eth0.ip = "10.0.0.2";
        eth0.name = "eth0";

        TInterface alias = new TInterface();
        alias.ip = "10.0.0.2";
        alias.name = "eth0:1";

        THost host = new THost();
        host.description = "host description";
        host.justification = "host just";
        host.gw = "10.0.0.1";
        host.interfaces = new ArrayList<>(Arrays.asList(eth0, alias));
        host.services = new ArrayList<>(Arrays.asList(service));
        return host;
    }
}
