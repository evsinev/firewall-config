package com.payneteasy.firewall;

import com.payneteasy.firewall.dao.ConfigDaoYaml;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TService;
import com.payneteasy.firewall.shell.AbstractDirPrefixFilterCommand;
import picocli.CommandLine;

import java.util.List;

import static com.payneteasy.firewall.util.Strings.splitParams;

@CommandLine.Command(
        name = "MainExternalServices"
        , mixinStandardHelpOptions = true
        , description = "Generates Redmine table with external services"
)
public class MainExternalServices extends AbstractDirPrefixFilterCommand {

    @CommandLine.Option(names = {"-t", "--tags"}, description = "Tags. Default is 'ingress-nginx'")
    String tagsOption = "ingress-nginx";

    @CommandLine.Option(names = {"-a", "--access"}, description = "Access from. Default is 'internet,pub'")
    String accessOption = "internet";

    @Override
    public Integer call() throws Exception {
        IConfigDao  configDao = new ConfigDaoYaml(dir);
        List<THost> hosts     = configDao.listHosts();

        for (THost host : hosts) {
            for (TService service : host.services) {
                if(isExternal(service)) {
                    printRow(
                              host.name              // |_. host.name
                            , host.description       // |_. host.description
                            , host.justification     // |_. host.justification
                            , service.access         // |_. service.access
                            , service.name           // |_. service.name
                            , service.description    // |_. service.description
                            , service.justification  // |_. service.justification
                            , service.program        // |_. service.program
                            , service.url            // |_. service.url
                            , service.nat            // |_. service.nat
                    );
                }
            }
        }

        return 0;
    }

    private boolean isExternal(TService aService) {
        List<String> tags = aService.tags;
        if(tags != null) {
            for (String tag : splitParams(tagsOption)) {
                if(tags.contains(tag)) {
                    return true;
                }
            }
        }

        for (String substring : splitParams(accessOption)) {
            for (String access : aService.access) {
                if(access.contains(substring)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void printRow(Object ... values) {
        StringBuilder sb = new StringBuilder();
        sb.append("|");
        for (Object value : values) {

            sb.append(' ');
            sb.append(value == null ? "" : value);

            sb.append(" |");
        }
        System.out.println(sb);
    }

    public static void main(String[] args) {
        runCommand(new MainExternalServices(), args);
    }

}
