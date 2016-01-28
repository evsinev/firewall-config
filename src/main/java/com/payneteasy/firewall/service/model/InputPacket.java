package com.payneteasy.firewall.service.model;

/**
 *
 */
public class InputPacket  extends AbstractPacket  {

    public String source_address;
    public String input_interface;
    public String source_address_name;

    public String getSource_address_name() {
        return source_address_name;
    }

    public String getSource_address() {
        return source_address;
    }

    public String getInput_interface() {
        return input_interface;
    }
}
