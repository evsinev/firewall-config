package com.payneteasy.firewall.critsoft;

import com.google.common.base.Preconditions;
import com.payneteasy.firewall.redmine.RedmineEasyClient;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CritSoftCollectorMain {

    public static void main(String[] args) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        if(args.length<3) {
            System.out.println("usage: CritSoftCollectorMain redmine-url remin-page redmine-key software-cards-dir");
            System.exit(1);
        }

        String redmineUrl = args[0];
        String redminePage = args[1];
        String redmineKey = args[2];
        Path softwareCardsDir = Paths.get(args[3]);

        Preconditions.checkArgument(Files.exists(softwareCardsDir), "Directory %s does not exist", softwareCardsDir.toAbsolutePath());

        SoftwareCardHandler handler = new SoftwareCardHandler();
        Map<Pair<String, String>, List<SoftwareCardDetails>> map = Collections.emptyMap();

        map = handler.parseDir(softwareCardsDir);

        String redmineTextileTable = handler.buildRedmineTextileTable(map);

        RedmineEasyClient redmineClient = new RedmineEasyClient(redmineUrl, redmineKey);

        redmineClient.executeCreateOrUpdateWikiPage(
                redminePage // page name
                , "Critical Software In Use" // title
                , redmineTextileTable
                , "Updated critical software list collected from CDE"
        );


    }
}
