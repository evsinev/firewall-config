package com.payneteasy.firewall;

import com.payneteasy.firewall.dao.ConfigDaoYaml;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.l2.editor.L2EditorComponent;
import com.payneteasy.firewall.l2.editor.create.L2GraphCreator;
import com.payneteasy.firewall.shell.AbstractDirPrefixFilterCommand;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import picocli.CommandLine;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@CommandLine.Command(
          name = "MainL2SvgDiagram"
        , mixinStandardHelpOptions = true
        , description = "Generate L2 SVG image"
)
public class MainL2SvgDiagram extends AbstractDirPrefixFilterCommand {

    @Override
    public Integer call() throws Exception {

        System.out.printf("Processing %s* in the %s directory...\n", prefix, dir);

        IConfigDao     configDao = new ConfigDaoYaml(dir);
        L2GraphCreator creator   = new L2GraphCreator(configDao, dir, prefix);
        creator.create(getFilterArray());

        // paint on large surface
        L2EditorComponent component = new L2EditorComponent(creator.getHosts(), creator.getLinks());
        SVGGraphics2D     graphics  = new SVGGraphics2D(10000, 10000);
        component.paintComponent(graphics);

        // calculate preferred size and paint once again
        Dimension size = component.getPreferredSize();
        graphics = new SVGGraphics2D(size.width, size.height);
        component.paintComponent(graphics);

        File svgFile = new File(prefix + "-l2.svg");
        try(FileWriter out     = new FileWriter(svgFile)) {
            out.write(graphics.getSVGDocument());
        }

        System.out.printf("Wrote to file %s\n", svgFile.getAbsolutePath());

        return 0;
    }

    public static void main(String[] args) throws IOException {
        System.exit(
            new CommandLine(
                new MainL2SvgDiagram()
            ).execute(args)
        );
    }
}
