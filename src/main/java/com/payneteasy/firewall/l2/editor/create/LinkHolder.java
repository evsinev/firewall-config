package com.payneteasy.firewall.l2.editor.create;

import com.payneteasy.firewall.l2.editor.model.LinkType;

public class LinkHolder implements Comparable {
    public final String leftHost;
    public final String leftPort;
    public final String rightHost;
    public final String rightPort;
    public final LinkType linkType;

    public LinkHolder(String leftHost, String leftPort, String rightHost, String rightPort, LinkType aLinkType) {
        this.leftHost = leftHost;
        this.leftPort = leftPort;
        this.rightHost = rightHost;
        this.rightPort = rightPort;
        linkType = aLinkType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LinkHolder that = (LinkHolder) o;

        if (leftHost != null ? !leftHost.equals(that.leftHost) : that.leftHost != null) return false;
        if (leftPort != null ? !leftPort.equals(that.leftPort) : that.leftPort != null) return false;
        if (rightHost != null ? !rightHost.equals(that.rightHost) : that.rightHost != null) return false;
        return !(rightPort != null ? !rightPort.equals(that.rightPort) : that.rightPort != null);

    }

    @Override
    public int hashCode() {
        int result = leftHost != null ? leftHost.hashCode() : 0;
        result = 31 * result + (leftPort != null ? leftPort.hashCode() : 0);
        result = 31 * result + (rightHost != null ? rightHost.hashCode() : 0);
        result = 31 * result + (rightPort != null ? rightPort.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(Object o) {
       return toString().compareTo(o.toString());
    }

    @Override
    public String toString() {
        return "LinkHolder{"
                 + leftHost + '\'' +
                "/" + leftPort  +
                "  -> " + rightHost +
                "/" + rightPort  +
                '}';
    }
}
