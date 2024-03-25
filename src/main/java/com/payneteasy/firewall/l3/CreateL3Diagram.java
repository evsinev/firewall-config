package com.payneteasy.firewall.l3;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TInterface;
import com.payneteasy.firewall.util.CommandProcess;
import com.payneteasy.firewall.util.Files;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

public class CreateL3Diagram {

    private final File     configDir;
    private final boolean  runNwdiag;
    private final String[] filter;

    public CreateL3Diagram(File configDir, boolean runNwdiag, String[] filter) {
        this.configDir = configDir;
        this.runNwdiag = runNwdiag;
        this.filter    = filter;
    }

    public void create(IConfigDao aConfigDao) {

        printNetwork(
            sortNetworks(
                convertToNetworks(
                          createNetworkHosts(aConfigDao)
                        , aConfigDao.listNetworksNames()
                )
            )
        );

    }

    private List<Network> convertToNetworks(Map<String, List<THost>> aNetworkHosts, Map<String, String> aNetworkNames) {

        List<Network> nets = new ArrayList<>();
        for (Map.Entry<String, List<THost>> entry : aNetworkHosts.entrySet()) {

            String networkName = aNetworkNames.get(entry.getKey());

            if (networkName == null) {
                throw new IllegalStateException("Network  " + entry.getKey() + " has no name, please add it to networks.yml file");
            }

            if(networkName.startsWith("skip")) {
                continue;
            }

            Network net = new Network();

            net.address = entry.getKey();
            net.name    = networkName;
            net.hosts   = convertToHosts(net.address, entry.getValue());

            nets.add(net);
        }

        return nets;
    }

    private static List<Network> sortNetworks(List<Network> nets) {
        nets.sort((left, right) -> {

            if ("internet".equals(left.name)) {
                return -1;
            }

            if ("internet".equals(right.name)) {
                return 1;
            }

            if (left.name == null) {
                throw new IllegalStateException("Left name is null for " + left);
            }

            if (right.name == null) {
                throw new IllegalStateException("Right name is null for " + right);
            }

            return left.name.compareTo(right.name);
        });

        return nets;
    }

    private Set<Host> convertToHosts(String aNetworkAddress, List<THost> aValue) {
        Set<Host> ret = new HashSet<>();
        for (THost host : aValue) {
            Host h = new Host();
            h.name = host.name;
            StringBuilder sb = new StringBuilder();
            for (TInterface iface : host.interfaces) {
                    if (isInNetwork(iface, aNetworkAddress)) {
                        if (sb.length() > 0) {
                            sb.append(", ");
                        }
                        sb.append(iface.ip);
                    }
            }
            h.ip = sb.toString();
            ret.add(h);
        }
        return ret;
    }

    private boolean isInNetwork(TInterface aInterface, String aNetworkAddress) {
        for (String ip : aInterface.getAllIpAddresses()) {

            int left = ip.lastIndexOf('.');
            int right = aNetworkAddress.lastIndexOf('.');


            if(ip.substring(0, left).equals(aNetworkAddress.substring(0, right))) {
                return true;
            }
        }
        return false;
    }

    private void printNetwork(List<Network> aNets) {
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile("nwdiag.mustache");

        try {
            Map<String, Object> scope = new HashMap<>();
            scope.put("networks", aNets);
            scope.put("custom", Files.readFile(new File(configDir, "nwdiag-custom.diag")));

            mustache.execute(new PrintWriter(System.out), scope).flush();
            mustache.execute(new FileWriter("target/network.diag"), scope).flush();

            if(runNwdiag) {
                System.out.println("nwdiag is generating png ...");
                new CommandProcess("nwdiag", "nwdiag -a --no-transparency target/network.diag").waitSuccess();

                System.out.println("Opening file ...");
                new CommandProcess("open", "open target/network.png").waitSuccess();
            }

        } catch (Exception e) {
            throw new IllegalStateException("Could not write file", e);
        }
    }

    private Map<String, List<THost>> createNetworkHosts(IConfigDao aConfigDao) {
        Map<String, List<THost>> networkHosts = new HashMap<>();

        for (THost host : aConfigDao.listHostsByFilter(filter)) {
            for (TInterface iface : host.interfaces) {
                if (iface.ip != null && !"skip".equals(iface.ip)) {
                    String network = getNetwork(iface.ip);
                    addHostToNetwork(networkHosts, network, host);
                }
            }
        }

        return networkHosts;
    }

    private void addHostToNetwork(Map<String, List<THost>> aNetworks, String aNetwork, THost aHost) {
        List<THost> hosts = aNetworks.get(aNetwork);
        if (hosts == null) {
            hosts = new ArrayList<>();
            aNetworks.put(aNetwork, hosts);
        }

        hosts.add(aHost);
    }

    private String getNetwork(String aIp) {
        int pos = aIp.lastIndexOf('.');
        return aIp.substring(0, pos) + ".0";
    }

    static class Network {
        String address;
        String name;
        Set<Host> hosts;

        @Override
        public String toString() {
            return "Network{" +
                    "address='" + address + '\'' +
                    ", name='" + name + '\'' +
                    ", hosts=" + hosts +
                    '}';
        }
    }

    static class Host {
        String name;
        String ip;

        @Override
        public String toString() {
            return "Host{" +
                    "name='" + name + '\'' +
                    ", ip='" + ip + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Host host = (Host) o;

            if (name != null ? !name.equals(host.name) : host.name != null) return false;
            return !(ip != null ? !ip.equals(host.ip) : host.ip != null);

        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (ip != null ? ip.hashCode() : 0);
            return result;
        }
    }
}
