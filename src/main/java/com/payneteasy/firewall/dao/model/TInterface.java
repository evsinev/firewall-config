package com.payneteasy.firewall.dao.model;

import com.payneteasy.firewall.util.Networks;

import java.util.ArrayList;
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

    /** VRRP priority, default is 100 */
    public String vrrpPriority;

    public String vlan;

    /**
     * TCP MSS Parameter
     * INPUT -s $ADDRESS -p tcp -m tcp --tcp-flags SYN,RST SYN -j TCPMSS --set-mss 1300
     */
    public Integer mss;

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

    public List<String> getAllIpAddresses() {
        List<String> ips = new ArrayList<>();
        if(Networks.isIpAddress(ip)) {
            ips.add(ip);
        }
        if(Networks.isIpAddress(vip)) {
            ips.add(vip);
        }
        if(vips != null) {
            for (TVirtualIpAddress virtualIpAddress : vips) {
                if(Networks.isIpAddress(virtualIpAddress.ip)) {
                    ips.add(virtualIpAddress.ip);
                }
            }
        }
        return ips;
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
                ", vrrpPriority='" + vrrpPriority + '\'' +
                ", vlan='" + vlan + '\'' +
                ", vips=" + vips +
                '}';
    }
}
