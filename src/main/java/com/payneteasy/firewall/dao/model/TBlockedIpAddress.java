package com.payneteasy.firewall.dao.model;

public class TBlockedIpAddress {

    public String    ip;
    public String    reason;
    public BlockType type = BlockType.REJECT;

    public enum BlockType {
        REJECT, DROP
    }

    public String getIp() {
        return ip;
    }

    public String getReason() {
        return reason;
    }

    public BlockType getType() {
        return type;
    }
}
