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
    String prefix;

    public L2Editor() {
        frame = new JFrame("L2" );
        frame.setSize(1916, 1057);
        frame.addKeyListener(this);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

    }

    public void show(File aConfigDir, String aPrefix) throws IOException {
        prefix = aPrefix;
        configDir = aConfigDir;

        IConfigDao configDao = new ConfigDaoYaml(aConfigDir);
        L2GraphCreator creator = new L2GraphCreator(configDao, aConfigDir, aPrefix);
        creator.create();

        final Hosts hosts = creator.getHosts();
        component = new L2EditorComponent(hosts, creator.getLinks());

        mouseListener = new MouseMovementListener(hosts, component);
        component.addMouseListener(mouseListener);
        component.addMouseMotionListener(mouseListener);

        frame.add(new JScrollPane(component));
        frame.setTitle(prefix + " L2");
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
                System.out.println(frame.getBounds());
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
                    show(configDir, prefix);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return;

            case KeyEvent.VK_S:
                save();
                return;

            case KeyEvent.VK_RIGHT:
                if(e.isShiftDown()) {
                    component.shiftRight();
                }
                return;

            case KeyEvent.VK_LEFT:
                if(e.isShiftDown()) {
                    component.shiftLeft();
                }
                return;

            case KeyEvent.VK_UP:
                if(e.isShiftDown()) {
                    component.shiftTop();
                }
                return;

            case KeyEvent.VK_DOWN:
                if(e.isShiftDown()) {
                    component.shiftBottom();
                }
                return;

            default:

        }
    }

    private void save() {
        try {
            Properties props = new Properties();
            IPointSaver saver = new PointSaverProperties(props);
            component.save(saver);
            final File file = new File(configDir, prefix + "-l2-positions.properties");
            props.store(new FileWriter(file), "");
            System.out.println("Saved to " + file);
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't store points", e);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}
