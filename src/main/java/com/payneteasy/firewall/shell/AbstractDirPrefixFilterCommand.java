package com.payneteasy.firewall.shell;

import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;

import static com.payneteasy.firewall.util.Strings.split;

public abstract class AbstractDirPrefixFilterCommand implements Callable<Integer> {

    @CommandLine.Parameters(index = "0", description = "Working directory")
    protected File dir;

    @CommandLine.Parameters(index = "1", description = "Prefix for config file. Example: current")
    protected String prefix;

    @CommandLine.Option(names = {"-f", "--filter"}, description = "Filter. Default is 'internal,ipmi,internet'")
    String filter = "internal,ipmi,internet";

    protected String[] getFilterArray() {
        return split(filter, ',', ';', ' ', '\t', '\n', '\r');
    }

}
