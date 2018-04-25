package com.payneteasy.firewall.l2.editor.create;

import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.l2.editor.model.*;
import com.payneteasy.firewall.util.Strings;

import java.awt.*;
import java.util.*;
import java.util.List;

public class HostAndLinkBuilder {

    private final Map<String, HostHolder> pengindHostMap;
    private final Set<LinkHolder>         linksSet;
    private final Map<String, Color>      vlanColors;
    private final List<LinkHolder>        removedLinks;
    private final L2CustomParameters      customParameters;
    private final IPositionManager        positions;
    private final IConfigDao              configDao;

    private       Hosts                   hosts;

    public HostAndLinkBuilder(IPositionManager positions, L2CustomParameters aCustomParameters, IConfigDao aConfigDao) {
        this.positions   = positions;
        customParameters = aCustomParameters;
        vlanColors       = aCustomParameters.getVlanColors();
        pengindHostMap   = new HashMap<>();
        linksSet         = aCustomParameters.getLinks();
        removedLinks     = aCustomParameters.getRemovedLinks();
        configDao        = aConfigDao;
//        vlanColors.put("vlan_inside", new Color(0xF78181));
//        vlanColors.put("vlan_trans", new Color(0x81F7D8));
//        vlanColors.put("vlan_backup", new Color(0xA9BCF5));
//        vlanColors.put("vlan_ipmi", new Color(0xF2F5A9));
    }

    public void addHost(String aHostname) {
        if(pengindHostMap.containsKey(aHostname)) {

            //throw new IllegalStateException("Host " + aHostname + " already added");
        } else {
            pengindHostMap.put(aHostname, new HostHolder(aHostname));
        }
    }

    public void addPort(String aHostname, String aInterfaceName, String aVlan) {
        HostHolder hostHolder = pengindHostMap.get(aHostname);
        if(hostHolder == null) {
            throw new IllegalStateException("Host " + aHostname + " not found");
        }
        hostHolder.addPort(aInterfaceName, aVlan);
    }

    public void addLink(String aLeftHostname, String aLeftInterface, String aRightLinkDefinition) {
        try {
            checkHostAndPort(aLeftHostname, aLeftInterface);
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't add link from " + aLeftHostname+"/"+aLeftInterface + " to " + aRightLinkDefinition, e);
        }

        System.out.println(aLeftHostname + "/" + aLeftInterface +"-> " + aRightLinkDefinition);

        StringTokenizer st = new StringTokenizer(aRightLinkDefinition, "/ ");
        String rightHostname  = st.nextToken();
        String rightPort      = st.nextToken();
        checkHostAndPort(rightHostname, rightPort);
        linksSet.add(new LinkHolder(aLeftHostname, aLeftInterface, rightHostname, rightPort, LinkType.COMMON));
    }

    private void checkHostAndPort(String aLeftHostname, String aLeftInterface) {
        HostHolder hostHolder = pengindHostMap.get(aLeftHostname);
        if(hostHolder == null) {
            throw new IllegalStateException("Host " + aLeftHostname + " not found");
        }

        if(!hostHolder.hasPort(aLeftInterface)) {
            throw new IllegalStateException("Host " + aLeftHostname+" does not have port " + aLeftInterface);
        }
    }

