package com.payneteasy.firewall.l2.editor.graphics;

import java.awt.*;

public interface ICanvas {

    void drawText(Color aColor, int aX, int aY, String aName);

    void fillRect(Color aColor, int aX, int aY, int aWidth, int aHeight);
    void drawRect(Color aColor, int aX, int aY, int aWidth, int aHeight);
    void drawLine(Color aColor, int aX, int aY, int aWidth, int aHeight);

    ICanvas createChild(int aOffsetX, int aOffsetY);

    int getTextWidth(String aText);

    int getTextHeight(String aText);
}
