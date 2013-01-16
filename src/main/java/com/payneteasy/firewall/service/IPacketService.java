package com.payneteasy.firewall.service;

import com.payneteasy.firewall.service.model.Packet;

import java.util.List;

/**
 *
 */
public interface IPacketService {

    List<Packet> getForwardPackets(String aHostname) throws ConfigurationException;
}
