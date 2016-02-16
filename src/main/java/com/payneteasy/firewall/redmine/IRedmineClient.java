package com.payneteasy.firewall.redmine;

import java.io.IOException;

public interface IRedmineClient {

    void executeCreateOrUpdateWikiPage(String pageName, String title, String text, String comment) throws IOException;

}