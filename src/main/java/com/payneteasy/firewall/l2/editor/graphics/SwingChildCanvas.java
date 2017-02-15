package com.payneteasy.firewall.l2.editor.graphics;

import java.awt.*;

public class SwingChildCanvas implements ICanvas {

    private final ICanvas parent;

    private final int xoffset;
    private final int yoffset;

    public SwingChildCanvas(ICanvas aCanvas, int xoffset, int yoffset) {
        this.xoffset = xoffset;
        this.yoffset = yoffset;
        parent = aCanvas;
    }

    @Override
    public void drawText(Color aColor, int aX, int aY, String aName) {
        parent.drawText(aColor, xoffset + aX, yoffset + aY, aName);
    }

    @Override
    public void fillRect(Color aColor, int aX, int aY, int aWidth, int aHeight) {
        parent.fillRect(aColor, xoffset + aX, yoffset + aY, aWidth, aHeight);
    }

    @Override
    public void drawRect(Color aColor, int aX, int aY, int aWidth, int aHeight) {
        parent.drawRect(aColor, xoffset + aX, yoffset + aY, aWidth, aHeight);
    }

    @Override
    public ICanvas createChild(int aOffsetX, int aOffsetY) {
        return new SwingChildCanvas(this, aOffsetX, aOffsetY);
    }

    @Override
    public void drawLine(Color aColor, int aX, int aY, int aWidth, int aHeight) {
        parent.drawLine(aColor, xoffset + aX, yoffset + aY, aWidth, aHeight);
    }

    @Override
    public int getTextWidth(String aText) {
        return parent.getTextWidth(aText);
    }

    @Override
    public int getTextHeight(String aText) {
        return parent.getTextHeight(aText);
    }
}
