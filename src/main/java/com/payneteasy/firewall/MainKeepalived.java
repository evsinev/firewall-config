package com.payneteasy.firewall;

import com.payneteasy.firewall.dao.ConfigDaoYaml;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.dao.model.HostInterface;
import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TInterface;
import com.payneteasy.firewall.util.ShellFilePrinter;
import com.payneteasy.firewall.util.Strings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainKeepalived {
    IConfigDao dao;

    public MainKeepalived(IConfigDao aDao) {
        dao = aDao;
    }

    public static void main(String[] args) throws IOException {
        File configDir = new File(args[0]);
        String host = args[1];
        IConfigDao configDao = new ConfigDaoYaml(configDir);

        MainKeepalived generator = new MainKeepalived(configDao);
        generator.createKeepalivedConf(host);

    }

    private void createKeepalivedConf(String aHostname) {
        THost host = dao.getHostByName(aHostname);

        List<VrrpInterface> interfaces = new ArrayList<>();
        ShellFilePrinter p = new ShellFilePrinter("/etc/keepalived/keepalived.conf");
        for (TInterface iface : host.interfaces) {
            if(Strings.hasText(iface.vip)) {
                checkPairedInterface(host, iface);
                VrrpInterface vi = new VrrpInterface();
                vi.instance = extractVlan(iface.name);
                vi.interfaceName = iface.name;
                vi.ip = iface.vip;
                vi.netmask = iface.netmask;
                vi.priority = iface.getVrrpPriority();

                interfaces.add(vi);
            }
        }
        p.mustache("keepalived.mustache", "interfaces", interfaces);
        p.close();
    }

    private String extractVlan(String aName) {
        int pos = aName.indexOf('.');
        if(pos < 0) {
            throw new IllegalStateException("interface does not have VLAN suffux. Example: eth0.100");
        }
        return aName.substring(pos+1);
    }


    private void checkPairedInterface(THost aHost, TInterface aInterface) {
        String vip = aInterface.vip;
        HostInterface right = new HostInterface(aHost, aInterface);

        List<HostInterface> found = new ArrayList<>();
        for (THost leftHost : dao.listHosts()) {
            for (TInterface leftInterface : leftHost.interfaces) {
                HostInterface left = new HostInterface(leftHost, leftInterface);
                if(left.equals(right)) {
                    continue;
                }
                if(vip.equals(leftInterface.vip)) {
                    if(leftInterface.getVrrpPriority().equals(aInterface.getVrrpPriority())) {
                        throw new IllegalStateException("Both interfaces "
                                + left
                                + " and "+ right
                                + " have save VRRP priority ("+aInterface.getVrrpPriority()+")"
                        );
                    }
                    found.add(left);
                }
            }
        }

        if(found.isEmpty()) {
            throw new IllegalStateException("No pared interface found for "+right);
        }

        if(found.size()>1){
            throw new IllegalStateException("There are more then one paired interface for "+right+ " : "+found);
        }

    }

    public static class VrrpInterface {
        public String instance;
        public String interfaceName;
        public String priority;
        public String ip;
        public String netmask;

    }
}
