package com.payneteasy.firewall.service.model;

import java.util.Objects;

public class InputMssPacket {

    private final String source_address;
    private final int    mss;
    private final String source_address_name;
    private final String destination_service;


    public InputMssPacket(String source_address, int mss, String source_address_name, String destination_service) {
        this.source_address = source_address;
        this.mss = mss;
        this.source_address_name = source_address_name;
        this.destination_service = destination_service;
    }

    public String getSource_address_name() {
        return source_address_name;
    }

    public String getDestination_service() {
        return destination_service;
    }

    public int getMss() {
        return mss;
    }

    public String getSource_address() {
        return source_address;
    }

    @Override
    public String toString() {
        return "InputMssPacket{" +
                "source_address='" + source_address + '\'' +
                ", mss=" + mss +
                ", source_address_name='" + source_address_name + '\'' +
                ", destination_service='" + destination_service + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InputMssPacket that = (InputMssPacket) o;
        return Objects.equals(source_address, that.source_address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source_address);
    }
}
