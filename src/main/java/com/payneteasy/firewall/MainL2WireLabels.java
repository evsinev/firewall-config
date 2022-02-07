package com.payneteasy.firewall;

import com.payneteasy.firewall.dao.ConfigDaoYaml;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.l2.labels.wire.L2WireSvgFile;
import com.payneteasy.firewall.l2.labels.wire.ResourcePath;
import com.payneteasy.firewall.l2.labels.wire.TemplateText;
import com.payneteasy.firewall.service.model.LinkInfo;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "MainL2WireLabels"
        , mixinStandardHelpOptions = true
        , description = "Generate L2 wire labels"
)
public class MainL2WireLabels implements Callable<Integer> {

    @CommandLine.Parameters(index = "0", description = "Working directory")
    private File dir;

    @Override
    public Integer call() throws Exception {
        File configDir = dir;
        if(!configDir.exists()) throw new IllegalStateException("Config dir "+configDir.getAbsolutePath()+" is not exists");

        IConfigDao     configDao = new ConfigDaoYaml(configDir);
        MainL2Labels   main      = new MainL2Labels();
        List<LinkInfo> infos     = sort(main.createLinkInfos(configDao, configDir));

        L2WireSvgFile svgFile = new L2WireSvgFile();
        svgFile.openOutputFile(new File("test.svg"));

        ResourcePath groupResource = new ResourcePath("labels/wire/l2wire-group.svg");
        TemplateText templateText  = new TemplateText(groupResource.getText());

        try {
            svgFile.addHeader(new ResourcePath("labels/wire/l2wire-a4.svg"));

            double x_increment = 22 ;
            double x           = 5 - x_increment;

            int i = 0;

            double y_increment = 30;
            double y = 3;

            for (LinkInfo info : infos) {

                i++;
                if(i > 8) {
                    i = 0;
                    x = 5;
                    y += y_increment;
                } else {
                    x += x_increment;
                }

                svgFile.addText(templateText.replace(
                        "$left", normalizeName(info.leftAddress)
                        , "$right", normalizeName(info.rightAddress)
                        ,  "$group_x", x +""
                        ,  "$group_y", y +""
                ));

            }

        } finally {
            svgFile.close();
        }

        return 0;
    }

    public static void main(String[] args) throws IOException {
        System.exit(
                new CommandLine(
                        new MainL2WireLabels()
                ).execute(args)
        );
    }

    private static String normalizeName(String aText) {
        return aText
                .replace("/eth"     , ".")
                .replace("/br"      , ".")
                .replace("/"        , ".")
                .replace("remote"   , "rmt")
                .replace("remote"   , "rmt")
                .replace(".ipmi"    , ".mi")
                .replace(".sfp"     , ".s")
                ;
    }

    private static List<LinkInfo> sort(List<LinkInfo> aLinks) {
        aLinks.sort(Comparator.comparing(left -> (left.leftAddress + " " + left.rightAddress + " " + left.colorHex)));
        return aLinks;
    }

}
