package com.payneteasy.firewall.service;

import com.payneteasy.firewall.service.model.InputPacket;
import com.payneteasy.firewall.service.model.OutputPacket;
import com.payneteasy.firewall.service.model.Packet;
import com.payneteasy.firewall.service.model.VrrpPacket;

import java.util.List;

/**
 *
 */
public interface IPacketService {

    List<Packet> getForwardPackets(String aHostname) throws ConfigurationException;

    List<InputPacket> getInputPackets(String aHostname) throws ConfigurationException;

    List<OutputPacket> getOutputPackets(String aHostname) throws ConfigurationException;

    List<VrrpPacket> getVrrpPackets(String aHostname);
}
