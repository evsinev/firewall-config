package com.payneteasy.firewall.service.impl;

import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TInterface;
import com.payneteasy.firewall.dao.model.TProtocol;
import com.payneteasy.firewall.dao.model.TService;
import com.payneteasy.firewall.service.ConfigurationException;
import com.payneteasy.firewall.service.IPacketService;
import com.payneteasy.firewall.service.model.Packet;
import com.payneteasy.firewall.service.model.ServiceInfo;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 *
 */
public class PacketServiceImpl implements IPacketService {

    public PacketServiceImpl(IConfigDao aConfigDao) {
        theConfigDao = aConfigDao;
    }

    @Override
    public List<Packet> getForwardPackets(String aHostname) throws ConfigurationException {
        List<Packet> ret = new ArrayList<Packet>();
        THost middleHost = theConfigDao.getHostByName(aHostname);

        // find all connected hosts by gw
        List<THost> destinations = theConfigDao.findHostByGw(middleHost.interfaces);
        for (THost destinationHost : destinations) {
            // list services
            for (TService service : destinationHost.services) {

                ServiceInfo info;
                try {
                    info = getServiceInfo(service, destinationHost.interfaces);
                } catch (Exception e) {
                    throw new ConfigurationException("Can't create service info for " + destinationHost.name + " and service " + service.url, e);
                }

                for (THost sourceHost : info.access) {

                    Packet packet = new Packet();
                    packet.source_address = sourceHost.getDefaultIp();
                    packet.input_interface = findInterface(middleHost, sourceHost);

                    packet.destination_address = info.address;
                    packet.destination_port = info.port;
                    packet.output_interface = findInterface(middleHost, destinationHost);

                    packet.protocol = info.protocol;

                    packet.type = "FORWARD";

                    packet.appProtocol = info.appProtocol;
                    packet.program = info.program;

                    // SNAT
                    //if(isPublicAddress(destinationHost.getDefaultIp())) {

                        //packet.source_nat_address = ;

                    //}

                    // DNAT
                    //packet.destination_nat_address;
                    //packet.destination_nat_port;

                    ret.add(packet);
                }

            }
        }

        return ret;
    }

    private String findInterface(THost aMiddleHost, THost aConnectedHost) throws ConfigurationException {
        for (TInterface iface : aMiddleHost.interfaces) {
            if(iface.ip.equals(aConnectedHost.gw)) {
                return iface.name;
            }
        }
        throw new ConfigurationException("Can't find interface on host "+aMiddleHost.name+" that connected to "+aConnectedHost.name);
    }

    private ServiceInfo getServiceInfo(TService service, List<TInterface> aInterfaces) throws ConfigurationException {

        String name;
        Integer port;
        String address;

        if (service.url.contains(" ")) {
            StringTokenizer st = new StringTokenizer(service.url, ":;.- \t");
            name = st.nextToken();
            port = Integer.parseInt(st.nextToken());
            address = aInterfaces.get(0).ip;

        } else if (service.url.contains("/")) {
            try {
                URL url = new URL(service.url);
                name = url.getProtocol();
                address = url.getHost();
                port = url.getPort();
            } catch (MalformedURLException e) {
                throw new ConfigurationException("Can't parse url " + service.url, e);
            }
        } else {
            name = service.url;
            address = aInterfaces.get(0).ip;
            port = null;
        }

        TProtocol protocol = theConfigDao.findProtocol(name);

        ServiceInfo info = new ServiceInfo();

        info.appProtocol = name;
        info.protocol = protocol.protocol;
        info.port = port != null ? port : protocol.port;
        info.address = address;
        info.program = protocol.program;
        info.description = protocol.description;
        info.justification = protocol.justification;
        info.access = createAccessList(service.access);

        return info;
    }

    private List<THost> createAccessList(List<String> aAccess) {
        if (aAccess == null) throw new IllegalStateException("Access list parameter is empty");

        List<THost> list = new ArrayList<THost>();
        for (String hostname : aAccess) {
            if (hostname.startsWith("group-")) {
                String groupName = hostname.replaceAll("group-", "");
                list.addAll(theConfigDao.findHostsByGroup(groupName));
            } else {
                THost host = theConfigDao.getHostByName(hostname);
                list.add(host);
            }
        }
        return list;
    }

    private final IConfigDao theConfigDao;
}
