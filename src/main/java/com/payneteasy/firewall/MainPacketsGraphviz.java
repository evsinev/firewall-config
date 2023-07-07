package com.payneteasy.firewall;

import com.payneteasy.firewall.dao.ConfigDaoYaml;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.service.ConfigurationException;
import com.payneteasy.firewall.service.IPacketService;
import com.payneteasy.firewall.service.impl.PacketServiceImpl;
import com.payneteasy.firewall.service.model.Packet;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class MainPacketsGraphviz {

    public static void main(String[] args) throws IOException, ConfigurationException {
        File configDir = new File(args[0]);
        if (!configDir.exists())
            throw new IllegalStateException("Config dir " + configDir.getAbsolutePath() + " is not exists");

        IConfigDao configDao = new ConfigDaoYaml(configDir);

        IPacketService packetService = new PacketServiceImpl(configDao);

        Collection<? extends THost> hosts = configDao.findHostsByGroup("internal");

        Set<String> links = new TreeSet<>();
//        for (THost host : hosts) {
//            for (TService service : host.services) {
//                for (String access : service.access) {
////                    printLink(access, host.name, service.url);
//                    links.add(createLinkText(access, host.name));
//                }
//            }
//        }

        for (THost host : hosts) {
            List<Packet> forwardPackets = packetService.getForwardPackets(host.name);
            for (Packet packet : forwardPackets) {
//                printLink(packet.getSource_address_name(), packet.getDestination_address_name(), packet.program);
                String linkText = createLinkText(packet.getSource_address_name(), packet.getDestination_address_name(), packet.app_protocol);
                links.add(linkText);
            }
        }

        for (String link : links) {
            System.out.println(link);
        }

    }

    private static String createLinkText(String aFrom, String aTo) {
        if(aTo.startsWith("iman-")) {
            return "";
        }

        if(aFrom.startsWith("iman-")) {
            return "";
        }

        if(aTo.startsWith("log-")) {
            return "";
        }

        if(aFrom.startsWith("iadm-")) {
            return "";
        }

        return String.format("\"%s\" -> \"%s\";", aFrom, aTo);
    }

    private static String createLinkText(String aFrom, String aTo, String aName) {
        if(aTo.startsWith("iman-")) {
            return "";
        }

        if(aFrom.startsWith("iman-")) {
            return "";
        }

        if(aTo.startsWith("log-")) {
            return "";
        }

        if(aFrom.startsWith("iadm-")) {
            return "";
        }

        if(aFrom.contains(".")) {
            return "";
        }
        if(aTo.contains(".")) {
            return "";
        }

        if(aName.equals("ntp")) {
            return "";
        }
        
        if(aName.equals("dns")) {
            return "";
        }

        return String.format("\"%s\" -> \"%s\" [label=\"%s\"];", aFrom, aTo, aName);
    }

    private static void printLink(String aSource, String aDest, String aName) {
        System.out.printf("\"%s\" -> \"%s\" labe=[\"%s\"];\n", aSource, aDest, aName);
//        System.out.printf("\"%s\" -> \"%s\" [label=\"%s\"];\n", aSource, aDest, aName);
    }


}
