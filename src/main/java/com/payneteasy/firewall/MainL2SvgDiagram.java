package com.payneteasy.firewall;

import com.payneteasy.firewall.dao.ConfigDaoYaml;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.l2.editor.L2EditorComponent;
import com.payneteasy.firewall.l2.editor.create.L2GraphCreator;
import org.jfree.graphics2d.svg.SVGGraphics2D;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MainL2SvgDiagram {

    public static void main(String[] args) throws IOException {
        File dir = new File(args[0]);
        String prefix = args[1];

        IConfigDao configDao = new ConfigDaoYaml(dir);
        L2GraphCreator creator = new L2GraphCreator(configDao, dir, prefix);
        creator.create();


        L2EditorComponent component = new L2EditorComponent(creator.getHosts(), creator.getLinks());
        SVGGraphics2D graphics = new SVGGraphics2D(10000, 10000);
        component.paintComponent(graphics);

        Dimension size = component.getPreferredSize();
        graphics = new SVGGraphics2D(size.width, size.height);
        component.paintComponent(graphics);
        FileWriter out = new FileWriter(new File(prefix+ "-l2.svg"));
        out.write(graphics.getSVGDocument());
        out.close();

        System.exit(0);
    }
}
