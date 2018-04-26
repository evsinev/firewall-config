package com.payneteasy.firewall.critsoft;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SoftwareCardHandler {

    public static final String TEXTILE_TABLE_DELIMITER = "|";

    public SoftwareCardDetails parseLine(String line) {
        String[] parts = line.split("\\|");
        if (parts.length !=6) {
            // Expect something like this "| srv-1 | Centos | CentOS Linux release 7.4.1708 (Core) | OS | https://path/to/hardening/Centos-7_hg |"
            throw new RuntimeException("Unexpected line format: " + line);
        }
        //assert parts.length != 5;
        return new SoftwareCardDetails(parts[1].trim(), parts[2].trim(), parts[3].trim(), parts[4].trim(), parts[5].trim());
    }

    public Map<Pair<String, String>, List<SoftwareCardDetails>> parseFile(Path softwareCardFile) throws IOException {
        Map<Pair<String, String>, List<SoftwareCardDetails>> softwareNameAndVersionToSoftwareCardDetails = new HashMap<>();
        parseLineWithMap(softwareCardFile, softwareNameAndVersionToSoftwareCardDetails);
        return softwareNameAndVersionToSoftwareCardDetails;
    }

    private void parseLineWithMap(Path softwareCardFile, Map<Pair<String, String>, List<SoftwareCardDetails>> softwareNameAndVersionToSoftwareCardDetails) throws IOException {
        Files.lines(softwareCardFile).forEach(line -> {
            if (StringUtils.isNotBlank(line)) {
                SoftwareCardDetails details = parseLine(line);
                softwareNameAndVersionToSoftwareCardDetails.merge(Pair.of(details.getSoftwareName(), details.getSoftwareVersion()), Arrays.asList(details), (o, n) -> Stream.concat(o.stream(), n.stream()).distinct().collect(Collectors.toList()));
            }
        });
    }

    public String buildRedmineTextileTable(Map<Pair<String, String>, List<SoftwareCardDetails>> softwareNameAndVersionToSoftwareCardDetails) {
        StringBuilder builder = new StringBuilder();
        builder.append("h1. Critical Software In Use\n\n");
        builder.append("|_. Software Name|_. Software Version|_. Software Description|_. Hardening Guide|_. Hosts|\n");
        softwareNameAndVersionToSoftwareCardDetails.forEach((softwareNameAndVersionPair, softwareCardDetailsList) -> {
            builder.append(TEXTILE_TABLE_DELIMITER)
                    .append(softwareNameAndVersionPair.getLeft())
                    .append(TEXTILE_TABLE_DELIMITER)
                    .append(softwareNameAndVersionPair.getRight())
                    .append(TEXTILE_TABLE_DELIMITER)
                    .append(softwareCardDetailsList.stream().map(SoftwareCardDetails::getSoftwareDescription).distinct().collect(Collectors.joining(",\n")))
                    .append(TEXTILE_TABLE_DELIMITER)
                    .append(softwareCardDetailsList.stream().map(SoftwareCardDetails::getLinkToHardeningGuide).distinct().collect(Collectors.joining(",\n")))
                    .append(TEXTILE_TABLE_DELIMITER)
                    .append(softwareCardDetailsList.stream().map(d->wrapWithLinkToDetailsPage(d.getServer())).distinct().collect(Collectors.joining(",")))
                    .append(TEXTILE_TABLE_DELIMITER)
                    .append("\n");

        });
        return builder.toString();
    }

    private String wrapWithLinkToDetailsPage(String server) {
        return new StringBuilder("[[").append(server).append(server.contains("_")?"_run_installed":"_details").append(TEXTILE_TABLE_DELIMITER).append(server).append("]]").toString();
    }

    public Map<Pair<String, String>, List<SoftwareCardDetails>> parseDir(Path softwareCardDir) throws IOException {
        Map<Pair<String, String>, List<SoftwareCardDetails>> softwareNameAndVersionToSoftwareCardDetails = new TreeMap<>((o1, o2) -> {
            int compare = o1.getLeft().compareTo(o2.getLeft());
            if (compare == 0)
                return o1.getRight().compareTo(o2.getRight());
            else
                return compare;
        });

        SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                parseLineWithMap(file, softwareNameAndVersionToSoftwareCardDetails);

                return FileVisitResult.CONTINUE;
            }
        };
        Files.walkFileTree(softwareCardDir, visitor);
        return softwareNameAndVersionToSoftwareCardDetails;
    }
}
