package com.payneteasy.firewall.dao;

import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TInterface;
import com.payneteasy.firewall.dao.model.TProtocol;
import com.payneteasy.firewall.dao.model.TProtocols;
import com.payneteasy.firewall.service.ConfigurationException;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static java.lang.String.format;

/**
 *
 */
public class ConfigDaoYaml implements IConfigDao {

    public ConfigDaoYaml(File aDir) throws IOException {
        theYaml = new Yaml();

        theHosts = new ArrayList<THost>();
        loadHosts(theHosts, new File(aDir, "hosts"));

        theMap = createMap(theHosts);

        theProtocols = new HashMap<String, TProtocol>();
        loadProtocols(new File(aDir, "protocols.yml"));
    }

    private void loadProtocols(File aFile) throws IOException {
        FileReader in = new FileReader(aFile);
        try {
            TProtocols protocols = theYaml.loadAs(in, TProtocols.class);

            for (TProtocol protocol : protocols.protocols) {
                if(protocol.protocol==null) throw new IllegalStateException("protocol is null for "+protocol.name);
                if(protocol.port==0) throw new IllegalStateException("Port is empty for "+protocol.name);
                theProtocols.put(protocol.name, protocol);
            }
        } finally {
            in.close();
        }
    }

    @Override
    public TProtocol findProtocol(String aName) {
        TProtocol protocol = theProtocols.get(aName);
        if(protocol==null) throw new IllegalStateException("Protocol "+aName+" not found in protocols.yml");
        return protocol;
    }

    @Override
    public Collection<? extends THost> findHostsByGroup(String aGroupName) {
        List<THost> ret = new ArrayList<THost>();
        for (THost host : theHosts) {
            if(aGroupName.equals(host.group)) {
                ret.add(host);
            }
        }
        return ret;
    }

    private Map<String, THost> createMap(List<THost> aHosts) {
        Map<String, THost> map = new HashMap<String, THost>();
        for (THost host : aHosts) {
            map.put(host.name, host);
        }
        return map;
    }

    private void loadHosts(List<THost> aList, File aDir) throws IOException {
        if(aDir==null) throw new IllegalStateException("aDir is null");
        File[] entries = aDir.listFiles();
        if(entries==null) return;

        for (File entry : entries) {
            if(entry.isDirectory()) {
                loadHosts(aList, entry);
            } else if(entry.isFile()) {
                THost host = loadHost(entry);
                aList.add(host);
            }
        }
    }

    private THost loadHost(File aFile) throws IOException {
        FileReader in = new FileReader(aFile);
        try {

            THost host = theYaml.loadAs(in, THost.class);
            host.name = aFile.getName().replaceAll(".yml", "");
            host.group = aFile.getParentFile().getName();

            return host;
        } finally {
            in.close();
        }
    }

    @Override
    public THost getHostByName(String aHostname) {
        THost host = theMap.get(aHostname);
        if(host==null) throw new IllegalStateException("Host "+aHostname+" not found");
        return host;
    }

    @Override
    public List<THost> listHosts() {
        return theHosts;
    }

    @Override
    public List<THost> findHostByGw(List<TInterface> aInterfaces) {
        List<THost> ret = new ArrayList<THost>();
        for (THost host : theHosts) {
            for (TInterface iface : aInterfaces) {
                if(host.gw.equals(iface.ip)) {
                    ret.add(host);
                }
            }
        }
        return ret;
    }

    @Override
    public String resolveDns(String aName) throws ConfigurationException {
        String found = null;
        for (THost host : theHosts) {
            for (TInterface iface : host.interfaces) {
                if(aName.equals(iface.dns)) {
                    if(found!=null) throw  new ConfigurationException(format("DNS name %s has two ip addresses %s and %s", aName, found, iface.ip));
                    found = iface.ip;

                }
            }
        }
        if(found==null) {
            throw new ConfigurationException(format("DNS name %s not found", aName));
        }
        return found;
    }

    public final List<THost> theHosts;
    public final Map<String, THost> theMap;
    public final Yaml theYaml;
    public final Map<String, TProtocol> theProtocols;
}
