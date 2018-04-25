package com.payneteasy.firewall.critsoft;

public class SoftwareCardHandler {

    public SoftwareCardDetails parseLine(String line) {
        String[] parts = line.split("\\|");
        if (parts.length !=6) {
            // Expect something like this "| srv-1 | Centos | CentOS Linux release 7.4.1708 (Core) | OS | https://path/to/hardening/Centos-7_hg |"
            throw new RuntimeException("Unexpected line format");
        }
        //assert parts.length != 5;
        return new SoftwareCardDetails(parts[1].trim(), parts[2].trim(), parts[3].trim(), parts[4].trim(), parts[5].trim());
    }
}
