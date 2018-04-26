package com.payneteasy.firewall.critsoft;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


public class SoftwareCardHandlerTest {

    @Test
    public void testParseLine() {
        String line = "| srv-1 | Centos | CentOS Linux release 7.4.1708 (Core) | OS | https://path/to/hardening/Centos-7_hg |";
        SoftwareCardHandler handler = new SoftwareCardHandler();
        SoftwareCardDetails softwareCardDetails = handler.parseLine(line);
        assertThat("Server Name is not parsed properly", softwareCardDetails.getServer(), is("srv-1"));
        assertThat("Software Name is not parsed properly", softwareCardDetails.getSoftwareName(), is("Centos"));
        assertThat("Software Version is not parsed properly", softwareCardDetails.getSoftwareVersion(), is("CentOS Linux release 7.4.1708 (Core)"));
        assertThat("Software Description is not parsed properly", softwareCardDetails.getSoftwareDescription(), is("OS"));
        assertThat("Link To Hardening Guide is not parsed properly", softwareCardDetails.getLinkToHardeningGuide(), is("https://path/to/hardening/Centos-7_hg"));
    }

    @Test (expected = RuntimeException.class)
    public void testParseUnexpectedLineFormat() {
        String line = "\n7.4.1708 ";
        SoftwareCardHandler handler = new SoftwareCardHandler();
        //Should rase RuntimeException
        SoftwareCardDetails softwareCardDetails = handler.parseLine(line);
    }

    @Test
    public void testParseFileWithOneEntryPerSoftware () throws IOException {
        Path softwareCardFile = Paths.get("src/test/resources/crit_soft/srv-2");
        SoftwareCardHandler handler = new SoftwareCardHandler();
        Map<Pair<String, String>, List<SoftwareCardDetails>> softwareNameAndVersionToSoftwareCardDetails = handler.parseFile(softwareCardFile);
        assertThat("One entry per software file is not parsed properly", softwareNameAndVersionToSoftwareCardDetails.size(), is(8));
    }

    @Test
    public void testParseFileWithSeveralEntriesPerTheSameSoftware () throws IOException {
        Path softwareCardFile = Paths.get("src/test/resources/crit_soft/srv-1");
        SoftwareCardHandler handler = new SoftwareCardHandler();
        Map<Pair<String, String>, List<SoftwareCardDetails>> softwareNameAndVersionToSoftwareCardDetails = handler.parseFile(softwareCardFile);
        assertThat("Several entries per software file is not parsed properly", softwareNameAndVersionToSoftwareCardDetails.size(), is(13));
    }

    @Test
    public void testParseDirectoryContainingSoftwareCards() throws IOException {
        Path softwareCardDir = Paths.get("src/test/resources/crit_soft");
        SoftwareCardHandler handler = new SoftwareCardHandler();
        Map<Pair<String, String>, List<SoftwareCardDetails>> softwareNameAndVersionToSoftwareCardDetails = handler.parseDir(softwareCardDir);
        assertThat("Several entries per software file is not parsed properly", softwareNameAndVersionToSoftwareCardDetails.size(), is(17));

    }

    @Test
    public void testBuildRedmineTextileTableFromSeveralSoftwareCardDetailsRelatedToOneSoftware() {
        Map<Pair<String, String>, List<SoftwareCardDetails>> softwareNameAndVersionToSoftwareCardDetails = new TreeMap<>(new Comparator<Pair<String, String>>() {
            @Override
            public int compare(Pair<String, String> o1, Pair<String, String> o2) {
                int compare = o1.getLeft().compareTo(o2.getLeft());
                if (compare == 0) {
                    return o1.getRight().compareTo(o2.getRight());
                } else  return compare;
            }
        });
        SoftwareCardDetails javaSrv1 = new SoftwareCardDetails("srv-1_app1", "java", "1.8.0", "Java Development Kit", "http://link/to/hardening/java");
        SoftwareCardDetails javaSrv2 = new SoftwareCardDetails("srv-2", "java", "1.8.0", "Java Development Kit", "http://link/to/hardening/java");
        SoftwareCardDetails openssl = new SoftwareCardDetails("srv-3", "openssl", "1.0.2k-8.el7", "Openssl Toolkit", "http://link/to/hardening/openssl");
        softwareNameAndVersionToSoftwareCardDetails.put(Pair.of(javaSrv1.getSoftwareName(), javaSrv1.getSoftwareVersion()), Arrays.asList(javaSrv1, javaSrv2));
        softwareNameAndVersionToSoftwareCardDetails.put(Pair.of(openssl.getSoftwareName(), openssl.getSoftwareVersion()), Arrays.asList(openssl));
        SoftwareCardHandler handler = new SoftwareCardHandler();
        String textileOutput = handler.buildRedmineTextileTable(softwareNameAndVersionToSoftwareCardDetails);
        String textileEtalonOutput = "h1. Critical Software In Use\n\n" +
                "|_. Software Name|_. Software Version|_. Software Description|_. Hardening Guide|_. Hosts|\n" +
                "|java|1.8.0|Java Development Kit|http://link/to/hardening/java|[[srv-1_app1_run_installed|srv-1_app1]],[[srv-2_details|srv-2]]|\n"+
                "|openssl|1.0.2k-8.el7|Openssl Toolkit|http://link/to/hardening/openssl|[[srv-3_details|srv-3]]|\n";
        assertThat("Redmine Textile table from several software cards related to one software is not built properly", textileOutput, is(textileEtalonOutput));
    }




}
