package com.payneteasy.firewall.l2.editor.create;

import com.payneteasy.firewall.l2.editor.model.*;

import java.awt.*;
import java.util.*;
import java.util.List;

public class HostAndLinkBuilder {

    Map<String, HostHolder> pengindHostMap = new TreeMap<>();
    Set<LinkHolder> linksSet = new TreeSet<>();
    Hosts hosts;

    final IPositionManager positions;

    public HostAndLinkBuilder(IPositionManager positions) {
        this.positions = positions;
    }

    public void addHost(String aHostname) {
        if(pengindHostMap.containsKey(aHostname)) {

            //throw new IllegalStateException("Host " + aHostname + " already added");
        }
        pengindHostMap.put(aHostname, new HostHolder());
    }

    public void addPort(String aHostname, String aInterfaceName) {
        HostHolder hostHolder = pengindHostMap.get(aHostname);
        if(hostHolder == null) {
            throw new IllegalStateException("Host " + aHostname + " not found");
        }
        hostHolder.interfacesMap.add(aInterfaceName);
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
        linksSet.add(new LinkHolder(aLeftHostname, aLeftInterface, rightHostname, rightPort));
    }

    private void checkHostAndPort(String aLeftHostname, String aLeftInterface) {
        HostHolder hostHolder = pengindHostMap.get(aLeftHostname);
        if(hostHolder == null) {
            throw new IllegalStateException("Host " + aLeftHostname + " not found");
        }

        if(!hostHolder.interfacesMap.contains(aLeftInterface)) {
            throw new IllegalStateException("Host " + aLeftHostname+" does not have port " + aLeftInterface);
        }
    }

    public Hosts createHosts() {
        // add all hosts from links
        Map<String, HostHolder> map = new HashMap<>();
        for (LinkHolder linkHolder : linksSet) {
            System.out.println("linkHolder = " + linkHolder);
            addHostAndPort(map, linkHolder.leftHost, linkHolder.leftPort);
            addHostAndPort(map, linkHolder.rightHost, linkHolder.rightPort);
        }

        // find all host and add all its ports
        for (Map.Entry<String, HostHolder> entry : map.entrySet()) {
            HostHolder pendingHolder = pengindHostMap.get(entry.getKey());
            for (String pendingPort : pendingHolder.interfacesMap) {

                if(isVirtualPort(pendingPort)) { // skip virtual port
                    continue;
                }
                entry.getValue().interfacesMap.add(pendingPort);
            }
        }

        // create hosts
        List<Host> hosts = new ArrayList<>();
        for (Map.Entry<String, HostHolder> entry : map.entrySet()) {
            List<Port> ports = new ArrayList<>();
            for (String portName : entry.getValue().interfacesMap) {
                Point portPoint = positions.getPortPosition(entry.getKey(), portName);
                ports.add(new Port(portName, portPoint.x, portPoint.y));
            }
            Point hostPoint = positions.getHostPosition(entry.getKey());
            hosts.add(new Host(entry.getKey()
                    , hostPoint.x
                    , hostPoint.y
                    , new Ports(ports)));
        }
        this.hosts = new Hosts(hosts);
        return this.hosts;
    }

    private boolean isVirtualPort(String aPortName) {
        return aPortName.contains(":")
                || aPortName.startsWith("tun");
    }

    private static void addHostAndPort(Map<String, HostHolder> map, String leftHost, String port) {
        HostHolder holder = map.get(leftHost);
        if(holder == null) {
            holder = new HostHolder();
            map.put(leftHost, holder);
        }
        holder.interfacesMap.add(port);
    }

    public Links createLinks() {
        List<Link> links = new ArrayList<>();
        for (LinkHolder holder : linksSet) {
            links.add(hosts.createLink(holder.leftHost, holder.leftPort, holder.rightHost, holder.rightPort));
        }
        return new Links(links);
    }

    static class HostHolder {
        TreeSet<String> interfacesMap = new TreeSet<>();

    }

    static class LinkHolder implements Comparable {
        String leftHost;
        String leftPort;
        String rightHost;
        String rightPort;

        public LinkHolder(String leftHost, String leftPort, String rightHost, String rightPort) {
            this.leftHost = leftHost;
            this.leftPort = leftPort;
            this.rightHost = rightHost;
            this.rightPort = rightPort;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            LinkHolder that = (LinkHolder) o;

            if (leftHost != null ? !leftHost.equals(that.leftHost) : that.leftHost != null) return false;
            if (leftPort != null ? !leftPort.equals(that.leftPort) : that.leftPort != null) return false;
            if (rightHost != null ? !rightHost.equals(that.rightHost) : that.rightHost != null) return false;
            return !(rightPort != null ? !rightPort.equals(that.rightPort) : that.rightPort != null);

        }

        @Override
        public int hashCode() {
            int result = leftHost != null ? leftHost.hashCode() : 0;
            result = 31 * result + (leftPort != null ? leftPort.hashCode() : 0);
            result = 31 * result + (rightHost != null ? rightHost.hashCode() : 0);
            result = 31 * result + (rightPort != null ? rightPort.hashCode() : 0);
            return result;
        }

        @Override
        public int compareTo(Object o) {
           return toString().compareTo(o.toString());
        }

        @Override
        public String toString() {
            return "LinkHolder{"
                     + leftHost + '\'' +
                    "/" + leftPort  +
                    "  -> " + rightHost +
                    "/" + rightPort  +
                    '}';
        }
    }



}
