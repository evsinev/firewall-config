package com.payneteasy.firewall.l2.editor.create;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class EmptyPositionManager implements IPositionManager {

    Map<String, Integer> lastPortPositions = new HashMap<>();

    int lastHostPosition = -40;

    @Override
    public Point getPortPosition(String aHostname, String aPortName) {
        Integer position = lastPortPositions.get(aHostname);
        if(position == null) {
            position = 10;
        } else {
            position += 50;
        }

        lastPortPositions.put(aHostname, position);

        return new Point(position, 10);
    }

    @Override
    public Point getHostPosition(String aHostname) {
        lastHostPosition += 70;
        return new Point(10, lastHostPosition);
    }
}
