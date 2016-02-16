package com.payneteasy.firewall.redmine;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class RedmineFileStoreClient implements IRedmineClient {


    private final File dir;


    public RedmineFileStoreClient(String aDirname) {
        dir = new File(aDirname);
        dir.mkdirs();
        System.out.println("Storing wiki file to "+aDirname);
    }

    @Override
    public void executeCreateOrUpdateWikiPage(String pageName, String title, String text, String comment) throws IOException {
        Files.write(text, new File(dir, pageName+".wiki"), Charset.defaultCharset());
    }
}
