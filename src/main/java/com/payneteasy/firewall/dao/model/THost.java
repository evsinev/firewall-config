package com.payneteasy.firewall.dao.model;

import com.google.common.collect.Lists;
import com.payneteasy.firewall.util.Networks;

import java.util.List;

/**
 *
 */
public class THost {

    public String name;

    public String description;
    public String justification;

    public String gw;

    public String group;

    public List<TInterface> interfaces;

    public List<TService> services = Lists.newArrayList();

    public String color;

    public String getDefaultIp() {
        String ip = interfaces.get(0).ip;
        if(Networks.isIpAddress(ip)) {
            return ip;
        }

        for (TInterface iface : interfaces) {
            if(Networks.isIpAddress(iface.ip)) {
                return iface.ip;
            }
        }
        throw new IllegalStateException("There no default ip address for host "+name);
    }

    public String services_links;

    @Override
    public String toString() {
        return "THost{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", justification='" + justification + '\'' +
                ", gw='" + gw + '\'' +
                ", group='" + group + '\'' +
                ", interfaces=" + interfaces +
                ", services=" + services +
                ", color='" + color + '\'' +
                ", services_links='" + services_links + '\'' +
                '}';
    }
}
