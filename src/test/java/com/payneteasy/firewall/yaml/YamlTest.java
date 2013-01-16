package com.payneteasy.firewall.yaml;

import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TInterface;
import com.payneteasy.firewall.dao.model.TService;
import org.junit.Test;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 */
public class YamlTest {

    @Test
    public void test() throws IOException {
        TService service = new TService();
        service.nat = "http://gate.ariuspay.ru";
        service.program = "nginx";
        service.access = Arrays.asList("internet", "remote");

        TInterface iface = new TInterface();
        iface.ip = "10.0.0.2";
        iface.name = "eth0";
        //iface.services.add(service);

        TInterface iface2 = new TInterface();
        iface2.ip = "10.0.0.2";
        iface2.name = "eth0:1";
        //iface2.services.add(service);

        THost host = new THost();
        host.description = "host description";
        host.justification = "host just";
        host.gw = "10.0.0.1";
        host.interfaces  = new ArrayList<TInterface>();
        host.interfaces.add(iface);
        host.interfaces.add(iface2);
        host.services = new ArrayList<TService>();

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.AUTO);
//        options.setPrettyFlow(true);

        Yaml yaml = new Yaml(options);

        FileWriter out = new FileWriter("target/host.yaml");
        try {
            yaml.dump(host, out);
        } finally {
            out.close();
        }

        FileReader in = new FileReader("target/host.yaml");
        try {
            THost loadedHost = yaml.loadAs(in, THost.class);
            String text =  yaml.dumpAs(loadedHost, Tag.MAP, null);
            System.out.println("text = " + text);
        } finally {
            in.close();
        }
    }
}
