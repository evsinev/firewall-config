package com.payneteasy.firewall.service.model;

import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.util.Strings;

public class Access implements Comparable<Access> {

    public Access() {
    }

    public Access(THost host) {
        this.host = host;
    }

    public Access(THost host, String serviceName) {
        this.host = host;
        this.serviceName = serviceName;
    }

    @Override
    public int compareTo(Access other) {
        if(host == null || other == null || other.host == null) {
            return 0;
        }
        return host.name.compareTo(other.host.name);
    }

    public THost host;
    public String serviceName;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(host.name);
        if(Strings.hasText(serviceName)) {
            sb.append("[");
            sb.append(serviceName);
            sb.append("]");
        }
        return sb.toString();
    }
}
