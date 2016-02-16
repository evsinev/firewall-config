package com.payneteasy.firewall.util;

import com.payneteasy.firewall.dao.model.TInterface;

import java.util.StringTokenizer;

public class Networks {

    public static boolean isInNetwork(TInterface aLeftInterface, TInterface aRightInterface) {
        String leftIp = aLeftInterface.ip;
        String rightIp = aRightInterface.ip;

        if(leftIp==null) return false;

        int left = leftIp.lastIndexOf('.');
        int right = rightIp.lastIndexOf('.');

        return leftIp.substring(0, left).equals(rightIp.substring(0, right));
    }

    public static String get24NetworkReverse(String aIp) {
        int pos = aIp.lastIndexOf('.');
        String network = aIp.substring(0, pos);
        String[] numbers = network.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (String number : numbers) {
            if(sb.length()!=0) {
                sb.insert(0, '.');
            }
            sb.insert(0, number);
        }
        return sb.toString();

    }

    public static String get24MaskAddress(String aIp) {
        int pos = aIp.lastIndexOf('.');
        return aIp.substring(pos+1);
    }

    public static boolean isIpAddress(String aAddress) {
        return Strings.hasText(aAddress) && !"skip".equals(aAddress);
    }
}
