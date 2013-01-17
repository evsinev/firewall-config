package com.payneteasy.firewall.service.model;

/**
 *
 */
public class OutputPacket {

    public String app_protocol;
    public String destination_address_name;
    public String destination_address;
    public String protocol;
    public int destination_port;

    public String getApp_protocol() {
        return app_protocol;
    }

    public String getDestination_address_name() {
        return destination_address_name;
    }

    public String getDestination_address() {
        return destination_address;
    }

    public String getProtocol() {
        return protocol;
    }

    public int getDestination_port() {
        return destination_port;
    }
}
