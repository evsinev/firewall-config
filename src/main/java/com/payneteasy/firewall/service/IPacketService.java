package com.payneteasy.firewall.service;

import com.payneteasy.firewall.dao.model.ChainType;
import com.payneteasy.firewall.dao.model.TBlockedIpAddress;
import com.payneteasy.firewall.dao.model.TCustomRule;
import com.payneteasy.firewall.service.model.*;

import java.util.List;
import java.util.Set;

/**
 *
 */
public interface IPacketService {

    List<Packet> getForwardPackets(String aHostname) throws ConfigurationException;

    List<InputPacket> getInputPackets(String aHostname) throws ConfigurationException;

    Set<InputMssPacket> getInputMssPackets(String aHostname) throws ConfigurationException;

    List<OutputPacket> getOutputPackets(String aHostname) throws ConfigurationException;

    List<VrrpPacket> getVrrpPackets(String aHostname);

    List<LinkedVrrpPacket> getLinkedVrrpPackets(String aHostname);

    List<TBlockedIpAddress> getBlockedIpAddresses(String aHostname);

    List<TCustomRule> getCustomRules(String aHost, ChainType aChainType);
}
