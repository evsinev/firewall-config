package com.payneteasy.firewall.dao.model;

import com.google.common.collect.Lists;

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

    public String getDefaultIp() {
        return interfaces.get(0).ip;
    }

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
                '}';
    }
}
