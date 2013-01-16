package com.payneteasy.firewall.service.model;

import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TProtocol;

import java.util.List;

/**
 */
public class ServiceInfo {

    public String appProtocol;
    public String protocol;
    public int port;

    public String description;
    public String justification;

    public String address;

    public String program;

    public List<THost> access;
}
