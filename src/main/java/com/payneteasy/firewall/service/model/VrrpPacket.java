package com.payneteasy.firewall.service.model;

public class VrrpPacket {

    public String local_address;
    public String remote_address;
    public String local_interface;
    public String remote_host;
    public String remote_interface;
    public String virtual_address;

    public static VrrpPacket createWithLocal(String aAddress, String aInterface, String aVirtualAddress) {
        VrrpPacket packet = new VrrpPacket();
        packet.local_address = aAddress;
        packet.local_interface = aInterface;
        packet.virtual_address = aVirtualAddress;
        return packet;
    }

    public String getLocal_address() {
        return local_address;
    }

    public String getRemote_address() {
        return remote_address;
    }

    public String getLocal_interface() {
        return local_interface;
    }

    public String getRemote_host() {
        return remote_host;
    }

    public String getRemote_interface() {
        return remote_interface;
    }
}
