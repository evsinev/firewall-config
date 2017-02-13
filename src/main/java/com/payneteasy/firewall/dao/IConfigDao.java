package com.payneteasy.firewall.dao;

import com.payneteasy.firewall.dao.model.*;
import com.payneteasy.firewall.service.ConfigurationException;
import com.payneteasy.firewall.service.model.Access;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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

    List<String> listGroups();

    Collection<? extends THost> findHostsByGroup(String aGroupName);

    Collection<? extends THost> findHostsByGroups(String ... aGroups);

    TProtocols listProtocols();

    void persistPagesHistory() throws FileNotFoundException;

    String resolveDns(String aName) throws ConfigurationException;

    HostInterface findLinkedInterface(THost aHost, TInterface anInterface);

    Collection<THost> getHostByPattern(String aPattern);

    Collection<THost> listHostsByFilter(String ... aArguments);

    Map<String, String> listNetworksNames();
}
