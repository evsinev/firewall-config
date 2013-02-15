package com.payneteasy.firewall.dao;

import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TInterface;
import com.payneteasy.firewall.dao.model.TPageHistory;
import com.payneteasy.firewall.dao.model.TProtocol;
import com.payneteasy.firewall.dao.model.TProtocols;
import com.payneteasy.firewall.service.ConfigurationException;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public interface IConfigDao {

    List<THost> findHostByGw(List<TInterface> aInterfaces);

    TPageHistory findPageHistory(String aPageName);

    TProtocol findProtocol(String aName);

    THost getHostByName(String aHostname);

    List<THost> listHosts();

    Collection<? extends THost> findHostsByGroup(String aGroupName);

    TProtocols listProtocols();

    void persistPagesHistory() throws FileNotFoundException;

    String resolveDns(String aName) throws ConfigurationException;
}
