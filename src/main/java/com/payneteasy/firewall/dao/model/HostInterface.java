package com.payneteasy.firewall.dao.model;

public class HostInterface {
    public final THost host;
    public final TInterface iface;
    public final String link;

    public HostInterface(THost host, TInterface iface) {
        this.host = host;
        this.iface = iface;
        link = host.name+"/"+iface.name;
    }

    @Override
    public String toString() {
        return link;
    }
}