    public Hosts createHosts() {
        // remove links
        for (LinkHolder link : removedLinks) {
            if(!linksSet.remove(link)) {
                throw new IllegalStateException("Couldn't find " + link + " to delete");
            }
        }

        customParameters.getPendingHostMap(pengindHostMap);

        // add all hosts from links
        Map<String, HostHolder> map = new HashMap<>();
        for (LinkHolder linkHolder : linksSet) {
            System.out.println("linkHolder = " + linkHolder);
            addHostAndPort(map, linkHolder.leftHost, linkHolder.leftPort, pengindHostMap);
            addHostAndPort(map, linkHolder.rightHost, linkHolder.rightPort, pengindHostMap);
        }

        // find all host and add all its ports
        for (Map.Entry<String, HostHolder> entry : map.entrySet()) {
            HostHolder pendingHolder = pengindHostMap.get(entry.getKey());
            for (PortHolder pendingPort : pendingHolder.getPorts()) {

                if(isVirtualPort(pendingPort)) { // skip virtual port
                    continue;
                }
                entry.getValue().addPort(pendingPort.name, pendingPort.vlan);
            }
        }

        // create hosts
        List<Host> hosts = new ArrayList<>();
        for (Map.Entry<String, HostHolder> entry : map.entrySet()) {
            List<Port> ports = new ArrayList<>();
            for (PortHolder port : entry.getValue().getPorts()) {
                Point portPoint = positions.getPortPosition(entry.getKey(), port.name);
                if(canSkiPort(port.name))  {
                    continue;
                }
                ports.add(new Port(port.name, portPoint.x, portPoint.y, findVlanColor(port.vlan)));
            }
            Point hostPoint = positions.getHostPosition(entry.getKey());
            hosts.add(new Host(entry.getKey()
                    , hostPoint.x
                    , hostPoint.y
                    , new Ports(ports)
                    , getHostColor(entry.getKey()))
            );
        }
        this.hosts = new Hosts(hosts, vlanColors);
        return this.hosts;
    }

    private Color getHostColor(String aHostname) {
        THost host = configDao.getHostByName(aHostname);
        if(host.color == null) {
            return new Color(0xf8ecc9);
        } else {
            String textColor = host.color;
            return Color.decode(textColor);
        }
    }

    private boolean canSkiPort(String aName) {
        return     aName.startsWith("eth0.")
                || aName.startsWith("eth1.")
                || aName.startsWith("eth4.")
                || "ipmi_nuc".equals(aName);
    }

    private Color findVlanColor(String aVlan) {
        if(Strings.isEmpty(aVlan)) {
            return Color.WHITE;
        }
        final Color color = vlanColors.get(aVlan);
        if(color == null) {
            throw new IllegalStateException("No color for vlan " + aVlan + " in " + vlanColors);
        }
        return color;
    }

    private boolean isVirtualPort(PortHolder aPort) {
        return aPort.name.contains(":")
                || aPort.name.startsWith("tun");
    }

    private static void addHostAndPort(Map<String, HostHolder> map, String leftHost, String port, Map<String, HostHolder> aPendingMap) {
        HostHolder holder = map.get(leftHost);
        if(holder == null) {
            holder = new HostHolder(leftHost);
            map.put(leftHost, holder);
        }
        holder.addPort(port, findVlan(aPendingMap, leftHost, port));
    }

    private static String findVlan(Map<String, HostHolder> aPendingMap, String aHostname, String aPortName) {
        final HostHolder host = aPendingMap.get(aHostname);
        if(host == null) {
            throw new IllegalStateException("Host " + aHostname + " not found");
        }
        return host.getPortVlan(aPortName);
    }

    public Links createLinks() {
        List<Link> links = new ArrayList<>();
        for (LinkHolder holder : linksSet) {
            links.add(hosts.createLink(holder.leftHost, holder.leftPort, holder.rightHost, holder.rightPort, holder.linkType));
        }
        return new Links(links);
    }

    static class HostHolder {
        Map<String, PortHolder> portMap = new HashMap<>();
        private final String name;

        public HostHolder(String name) {
            this.name = name;
        }

        void addPort(String aName, String aVlan) {
            portMap.put(aName, new PortHolder(aName, aVlan));
        }

        Collection<PortHolder> getPorts() {
            return portMap.values();
        }

        public boolean hasPort(String aPortName) {
            return portMap.containsKey(aPortName);
        }

        public String getPortVlan(String aPortName) {
            PortHolder port = portMap.get(aPortName);
            if(port == null) {
                throw new IllegalStateException("No port " + aPortName + " for host " + name + " in ports " + portMap.values());
            }
            return port.vlan;
        }
    }

    static class PortHolder {
        final String name;
        final String vlan;

        public PortHolder(String name, String vlan) {
            this.name = name;
            this.vlan = vlan;
        }

        @Override
        public String toString() {
            return name + "("+vlan+")" ;
        }
    }


}
