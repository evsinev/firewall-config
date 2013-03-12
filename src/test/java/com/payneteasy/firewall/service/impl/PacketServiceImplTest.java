package com.payneteasy.firewall.service.impl;

import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TInterface;
import com.payneteasy.firewall.service.ConfigurationException;
import junit.framework.Assert;
import org.junit.Test;

import java.util.ArrayList;

import static com.payneteasy.firewall.service.impl.PacketServiceImpl.findAddress;
import static junit.framework.Assert.assertEquals;

/**
 */
public class PacketServiceImplTest {

    @Test
    public void testFindAddress() throws ConfigurationException {
        THost host = new THost();
        host.interfaces = new ArrayList<TInterface>();
        add(host, "eth0", "10.0.1.1");
        add(host, "eth0", "10.0.2.1");
        add(host, "eth0", "10.0.3.1");
        add(host, "eth0", "10.0.4.1");
        add(host, "eth0", "10.0.5.1");

        assertEquals("10.0.4.1", findAddress("10.0.4.110", host));
        assertEquals("10.0.2.1", findAddress("10.0.2.255", host));
        assertEquals("10.0.1.1", findAddress("10.0.102.255", host));
    }

    private void add(THost aHost, String aEth, String aIp) {
        TInterface iface = new TInterface();
        iface.ip = aIp;
        iface.name = aEth;
        aHost.interfaces.add(iface);
    }
}
