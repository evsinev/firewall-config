package com.payneteasy.firewall;

import com.payneteasy.firewall.dao.ConfigDaoYaml;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.l3.CreateL3Diagram;
import com.payneteasy.firewall.shell.AbstractDirPrefixFilterCommand;
import picocli.CommandLine;

import java.io.IOException;

@CommandLine.Command(
        name = "MainL3Diagram"
        , mixinStandardHelpOptions = true
        , description = "Generate L3 image"
)
public class MainL3Diagram extends AbstractDirPrefixFilterCommand {

    @CommandLine.Option(names = {"--run-nwdiag"}, description = "Run nwdiag")
    private boolean runNwdiag = true;

    @Override
    public Integer call() throws Exception {

        IConfigDao configDao = new ConfigDaoYaml(dir);

        CreateL3Diagram creator = new CreateL3Diagram(dir, runNwdiag, getFilterArray());
        creator.create(configDao);

        return 0;
    }

    public static void main(String[] args) throws IOException {
        System.exit(
                new CommandLine(
                        new MainL3Diagram()
                ).execute(args)
        );
    }

}
