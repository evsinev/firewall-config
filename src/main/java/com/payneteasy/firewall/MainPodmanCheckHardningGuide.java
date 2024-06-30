package com.payneteasy.firewall;

import com.google.gson.Gson;
import com.payneteasy.firewall.podmancheck.CreatePodmanIssueImpl;
import com.payneteasy.firewall.podmancheck.CreatePodmanIssueNoOp;
import com.payneteasy.firewall.podmancheck.ICreatePodmanIssue;
import com.payneteasy.firewall.podmancheck.model.PodmanSecurityResultType;
import com.payneteasy.firewall.podmancheck.model.TPodmanSecurityCheck;
import com.payneteasy.firewall.podmancheck.model.TPodmanSecurityGroup;
import com.payneteasy.firewall.podmancheck.model.TPodmanSecurityResult;
import com.payneteasy.firewall.util.Strings;
import picocli.CommandLine;

import java.io.*;
import java.util.Objects;
import java.util.concurrent.Callable;


@CommandLine.Command(
        name = "MainPodmanCheck"
        , mixinStandardHelpOptions = true
        , description = "Generates redmine wiki page for podman-security-benchmark"
)
public class MainPodmanCheckHardningGuide implements Callable<Integer>  {

    private final Gson gson = new Gson();

    @CommandLine.Option(names = "--result-file")
    protected File resultFile;

    @CommandLine.Option(names = "--redmine-url")
    String redmineUrl;

    @CommandLine.Option(names = "--redmine-key")
    String redmineKey;

    @CommandLine.Option(names = "--redmine-project")
    String redmineProject;

    @CommandLine.Option(names = "--redmine-parent-issue-id")
    int redmineParentIssueId;

    @CommandLine.Option(names = "--redmine-enabled", defaultValue = "false")
    boolean redmineEnabled;

    private ICreatePodmanIssue createPodmanIssue;

    @Override
    public Integer call() {
        createPodmanIssue = redmineEnabled
                ? new CreatePodmanIssueImpl(redmineUrl, redmineKey, redmineProject)
                : new CreatePodmanIssueNoOp();

        try ( PrintWriter out = new PrintWriter(System.out)) {
            createWikiPage(resultFile, out);
        }
        return 0;
    }

    private TPodmanSecurityCheck load(File aFile) {
        try {
            try (FileReader in = new FileReader(aFile)) {
                return gson.fromJson(in, TPodmanSecurityCheck.class);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + aFile, e);
        }
    }

    private void createWikiPage(File aFile, PrintWriter out) {
        TPodmanSecurityCheck check = load(aFile);
        for (TPodmanSecurityGroup group : check.getTests()) {
            out.printf("h2. %s %s\n", group.getGroupId(), group.getGroupDescription());
            out.println();
            for (TPodmanSecurityResult result : group.getResults()) {
                out.printf("h3. %s %s\n", result.getResultId(), result.getResultDescription());
                out.println();

                printDetails(out, result);
                createIssue(result);
            }
        }
    }

    private void createIssue(TPodmanSecurityResult result) {
        if (!shouldCreateIssue(result)) {
            return;
        }

        StringWriter stringWriter = new StringWriter();
        PrintWriter  printWriter  = new PrintWriter(stringWriter);
        printDetails(printWriter, result);
        printWriter.close();

        createPodmanIssue.createIssue(
                result.getResultId() + " " + result.getResultDescription()
                , redmineParentIssueId
                , stringWriter.toString()
        );
    }

    private void printDetails(PrintWriter out, TPodmanSecurityResult result) {
        printParagraph(out, "Status", getWikiResultWithColor(result.getResult()));
        printParagraph(out, "Details", result.getDetails());
        printParagraph(out, "Remediation", result.getRemediation());
        printParagraph(out, "Remediation Impact", result.getRemediationImpact());
    }

    private boolean shouldCreateIssue(TPodmanSecurityResult result) {
        return result.getResultDescription().contains("(Manual)")
                || result.getResult() == PodmanSecurityResultType.NOTE
                || result.getResult() == PodmanSecurityResultType.WARN;
    }

    private void printParagraph(PrintWriter out, String aName, String aText) {
        if (Strings.isEmpty(aText)) {
            return;
        }
        out.print("%{background:lightgray}");
        out.print(aName);
        out.print("% : ");
        out.println(aText);
        out.println();
    }

    private String getWikiResultWithColor(PodmanSecurityResultType result) {

        if (Objects.requireNonNull(result) == PodmanSecurityResultType.PASS) {
            return "%{background:lightgreen}PASS%";
        } else if (result == PodmanSecurityResultType.INFO) {
            return "%{color:#82B6E1}INFO%";
        } else if (result == PodmanSecurityResultType.NOTE) {
            return "NOTE";
        } else if (result == PodmanSecurityResultType.WARN) {
            return "%{background:yellow}WARN%";
        }

        throw new IllegalStateException("Unknown " + result);
    }

    public static void main(String[] args) throws IOException {
        System.exit(
                new CommandLine(
                        new MainPodmanCheckHardningGuide()
                ).execute(args)
        );
    }
}
