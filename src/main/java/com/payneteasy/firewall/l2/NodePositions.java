package com.payneteasy.firewall.l2;

import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TInterface;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.*;

public class NodePositions {

    final TreeMap<String, Point> map = new TreeMap<>();

    final File file;

    public NodePositions(File aFile) {
        file = aFile;
        if(!aFile.exists()) {
            return;
        }

        Properties props = new Properties();
        try {
            try (FileInputStream in = new FileInputStream(file)) {
                props.load(in);
            }
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                String values = (String) entry.getValue();
                StringTokenizer st = new StringTokenizer(values, " ,x;.");
                int x = Integer.parseInt(st.nextToken());
                int y = Integer.parseInt(st.nextToken());
                map.put((String) entry.getKey(), new Point(x, y));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not load "+file, e);
        }
    }

    public Point getPosition(THost aHost, TInterface aIface) {
        String ifaceName = aIface.port != null ? aIface.port : aIface.name;
        String key = aHost.name+"."+ifaceName;
        return map.get(key);
    }

    public void clear() {
        map.clear();
    }

    public void add(String aKey, int x, int y) {
        map.put(aKey, new Point(x, y));
    }

    public void save() {
        try {
            try (PrintWriter out = new PrintWriter(file)) {
                for (Map.Entry<String, Point> entry : map.entrySet()) {
                    Point point = entry.getValue();
                    out.println(entry.getKey() + " = " + point.x + ", " + point.y);
                }
            }

        } catch (Exception e) {
            throw new IllegalStateException("Could no save", e);
        }
    }
}
