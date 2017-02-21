package com.payneteasy.firewall.l2.editor.create;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;
import java.util.List;

public class L2CustomParameters {

    public Map<String, String> vlanColors;
    public List<String>        addedLinks;
    public List<String>        removedLinks;

    public static void main(String[] args) {
        L2CustomParameters params = new L2CustomParameters();
        params.vlanColors = new HashMap<>();
        params.vlanColors.put("vlan_inside", "F78181");
        params.vlanColors.put("vlan_trans" , "81F7D8");
        params.vlanColors.put("vlan_backup", "A9BCF5");
        params.vlanColors.put("vlan_ipmi"  , "F2F5A9");

        params.addedLinks = new ArrayList<>();
        params.addedLinks.add("sw-1/ether1 (vlan_inside) >>>> host-1/eth0 (vlan_inside) ");
        params.addedLinks.add("sw-1/ether1 (vlan_inside) >>>> host-1/eth0 (vlan_inside) ");

        params.removedLinks = new ArrayList<>();
        params.removedLinks.add("sw-1/ether1 (vlan_inside) >>>> host-1/eth0 (vlan_inside) ");
        params.removedLinks.add("sw-1/ether1 (vlan_inside) >>>> host-1/eth0 (vlan_inside) ");

        final DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setIndent(4);
        dumperOptions.setPrettyFlow(true);
        Yaml yaml = new Yaml(dumperOptions);
        final String text = yaml.dump(params);
        System.out.println("text = " + text);
    }

    public Map<String, Color> getVlanColors() {
        Map<String, Color> map = new HashMap<>();
        for (Map.Entry<String, String> entry : vlanColors.entrySet()) {
            map.put(entry.getKey(), Color.decode(entry.getValue()));
        }
        return map;
    }

    public Map<String, HostAndLinkBuilder.HostHolder> getPendingHostMap(Map<String, HostAndLinkBuilder.HostHolder> hosts) {
        if(addedLinks == null) {
            addedLinks = new ArrayList<>();
        }
        for (String linkText : addedLinks) {
            LinkDescription link = new LinkDescription(linkText);
            addHostAndPort(hosts, link.leftHost, link.leftPort, link.leftVlan);
            addHostAndPort(hosts, link.rightHost, link.rightPort, link.rightVlan);
        }
        return hosts;
    }

    private void addHostAndPort(Map<String, HostAndLinkBuilder.HostHolder> hosts, String hostname, String portName, String vlanName) {
        HostAndLinkBuilder.HostHolder leftHost = hosts.get(hostname);
        if(leftHost == null) {
            leftHost = new HostAndLinkBuilder.HostHolder(hostname);
            hosts.put(hostname, leftHost);
        }
        leftHost.addPort(portName, vlanName);
    }

    public Set<LinkHolder> getLinks() {
        Set<LinkHolder> links = new HashSet<>();
        for (String link : addedLinks) {
            links.add(new LinkDescription(link).getLinkHolder());
        }
        return links;
    }

    public List<LinkHolder> getRemovedLinks(){
        if(removedLinks == null) {
            return Collections.emptyList();
        }

        List<LinkHolder> links = new ArrayList<>();
        for (String link : removedLinks) {
            links.add(new LinkDescription(link).getLinkHolder());
        }
        return links;
    }

    public static L2CustomParameters load(File aFile) {
        Yaml yaml = new Yaml();
        try {
            return yaml.loadAs(new FileReader(aFile), L2CustomParameters.class);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Couldn't read file " + aFile.getAbsolutePath(), e);
        }
    }

    static class LinkDescription {

        final String leftHost;
        final String leftPort;
        final String leftVlan;
        final String rightHost;
        final String rightPort;
        final String rightVlan;

        public LinkDescription(String aText) {
            StringTokenizer st = new StringTokenizer(aText, "/ >");
            leftHost  = st.nextToken();
            leftPort  = st.nextToken();

            String tempLeft = st.nextToken();
            if(tempLeft.startsWith("(")) {
                leftVlan = removeBracked(tempLeft);
                rightHost = st.nextToken();
            } else {
                leftVlan = null;
                rightHost = tempLeft;
            }

            rightPort = st.nextToken();
            rightVlan = st.hasMoreTokens() ? removeBracked(st.nextToken()) : null;

        }

        private String removeBracked(String aText) {
            return aText.replace("(", "").replace(")", "");
        }

        public LinkHolder getLinkHolder() {
            return new LinkHolder(leftHost, leftPort, rightHost, rightPort);
        }
    }
}
