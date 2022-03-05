package com.payneteasy.firewall.dao.model;

import java.util.List;

/**
 *
 */
public class TService {

    public String url;
    public String program;
    public String description;
    public String nat;
    public String dip;
    public List<String> access;
    public List<String> tags;

    public String name;
    public String justification;

    @Override
    public String toString() {
        return "TService{" +
                "url='" + url + '\'' +
                ", program='" + program + '\'' +
                ", description='" + description + '\'' +
                ", nat='" + nat + '\'' +
                ", dip='" + dip + '\'' +
                ", access=" + access +
                ", name='" + name + '\'' +
                ", justification='" + justification + '\'' +
                '}';
    }
}
