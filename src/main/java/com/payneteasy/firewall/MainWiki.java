package com.payneteasy.firewall;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.hash.HashCodes;
import com.payneteasy.firewall.dao.ConfigDaoYaml;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.dao.model.TPageHistory;
import com.payneteasy.firewall.redmine.RedmineEasyClient;
import com.payneteasy.firewall.service.ConfigurationException;
import com.payneteasy.firewall.service.IPacketService;
import com.payneteasy.firewall.service.IWikiService;
import com.payneteasy.firewall.service.impl.PacketServiceImpl;
import com.payneteasy.firewall.service.impl.WikiServiceImpl;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainWiki {    
    
    private static final Logger LOGGER = Logger.getLogger(MainWiki.class.getName());

    public static void main(String[] args) throws IOException, KeyManagementException, NoSuchAlgorithmException, ConfigurationException {
        Preconditions.checkArgument(args.length >= 3, "usage: redmine-wiki-address redmine-key config-dir [OPTION]\n\n"
                + "  -f, --force\n               force to update all pages");

        String redmineUrl = args[0];
        String redmineKey = args[1];
        
        File configDir = new File(args[2]);
        if (!configDir.exists()) {
            throw new IllegalStateException("Config dir " + configDir.getAbsolutePath() + " is not exists");
        }
       
        final boolean force = args.length >=4 && ("-f".equals(args[3]) || "--force".equals(args[3]));        

        IConfigDao configDao = new ConfigDaoYaml(configDir);
        IPacketService packetService = new PacketServiceImpl(configDao);
        IWikiService wikiService = new WikiServiceImpl(configDao, packetService);

        RedmineEasyClient client = new RedmineEasyClient(redmineUrl, redmineKey);

        String pageName;
        String pageTitle;
        String content;
        boolean needHistoryUpdate = false;

        for (THost host : configDao.listHosts()) {            
            String hostName = host.name;
            try {
                pageName = hostName + "_details";
                pageTitle = hostName + " details";
                content = wikiService.createDetailsPage(hostName);
                needHistoryUpdate |= createOrUpdateWikiPage(client, configDao, force, pageName, pageTitle, content);

                pageName = hostName + "_packets";
                pageTitle = hostName + " packets";
                content = wikiService.createPacketsPage(hostName);
                needHistoryUpdate |= createOrUpdateWikiPage(client, configDao, force, pageName, pageTitle, content);                
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Cannot process host [" + hostName + "].", ex);
            }
        }
        
        content = wikiService.createExternalServersPage();
        pageName = "external_group";
        pageTitle = "External servers";
        needHistoryUpdate |= createOrUpdateWikiPage(client, configDao, force, pageName, pageTitle, content);

        content = wikiService.createInternalServersPage();
        pageName = "internal_group";
        pageTitle = "Internal servers";
        needHistoryUpdate |= createOrUpdateWikiPage(client, configDao, force, pageName, pageTitle, content);
        
        content = wikiService.createServicesPage();
        pageName = "services";
        pageTitle = "Services";
        needHistoryUpdate |= createOrUpdateWikiPage(client, configDao, force, pageName, pageTitle, content);
        
        if (needHistoryUpdate) {
            configDao.persistPagesHistory();
        }
    }

    private static boolean createOrUpdateWikiPage(RedmineEasyClient client, IConfigDao configDao, boolean force,
            String pageName, String pageTitle, String content) throws IOException {
        long hashCode = content.hashCode();
        TPageHistory pageHistory = configDao.findPageHistory(pageName);
        boolean createOrUpdate = force || pageHistory.pageHash != hashCode;
        if (createOrUpdate) {
            pageHistory.pageHash = hashCode;
            client.executeCreateOrUpdateWikiPage(pageName, pageTitle, content, "updated: " + new Date());
        }
        return createOrUpdate; 
    }
}
