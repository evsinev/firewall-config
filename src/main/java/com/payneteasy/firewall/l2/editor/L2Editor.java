package com.payneteasy.firewall.l2.editor;

import com.payneteasy.firewall.dao.ConfigDaoYaml;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.l2.editor.actions.IPointSaver;
import com.payneteasy.firewall.l2.editor.actions.MouseMovementListener;
import com.payneteasy.firewall.l2.editor.actions.PointSaverProperties;
import com.payneteasy.firewall.l2.editor.create.L2GraphCreator;
import com.payneteasy.firewall.l2.editor.model.Hosts;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;

public class L2Editor implements KeyListener {

    private final JFrame frame;
    L2EditorComponent component;
    MouseMovementListener mouseListener;
    double scale = 1;
    File configDir;

    public L2Editor() {
        frame = new JFrame("L2");
        frame.setSize(400, 500);
        frame.addKeyListener(this);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

    }

    public void show(File aConfigDir) throws IOException {

        configDir = aConfigDir;

        IConfigDao configDao = new ConfigDaoYaml(aConfigDir);
        L2GraphCreator creator = new L2GraphCreator(configDao, aConfigDir);
        creator.create();

        final Hosts hosts = creator.getHosts();
        component = new L2EditorComponent(hosts, creator.getLinks());

        mouseListener = new MouseMovementListener(hosts, component);
        component.addMouseListener(mouseListener);
        component.addMouseMotionListener(mouseListener);

        frame.add(new JScrollPane(component));
        frame.setVisible(true);
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        System.out.println("e = " + e);
        switch (e.getKeyCode()){
            case KeyEvent.VK_Q:
                System.exit(0);
                return;

            case KeyEvent.VK_EQUALS:
                scale = scale + 0.01;
                component.setScale(scale);
                component.repaint();
                return;

            case KeyEvent.VK_MINUS:
                scale = scale - 0.01;
                component.setScale(scale);
                component.repaint();
                return;

            case KeyEvent.VK_R:
                try {
                    component.removeMouseListener(mouseListener);
                    component.removeMouseMotionListener(mouseListener);
                    frame.removeAll();
                    show(configDir);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return;

            case KeyEvent.VK_S:
                save();
                return;

            default:

        }
    }

    private void save() {
        try {
            Properties props = new Properties();
            IPointSaver saver = new PointSaverProperties(props);
            component.save(saver);
            props.store(new FileWriter(new File(configDir, "positions.properties")), "");
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't store points", e);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}
