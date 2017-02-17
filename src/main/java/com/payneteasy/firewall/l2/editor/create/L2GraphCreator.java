package com.payneteasy.firewall.l2.editor.create;

import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TInterface;
import com.payneteasy.firewall.l2.editor.create.HostAndLinkBuilder;
import com.payneteasy.firewall.l2.editor.model.Hosts;
import com.payneteasy.firewall.l2.editor.model.Links;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.util.Collection;

import static com.payneteasy.firewall.util.Strings.hasText;

public class L2GraphCreator {

    final IConfigDao configDao;
    HostAndLinkBuilder builder;


    public L2GraphCreator(IConfigDao configDao, File aConfigDir, String aPrefix) {
        this.configDao = configDao;
        PropertiesPositionManager positionManager = new PropertiesPositionManager(new File(aConfigDir, aPrefix  + "-l2-positions.properties"), new EmptyPositionManager());
        builder = new HostAndLinkBuilder( positionManager, L2CustomParameters.load(new File(aConfigDir, aPrefix + "-l2-additions.yml")));
    }

    public void create() {
        final Collection<THost> hosts = configDao.listHostsByFilter("internal", "ipmi", "internet");

        // add hosts
        for (THost host : hosts) {
            builder.addHost(host.name);
            for (TInterface iface : host.interfaces) {
                if(hasText(iface.name)) {
                    builder.addPort(host.name, iface.name, iface.vlan);
                }
//                if(hasText(iface.port)) {
//                    builder.addPort(host.name, iface.port);
//                }
            }
        }

        // links
        for (THost host : hosts) {
            for (TInterface iface : host.interfaces) {
                if(hasText(iface.link)) {
                    builder.addLink(host.name, iface.name, iface.link);
                }
            }
        }

    }

    public Hosts getHosts() {
        return builder.createHosts();
    }

    public Links getLinks() {
        return builder.createLinks();
    }


}
