package com.payneteasy.firewall.dao.model;

/**
 *
 */
public class TInterface {
    public String name;
    public String ip;
    public String dns;
    public String link;
    public String port;
    public String vip;
    public String vlan;

    @Override
    public String toString() {
        return "TInterface{" +
                "name='" + name + '\'' +
                ", ip='" + ip + '\'' +
                ", dns='" + dns + '\'' +
                ", link='" + link + '\'' +
                ", port='" + port + '\'' +
                ", vip='" + vip + '\'' +
                ", vlan='" + vlan + '\'' +
                '}';
    }
}
