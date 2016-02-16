package com.payneteasy.firewall.service.model;

import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.dao.model.TProtocol;
import com.payneteasy.firewall.service.ConfigurationException;
import com.payneteasy.firewall.util.Networks;

import java.util.StringTokenizer;

/**
 *
 */
public class UrlInfo {

    private UrlInfo(String aProtocol, int aPort, String aAddress) {
        protocol = aProtocol;
        port = aPort;
        address = aAddress;
    }

    public static UrlInfo parse(String aText, String aDefaultIp, IConfigDao aConfigDao) throws ConfigurationException {
        String name;
        Integer port;
        String address;

        StringTokenizer st = new StringTokenizer(aText, " :;\t");

        if(st.hasMoreTokens()) {
            // case 'http'
            name = st.nextToken();
        } else {
            throw new ConfigurationException("Url is empty");
        }

        if(st.hasMoreTokens()) {
            String tmp = st.nextToken();

            if(st.hasMoreTokens()) {
                // case 'http://1.2.3.4:8080'
                address = tmp.substring(2);
                port = Integer.parseInt(st.nextToken());
            } else {
                if(tmp.startsWith("//")) {
                    // case 'http://1.2.3.4'
                    port = null;
                    address = tmp.substring(2);
                } else {
                    // case 'http 8080'
                    port = Integer.parseInt(tmp);
                    address = aDefaultIp;
                }
            }

        } else {
            // case 'http://1.2.3.4'
            address = aDefaultIp;
            port = null;
        }

        if(port==null || port<1) {
            TProtocol protocol = aConfigDao.findProtocol(name);
            port = protocol.port;
        }

        if(Networks.isIpAddress(address) && !Character.isDigit(address.charAt(0))) {
            address = aConfigDao.resolveDns(address);
        }
        return new UrlInfo(name, port, address);
    }

    public final String protocol;
    public final int port;
    public final String address;
}
