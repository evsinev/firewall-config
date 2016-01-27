package com.payneteasy.firewall.dao;

import com.payneteasy.firewall.dao.model.*;
import com.payneteasy.firewall.service.ConfigurationException;
import com.payneteasy.firewall.service.model.Access;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public interface IConfigDao {

    List<THost> findHostByGw(List<TInterface> aInterfaces);

    List<HostInterface> findHostInterfacesByGw(List<TInterface> aInterfaces);

    TPageHistory findPageHistory(String aPageName);

    TProtocol findProtocol(String aName);

    THost getHostByName(String aHostname);

    boolean isHostExist(String aHostname);

    List<THost> listHosts();

    Collection<? extends THost> findHostsByGroup(String aGroupName);

    TProtocols listProtocols();

    void persistPagesHistory() throws FileNotFoundException;

    String resolveDns(String aName) throws ConfigurationException;

    HostInterface findLinkedInterface(THost aHost, TInterface anInterface);

    Collection<THost> getHostByPattern(String aPattern);
}
