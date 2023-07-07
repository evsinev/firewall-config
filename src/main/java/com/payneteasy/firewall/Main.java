package com.payneteasy.firewall;

import com.payneteasy.firewall.dao.ConfigDaoYaml;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.dao.model.TBlockedIpAddress;
import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.service.ConfigurationException;
import com.payneteasy.firewall.service.IPacketService;
import com.payneteasy.firewall.service.impl.PacketServiceImpl;
import com.payneteasy.firewall.service.model.*;
import com.payneteasy.firewall.util.VelocityBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class Main {


    public static void main(String[] args) throws IOException, ConfigurationException {
        if(args.length!=3) throw new IllegalStateException("usage: firewall-config.sh config-dir host output-dir");

        File configDir = new File(args[0]);
        if(!configDir.exists()) throw new IllegalStateException("Config dir "+configDir.getAbsolutePath()+" is not exists");

        String host = args[1];
        String dir  = args[2];

        IConfigDao configDao = new ConfigDaoYaml(configDir);

        IPacketService packetService = new PacketServiceImpl(configDao);
        if(host.startsWith("group-")) {
            String groupName = host.replaceAll("group-", "");
            Collection<? extends THost> hosts = configDao.findHostsByGroup(groupName);
            for (THost h : hosts) {
                System.out.print(".");
                createFirewallConfig(h.name, dir, packetService);
            }
            System.out.println();
        } else {
            createFirewallConfig(host, dir, packetService);
        }
    }

    private static void createFirewallConfig(String host, String aDir, IPacketService packetService) throws ConfigurationException, IOException {
        List<Packet>            forwards           = packetService.getForwardPackets(host);
        List<InputPacket>       inputs             = packetService.getInputPackets(host);
        Set<InputMssPacket>     mssInputs          = packetService.getInputMssPackets(host);
        List<OutputPacket>      outputs            = packetService.getOutputPackets(host);
        List<VrrpPacket>        vrrpPackets        = packetService.getVrrpPackets(host);
        List<LinkedVrrpPacket>  linkedVrrpPackets  = packetService.getLinkedVrrpPackets(host);
        List<TBlockedIpAddress> blockedIpAddresses = packetService.getBlockedIpAddresses(host);

        VelocityBuilder velocity = new VelocityBuilder();
        velocity.add("generated-date", new Date());
        velocity.add("generated-user", System.getenv("USER"));
        velocity.add("forward-packets", forwards);
        velocity.add("input-packets", inputs);
        velocity.add("input-mss", mssInputs);
        velocity.add("output-packets", outputs);
        velocity.add("vrrp-packets", vrrpPackets);
        velocity.add("linked-vrrp-packets", linkedVrrpPackets);
        velocity.add("blocked-ip-addresses", blockedIpAddresses);

        PrintWriter out = new PrintWriter(new FileWriter(new File(aDir, host)));
        try {
            velocity.processTemplate(Main.class.getResource("/iptables.vm"), out);
        } finally {
            out.flush();
        }
    }


}
