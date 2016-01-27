package com.payneteasy.firewall.dao.model;

import java.util.List;

/**
 *
 */
public class TInterface {
    public String name;
    public String ip;

    // default is 24
    public String netmask;
    public String dns;
    public String link;
    public String port;

    /** VRRP virtual IP address */
    public String vip;

    /** On Intel NUC ipmi and primary ethernet are on the same physical interface */
    public String ipmi_ip;

    /** VRRP priority, default is 100 */
    public String vrrpPriority;

    public String vlan;

    public List<TVirtualIpAddress> vips;

    public String getLongNetmask() {
        if(netmask == null) {
            return "255.255.255.0";
        }
        switch (netmask) {
            case "16": return "255.255.0.0";
            default:
                throw new IllegalStateException("Netmask not supported yet: "+netmask+". Please add support to TInterface.getLongNetmask() method");
        }
    }

    public String getVrrpPriority() {
        return vrrpPriority != null ? vrrpPriority : "100";
    }

    public boolean skipIpAddress() {
        return "skip".equals(ip);
    }

    @Override
    public String toString() {
        return "TInterface{" +
                "name='" + name + '\'' +
                ", ip='" + ip + '\'' +
                ", netmask='" + netmask + '\'' +
                ", dns='" + dns + '\'' +
                ", link='" + link + '\'' +
                ", port='" + port + '\'' +
                ", vip='" + vip + '\'' +
                ", ipmi_ip='" + ipmi_ip + '\'' +
                ", vrrpPriority='" + vrrpPriority + '\'' +
                ", vlan='" + vlan + '\'' +
                ", vips=" + vips +
                '}';
    }
}
