package com.payneteasy.firewall.service.impl;

import com.google.common.net.InetAddresses;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.dao.model.*;
import com.payneteasy.firewall.service.ConfigurationException;
import com.payneteasy.firewall.service.IPacketService;
import com.payneteasy.firewall.service.model.*;
import com.payneteasy.firewall.util.Strings;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import static com.payneteasy.firewall.util.Strings.hasText;
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
                    throw new ConfigurationException("Error creating forward packets for host " + aHostname
                            +": could't create serviceConfig service for destination host '" + destinationHost.name
                            + "' and serviceConfig " + serviceConfig.url, e);
                }

                for (Access access : service.access) {

                    THost sourceHost = access.host;

                    // skip access for the same host
                    if(middleHost.name.equals(sourceHost.name)) continue;
                    if(sourceHost.name.equals(destinationHost.name)) continue;
                    if (hasSameVipAddress(sourceHost, middleHost, destinationHost.gw)) continue;

                    Packet packet = new Packet();
                    packet.source_address = sourceHost.getDefaultIp();
                    packet.source_address_name = sourceHost.name;
                    try {
                        packet.input_interface = findInterface(middleHost, sourceHost);
                    } catch (Exception e) {
                        throw new ConfigurationException("Error creating forward packets for host " + aHostname
                                + ": couldn't find input interface for service '"
                                + service.appProtocol + "' at destination host '" + destinationHost.name
                                + "' which accesses from '" + access + "'"
                                + "\n Packet flow is: " + access + " --> " + middleHost.name + " --> " + destinationHost.name
                                , e);
                    }

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
                        }
                        if(service.nat == null) {
                            throw new ConfigurationException("No nat for service "+service.appProtocol + " at host "+destinationHost.name);
                        }
                        packet.type = "DNAT";
                        packet.destination_nat_address = service.nat.address;
                        packet.destination_nat_port    = service.nat.port;

                    }


                    // checks
                    // not same interface
                    if(packet.input_interface.equals(packet.output_interface)) continue;

                    packet.source_service = access.serviceName;
                    packet.destination_service = serviceConfig.name;

                    ret.add(packet);
                }

            }
        }

        Collections.sort(ret, new Comparator<Packet>() {
            @Override
            public int compare(Packet aLeft, Packet aRight) {
                int ret = aLeft.destination_address_name.compareTo(aRight.destination_address_name);
                if(ret==0) {
                    ret = aLeft.source_address_name.compareTo(aRight.source_address_name);
                }
                return ret;
            }
        });
        return ret;
    }

    private boolean hasSameVipAddress(THost aLeft, THost aRight, String aGateway) {
        boolean leftFound = false;
        for (TInterface iface : aLeft.interfaces) {
            if (aGateway.equals(iface.vip)) {
                leftFound = true;
                break;
            }
        }

        if(!leftFound) {
            return false;
        }

        for (TInterface iface : aRight.interfaces) {
            if (aGateway.equals(iface.vip)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<InputPacket> getInputPackets(String aHostname) throws ConfigurationException {
        List<InputPacket> ret = new ArrayList<InputPacket>();

        THost targetHost = theConfigDao.getHostByName(aHostname);
        for (TService serviceConfig : targetHost.services) {

            UrlInfo serviceUrl = UrlInfo.parse(serviceConfig.url, targetHost.getDefaultIp(), theConfigDao);

            List<Access> accessList;
            try {
                accessList = createAccessList("host " + aHostname, serviceConfig.access);
            } catch (Exception e) {
                throw new ConfigurationException("Could not create INPUT access list for host "+aHostname, e);
            }

            for (Access access : accessList) {
                InputPacket packet = new InputPacket();

                THost sourceHost = access.host;

                packet.destination_port = serviceUrl.port;
                if("skip".equals(serviceUrl.address)) {
                    throw new ConfigurationException("Service " + serviceUrl.protocol + " has skip address at host " + aHostname
                            + ". Check that default interface has no address 'skip' or "
                            + " or specify ip address for the service."
                    );
                }
                packet.destination_address = serviceUrl.address;
                packet.input_interface  = findInterfaceByIp(serviceUrl.address, targetHost.interfaces, aHostname);
                packet.app_protocol = hasText(serviceConfig.name) ? serviceUrl.protocol + ":" + serviceConfig.name : serviceUrl.protocol;
                packet.protocol = theConfigDao.findProtocol(serviceUrl.protocol).protocol;
                packet.source_address = findAddress(packet.destination_address, sourceHost);
                packet.source_address_name = hasText(access.serviceName) ? sourceHost.name + ":" + access.serviceName : sourceHost.name;

                ret.add(packet);
            }
        }

        Collections.sort(ret, new Comparator<InputPacket>() {
            @Override
            public int compare(InputPacket aLeft, InputPacket aRight) {
                int ret = aLeft.source_address_name.compareTo(aRight.source_address_name);
                if(ret==0) {
                    ret = aLeft.app_protocol.compareTo(aRight.app_protocol);
                }
                return ret;
            }
        });
        return ret;
    }

    protected static String findAddress(String aAddress, THost aHost) throws ConfigurationException {
        List<TInterface> interfaces = aHost.interfaces;
        if(interfaces==null || interfaces.isEmpty()) throw new IllegalStateException("No interfaces for host "+aHost.name);
        if(interfaces.size()==1) return aHost.getDefaultIp();

        int right = parseAddress(aAddress);
        int max = -1 ;
        String maxAddress  = null;
        for (TInterface iface : interfaces) {
            if(iface.skipIpAddress()) {
                continue;
            }
            int left = parseAddress(iface.ip);
            int count = calcEqualsBits(right, left);
//            System.out.println(count+" " +Integer.toBinaryString(right) + " " + Integer.toBinaryString(left)+" "+left+" "+iface.ip);
            if(count>max) {
                max = count;
                maxAddress = iface.ip;
            }
        }
        if(maxAddress==null) {
            throw new ConfigurationException("Can't find address for "+aAddress+" in "+aHost.name);
        }
        return maxAddress;
    }

    private static int calcEqualsBits(int aLeft, int aRight) {
        String left = Integer.toBinaryString(aLeft);
        String right = Integer.toBinaryString(aRight);
        for(int i=0; i<left.length() && i<right.length(); i++) {
            if(left.charAt(i)!=right.charAt(i)) {
                return i-1;
            }
        }
        return left.length()-1;
    }

    private static int parseAddress(String aIp) throws ConfigurationException {
        try {
            InetAddress address = Inet4Address.getByName(aIp);
            byte[] bytes = new byte[8];
            System.arraycopy(address.getAddress(), 0, bytes, 4, 4);
//            return ByteBuffer.wrap(bytes).getLong();

            return InetAddresses.coerceToInteger(address);
        } catch (UnknownHostException e) {
            throw new ConfigurationException("Can't parse ip address "+aIp);
        }
    }

    @Override
    public List<VrrpPacket> getVrrpPackets(String aHostname) {
        THost localHost = theConfigDao.getHostByName(aHostname);
        List<VrrpPacket> packets = new ArrayList<>();

        // fills local address and interface
        for (TInterface iface : localHost.interfaces) {
            if(hasText(iface.vip)) {
                packets.add(VrrpPacket.createWithLocal(iface.ip, iface.name, iface.vip));
            }
            if(iface.vips != null) {
                for (TVirtualIpAddress vip : iface.vips) {
                    packets.add(VrrpPacket.createWithLocal(iface.ip, iface.name, vip.ip));
                }
            }
        }

        // finds paired remote address
        for (VrrpPacket packet : packets) {
            findPairedRemoteVirtualAddress(packet.virtual_address, localHost.name, packet);
        }
        return packets;
    }

    private void findPairedRemoteVirtualAddress(String aAddress, String aIgnoreHost, VrrpPacket aPacket) {
        TInterface foundInterface = null;
        THost foundHost = null;

        for (THost host : theConfigDao.listHosts()) {
            if(host.name.equals(aIgnoreHost)) {
                continue;
            }

            for (TInterface iface : host.interfaces) {
                if(aAddress.equals(iface.ip)) {
                    throw new IllegalStateException(format("Virtual ip address '%s' has pair with bare interface %s.%s", aAddress, host.name, iface.name));
                }

                if(aAddress.equals(iface.vip)) {
                    if(foundInterface != null) {
                        throw new IllegalStateException(format("There are two additional virtual addresses %s.%s and %s.%s"
                                , foundHost.name, foundInterface.name, host.name, iface.name));
                    }
                    foundInterface = iface;
                    foundHost = host;
                }

                if(iface.vips!=null) {
                    for (TVirtualIpAddress vip : iface.vips) {
                        if(aAddress.equals(vip.ip)) {
                            if(foundInterface != null) {
                                throw new IllegalStateException(format("There are two additional virtual addresses %s.%s and %s.%s"
                                        , foundHost.name, foundInterface.name, host.name, iface.name));
                            }
                            foundInterface = iface;
                            foundHost = host;

                        }
                    }
                }
            }

        }

        if(foundInterface == null) {
             throw new IllegalStateException("There no any additional virtual interface with ip address "+aAddress);
        }

        aPacket.remote_host      = foundHost.name;
        aPacket.remote_interface = foundInterface.name;
        aPacket.remote_address   = foundInterface.ip;
    }

    @Override
    public List<OutputPacket> getOutputPackets(String aHostname) throws ConfigurationException {
        List<OutputPacket> ret = new ArrayList<OutputPacket>();

//        THost sourceHost = theConfigDao.getHostByName(aHostname);

        for (THost destinationHost : theConfigDao.listHosts()) {
            for (TService serviceConfig : destinationHost.services) {
                List<Access> accesses = createAccessList("host " + destinationHost.name, serviceConfig.access);
                for (Access access : accesses) {

                    THost sourceHost = access.host;

                    if(aHostname.equals(sourceHost.name)) {
                        OutputPacket packet = new OutputPacket();

                        UrlInfo serviceUrl = UrlInfo.parse(serviceConfig.url, destinationHost.getDefaultIp(), theConfigDao);

                        packet.app_protocol = serviceUrl.protocol;
                        packet.destination_address = serviceUrl.address;
                        packet.destination_address_name = destinationHost.name;
                        packet.protocol = theConfigDao.findProtocol(serviceUrl.protocol).protocol;
                        packet.destination_port = serviceUrl.port;

                        packet.source_service = access.serviceName;
                        packet.destination_service = serviceConfig.name;

                        ret.add(packet);
                    }
                }
            }
        }

        Collections.sort(ret, new Comparator<OutputPacket>() {
            @Override
            public int compare(OutputPacket aLeft, OutputPacket aRight) {
                int ret = aLeft.destination_address_name.compareTo(aRight.destination_address_name);
                if(ret==0) {
                    ret = aLeft.app_protocol.compareTo(aRight.app_protocol);
                }
                return ret;
            }
        });
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
        return !aAddress.startsWith("10.2") && !aAddress.startsWith("10.0") && !aAddress.startsWith("172.16") && !aAddress.startsWith("192.168");
    }

    private String findInterface(THost aMiddleHost, THost aConnectedHost) throws ConfigurationException {
        for (TInterface iface : aMiddleHost.interfaces) {
            if(iface.ip == null) {
                throw new ConfigurationException("Host '" + aMiddleHost.name + "' hasn't got ip address for interface '" + iface.name + "'");
            }

            if(iface.skipIpAddress()) {
                continue;
            }

            if(iface.ip.equals(aConnectedHost.gw)) {
                return iface.name;
            }

            if(aConnectedHost.gw.equals(iface.vip)) {
                return iface.name;
            }

            String interfaceName;
            if( hasText(interfaceName = findInVirtualAddresses(aConnectedHost.gw, aMiddleHost.interfaces)) ) {
                return interfaceName;
            }
        }
        throw new ConfigurationException(
                "Can't find interface at host " + aMiddleHost.name + " which connected to " + aConnectedHost.name +"."
                        + "\nCheck gateway (" + aConnectedHost.gw + ") at host " + aConnectedHost.name + " or ip addresses at host " + aMiddleHost.name
        );
    }

    private String findInVirtualAddresses(String aAddress, List<TInterface> interfaces) {
        for (TInterface iface : interfaces) {
            if(aAddress.equals(iface.vip)) {
                return iface.name;
            }

            if(iface.vips == null) {
                continue;
            }

            for (TVirtualIpAddress vip : iface.vips) {
                if(aAddress.equals(vip.ip)) {
                    return iface.name;
                }
            }
        }
        return null;
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
        info.access = createAccessList("service "+service.name, service.access);
        if(service.nat!=null) {
            info.nat = UrlInfo.parse(service.nat, url.address, theConfigDao);
        }

        return info;
    }

    private List<Access> createAccessList(String aSource, List<String> aAccess) {
        if (aAccess == null) throw new IllegalStateException("Access list parameter is null for source " + aSource);

        Set<Access> list = new TreeSet<>();
        for (String hostname : aAccess) {
            // group
            if (hostname.startsWith("group-")) {
                String groupName = hostname.replaceAll("group-", "");
                list.addAll(convertToAccesses(theConfigDao.findHostsByGroup(groupName)));

            // ordinary host
            } else if(theConfigDao.isHostExist(hostname)) {
                THost host = theConfigDao.getHostByName(hostname);
                list.add(new Access(host));

            // service
            } else if( isServiceRegistered(hostname) ) {
                list.addAll(findHostsWithService(hostname));

            // has pattern
            } else if(hostname.endsWith("-*")) {
                list.addAll(convertToAccesses(theConfigDao.getHostByPattern(hostname)));

            } else {
                throw new IllegalStateException("Host or service '" + hostname + "' not found in access list for "+aSource);
            }
        }
        return new ArrayList<>(list);
    }

    private boolean isServiceRegistered(String aServiceName) {
        aServiceName = aServiceName.replace(".service", "");
        for (THost host : theConfigDao.listHosts()) {
            for (TService service : host.services) {
                if(aServiceName.equals(service.name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<Access> findHostsWithService(String aServiceName) {
        aServiceName = aServiceName.replace(".service", "");
        List<Access> accessList = new ArrayList<>();
        for (THost host : theConfigDao.listHosts()) {
            for (TService service : host.services) {
                if(aServiceName.equals(service.name)) {
                    accessList.add(new Access(host, aServiceName));
                }
            }
        }

        if(accessList.isEmpty()) {
            throw new IllegalStateException("No hosts with service " + aServiceName);
        }
        return accessList;
    }

    private Collection<Access> convertToAccesses(Collection<? extends THost> aHosts) {
        List<Access> accesses = new ArrayList<>();
        for (THost host : aHosts) {
            accesses.add(new Access(host));
        }
        return accesses;
    }

    private final IConfigDao theConfigDao;
}
