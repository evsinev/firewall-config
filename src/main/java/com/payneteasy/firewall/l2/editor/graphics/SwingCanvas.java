package com.payneteasy.firewall.l2.editor.graphics;

import java.awt.*;

public class SwingCanvas implements ICanvas {

    private final Graphics2D graphics;

    public SwingCanvas(Graphics2D graphics) {
        this.graphics = graphics;
        graphics.setFont(graphics.getFont().deriveFont(16.0f));
    }

    @Override
    public void drawText(Color aColor, int aX, int aY, String aName) {
        graphics.setColor(aColor);
        graphics.drawString(aName, aX, aY);
    }

    @Override
    public void drawRect(Color aColor, int aX, int aY, int aWidth, int aHeight) {
        graphics.setColor(aColor);
        graphics.drawRect(aX, aY, aWidth, aHeight);
    }

    @Override
    public void drawLine(Color aColor, float width, int aX, int aY, int aWidth, int aHeight) {
        graphics.setColor(aColor);
        graphics.setStroke(new BasicStroke(width));
        graphics.drawLine(aX, aY, aWidth, aHeight);
        graphics.setStroke(new BasicStroke(1));
    }

    @Override
    public void fillRect(Color aColor, int aX, int aY, int aWidth, int aHeight) {
        graphics.setColor(aColor);
        graphics.fillRect(aX, aY, aWidth, aHeight);


    }

    @Override
    public ICanvas createChild(int aOffsetX, int aOffsetY) {
        return new SwingChildCanvas(this, aOffsetX, aOffsetY);
    }

    @Override
    public int getTextWidth(String aText) {
        return graphics.getFontMetrics().stringWidth(aText);
    }

    @Override
    public int getTextHeight(String aText) {
        return graphics.getFontMetrics().getHeight();
    }
}
