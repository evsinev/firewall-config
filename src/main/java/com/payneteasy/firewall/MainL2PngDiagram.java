package com.payneteasy.firewall;

import com.payneteasy.firewall.dao.ConfigDaoYaml;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.l2.editor.L2EditorComponent;
import com.payneteasy.firewall.l2.editor.create.L2GraphCreator;
import com.payneteasy.firewall.shell.AbstractDirPrefixFilterCommand;
import picocli.CommandLine;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@CommandLine.Command(
        name = "MainL2PngDiagram"
        , mixinStandardHelpOptions = true
        , description = "Generates L2 png diagram"
)
public class MainL2PngDiagram extends AbstractDirPrefixFilterCommand {

    @Override
    public Integer call() throws Exception {
        IConfigDao configDao = new ConfigDaoYaml(dir);
        L2GraphCreator creator = new L2GraphCreator(configDao, dir, prefix);
        creator.create(getFilterArray());


        L2EditorComponent component = new L2EditorComponent(creator.getHosts(), creator.getLinks());

        { // first time draw
            BufferedImage bufferedImage = new BufferedImage(10_000, 10_000, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = bufferedImage.createGraphics();
            component.paintComponent(graphics);
        }

        Dimension size = component.getPreferredSize();

        // second time draw with preferred size
        BufferedImage bufferedImage = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = bufferedImage.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, size.width, size.height);
        graphics.setColor(Color.BLACK);
        component.paintComponent(graphics);

        ImageIO.write(bufferedImage, "PNG", new File(prefix+ "-l2.png"));

        return 0;
    }

    public static void main(String[] args) throws IOException {
        System.exit(
                new CommandLine(
                        new MainL2Labels()
                ).execute(args)
        );
    }
}
