package com.payneteasy.firewall;

import com.google.gson.Gson;
import com.payneteasy.firewall.podmancheck.model.PodmanSecurityResultType;
import com.payneteasy.firewall.podmancheck.model.TPodmanSecurityCheck;
import com.payneteasy.firewall.podmancheck.model.TPodmanSecurityGroup;
import com.payneteasy.firewall.podmancheck.model.TPodmanSecurityResult;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.payneteasy.firewall.util.Strings.isEmpty;

@CommandLine.Command(
        name = "MainPodmanCheckTable"
        , mixinStandardHelpOptions = true
        , description = "Generates redmine wiki page for podman-security-benchmark"
)
public class MainPodmanCheckTable implements Callable<Integer>  {

    private final Gson gson = new Gson();
    
    @CommandLine.Option(required = true, names = "--results-dir", description = "zip files with test results")
    protected File resultsDir;

    private PrintWriter out;

    private final StringBuilder sb = new StringBuilder();

    @Override
    public Integer call() {
        try ( PrintWriter out = new PrintWriter(System.out)) {
            this.out = out;
            createTable();
        }
        return 0;
    }

    private void createTable() {
        File[] files = resultsDir.listFiles((it) -> it.getName().endsWith(".zip"));
        Arrays.sort(files);
        printTableHeader(files);
        SortedSet<String> names = getCheckNames(files);

        // print row
        for (String name : names) {
            out.print("|" + name);
            for (File file : files) {
                out.print(" | " + getCheckResult(name, file));
            }
            out.println(" |");
        }

        out.println();
        out.println("h2. Notes");
        out.println();
        out.println(sb);

    }

    private String getCheckResult(String name, File file) {
        TPodmanSecurityCheck check    = load(file);
        String               hostname = getHostname(file);
        for (TPodmanSecurityGroup group : check.getTests()) {
            for (TPodmanSecurityResult result : group.getResults()) {
                if (result.getResultDescription() == null) {
                    continue;
                }
                if (name.equals(getResultTitle(result))) {
                    if(result.getResult() == PodmanSecurityResultType.NOTE || result.getResult() == PodmanSecurityResultType.WARN) {
                        appendRemediations(result, hostname);
                    }
                    return getWikiResultWithColor(result.getResult(), hostname, result.getResultId());
                }
            }
        }
        return " - ";
    }

    private void appendRemediations(TPodmanSecurityResult result, String hostname) {
        sb.append("h3. " + hostname + " " + result.getResultId());
        sb.append("\n");
        sb.append("\n");
        printParagraph("Remediation", result.getRemediation());
        printParagraph("Remediation Impact", result.getRemediationImpact());
        if (result.getItems() != null && !"4.3".equals(result.getResultId())) {
            sb.append("Items:\n");
            for (String item : result.getItems()) {
                sb.append("* ").append(item).append("\n");
            }
            sb.append("\n");
        }

        if (!"4.3".equals(result.getResultId())) {
            printParagraph("Details", result.getDetails());
        }
    }

    private SortedSet<String> getCheckNames(File[] files) {
        TreeSet<String> names = new TreeSet<>();
        for (File file : files) {
            TPodmanSecurityCheck check = load(file);
            for (TPodmanSecurityGroup group : check.getTests()) {
                for (TPodmanSecurityResult result : group.getResults()) {
                    if (result.getResultDescription() == null) {
                        continue;
                    }
                    names.add(getResultTitle(result));
                }
            }
        }
        return names;
    }

    private static String getResultTitle(TPodmanSecurityResult result) {
        return result.getResultId() + " " + result.getResultDescription();
    }

    private void printTableHeader(File[] aFiles) {
        out.print("|_.Check");
        for (File file : aFiles) {
            out.print("|_." + getHostname(file));
        }
        out.println("|");
    }

    private static String getHostname(File file) {
        return file.getName().replace(".zip", "");
    }
    
    private TPodmanSecurityCheck load(File aHostZipFile) {
        try(ZipFile zipFile = new ZipFile(aHostZipFile)) {
            ZipEntry entry = zipFile.getEntry("log/podman-security-bench.log.json");
            try(InputStreamReader in = new InputStreamReader(zipFile.getInputStream(entry), StandardCharsets.UTF_8)) {
                return gson.fromJson(in, TPodmanSecurityCheck.class);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot process " + aHostZipFile.getAbsolutePath(), e);
        }
    }

    private void printParagraph(PrintWriter out, String aName, String aText) {
        if (isEmpty(aText)) {
            return;
        }
        out.print("%{background:lightgray}");
        out.print(aName);
        out.print("% : ");
        out.println(aText);
        out.println();
    }

    private void printParagraph(String aName, String aText) {
        if (isEmpty(aText)) {
            return;
        }
        sb.append("%{background:lightgray}");
        sb.append(aName);
        sb.append("% : ");
        sb.append(aText);
        sb.append('\n');
        sb.append('\n');
    }

    private String getWikiResultWithColor(PodmanSecurityResultType result, String aHostname, String aResultId) {
        String link = "[[#" + aHostname + "-" + aResultId.replace(".", "") + "|" + result + "]]";

        if (Objects.requireNonNull(result) == PodmanSecurityResultType.PASS) {
            return "%{background:lightgreen}PASS%";
        } else if (result == PodmanSecurityResultType.INFO) {
            return "%{color:green}INFO%";
        } else if (result == PodmanSecurityResultType.NOTE) {
            return "%{background:magenta} " + link + "%";
        } else if (result == PodmanSecurityResultType.WARN) {
            return "%{background:yellow} " + link + "%";
        }

        throw new IllegalStateException("Unknown " + result);
    }

    public static void main(String[] args) throws IOException {
        System.exit(
                new CommandLine(
                        new MainPodmanCheckTable()
                ).execute(args)
        );
    }
}
