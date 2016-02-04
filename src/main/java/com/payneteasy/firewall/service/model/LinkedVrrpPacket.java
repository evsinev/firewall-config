package com.payneteasy.firewall.service.model;

public class LinkedVrrpPacket {

    public String local_interface;
    public String remote_host;
    public String remote_address;
    public String virtual_address;

    public String getLocal_interface() {
        return local_interface;
    }

    public String getRemote_host() {
        return remote_host;
    }

    public String getRemote_address() {
        return remote_address;
    }

    public String getVirtual_address() {
        return virtual_address;
    }

}
