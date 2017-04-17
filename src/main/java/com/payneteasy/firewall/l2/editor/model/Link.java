package com.payneteasy.firewall.l2.editor.model;

import com.payneteasy.firewall.l2.editor.graphics.ICanvas;

import java.awt.*;

public class Link {
    private final Host leftHost;
    private final Port leftPort;

    private final Host rightHost;
    private final Port rightPort;

    private final LinkType linkType;

    public Link(Host leftHost, Port leftPort, Host rightHost, Port rightPort, LinkType aType) {
        this.leftHost = leftHost;
        this.leftPort = leftPort;
        this.rightHost = rightHost;
        this.rightPort = rightPort;
        linkType = aType;
    }

    public void draw(ICanvas aCanvas) {

        Rectangle leftRectangle   = leftPort.createRectangle();
        Rectangle rightRectangle  = rightPort.createRectangle();

        Point leftStart  = leftHost.getPoint();
        Point rightStart = rightHost.getPoint();

        Color color = linkType == LinkType.CUSTOM_ADDED ? Color.RED : Color.DARK_GRAY;
        float width = linkType == LinkType.CUSTOM_ADDED ? 2 : 1;

        aCanvas.drawLine(color
                , width
                , leftStart.x + leftRectangle.x
                , leftStart.y + leftRectangle.y
                , rightStart.x + rightRectangle.x
                , rightStart.y + rightRectangle.y
        );
    }
}
