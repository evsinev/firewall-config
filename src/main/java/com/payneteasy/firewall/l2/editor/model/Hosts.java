package com.payneteasy.firewall.l2.editor.model;

import com.payneteasy.firewall.l2.editor.actions.IPointSaver;
import com.payneteasy.firewall.l2.editor.graphics.ICanvas;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Hosts {

    private final List<Host> hosts;

    private INode pickedNode;
    private Point offsetPoint;

    public Hosts(List<Host> aHosts) {
        hosts = aHosts;
    }

    public void draw(ICanvas aCanvas) {
        for (Host host : hosts) {
            host.draw(aCanvas);
        }
    }

//    public static Hosts load() {
//        List<Host> hosts = new ArrayList<>();
//        {
//            List<Port> ports = new ArrayList<>();
//            ports.add(new Port("1", 30, 30));
//            hosts.add(new Host("host1", 10, 10, new Ports(ports)));
//        }
//
//        {
//            List<Port> ports = new ArrayList<>();
//            ports.add(new Port("1", 50, 50));
//            hosts.add(new Host("host2", 210, 210, new Ports(ports)));
//        }
//
//        return new Hosts(hosts);
//    }

    public INode pick(int aX, int aY) {
        pickedNode = null;
        for (Host host : hosts) {
            pickedNode = host.findNode(aX, aY);
            if(pickedNode != null) {
                offsetPoint = pickedNode.createOffset(aX, aY);
                break;
            }
        }
        return pickedNode;
    }

    public void unpick() {
        offsetPoint = null;
        pickedNode = null;
    }

    public void movePicked(int aX, int aY) {
        if(pickedNode == null) {
            return;
        }
        pickedNode.moveTo(aX + offsetPoint.x, aY + offsetPoint.y);
    }

    public Link createLink(String aLeftHost, String aLeftPort, String aRightHost, String aRightPort) {
        Host leftHost = findHost(aLeftHost);
        Host rightHost = findHost(aRightHost);
        return new Link(
                leftHost
                , leftHost.findPort(aLeftPort)
                , rightHost
                , rightHost.findPort(aRightPort)
        );
    }

    private Host findHost(String aHostName) {
        for (Host host : hosts) {
            if(host.hasName(aHostName)) {
                return host;
            }
        }
        throw new IllegalStateException("Host not found " + aHostName);
    }

    public int getMaxWidth() {
        int max = 0;
        for (Host host : hosts) {
            int hostWidth = host.getMaxWidth();
            if(max < hostWidth) {
                max = hostWidth;
            }
        }
        return max;
    }

    public int getMaxHeight() {
        int max = 0;
        for (Host host : hosts) {
            int hostHeight = host.getMaxHeight();
            if(max < hostHeight) {
                max = hostHeight;
            }
        }
        return max;
    }

    public void save(IPointSaver aSaver) {
        for (Host host : hosts) {
            host.save(aSaver);
        }
    }
}
