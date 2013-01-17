package com.payneteasy.firewall.service.model;

/**
 *
 */
public class InputPacket {

    public String protocol;
    public String source_address;
    public int destination_port;
    public String input_interface;
    public String source_address_name;
    public String app_protocol;

    public String getSource_address_name() {
        return source_address_name;
    }

    public String getApp_protocol() {
        return app_protocol;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getSource_address() {
        return source_address;
    }

    public int getDestination_port() {
        return destination_port;
    }

    public String getInput_interface() {
        return input_interface;
    }
}
