package com.payneteasy.firewall.l2.editor.model;

import com.payneteasy.firewall.l2.editor.actions.IPointSaver;
import com.payneteasy.firewall.l2.editor.graphics.ICanvas;

import java.awt.*;

public class Host implements INode {

    public static final int PADDING = 10;

    public Color backgroundColor = new Color(0xf8ecc9);
    private final String name;
    private int x;
    private int y;
    private final Ports ports;

    public Host(String aName, int aX, int aY, Ports aPorts) {
        name = aName;
        x = aX;
        y = aY;
        ports = aPorts;
    }

    public void draw(ICanvas aCanvas) {
        aCanvas.fillRect(backgroundColor, x, y, calculateWidth(), calculateHeight());
        aCanvas.drawText(Color.BLACK, x, y - 5, name);
        aCanvas.drawRect(Color.BLUE, x, y, calculateWidth(), calculateHeight());
        ports.draw(aCanvas.createChild(x, y));
    }

    private int calculateHeight() {
        return ports.getMaxY() + PADDING;
    }

    private int calculateWidth() {
        return ports.getMaxX() + PADDING;
    }


    @Override
    public String toString() {
        return "Host{" +
                "name='" + name + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", ports=" + ports +
                '}';
    }

    public INode findNode(int aX, int aY) {

        Port port = ports.findNode(aX - x, aY - y);
        if(port != null) {
            return port;
        }

        final int x2 = x + calculateWidth();
        final int y2 = y + calculateHeight();
        if(        x  < aX
                && y  < aY
                && x2 > aX
                && y2 > aY) {
            return this;
        }

        return null;
    }

    @Override
    public void moveTo(int aX, int aY) {
        x = aX;
        y = aY;
    }

    @Override
    public Point createOffset(int aX, int aY) {
        return new Point(x - aX, y - aY);
    }

    @Override
    public void save(IPointSaver aSaver) {
        aSaver.save(name, x, y);
        ports.save(aSaver.createChild(name));
    }

    public Point getPoint() {
        return new Point(x, y);
    }

    public boolean hasName(String aHostName) {
        return name.equals(aHostName);
    }

    public Port findPort(String aPortName) {
        return ports.findPort(aPortName);
    }

    public int getMaxWidth() {
        return x + calculateWidth();
    }

    public int getMaxHeight() {
        return y + calculateHeight();
    }
}
