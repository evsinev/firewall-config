package com.payneteasy.firewall.l2.editor.create;

import com.payneteasy.firewall.l2.NodePositions;

import java.awt.*;
import java.io.File;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

public class PlainPositions implements IPositionManager {


    final TreeMap<String, HostPosition> hostPoints;

    public PlainPositions(File aFile) {
        NodePositions nodePositions = new NodePositions(aFile);
        final TreeMap<String, Point> map = nodePositions.getMap();

        hostPoints = new TreeMap<>();
        for (Map.Entry<String, Point> entry : map.entrySet()) {
            StringTokenizer st = new StringTokenizer(entry.getKey(), ",. ");
            String hostname = st.nextToken();
            String portName = st.nextToken();
            addPosition(hostname, portName, entry.getValue());
        }

        // find relative
        for (HostPosition hostPosition : hostPoints.values()) {
            hostPosition.recalculate();
        }

        // get min x and y
        int minX = 2000;
        int minY = 2000;
        for (HostPosition position : hostPoints.values()) {
            if(position.hostPoint.x < minX) {
                minX = position.hostPoint.x;
            }
            if(position.hostPoint.y < minY) {
                minY = position.hostPoint.y;
            }
        }

        minX -= 20;
        minY -= 20;
        // shift
        for (HostPosition position : hostPoints.values()) {
            position.shift(minX, minY);
        }

    }

    private void addPosition(String aHostname, String aPortName, Point aPoint) {
        HostPosition host = hostPoints.get(aHostname);
        if(host == null) {
            host = new HostPosition();
            hostPoints.put(aHostname, host);
        }
        host.addPort(aPortName, aPoint);

    }

    public Point getHostPosition(String aHostname) {
        HostPosition hostPosition = hostPoints.get(aHostname);
        if(hostPosition == null) {
            return new Point(0,0);
        }
        return hostPosition.hostPoint;
    }

    public Point getPortPosition(String aHostname, String aPortName) {
        HostPosition hostPosition = hostPoints.get(aHostname);
        if(hostPosition == null) {
            return new Point(0,0);
        }
        Point portPoint = hostPosition.relativePoints.get(aPortName);
        if(portPoint == null) {
            portPoint = hostPosition.relativePoints.get(aPortName.replace("ether", ""));
            if(portPoint == null) {
                return new Point(0, 0);
            }
        }
        return portPoint;
    }

    static class HostPosition {
        Point hostPoint;
        TreeMap<String, Point> portPoints = new TreeMap<>();
        TreeMap<String, Point> relativePoints = new TreeMap<>();

        void addPort(String aPortName, Point aPosition) {
            portPoints.put(aPortName, aPosition);
        }

        public void recalculate() {
            int minX = 2000;
            int minY = 2000;
            for (Point point: portPoints.values()) {
                if(point.x < minX) {
                    minX = point.x;
                }
                if(point.y < minY ) {
                    minY = point.y;
                }
            }

            minX -= 5;
            minY -= 5;

            hostPoint = new Point(minX, minY);
            for (Map.Entry<String, Point> entry : portPoints.entrySet()) {
                final Point point = entry.getValue();
                relativePoints.put(entry.getKey(), new Point(point.x - minX, point.y - minY));
            }

        }

        public void shift(int aMinX, int aMinY) {
            hostPoint = new Point(hostPoint.x - aMinX, hostPoint.y - aMinY);
        }
    }
}
