package com.payneteasy.firewall.dao.model;

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

    public List<TService> services;

    public String getDefaultIp() {
        return interfaces.get(0).ip;
    }
}
