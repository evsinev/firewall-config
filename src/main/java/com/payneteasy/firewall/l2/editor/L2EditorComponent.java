package com.payneteasy.firewall.l2.editor;

import com.payneteasy.firewall.l2.editor.actions.IPointSaver;
import com.payneteasy.firewall.l2.editor.graphics.ICanvas;
import com.payneteasy.firewall.l2.editor.graphics.SwingCanvas;
import com.payneteasy.firewall.l2.editor.model.Hosts;
import com.payneteasy.firewall.l2.editor.model.Links;

import javax.swing.*;
import java.awt.*;
import java.util.Date;

public class L2EditorComponent extends JComponent {

    private final Hosts hosts;
    private final Links links;
    private double scale = 1.0;

    public L2EditorComponent(Hosts aHosts, Links aLinks) {
        hosts = aHosts;
        links = aLinks;

    }

    public void save(IPointSaver aSaver) {
        hosts.save(aSaver);
    }

    @Override
    public void paintComponent(Graphics g){
        final Graphics2D g2 = (Graphics2D) g;

        g2.drawString("Generated at " + new Date(), 10, 20);

        if(scale != 1.0) {
            g2.scale(scale, scale);
        }

        ICanvas canvas = new SwingCanvas(g2);
        hosts.draw(canvas);
        links.draw(canvas);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(hosts.getMaxWidth() + 100, hosts.getMaxHeight() + 100);
    }

    public void setScale(double aScale) {
        scale = aScale;
    }
}
