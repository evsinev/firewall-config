package com.payneteasy.firewall.service;

public interface IWikiService {
    
    String createDetailsPage(String hostName) throws ConfigurationException;

    String createServersPage(String aName);

    String createPacketsPage(String hostName) throws ConfigurationException;
    
    String createServicesPage();
}
