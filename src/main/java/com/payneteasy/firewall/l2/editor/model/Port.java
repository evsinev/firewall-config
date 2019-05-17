package com.payneteasy.firewall.l2.editor.model;

import com.payneteasy.firewall.l2.editor.actions.IPointSaver;
import com.payneteasy.firewall.l2.editor.graphics.ICanvas;

import java.awt.*;

public class Port implements INode {

    private final String name;
    final String displayName;

    private int x;
    private int y;

    private int textWidth  = 0;
    private int textHeight = 0;

    final Color backgroundColor;

    public Port(String aName, int aX, int aY, Color aBackgroundColor) {
        backgroundColor = aBackgroundColor;
        name = aName;
        x = aX;
        y = aY;
        displayName = name.replace("ether", "").replace("bridge", "br");
    }

    public void draw(ICanvas aCanvas) {
        if(textWidth == 0 || textHeight == 0) {
            textWidth  = aCanvas.getTextWidth(displayName);
            textHeight = aCanvas.getTextHeight(displayName);
        }

        aCanvas.fillRect(backgroundColor, x, y, calculateWidth(), calculateHeight());
        aCanvas.drawRect(Color.RED , x, y, calculateWidth(), calculateHeight());

        aCanvas.drawText(Color.BLACK, x + 5, y + calculateHeight() - 5, displayName);
    }

    @Override
    public void save(IPointSaver aSaver) {
        aSaver.save(name, x, y);
    }

    public int calculateWidth() {
        return textWidth == 0 ? 30 : textWidth + 10;
    }

    public int calculateHeight() {
        return textHeight == 0 ? 30 : textHeight + 10;
    }

    @Override
    public void moveTo(int aX, int aY) {
        if(aX > 0) {
            x = aX;
        }
        if(aY > 0 ) {
            y = aY;
        }
    }

    @Override
    public String toString() {
        return "Port{" +
                "name='" + name + '\'' +
                ", x=" + x +
                ", y=" + y +
                '}';
    }

    public int getEndX() {
        return x + calculateWidth();
    }

    public int getEndY() {
        return y + calculateHeight();
    }

    public boolean hasPoint(int aX, int aY) {
        System.out.println(x + " - " + aX + " "+ getEndX());
        System.out.println(y + " - " + aY + " "+ getEndY());

        final boolean hasPoint = x < aX && getEndX() > aX
                && y < aY && getEndY() > aY;

        System.out.println("hasPoint = " + hasPoint);
        return hasPoint;
    }

    @Override
    public Point createOffset(int aX, int aY) {
        return new Point(x - aX, y - aY);
    }

    public Rectangle createRectangle() {
        return new Rectangle(x, y, calculateWidth(), calculateHeight());
    }

    public boolean hasName(String aPortName) {
        return name.equals(aPortName);
    }
}
