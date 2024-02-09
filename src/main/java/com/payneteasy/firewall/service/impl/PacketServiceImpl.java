package com.payneteasy.firewall.service.impl;

import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.dao.model.*;
import com.payneteasy.firewall.service.ConfigurationException;
import com.payneteasy.firewall.service.IPacketService;
import com.payneteasy.firewall.service.model.*;
import com.payneteasy.firewall.util.Networks;
import com.payneteasy.firewall.util.Strings;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.payneteasy.firewall.util.Strings.first;
import static com.payneteasy.firewall.util.Strings.hasText;
import static java.lang.String.format;

/**
 *
 */
public class PacketServiceImpl implements IPacketService {

    public PacketServiceImpl(IConfigDao aConfigDao) {
        theConfigDao = aConfigDao;
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setIndent(4);
        dumperOptions.setPrettyFlow(true);
        yaml = new Yaml(dumperOptions);
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
                    service = getServiceInfo(serviceConfig, destinationHost.interfaces, destinationHost.getDefaultIp());
                } catch (Exception e) {
                    throw new ConfigurationException("Error creating forward packets for host " + aHostname
                            +": could't create serviceConfig service for destination host '" + destinationHost.name
                            + "' and serviceConfig " + serviceConfig.url, e);
                }

                for (Access access : service.access) {

                    THost sourceHost = access.host;

                    if (filterHosts(sourceHost, middleHost, destinationHost, service)) {
                        continue;
                    }

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
                    packet.serviceJustification = service.justification;
                    packet.serviceDescription = service.description;

                    packet.protocol = service.protocol;

//                    packet.type = "FORWARD";

                    packet.app_protocol = service.appProtocol;
                    packet.program = service.program;

                    // SNAT
                    if(isPublicAddress(destinationHost.getDefaultIp())
                            || destinationHost.getDefaultIp().equals("10.12.12.50")
                            || destinationHost.getDefaultIp().equals("10.170.1.1")
                            || destinationHost.getDefaultIp().equals("10.53.50.33")
                            || destinationHost.getDefaultIp().equals("10.2.2.56")
                            || destinationHost.getDefaultIp().equals("10.2.2.57")
                            || destinationHost.getDefaultIp().equals("10.102.2.57")
                            || destinationHost.getDefaultIp().equals("10.102.2.56")
                            || destinationHost.getDefaultIp().equals("10.102.2.56")
                            || destinationHost.getDefaultIp().equals("10.58.36.1")
                            || destinationHost.getDefaultIp().equals("172.16.229.1")
                            || destinationHost.getDefaultIp().equals("10.201.88.200")
                            || destinationHost.getDefaultIp().startsWith("172.16.4.")
                            || destinationHost.getDefaultIp().equals("172.16.3.4")
                            ) { // todo hot fix for SNAT

                        checkNotNull(service.nat, "Direction %s -> %s:%s wants to use NAT address but no NAT address was found."
                                        + "\n\n    Source host     : %s"
                                        + "\n\n    Target service  : %s"
                                        + "\n\n    Destination host: %s"
                                , sourceHost.name, service.address, service.port
                                , yaml.dump(sourceHost), yaml.dump(service), yaml.dump(destinationHost));
                        
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

    private boolean filterHosts(THost sourceHost, THost middleHost, THost destinationHost, ServiceInfo aDestinationService) {
        // skip access for the same host
        if(middleHost.name.equals(sourceHost.name)) {
            return true;
        }

        if(sourceHost.name.equals(destinationHost.name)) {
            return true;
        }

        // same vip address
        if (hasSameVipAddress(sourceHost, middleHost, destinationHost.gw)) {
            return true;
        }

        // source and destination have addresses in the same network
        if ( Networks.isInSameNetwork(sourceHost, aDestinationService)) {
            return true;
        }

        if(Networks.hasCommonGateway(sourceHost, destinationHost)) {
            return true;
        }

        return false;
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
    public Set<InputMssPacket> getInputMssPackets(String aHostname) throws ConfigurationException {
        Set<InputMssPacket> ret = new HashSet<>();

        // 1. find all host where aHostname in in the access list
        // 2. interface all interfaces
        for (THost sourceHost : theConfigDao.listHosts()) {
            for (TService sourceServices : sourceHost.services) {
                for (TInterface sourceInterface : sourceHost.interfaces) {
                    if(sourceInterface.mss != null) {
                        for (String destinationHost : sourceServices.access) {
                            if(destinationHost.equals(aHostname)) {
                                if(destinationHost.equals(aHostname)) {
                                    ret.add(new InputMssPacket(
                                            sourceInterface.ip
                                            , sourceInterface.mss
                                            , sourceHost.name
                                            , sourceServices.url
                                    ));
                                }
                            }
                        }
                    }
                }
            }
        }

        return ret;
    }

    @Override
    public List<TBlockedIpAddress> getBlockedIpAddresses(String aHostname) {
        THost targetHost = theConfigDao.getHostByName(aHostname);
        return targetHost.blockedIpAddresses != null ? targetHost.blockedIpAddresses : Collections.emptyList();
    }

    @Override
    public List<InputPacket> getInputPackets(String aHostname) throws ConfigurationException {
        List<InputPacket> ret = new ArrayList<InputPacket>();

        THost targetHost = theConfigDao.getHostByName(aHostname);
        for (TService serviceConfig : targetHost.services) {

            UrlInfo serviceUrl = UrlInfo.parse(serviceConfig.url, targetHost.getDefaultIp(), theConfigDao);
            String serviceInterface = findInterfaceByIp(serviceUrl.address, targetHost.interfaces, aHostname);

            if(serviceInterface.equals("ipmi_nuc")) {
                continue;
            }

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
                packet.input_interface  = serviceInterface;
                packet.app_protocol = hasText(serviceConfig.name) ? serviceUrl.protocol + ":" + serviceConfig.name : serviceUrl.protocol;
                packet.protocol = theConfigDao.findProtocol(serviceUrl.protocol).protocol;
                packet.source_address = findAddress(packet.destination_address, sourceHost);
                packet.source_address_name = hasText(access.serviceName) ? sourceHost.name + ":" + access.serviceName : sourceHost.name;
                packet.serviceDescription = serviceConfig.description;
                packet.serviceJustification = serviceConfig.justification;

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

        if(aAddress.startsWith("0.0.0.0")) {
            return aHost.getDefaultIp();
        }

        int right = parseAddress(aAddress);
        int max = -1 ;
        String maxAddress  = null;
        for (TInterface iface : interfaces) {
            if(!Networks.isIpAddress(iface.ip)) {
                continue;
            }
            int left ;
            try {
                left= parseAddress(iface.ip);
            } catch (Exception e) {
                throw new IllegalStateException("Could not parse ip address "+iface.ip+ " for host "+aHost.name+" and interface "+iface.name);
            }
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
            int pos = aIp.indexOf('/');
            if(pos > 0) {
                aIp = aIp.substring(0, pos);
            }

            InetAddress address = Inet4Address.getByName(aIp);
            byte[] bytes = new byte[8];
            System.arraycopy(address.getAddress(), 0, bytes, 4, 4);
//            return ByteBuffer.wrap(bytes).getLong();

            return InetAddresses.coerceToInteger(address);
        } catch (UnknownHostException e) {
            throw new ConfigurationException("Can't parse ip address "+aIp, e);
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

    public List<LinkedVrrpPacket> getLinkedVrrpPackets(String aHostname) {
        THost localHost = theConfigDao.getHostByName(aHostname);
        List<LinkedVrrpPacket> packets = new ArrayList<>();

        // linked hosts with vrrp
        for (TInterface localInterface : localHost.interfaces) {
            HostInterface linkedHostInterface = findConnectedInterface(localInterface);

            if(linkedHostInterface == null) {
                continue;
            }

            if(hasText(linkedHostInterface.iface.vip) || linkedHostInterface.iface.vips != null) {
                LinkedVrrpPacket packet = new LinkedVrrpPacket();
                packet.local_interface  = localInterface.name;
                packet.remote_address   = linkedHostInterface.iface.ip;
                packet.remote_host      = linkedHostInterface.host.name;
                packet.virtual_address  = linkedHostInterface.iface.vip; // todo add support for vips
                packets.add(packet);
            }
        }

        return packets;
    }

    private HostInterface findConnectedInterface(TInterface aLeftInterface) {
        for (THost rightHost : theConfigDao.listHosts()) {
            if(hasIpaddress(rightHost.gw, aLeftInterface)) {
                for (TInterface rightInterface : rightHost.interfaces) {
                    if(Networks.isInNetwork(aLeftInterface, rightInterface)) {
                        return new HostInterface(rightHost, rightInterface);
                    }
                }
            }
        }

        return null;
    }

    private boolean hasIpaddress(String aAddress, TInterface aInterface) {
        if(aAddress.equals(aInterface.ip)) {
            return true;
        }

        if(aInterface.vip!= null && aAddress.equals(aInterface.vip)) {
            return true;
        }

        if(aInterface.vips != null) {
            for (TVirtualIpAddress vip : aInterface.vips) {
                if(aAddress.equals(vip.ip)) {
                    return true;
                }
            }
        }

        return false;
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
                        packet.serviceDescription = serviceConfig.description;
                        packet.serviceJustification = serviceConfig.justification;
                        packet.serviceName = serviceConfig.name;

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

    @Override
    public List<TCustomRule> getCustomRules(String aHostname, ChainType aChainType) {
        THost host = theConfigDao.getHostByName(aHostname);

        if (host.customRules == null) {
            return Collections.emptyList();
        }

        return host.customRules.stream()
                .filter(it -> it.chain == aChainType)
                .collect(Collectors.toList());
    }


    private String findInterfaceByIp(String aAddress, List<TInterface> aInterfaces, String aHostname) throws ConfigurationException {
        for (TInterface iface : aInterfaces) {
            if(aAddress.equals(iface.ip) || aAddress.equals(iface.vip) ) {
                return iface.name;
            }
        }
        throw new ConfigurationException(String.format("Can't find ip %s in interfaces for host %s", aAddress, aHostname));
    }

    private boolean isPublicAddress(String aAddress) {
        return  Networks.isIpAddress(aAddress)
                && !aAddress.startsWith("10.")
                && !aAddress.startsWith("172.16")
                && !aAddress.startsWith("192.168");
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

    private ServiceInfo getServiceInfo(TService service, List<TInterface> aInterfaces, String aDefaultIpAddress) throws ConfigurationException {

        UrlInfo url = UrlInfo.parse(service.url, aDefaultIpAddress, theConfigDao);

        TProtocol protocol = theConfigDao.findProtocol(url.protocol);

        ServiceInfo info = new ServiceInfo();

        info.appProtocol = url.protocol;
        info.protocol = protocol.protocol;
        info.port = url.port;
        info.address = url.address;
        info.program = protocol.program;
        info.description = first(service.description, protocol.description);
        info.justification = first(service.description, protocol.justification);
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
    private final Yaml       yaml;
}
