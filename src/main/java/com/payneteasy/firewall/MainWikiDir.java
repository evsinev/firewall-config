package com.payneteasy.firewall;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.payneteasy.firewall.redmine.RedmineEasyClient;
import com.payneteasy.firewall.service.IWikiService;
import com.payneteasy.firewall.service.impl.WikiServiceImpl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * Uploads wiki pages from a dir to a redmine server
 */
public class MainWikiDir {
    public static void main(String[] args) throws KeyManagementException, NoSuchAlgorithmException, IOException {
        if(args.length<3) {
            System.out.println("usage: MainWikiDir redmine-url redmine-key wiki-dir");
            System.exit(1);
        }

        String redmineUrl = args[0];
        String redmineKey = args[1];
        File wikiDir    = new File(args[2]);

        Preconditions.checkArgument(wikiDir.exists(), "Dir %s is not exists", wikiDir.getAbsolutePath());

        File[] files = wikiDir.listFiles();

        RedmineEasyClient redmineClient = new RedmineEasyClient(redmineUrl, redmineKey);

        for (File file : files) {
            System.out.println(file+"...");
            redmineClient.executeCreateOrUpdateWikiPage(
                    file.getName() // page name
                    , file.getName() // title
                    , Files.toString(file, Charset.defaultCharset())
                    , "update info from server"
            );
        }
    }
}
