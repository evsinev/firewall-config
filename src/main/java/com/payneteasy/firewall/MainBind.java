package com.payneteasy.firewall;

import com.payneteasy.firewall.dao.ConfigDaoYaml;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TInterface;
import com.payneteasy.firewall.dao.model.TService;
import com.payneteasy.firewall.dao.model.TVirtualIpAddress;
import com.payneteasy.firewall.util.MustacheFilePrinter;
import com.payneteasy.firewall.util.Networks;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.payneteasy.firewall.util.Strings.hasText;
import static com.payneteasy.firewall.util.Strings.maxLength;
import static com.payneteasy.firewall.util.Strings.padRight;

public class MainBind {

    IConfigDao dao;
    SimpleDateFormat format = new SimpleDateFormat("yyMMddHHmm");

    public MainBind(IConfigDao aDao) {
        dao = aDao;
    }

    public static void main(String[] args) throws IOException {
        File configDir = new File(args[0]);
        String domainName = args[1];
        File outputDir = new File(args[2]);

        IConfigDao configDao = new ConfigDaoYaml(configDir);

        MainBind generator = new MainBind(configDao);
        generator.createBindConfigs(domainName, outputDir);

    }

    private void createBindConfigs(String aDomainName, File aOutputDir) {
        Collection<Zone> zones = new TreeSet<>();
        createDirectZone(aDomainName, new File(aOutputDir, aDomainName+".zone"), zones);
        createReverseZone(aDomainName, aOutputDir, zones);
        createServiceZone("static", new File(aOutputDir,"static.zone"), zones);
        createZonesConf(aDomainName, zones, new File(aOutputDir, "zones.conf"));
    }

    private void createServiceZone(String aDomainName, File aZoneFile, Collection<Zone> aZones) {
        Collection<Address> addresses = new ArrayList<>();

        for (THost host : dao.findHostsByGroup("internal")) {
            for (TService service : host.services) {
                if(hasText(service.name)) {
                    Address address = new Address();
                    address.ip = host.getDefaultIp();
                    address.name = service.name;
                    addresses.add(address);
                }
            }
        }

        MustacheFilePrinter out = new MustacheFilePrinter("bind-zone.mustache");
        addHeader(aDomainName, out);
        out.add("addresses", addresses);
        out.write(aZoneFile);

        aZones.add(new Zone(aDomainName, aZoneFile.getName()));
    }

    private void createZonesConf(String aDomainName, Collection<Zone> aZones, File aFile) {
        MustacheFilePrinter out = new MustacheFilePrinter("bind-zones-conf.mustache");
        addHeader(aDomainName, out);
        out.add("zones", aZones);
        out.write(aFile);
    }

    private void createReverseZone(String aDomainName, File aOutputDir, Collection<Zone> aZones) {
        Collection<? extends THost> hosts = dao.findHostsByGroup("internal");
        TreeMap<String, Collection<Address>> zones = new TreeMap<>();

        for (THost host : hosts) {
            for(String ip : getIpAddresses(host.interfaces)) {
                String network = Networks.get24NetworkReverse(ip);
                Collection<Address> addresses = getAddresses(zones, network);
                Address address = new Address();
                address.ip = Networks.get24MaskAddress(ip);
                address.name = host.name;
                addresses.add(address);
            }
        }

        for (Map.Entry<String, Collection<Address>> entry : zones.entrySet()) {
            MustacheFilePrinter out = new MustacheFilePrinter("bind-reverse-zone.mustache");
            addHeader(aDomainName, out);
            out.add("addresses", entry.getValue());

            File output = new File(aOutputDir, entry.getKey()+".zone");
            out.write(output);

            aZones.add(new Zone(entry.getKey()+".in-addr.arpa", output.getName()));
        }
    }

    private List<String> getIpAddresses(List<TInterface> interfaces) {
        ArrayList<String> list = new ArrayList<>();
        for (TInterface iface : interfaces) {
            if(hasText(iface.ip) && !iface.ip.equals("skip")) {
                list.add(iface.ip);
            }
            if(hasText(iface.vip)) {
                list.add(iface.vip);
            }

            if(iface.vips != null) {
                for (TVirtualIpAddress vip : iface.vips) {
                    list.add(vip.ip);
                }
            }
        }
        return list;
    }

    private void addHeader(String aDomainName, MustacheFilePrinter out) {
        out.add("serial", format.format(new Date()));
        out.add("domain", aDomainName);
        out.add("date", new Date());
        try {
            out.add("host", InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            out.add("host", "unknown");
        }
        out.add("username", System.getProperty("user.name"));

    }

    private Collection<Address> getAddresses(TreeMap<String, Collection<Address>> zones, String aNetwork) {
        Collection<Address> addresses = zones.get(aNetwork);
        if(addresses == null) {
            addresses = new TreeSet<>();
            zones.put(aNetwork, addresses);
        }
        return addresses;
    }

    private void createDirectZone(String aDomainName, File aZoneFile, Collection<Zone> zones) {
        MustacheFilePrinter out = new MustacheFilePrinter("bind-zone.mustache");

        addHeader(aDomainName, out);
        out.add("addresses", createAddresses());

        out.write(aZoneFile);

        zones.add(new Zone(aDomainName, aZoneFile.getName()));
    }


    private Collection<Address> createAddresses() {
        TreeSet<Address> ret = new TreeSet<>();
        Collection<? extends THost> hosts = dao.findHostsByGroup("internal");
        int max = maxLength(hosts, aObj -> aObj.name);
        for (THost host : hosts) {
            String ip = host.getDefaultIp();
            if(hasText(ip) && !ip.equals("skip")) {
                Address address = new Address();
                address.name = padRight(host.name, max);
                address.ip   = ip;
                ret.add(address);
            } else {
                System.err.println("Host "+host.name+" hasn't got default ip address");
            }
        }
        return ret;
    }

    public static class Address implements Comparable<Address>{

        String name;
        String ip;


        @Override
        public int compareTo(Address o) {
            return name.compareTo(o.name);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static class Zone implements Comparable<Zone> {

        public Zone(String name, String filename) {
            this.name = name;
            this.filename = filename;
        }

        @Override
        public int compareTo(Zone o) {
            return name.compareTo(o.name);
        }

        String name;
        String filename;
    }
}
