package com.payneteasy.firewall;

import com.payneteasy.firewall.dao.ConfigDaoYaml;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.dao.model.HostInterface;
import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TInterface;
import com.payneteasy.firewall.util.*;

import java.io.File;
import java.io.IOException;

import static com.payneteasy.firewall.util.Printer.out;

public class MainMikrotik {

    final IConfigDao dao;
    public MainMikrotik(IConfigDao configDao) {
        dao = configDao;
    }

    public static void main(String[] args) throws IOException {
        File   configDir = new File(args[0]);
        String hostname  = args[1];

        IConfigDao configDao = new ConfigDaoYaml(configDir);

        MainMikrotik generator = new MainMikrotik(configDao);
        if(args.length >= 3) {
            String vlan = args[2];
            generator.showVlanConfig(hostname, vlan);
        } else {
            for (TInterface iface : configDao.getHostByName(hostname).interfaces) {
                if("trunk".equals(iface.vlan)) {
                    continue;
                }

                if(Strings.hasText(iface.vlan)) {
                    generator.showVlanConfig(hostname, iface.vlan);
                }
            }
        }

    }

    private void generateConfig(String aHostname, String aOutputFilename) {
        MustacheFilePrinter out = new MustacheFilePrinter("");
    }

    private void showVlanConfig(String aHostname, String aVlan) {
        THost host = dao.getHostByName(aHostname);
        out("# %s %s", aHostname, aVlan);
        out();
        out("/interface ethernet switch egress-vlan-tag");
        out("add tagged-ports=%s vlan-id=%s", findTrunk(host), aVlan);
        out();
        out("/interface ethernet switch ingress-vlan-translation");
        out("add new-customer-vid=%s ports=%s sa-learning=yes", aVlan, findVlanPorts(host, aVlan));
        out();
        out("/interface ethernet switch vlan");
        out("add ports=%s,%s vlan-id=%s", findTrunk(host), findVlanPorts(host, aVlan), aVlan);
        out();
    }

    private String findVlanPorts(THost aHost, String aVlan) {
        StringAppender sb = new UniqueStringAppender(",");
        for (TInterface iface : aHost.interfaces) {
            if(aVlan.equals(iface.vlan)) {
                sb.append(iface.name);
            } else if(iface.vlan == null) {
                // finds linked switch and get VLAN from it
                HostInterface linkedInterface = dao.findLinkedInterface(aHost, iface);
                if(linkedInterface!= null && aVlan.equals(linkedInterface.iface.vlan)) {
                    sb.append(iface.name);
                }
            }
        }
        return sb.toStringFailIfEmpty("Could not find interfaces with VLAN="+aVlan+" at host="+aHost.name);
    }

    private String findTrunk(THost aHost) {
        for (TInterface iface : aHost.interfaces) {
            if("trunk".equals(iface.vlan)) {
                return iface.name;
            }
        }
        throw new IllegalStateException("trunk port not found at "+aHost.name);
    }

}
