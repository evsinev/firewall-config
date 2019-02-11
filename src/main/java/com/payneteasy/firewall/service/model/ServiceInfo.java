package com.payneteasy.firewall.service.model;

import java.util.List;

/**
 */
public class ServiceInfo {

    public String appProtocol;
    public String protocol;
    public int port;

    public String description;
    public String justification;

    public String address;

    public String program;

    public UrlInfo nat;

    public List<Access> access;

    @Override
    public String toString() {
        return "ServiceInfo{" +
                "appProtocol='" + appProtocol + '\'' +
                ", protocol='" + protocol + '\'' +
                ", port=" + port +
                ", description='" + description + '\'' +
                ", justification='" + justification + '\'' +
                ", address='" + address + '\'' +
                ", program='" + program + '\'' +
                ", nat=" + nat +
                ", access=" + access +
                '}';
    }
}
