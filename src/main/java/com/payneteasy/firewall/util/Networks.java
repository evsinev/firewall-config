package com.payneteasy.firewall.util;

import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TInterface;
import com.payneteasy.firewall.service.model.ServiceInfo;

import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

public class Networks {

    public static boolean isInNetwork(TInterface aLeftInterface, TInterface aRightInterface) {
        return isInNetwork(aLeftInterface.ip, aRightInterface.ip);
    }

    public static boolean isInNetwork(String leftIp, String rightIp) {
        if(!isIpAddress(leftIp)) {
            return false;
        }

        if(!isIpAddress(rightIp)) {
            return false;
        }

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

    /**
     * Checks only the destination service ip address against all ip addresses in the source host
     * @param aSourceHost          source host
     * @param aDestinationService  destination service
     * @return source host is the same network with destination service
     */
    public static boolean isInSameNetwork(THost aSourceHost, ServiceInfo aDestinationService) {
        for (TInterface sourceInterface : aSourceHost.interfaces) {
            List<String> sourceAddresses = sourceInterface.getAllIpAddresses();
            String destAddress = aDestinationService.address;
            for (String sourceAddress : sourceAddresses) {
                if(isInNetwork(sourceAddress, destAddress)) {
                    return true;
                }
            }
        }
        return false;

    }

    /**
     * Checks for all destination addresses
     * @param aSourceHost source host
     * @param aDestinationHost destination host
     * @return both hosts are in the same network
     */
    public static boolean isInSameNetwork(THost aSourceHost, THost aDestinationHost) {
        for (TInterface sourceInterface : aSourceHost.interfaces) {
            List<String> sourceAddresses = sourceInterface.getAllIpAddresses();
            for (TInterface destInterface : aDestinationHost.interfaces) {
                List<String> destAddresses = destInterface.getAllIpAddresses();
                for (String sourceAddress : sourceAddresses) {
                    for (String destAddress : destAddresses) {
                        if(isInNetwork(sourceAddress, destAddress)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean hasCommonGateway(THost aLeft, THost aRight) {
        return hasIpAddress(aLeft.gw, aRight.getAllIpAddresses())
                || hasIpAddress(aRight.gw, aLeft.getAllIpAddresses());
    }

    private static boolean hasIpAddress(String aIp, List<String> aAddresses) {
        for (String address : aAddresses) {
            if(aIp.equals(address)) {
                return true;
            }
        }
        return false;
    }
}
