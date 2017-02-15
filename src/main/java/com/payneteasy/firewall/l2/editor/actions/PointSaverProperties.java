package com.payneteasy.firewall.l2.editor.actions;

import java.util.Properties;

public class PointSaverProperties implements IPointSaver {


    final Properties properties;
    final String prefix;


    public PointSaverProperties(Properties aProperties) {
        properties = aProperties;
        prefix = "";
    }

    public PointSaverProperties(Properties properties, String prefix) {
        this.properties = properties;
        this.prefix = prefix;
    }

    @Override
    public void save(String aName, int aX, int aY) {
        properties.setProperty(prefix + aName, aX + ", " + aY);
    }

    @Override
    public IPointSaver createChild(String aName) {
        return new PointSaverProperties(properties, aName+".");
    }
}
