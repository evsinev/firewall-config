package com.payneteasy.firewall.service.impl;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TInterface;
import com.payneteasy.firewall.dao.model.TProtocol;
import com.payneteasy.firewall.dao.model.TVirtualIpAddress;
import com.payneteasy.firewall.service.ConfigurationException;
import com.payneteasy.firewall.service.IPacketService;
import com.payneteasy.firewall.service.IWikiService;
import com.payneteasy.firewall.service.model.InputPacket;
import com.payneteasy.firewall.service.model.OutputPacket;
import com.payneteasy.firewall.service.model.Packet;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Strings.nullToEmpty;

public class WikiServiceImpl implements IWikiService {

    private static class InputPacketKey {

        static InputPacketKey fromInputPacket(InputPacket input) {
            return new InputPacketKey(input.app_protocol, input.input_interface, input.destination_address, input.destination_port, input.serviceJustification, input.serviceDescription);
        }

        private final String appProtocol;
        private final String inputInterface;
        private final String destinationAddress;
        private final int destinationPort;

        public final String serviceJustification;
        public final String serviceDescription;

        private InputPacketKey(String appProtocol, String inputInterface, String destinationAddress, int destinationPort
                               , String aServiceJustification
                               , String aServiceDescription
        ) {
            this.appProtocol = appProtocol;
            this.inputInterface = inputInterface;
            this.destinationAddress = destinationAddress;
            this.destinationPort = destinationPort;
            serviceDescription = aServiceDescription;
            serviceJustification = aServiceJustification;
        }

        @Override public int hashCode() {
            return Objects.hashCode(appProtocol, inputInterface, destinationAddress, destinationPort);
        }

