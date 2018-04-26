package com.payneteasy.firewall.critsoft;

public class SoftwareCardDetails {

    private String server;
    private final String softwareName;
    private final String softwareVersion;
    private final String softwareDescription;
    private final String linkToHardeningGuide;

    public SoftwareCardDetails(String server, String softwareName, String softwareVersion, String softwareDescription, String linkToHardeningGuide) {
        this.server = server;
        this.softwareName = softwareName;
        this.softwareVersion = softwareVersion;
        this.softwareDescription = softwareDescription;
        this.linkToHardeningGuide = linkToHardeningGuide;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getSoftwareName() {
        return softwareName;
    }

    public String getSoftwareVersion() {
        return softwareVersion;
    }

    public String getSoftwareDescription() {
        return softwareDescription;
    }

    public String getLinkToHardeningGuide() {
        return linkToHardeningGuide;
    }
}
