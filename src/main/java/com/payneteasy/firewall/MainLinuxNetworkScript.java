package com.payneteasy.firewall;

import com.payneteasy.firewall.dao.ConfigDaoYaml;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.dao.model.HostInterface;
import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TInterface;
import com.payneteasy.firewall.util.Printer;
import com.payneteasy.firewall.util.ShellFilePrinter;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.util.Collections.singletonList;

public class MainLinuxNetworkScript {


    IConfigDao dao;

    public MainLinuxNetworkScript(IConfigDao configDao) {
        dao = configDao;
    }

    public static void main(String[] args) throws IOException {
        File configDir = new File(args[0]);
        String host = args[1];
        String iface = args[2];

        IConfigDao configDao = new ConfigDaoYaml(configDir);

        new MainLinuxNetworkScript(configDao).showIfcfg(host, iface);
    }

    private void showIfcfg(String aHost, String iface) {
        THost host = dao.getHostByName(aHost);
        Printer.out("# %s", aHost+" "+iface);

        ShellFilePrinter printer = new ShellFilePrinter("/etc/sysconfig/network-scripts/ifcfg-"+iface);
        printer.out("DEVICE=%s", iface);
        printer.out("BOOTPROTO=none");
        printer.out("ONBOOT=yes");
        printer.out("IPADDR=%s", findIpAddress(host, iface));
        // todo find netmask
        printer.out("NETMASK=255.255.255.0");

        boolean shouldBeVlan = iface.contains(".");

        if(shouldBeVlan) {
            String vlan = getVlan(host, iface);
            printer.out("# vlan = %s", vlan);
            String vlanSubstring = extractVlan(iface);
            if(!vlan.equals(vlanSubstring)) {
                throw new IllegalStateException("Interface "+host.name+"."+iface+" does not match name conversion. Interface name must be end with the VLAN "+vlan+" but not with "+vlanSubstring);
            }
            printer.out("VLAN=yes");
        }

        printer.close();

    }

    private String extractVlan(String aInterfaceName) {
        int pos = aInterfaceName.indexOf(".");
        return aInterfaceName.substring(pos+1);
    }

    private String getVlan(THost aHost, String aInterfaceName) {

        List<HostInterface> hostInterfaces = dao.findHostInterfacesByGw(singletonList(findInterface(aHost, aInterfaceName)));
        if(hostInterfaces.size() == 0) {
            throw new IllegalStateException("No interfaces connected to host "+aHost.name+" and interface "+aInterfaceName);
        }

        // finds hostInterfaces connected to vlan
        Set<String> vlans = new TreeSet<>();
        for (THost leftHosts : dao.listHosts()) {
            for (TInterface leftInterface : leftHosts.interfaces) {
                for (HostInterface hostInterface : hostInterfaces) {
                    if(hostInterface.link.equals(leftInterface.link)) {
                        if(leftInterface.vlan!=null) {
                            vlans.add(leftInterface.vlan);
                        }
                    }
                }
            }
        }

        if(vlans.isEmpty()) {
            throw new IllegalStateException("No VLANs found for interfaces "+hostInterfaces);
        }

        if(vlans.size()>1) {
            throw new IllegalStateException("There are more than 1 VLAN ("+vlans+") for interfaces "+hostInterfaces);
        }

        return vlans.iterator().next();
    }

    private TInterface findInterface(THost aHost, String aInterfaceName) {
        for (TInterface iface : aHost.interfaces) {
            if(aInterfaceName.equals(iface.name)) {
                return iface;
            }
        }
        throw new IllegalStateException("Could not find "+aInterfaceName+ " in "+aHost.name);
    }

    private boolean isVlan(String aHost, String aInterfaceName) {
        String link = aHost+"/"+aInterfaceName;
        for (THost host : dao.listHosts()) {
            for (TInterface iface : host.interfaces) {
                if(link.equals(iface.link)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String findIpAddress(THost aHost, String aInterfaceName) {
        for (TInterface iface : aHost.interfaces) {
            if(aInterfaceName.equals(iface.name)) {
                return iface.ip;
            }
        }
        throw new IllegalStateException("Could not find interface "+aInterfaceName+ " in host "+aHost.name);
    }


}
