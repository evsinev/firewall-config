package com.payneteasy.firewall.l2.editor.create;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.StringTokenizer;

public class PropertiesPositionManager implements IPositionManager {

    final Properties properties;
    final IPositionManager missManager;

    public PropertiesPositionManager(File aDirectory, IPositionManager aMissManager) {
        properties = new Properties();
        missManager = aMissManager;
        try {
            final File file = new File(aDirectory, "positions.properties");
            if(file.exists()) {
                properties.load(new FileReader(file));
            } else {
                System.out.println("Properties file " + file.getAbsolutePath() + " does not exit");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't load property file", e);
        }
    }

    @Override
    public Point getPortPosition(String aHostname, String aPortName) {
        String text = properties.getProperty(aHostname+"."+aPortName);
        if(text == null) {
            return missManager.getPortPosition(aHostname, aPortName);
        }
        return parsePoint(text);
    }

    @Override
    public Point getHostPosition(String aHostname) {
        String text = properties.getProperty(aHostname);
        if(text == null) {
            return missManager.getHostPosition(aHostname);
        }
        return parsePoint(text);
    }

    private Point parsePoint(String aText) {
        StringTokenizer st = new StringTokenizer(aText, " ,;x");
        return new Point(Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken()));
    }
}
