package com.payneteasy.firewall.dao;

import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TInterface;
import com.payneteasy.firewall.dao.model.TProtocol;
import com.payneteasy.firewall.service.ConfigurationException;

import java.util.Collection;
import java.util.List;

/**
 *
 */
public interface IConfigDao {

    THost getHostByName(String aHostname);

    List<THost> listHosts();

    List<THost> findHostByGw(List<TInterface> aInterfaces);

    TProtocol findProtocol(String aName);

    Collection<? extends THost> findHostsByGroup(String aGroupName);

    String resolveDns(String aName) throws ConfigurationException;
}
