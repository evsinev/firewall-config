package com.payneteasy.firewall;

import com.payneteasy.firewall.l2.editor.L2Editor;
import com.payneteasy.firewall.shell.AbstractDirPrefixFilterCommand;
import picocli.CommandLine;

import java.io.IOException;

@CommandLine.Command(
        name = "MainL2DiagramEditor"
        , mixinStandardHelpOptions = true
        , description = "Edit L2 diagram"
)
public class MainL2DiagramEditor extends AbstractDirPrefixFilterCommand {

    @Override
    public Integer call() throws Exception {
        System.out.printf("Running MainL2DiagramEditor for %s* in the %s directory...\n", prefix, dir);

        new L2Editor().show(dir, prefix, getFilterArray());

        return 0;
    }

    public static void main(String[] args) throws IOException {
        new CommandLine(
                new MainL2DiagramEditor()
        ).execute(args);
    }
}
