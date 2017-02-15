package com.payneteasy.firewall.l2.editor.model;

import com.payneteasy.firewall.l2.editor.actions.IPointSaver;
import com.payneteasy.firewall.l2.editor.graphics.ICanvas;

import java.util.List;

public class Ports {

    private final List<Port> ports;

    public Ports(List<Port> ports) {
        this.ports = ports;
    }


    public void draw(ICanvas aCanvas) {
        for (Port port : ports) {
            port.draw(aCanvas);
        }
    }

    @Override
    public String toString() {
        return "Ports{" +
                "ports=" + ports +
                '}';
    }

    public int getMaxX() {
        int max = 0;
        for (Port port : ports) {
            int y = port.getEndX();
            if(y > max) {
                max = y;
            }
        }
        return max;
    }

    public int getMaxY() {
        int max = 0;
        for (Port port : ports) {
            int y = port.getEndY();
            if(y > max) {
                max = y;
            }
        }
        return max;
    }

    public Port findNode(int aX, int aY) {
        for (Port port : ports) {
            if(port.hasPoint(aX, aY)) {
                return port;
            }
        }
        return null;
    }

    public Port findPort(String aPortName) {
        for (Port port : ports) {
            if(port.hasName(aPortName)) {
                return port;
            }
        }
        throw new IllegalStateException("Port not found " + aPortName);
    }

    public void save(IPointSaver aSaver) {
        for (Port port : ports) {
            port.save(aSaver);
        }
    }
}
