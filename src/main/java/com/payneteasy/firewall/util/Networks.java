package com.payneteasy.firewall.util;

import com.payneteasy.firewall.dao.model.TInterface;

public class Networks {

    public static boolean isInNetwork(TInterface aLeftInterface, TInterface aRightInterface) {
        String leftIp = aLeftInterface.ip;
        String rightIp = aRightInterface.ip;

        if(leftIp==null) return false;

        int left = leftIp.lastIndexOf('.');
        int right = rightIp.lastIndexOf('.');

        return leftIp.substring(0, left).equals(rightIp.substring(0, right));
    }

}
