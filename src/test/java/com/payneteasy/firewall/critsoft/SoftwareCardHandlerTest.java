package com.payneteasy.firewall.critsoft;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

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
        SoftwareCardDetails softwareCardDetails = handler.parseLine(line);
    }

    public void testParseFile () {

    }

}
