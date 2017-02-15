package com.payneteasy.firewall.l2.editor.create;

import java.awt.*;

public interface IPositionManager {

    Point getPortPosition(String aHostname, String aPortName);

    Point getHostPosition(String aHostname);
}
