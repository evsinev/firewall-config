package com.payneteasy.firewall.service.model;

public class LinkInfo {

    public final String leftAddress;
    public final String rightAddress;
    public final String colorHex;

    public LinkInfo(String leftAddress, String rightAddress, String colorHex) {
        this.leftAddress = leftAddress;
        this.rightAddress = rightAddress;
        this.colorHex = colorHex;
    }

    public String getLeftAddress() {
        return leftAddress;
    }

    public String getRightAddress() {
        return rightAddress;
    }

    public String getColorHex() {
        return colorHex;
    }
}
