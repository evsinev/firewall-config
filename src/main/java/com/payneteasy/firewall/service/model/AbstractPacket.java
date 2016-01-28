package com.payneteasy.firewall.service.model;

public abstract class AbstractPacket {

    public String   protocol;

    public String   app_protocol;

    public String   destination_address;
    public int      destination_port;

    public String   source_service;
    public String   destination_service;

    public String getSource_service() {
        return source_service != null
                ? source_service
                : destination_service != null ? "*" : "";
    }

    public String getDestination_service() {
        return destination_service!= null
                ? destination_service
                : source_service != null ? "" + destination_port : "";
    }

    public String getSource_to_dest_service_sign() {
        return destination_service != null || source_service != null ? "-->" : "";
    }

    public String getProtocol() {
        return protocol;
    }

    public String getApp_protocol() {
        return app_protocol;
    }

    public int getDestination_port() {
        return destination_port;
    }

    public String getDestination_address() {
        return destination_address;
    }


}
