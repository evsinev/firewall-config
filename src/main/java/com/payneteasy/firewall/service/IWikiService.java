package com.payneteasy.firewall.service;

public interface IWikiService {
    
    String createDetailsPage(String hostName) throws ConfigurationException;

    String createExternalServersPage();
    
    String createInternalServersPage();
    
    String createPacketsPage(String hostName) throws ConfigurationException;
    
    String createServicesPage();
}