        @Override public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof InputPacketKey)) {
                return false;
            }
            InputPacketKey other = (InputPacketKey) obj;
            return Objects.equal(appProtocol, other.appProtocol)
                    && Objects.equal(inputInterface, other.inputInterface)
                    && Objects.equal(destinationAddress, other.destinationAddress)
                    && destinationPort == other.destinationPort;
        }
    }
    
    private static <T> String collectionToString(Collection<T> list) {
        StringBuilder sb = new StringBuilder();
        for (T t : list) {
            sb.append(t).append(' ');
        }
        return sb.toString();
    }
    
    private static String createDetailsLink(String host) {
        return "[[" + normalizeHostName(host) + " details|" + host + "]]";
    }
    
    private static String normalizeHostName(String host) {
        String name =  host.replaceAll("\\.", "_");
        int pos = name.indexOf(":");
        return pos > 0 ? name.substring(0, pos) : name;
    }
    
    private final IConfigDao configDao;    
    private final IPacketService packetService; 

    public WikiServiceImpl(IConfigDao configDao, IPacketService packetService) {
        this.configDao = configDao;
        this.packetService = packetService;
    }

    @Override public String createDetailsPage(String hostName) throws ConfigurationException {
        THost host = configDao.getHostByName(hostName);        
        StringBuilder sb = new StringBuilder();

        sb.append("h1. ").append(host.name).append("\n\n");
        sb.append(host.description);
        sb.append('\n');

        sb.append("\nh2. Business goal\n\n");
        sb.append(host.justification);
        sb.append('\n');
                
        sb.append("{{include(").append(normalizeHostName(hostName)).append("_packets)}}");
        sb.append('\n');
        
        if ("internal".equals(host.group) || "ipmi".equals(host.group)) {
            sb.append("{{include(").append(normalizeHostName(hostName)).append("_run)}}");
        }        
        return sb.toString();
    }

    @Override public String createPacketsPage(String hostName) throws ConfigurationException {
        StringBuilder sb = new StringBuilder();
        appendInterfaces(sb, hostName);
        appendInputTable(sb, hostName);
        appendOutputTable(sb, hostName);
        appendForwardTable(sb, hostName);
        return sb.toString();
    }

    @Override public String createServicesPage() {
        List<TProtocol> protocols = configDao.listProtocols().protocols;
        Collections.sort(protocols, new Comparator<TProtocol>() {
            @Override public int compare(TProtocol o1, TProtocol o2) {
                return o1.name.compareTo(o2.name);
            }
        });
        
        StringBuilder sb = new StringBuilder();
        sb.append("\nh2. Services\n\n");
        sb.append("|_.Name|_.Program|_.Protocol|_.Port|_.Description|_.Justification|\n");        
        for (TProtocol protocol : configDao.listProtocols().protocols) {
            sb.append('|').append(protocol.name);
            sb.append('|').append(protocol.program);
            sb.append('|').append(protocol.protocol);
            sb.append('|').append(protocol.port != -1 ? protocol.port : "");
            sb.append('|').append(protocol.description);
            sb.append('|').append(protocol.justification).append("|\n");
        }
        return sb.toString();
    }

    private void appendForwardTable(StringBuilder sb, String hostName) throws ConfigurationException {
        List<Packet> forwards = packetService.getForwardPackets(hostName);
        if (forwards.size() == 0) {
            return;
        }

        sb.append("\nh2. Forward packets\n\n");
        sb.append("|_.Source|_.Source address|_.Destination|_.Destination address|_.Interfaces (input/output)|_.Protocol|_.Port|_.SNAT|_.DNAT|_.Description|_.Justification|\n");
        for (Packet packet : forwards) {
            TProtocol protocol = configDao.findProtocol(packet.app_protocol);
            sb.append('|').append(createDetailsLink(packet.source_address_name));
            sb.append('|').append(packet.source_address);
            sb.append('|').append(createDetailsLink(packet.destination_address_name));
            sb.append('|').append(packet.destination_address);
            sb.append('|').append(packet.input_interface).append('/').append(packet.output_interface);
            sb.append('|').append(packet.app_protocol);
            sb.append('|').append(packet.protocol).append(':').append(packet.destination_port);
            sb.append('|').append(nullToEmpty(packet.source_nat_address));
            sb.append('|').append(packet.destination_nat_address != null ? packet.destination_address + ":" + packet.destination_nat_port : "");
            sb.append('|').append(packet.serviceDescription != null ? packet.serviceDescription : protocol.description);
            sb.append('|').append(packet.serviceJustification !=null ? packet.serviceJustification : protocol.justification).append("|\n");
        }        
    }

    private void appendInterfaces(StringBuilder sb, String hostName) {
        THost host = configDao.getHostByName(hostName);
        sb.append("\nh2. Interfaces\n\n");
        sb.append("|_.Name|_.IP|_.Virtual IPs|_.DNS|\n");
        for (TInterface interfaze : host.interfaces) {
            sb.append('|').append(interfaze.name);
            sb.append('|').append(interfaze.ip);
            sb.append('|').append(nullToEmpty(interfaze.vip));
            sb.append('|').append(nullToEmpty(interfaze.dns)).append("|\n");
            for (TVirtualIpAddress vip : notNull(interfaze.vips)) {
                sb.append('|').append(interfaze.name);
                sb.append('|').append("");
                sb.append('|').append(vip.ip);
                sb.append('|').append(vip.names).append("|\n");
            }
        }        
    }

    private List<TVirtualIpAddress> notNull(List<TVirtualIpAddress> aVips) {
        return aVips != null ? aVips : Collections.EMPTY_LIST;
    }

    private void appendInputTable(StringBuilder sb, String hostName) throws ConfigurationException {
        List<InputPacket> inputPackets = packetService.getInputPackets(hostName);
        if (inputPackets.size() == 0) {
            return;
        }

        Multimap<InputPacketKey, String> inputs = ArrayListMultimap.create();
        for (InputPacket inputPacket : inputPackets) {
            inputs.put(InputPacketKey.fromInputPacket(inputPacket), createDetailsLink(inputPacket.source_address_name));
        }

        sb.append("\nh2. Services and input packets\n\n");
        sb.append("|_.Service name|_.Bound interface|_.Interface address|_.Port|_.Accessed from|_.Description|_.Justification|\n");
        for (InputPacketKey key : inputs.keySet()) {
            sb.append('|').append(key.appProtocol);
            sb.append('|').append(key.inputInterface);
            sb.append('|').append(key.destinationAddress);
            sb.append('|').append(key.destinationPort != -1 ? key.destinationPort : "");
            sb.append('|').append(collectionToString(inputs.get(key)));

            TProtocol protocol = configDao.findProtocol(clearProtocol(key.appProtocol));
            sb.append('|').append(key.serviceDescription != null ? key.serviceDescription : protocol.description);
            sb.append('|').append(key.serviceJustification != null ? key.serviceJustification : protocol.justification).append("|\n");
        }        
    }

    private String clearProtocol(String aProtocolName) {
        int pos = aProtocolName.indexOf(':');
        return pos > 0 ? aProtocolName.substring(0, pos) : aProtocolName;
    }

    private void appendOutputTable(StringBuilder sb, String hostName) throws ConfigurationException {
        List<OutputPacket> outputs = packetService.getOutputPackets(hostName);
        if (outputs.size() == 0) {
            return;
        }

        sb.append("\nh2. Output packets\n\n");
        sb.append("|_.Remote hostname|_.Remote ip address|_.Protocol|_.Port|_.Service name|_.Description|_.Justification|\n");
        for (OutputPacket outputPacket : outputs) {
            TProtocol protocol = configDao.findProtocol(outputPacket.app_protocol);
            sb.append('|').append(createDetailsLink(outputPacket.destination_address_name));
            sb.append('|').append(outputPacket.destination_address);
            sb.append('|').append(outputPacket.protocol).append(" ").append((outputPacket.serviceName != null ? outputPacket.serviceName : ""));
            sb.append('|').append(outputPacket.destination_port != -1 ? outputPacket.destination_port : "");
            sb.append('|').append(protocol.name);
            sb.append('|').append(outputPacket.serviceDescription != null ? outputPacket.serviceDescription : protocol.description);
            sb.append('|').append(outputPacket.serviceJustification != null ? outputPacket.serviceJustification : protocol.justification).append("|\n");
        }        
    }

    public String createServersPage(final String group) {
        Iterator<THost> externalHosts = Iterators.filter(configDao.listHosts().iterator(), new Predicate<THost>() {
            @Override public boolean apply(THost input) {
                return group.equals(input.group);
            }
        });
        StringBuilder sb = new StringBuilder();
        sb.append("h2. ").append(group).append(" group\n\n");
        sb.append("|_.Host|_.IP address|_.Description|\n");
        while (externalHosts.hasNext()) {
            THost host = externalHosts.next();
            sb.append("|").append(createDetailsLink(host.name));
            sb.append("|").append(host.getDefaultIp());
            sb.append("|").append(host.description).append("|\n");
        }
        return sb.toString();
    }
}
