package com.payneteasy.firewall;

import com.payneteasy.firewall.dao.ConfigDaoYaml;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.service.ConfigurationException;
import com.payneteasy.firewall.service.IPacketService;
import com.payneteasy.firewall.service.impl.PacketServiceImpl;
import com.payneteasy.firewall.service.model.Packet;
import com.payneteasy.firewall.util.VelocityBuilder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

public class Main {


    public static void main(String[] args) throws IOException, ConfigurationException {
        if(args.length!=2) throw new IllegalStateException("usage: firewall-config.sh config-dir host");

        File configDir = new File(args[0]);
        if(!configDir.exists()) throw new IllegalStateException("Config dir "+configDir.getAbsolutePath()+" is not exists");

        String host = args[1];

        IConfigDao configDao = new ConfigDaoYaml(configDir);

        IPacketService packetService = new PacketServiceImpl(configDao);

        List<Packet> forwards = packetService.getForwardPackets(host);

        VelocityBuilder velocity = new VelocityBuilder();
        velocity.add("generated-date", new Date());
        velocity.add("forward-packets", forwards);

        PrintWriter out = new PrintWriter(System.out);
        try {
            velocity.processTemplate(Main.class.getResource("/iptables.vm"), out);
        } finally {
            out.flush();
        }
    }


}
