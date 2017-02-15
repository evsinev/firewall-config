package com.payneteasy.firewall.l2.editor.actions;

import com.payneteasy.firewall.l2.editor.model.Hosts;
import com.payneteasy.firewall.l2.editor.model.INode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class MouseMovementListener implements MouseListener, MouseMotionListener {

    private final Hosts hosts;
    private final JComponent component;

    public MouseMovementListener(Hosts hosts, JComponent aComponent) {
        this.hosts = hosts;
        component = aComponent;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        hosts.pick(e.getX(), e.getY());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        hosts.unpick();
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        hosts.movePicked(e.getX(), e.getY());
        component.repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }
}
