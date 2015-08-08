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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HostInterface that = (HostInterface) o;

        return link.equals(that.link);

    }

    @Override
    public int hashCode() {
        return link.hashCode();
    }
}
