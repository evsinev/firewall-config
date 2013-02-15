package com.payneteasy.firewall.dao;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TInterface;
import com.payneteasy.firewall.dao.model.TPageHistory;
import com.payneteasy.firewall.dao.model.TPagesHistory;
import com.payneteasy.firewall.dao.model.TProtocol;
import com.payneteasy.firewall.dao.model.TProtocols;
import com.payneteasy.firewall.service.ConfigurationException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import static java.lang.String.format;

/**
 *
 */
public class ConfigDaoYaml implements IConfigDao {

    public ConfigDaoYaml(File aDir) throws IOException {
        theYaml = new Yaml();
        theDir = aDir;

        theHosts = new ArrayList<THost>();
        loadHosts(theHosts, new File(aDir, "hosts"));

        theMap = createMap(theHosts);

        thePagesHistoryMap = Maps.newHashMap();
        loadPagesHistory(new File(aDir, "pages_history.yml"));
        
        theProtocolsMap = new HashMap<String, TProtocol>();
        theProtocols = loadProtocols(new File(aDir, "protocols.yml"));
    }

    private void loadPagesHistory(File aFile) throws IOException {
        TPagesHistory pagesHistory;
        if (aFile.exists()) {
            Reader in = null;
            try {
                in = Files.newReader(aFile, Charsets.UTF_8);
                pagesHistory = theYaml.loadAs(in, TPagesHistory.class);
                if (pagesHistory != null) {
                    for (TPageHistory pageHistory : pagesHistory.pageHistories) {
                        if (pageHistory.pageHash == 0) {
                            throw new IllegalStateException("page hash is empty for " + pageHistory.pageName);
                        }
                        thePagesHistoryMap.put(pageHistory.pageName, pageHistory);
                    }
                }
            } finally {
                Closeables.closeQuietly(in);
            }
        }      
    }
    
    private TProtocols loadProtocols(File aFile) throws IOException {
        FileReader in = new FileReader(aFile);
        TProtocols protocols; 
        try {
            protocols = theYaml.loadAs(in, TProtocols.class);

            for (TProtocol protocol : protocols.protocols) {
                if(protocol.protocol==null) throw new IllegalStateException("protocol is null for "+protocol.name);
                if(protocol.port==0) throw new IllegalStateException("Port is empty for "+protocol.name);
                theProtocolsMap.put(protocol.name, protocol);
            }
        } finally {
            in.close();
        }
        return protocols;
    }

    @Override public TPageHistory findPageHistory(String aPageName) {
        TPageHistory pageHistory = thePagesHistoryMap.get(aPageName);
        if (pageHistory == null) {
            pageHistory = new TPageHistory();
            pageHistory.pageName = aPageName;
            thePagesHistoryMap.put(aPageName, pageHistory);
        }
        return pageHistory;
    }

    @Override
    public TProtocol findProtocol(String aName) {
        TProtocol protocol = theProtocolsMap.get(aName);
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

    @Override public void persistPagesHistory() throws FileNotFoundException {
        File file = new File(theDir, "pages_history.yml");
        Writer writer = null; 
        try {
            writer = Files.newWriter(file, Charsets.UTF_8);
            TPagesHistory history = new TPagesHistory();
            history.pageHistories = Lists.newArrayList(thePagesHistoryMap.values());
            history.lastUpdateDate = new Date();
            theYaml.dump(history, writer);
        } finally {
            Closeables.closeQuietly(writer);
        }
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

    @Override public TProtocols listProtocols() {
        return theProtocols;
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

    public final File theDir;
    public final List<THost> theHosts;
    public final Map<String, THost> theMap;
    public final Yaml theYaml;
    public final Map<String, TPageHistory> thePagesHistoryMap;
    public final TProtocols theProtocols;
    public final Map<String, TProtocol> theProtocolsMap;
}
