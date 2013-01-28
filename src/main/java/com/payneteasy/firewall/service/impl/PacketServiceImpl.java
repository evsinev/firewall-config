package com.payneteasy.firewall.service.impl;

import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TInterface;
import com.payneteasy.firewall.dao.model.TProtocol;
import com.payneteasy.firewall.dao.model.TService;
import com.payneteasy.firewall.service.ConfigurationException;
import com.payneteasy.firewall.service.IPacketService;
import com.payneteasy.firewall.service.model.*;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

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
            for (TService serviceConfig : destinationHost.services) {

                ServiceInfo service;
                try {
                    service = getServiceInfo(serviceConfig, destinationHost.interfaces);
                } catch (Exception e) {
                    throw new ConfigurationException("Can't create serviceConfig service for " + destinationHost.name + " and serviceConfig " + serviceConfig.url, e);
                }

                for (THost sourceHost : service.access) {

                    if(middleHost.name.equals(sourceHost.name)) continue;
                    if(sourceHost.name.equals(destinationHost.name)) continue;

                    Packet packet = new Packet();
                    packet.source_address = sourceHost.getDefaultIp();
                    packet.source_address_name = sourceHost.name;
                    packet.input_interface = findInterface(middleHost, sourceHost);

                    packet.destination_address = service.address;
                    packet.destination_address_name = destinationHost.name;
                    packet.destination_port = service.port;
                    packet.output_interface = findInterface(middleHost, destinationHost);

                    packet.protocol = service.protocol;

//                    packet.type = "FORWARD";

                    packet.app_protocol = service.appProtocol;
                    packet.program = service.program;

                    // SNAT
                    if(isPublicAddress(destinationHost.getDefaultIp())) {
                        packet.source_nat_address = service.nat.address;
                        packet.type = "SNAT";
                    }

                    // DNAT
                    if(isPublicAddress(sourceHost.getDefaultIp())) {
                        if(packet.type!=null) {
                            throw new ConfigurationException(format("Trying to config both SNAT and DNAT with %s(%s) -> %s(%s)", sourceHost.name, sourceHost.getDefaultIp(), destinationHost.name, destinationHost.getDefaultIp()));
                        } else {
                            packet.type = "DNAT";
                            packet.destination_nat_address = service.nat.address;
                            packet.destination_nat_port    = service.nat.port;
                        }

                    }


                    // checks
                    // not same interface
                    if(packet.input_interface.equals(packet.output_interface)) continue;

                    ret.add(packet);
                }

            }
        }

        return ret;
    }

    @Override
    public List<InputPacket> getInputPackets(String aHostname) throws ConfigurationException {
        List<InputPacket> ret = new ArrayList<InputPacket>();

        THost targetHost = theConfigDao.getHostByName(aHostname);
        for (TService serviceConfig : targetHost.services) {

            UrlInfo serviceUrl = UrlInfo.parse(serviceConfig.url, targetHost.getDefaultIp(), theConfigDao);

            for (THost sourceHost : createAccessList(serviceConfig.access)) {
                InputPacket packet = new InputPacket();

                packet.destination_port = serviceUrl.port;
                packet.input_interface  = findInterfaceByIp(serviceUrl.address, targetHost.interfaces, aHostname);
                packet.app_protocol = serviceUrl.protocol;
                packet.protocol = theConfigDao.findProtocol(serviceUrl.protocol).protocol;
                packet.source_address = sourceHost.getDefaultIp();
                packet.source_address_name = sourceHost.name;

                ret.add(packet);
            }
        }

        return ret;
    }

    @Override
    public List<OutputPacket> getOutputPackets(String aHostname) throws ConfigurationException {
        List<OutputPacket> ret = new ArrayList<OutputPacket>();

//        THost sourceHost = theConfigDao.getHostByName(aHostname);

        for (THost destinationHost : theConfigDao.listHosts()) {
            for (TService serviceConfig : destinationHost.services) {
                List<THost> access = createAccessList(serviceConfig.access);
                for (THost sourceHost : access) {
                    if(aHostname.equals(sourceHost.name)) {
                        OutputPacket packet = new OutputPacket();

                        UrlInfo serviceUrl = UrlInfo.parse(serviceConfig.url, destinationHost.getDefaultIp(), theConfigDao);

                        packet.app_protocol = serviceUrl.protocol;
                        packet.destination_address = serviceUrl.address;
                        packet.destination_address_name = destinationHost.name;
                        packet.protocol = theConfigDao.findProtocol(serviceUrl.protocol).protocol;
                        packet.destination_port = serviceUrl.port;

                        ret.add(packet);
                    }
                }
            }
        }

        return ret;
    }

    private String findInterfaceByIp(String aAddress, List<TInterface> aInterfaces, String aHostname) throws ConfigurationException {
        for (TInterface iface : aInterfaces) {
            if(aAddress.equals(iface.ip)) {
                return iface.name;
            }
        }
        throw new ConfigurationException(String.format("Can't find ip %s in interfaces for host %s", aAddress, aHostname));
    }

    private boolean isPublicAddress(String aAddress) {
        // todo quick fix for other network other then 10.0
        return !aAddress.startsWith("10.0") && !aAddress.startsWith("172.16") && !aAddress.startsWith("192.168");
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

        UrlInfo url = UrlInfo.parse(service.url, aInterfaces.get(0).ip, theConfigDao);

        TProtocol protocol = theConfigDao.findProtocol(url.protocol);

        ServiceInfo info = new ServiceInfo();

        info.appProtocol = url.protocol;
        info.protocol = protocol.protocol;
        info.port = url.port;
        info.address = url.address;
        info.program = protocol.program;
        info.description = protocol.description;
        info.justification = protocol.justification;
        info.access = createAccessList(service.access);
        if(service.nat!=null) {
            info.nat = UrlInfo.parse(service.nat, url.address, theConfigDao);
        }

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
