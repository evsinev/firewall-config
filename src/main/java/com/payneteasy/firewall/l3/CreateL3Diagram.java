package com.payneteasy.firewall.l3;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TInterface;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateL3Diagram {

    public void create(IConfigDao aConfigDao) {

        Map<String, List<THost>> map = createNetworks(aConfigDao);
        List<Network> nets = convertToNetWorks(map);
        printNetwork(nets);
    }

    private List<Network> convertToNetWorks(Map<String, List<THost>> map) {
        Map<String, String> names = new HashMap<>();
        names.put("10.2.1.0", "dmz-public");
        names.put("10.2.2.0", "dmz-out");
        names.put("10.2.3.0", "dmz-ui");
        names.put("10.2.4.0", "dmz-vpn");
        names.put("10.2.5.0", "dmz-etc");

        names.put("10.2.20.0", "int-proc");
        names.put("10.2.21.0", "int-hsm");
        names.put("10.2.22.0", "int-db");
        names.put("10.2.23.0", "int-admin");
        names.put("10.2.24.0", "int-management");

        names.put("10.6.0.0", "ipmi");

        names.put("0.0.0.0", "internet");


        List<Network> nets = new ArrayList<>();
        for (Map.Entry<String, List<THost>> entry : map.entrySet()) {
            Network net = new Network();
            net.address = entry.getKey();
            net.name = names.get(entry.getKey());
            net.hosts = convertToHosts(net.address, entry.getValue());
            nets.add(net);
        }
        return nets;
    }

    private List<Host> convertToHosts(String aNetworkAddress, List<THost> aValue) {
        List<Host> ret = new ArrayList<>();
        for (THost host : aValue) {
            Host h = new Host();
            h.name = host.name;
            StringBuilder sb = new StringBuilder();
            for (TInterface iface : host.interfaces) {
                    if (isInNetwork(iface.ip, aNetworkAddress)) {
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

    private boolean isInNetwork(String aIp, String aNetworkAddress) {
        if(aIp==null) return false;

        int left = aIp.lastIndexOf('.');
        int right = aNetworkAddress.lastIndexOf('.');

        return aIp.substring(0, left).equals(aNetworkAddress.substring(0, right));

    }

    private void printNetwork(List<Network> aNets) {
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile("nwdiag.mustache");
        try {
            Map<String, List<Network>> scope = new HashMap<>();
            scope.put("networks", aNets);

            mustache.execute(new PrintWriter(System.out), scope).flush();
            mustache.execute(new FileWriter("target/network.diag"), scope).flush();

            Runtime.getRuntime().exec("nwdiag -a --no-transparency target/network.diag").waitFor();
            Runtime.getRuntime().exec("open target/network.png").waitFor();
        } catch (Exception e) {
            throw new IllegalStateException("Could not write file", e);
        }
    }

    private Map<String, List<THost>> createNetworks(IConfigDao aConfigDao) {
        Map<String, List<THost>> networks = new HashMap<>();

        for (THost host : aConfigDao.listHosts()) {
            for (TInterface iface : host.interfaces) {
                if (iface.ip != null) {
                    String network = getNetwork(iface.ip);
                    addHostToNetwork(networks, network, host);
                }
            }
        }

        return networks;
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
        List<Host> hosts;
    }

    static class Host {
        String name;
        String ip;
    }
}
