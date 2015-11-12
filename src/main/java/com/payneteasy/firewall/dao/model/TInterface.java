package com.payneteasy.firewall.dao.model;

/**
 *
 */
public class TInterface {
    public String name;
    public String ip;

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
                ", vrrpPriority='" + vrrpPriority + '\'' +
                ", vlan='" + vlan + '\'' +
                '}';
    }

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
}
