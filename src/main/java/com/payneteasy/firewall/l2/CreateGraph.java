package com.payneteasy.firewall.l2;

import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TInterface;
import y.base.Node;
import y.view.Graph2D;
import y.view.NodeRealizer;
import y.view.ProxyShapeNodeRealizer;
import y.view.hierarchy.HierarchyManager;

import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class CreateGraph {

    int y = 400;

    Map<String, Integer> horizontal = new HashMap<>();
    Map<String, Integer> vertical   = new HashMap<>();
    Set<String> links = new TreeSet<>();
    Map<String, String> ports = new HashMap<>();

    final NodePositions positions;

    public CreateGraph(NodePositions aPositions) {

          positions = aPositions;
//        add("fw-1", 0, 0);
//        add("fw-2", 600, 0);
//        add("sw-core-1", 0, 200);
//        add("sw-core-2", 1200, 400);
    }

//    private void add(String aName, int aX, int aY) {
//        horizontal.put(aName, aX);
//        vertical.put(aName, aY);
//    }

    public void populateGraph(IConfigDao aConfig, Graph2D aGraph) {

        aGraph.setHierarchyManager(new HierarchyManager(aGraph));
        HierarchyManager manager = aGraph.getHierarchyManager();


        Map<String, Node> map = new HashMap<>();

        // servers and switches
        aConfig.listHosts().stream().filter(this::isL2Host).forEach(host -> {
            String name = host.name;
            System.out.println(name);
            Node node = createGroup(aGraph, manager, name);

            for (TInterface iface : host.interfaces) {
                if(iface.name.contains(".")) {
                    continue;
                }
                Node port = aGraph.createNode(nextHorizontal(host, iface), nextVertical(host, iface), getInterfacename(iface));
                String portKey = getPortKey(host, iface);
                map.put(portKey, port);
                manager.setParentNode(port, node);
            }
        });

        // links
        aConfig.listHosts().stream().filter(this::isSwitch).forEach(host -> {
            host.interfaces.stream().filter(iface -> iface.link != null).forEach(iface -> {
                String portKey = getPortKey(host, iface);
                Node port = map.get(portKey);
                if (port == null) {
                    throw new IllegalStateException("No port " + portKey);
                }

                Node server = map.get(iface.link);
                if (server == null) {
                    throw new IllegalStateException("No host " + iface.link + " for interface " + host.name + " / " + iface.name);
                }

                checkLink(portKey, iface.link);
                aGraph.createEdge(port, server);
            });
        });


    }

    private void checkLink(String aLeft, String aRight) {
        // links
        String k1 = aLeft + " - " +aRight;
        if(links.contains(k1)) {
            throw new IllegalStateException("link "+k1+" already exists");
        }

        String k2 = aRight + " - " + aLeft;
        if(links.contains(k2)) {
            throw new IllegalStateException("link "+k2+" already exists");
        }
        links.add(k1);
        links.add(k2);

        // ports
        if(ports.containsKey(aLeft)) {
            throw new IllegalStateException("Trying to add "+k1+" but port "+aLeft+" already used in " + ports.get(aLeft));
        }
        if(ports.containsKey(aRight)) {
            throw new IllegalStateException("Trying to add "+k1+" but port "+aRight+" already used in + "+ ports.get(aRight));
        }
        ports.put(aLeft, k1);
        ports.put(aRight, k1);

    }

    private String getInterfacename(TInterface iface) {
        return iface.port!=null ? iface.port : iface.name;
    }

    private String getPortKey(THost host, TInterface iface) {
        return host.name + "/" + getInterfacename(iface);
    }

    private Node createGroup(Graph2D aGraph, HierarchyManager manager, String name) {
        Node node = manager.createGroupNode(aGraph);
        NodeRealizer nr = aGraph.getRealizer(node);
        if (nr instanceof ProxyShapeNodeRealizer) {
            ProxyShapeNodeRealizer pnr = (ProxyShapeNodeRealizer) nr;
            pnr.getRealizer(0).setLabelText(name);
            pnr.getRealizer(1).setLabelText(name);
        } else {
            nr.setLabelText(name);
        }
        return node;
    }


    private double nextVertical(THost aHost, TInterface iface) {

        Point point = positions.getPosition(aHost, iface);
        if(point != null) {
            return point.y;
        }

        Integer next = vertical.get(aHost.name);
        if(isEvenPort(iface)) {
            return next - 50;
        }

        if(next == null) {
            y += 100;
            vertical.put(aHost.name, y);
            return y;
        }

        return next;
    }

    private double nextHorizontal(THost aHost, TInterface iface) {
        Point point = positions.getPosition(aHost, iface);
        if(point != null) {
            return point.x;
        }

        Integer next = horizontal.get(aHost.name);

        if(next == null) {
            next = 0;
        }

        if(isEvenPort(iface)) {
            return next;
        }

        next+=100;
        horizontal.put(aHost.name, next);

        return next;
    }

    private boolean isEvenPort(TInterface aIface) {
        return aIface.port!=null && Integer.parseInt(aIface.port) % 2 == 0;
    }

    private boolean isL2Host(THost aHost) {
        return true;
    }

    private boolean isSwitch(THost aHost) {
        for (TInterface inter : aHost.interfaces) {
            if (inter.link != null) {
                return true;
            }
        }
        return false;
    }
}
