package com.payneteasy.firewall;

import com.payneteasy.firewall.dao.ConfigDaoYaml;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TInterface;
import com.payneteasy.firewall.util.Printer;
import com.payneteasy.firewall.util.ShellFilePrinter;

import java.io.File;
import java.io.IOException;

public class MainUbuntuBaseSetup {


    IConfigDao dao;

    public MainUbuntuBaseSetup(IConfigDao configDao) {
        dao = configDao;
    }

    public static void main(String[] args) throws IOException {
        File configDir = new File(args[0]);
        String host = args[1];
        String iface = args[2];

        IConfigDao configDao = new ConfigDaoYaml(configDir);

        MainUbuntuBaseSetup main = new MainUbuntuBaseSetup(configDao);
        main.showIfcfg(host, iface);
        main.createHosts(host);
        main.createHostname(host);
        main.createResolveConf();
    }

    private void createResolveConf() {
        ShellFilePrinter printer = new ShellFilePrinter("/etc/resolv.conf");
        printer.out("search idea");
        printer.out("nameserver 10.2.2.21");
        printer.out("nameserver 10.2.2.22");
        printer.close();
    }

    private void createHosts(String aHostname) {
        ShellFilePrinter printer = new ShellFilePrinter("/etc/hosts");
        printer.out("127.0.0.1  localhost localhost.localdomain");
        printer.out("127.0.0.1  %s", aHostname);
        printer.out("::1        localhost ip6-localhost ip6-loopback");
        printer.out("ff02::1    ip6-allnodes");
        printer.out("ff02::2    ip6-allrouters");
        printer.close();

    }

    private void createHostname(String aHost) {
        Printer.out("hostname "+aHost);
        ShellFilePrinter printer = new ShellFilePrinter("/etc/hostname");
        printer.out(aHost);
        printer.close();
    }

    private void showIfcfg(String aHost, String aInterfaceName) {
        THost host = dao.getHostByName(aHost);
        TInterface iface = findInterface(host, aInterfaceName);

        Printer.out("# %s", aHost+" "+aInterfaceName);

        ShellFilePrinter printer = new ShellFilePrinter("/etc/network/interfaces.d/"+aInterfaceName);
        printer.out("auto %s", aInterfaceName);
        printer.out("iface %s inet static", aInterfaceName);
        printer.out("address %s", findIpAddress(host, aInterfaceName));
        printer.out("netmask %s", iface.getLongNetmask());
        printer.out("gateway %s", host.gw);
        printer.close();

    }

    private TInterface findInterface(THost aHost, String aInterfaceName) {
        for (TInterface iface : aHost.interfaces) {
            if(aInterfaceName.equals(iface.name)) {
                return iface;
            }
        }
        throw new IllegalStateException("Could not find "+aInterfaceName+ " in "+aHost.name);
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
