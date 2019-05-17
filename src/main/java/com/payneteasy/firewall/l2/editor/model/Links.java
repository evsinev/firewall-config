package com.payneteasy.firewall.l2.editor.model;

import com.payneteasy.firewall.l2.editor.graphics.ICanvas;

import java.util.List;

public class Links {

    private final List<Link> links;

    public Links(List<Link> links) {
        this.links = links;
    }

    public void draw(ICanvas aCanvas) {
        for (Link link : links) {
            link.draw(aCanvas);
        }
    }

    public List<Link> getLinks() {
        return links;
    }
}
