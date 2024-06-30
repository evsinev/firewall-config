package com.payneteasy.firewall.redmine;

import com.payneteasy.firewall.redmine.messages.RedmineIssueCreateRequest;

public interface IRedmineIssueClient {

    void createIssue(RedmineIssueCreateRequest aRequest);

    void getIssuesByParentId(String aProjectId, int aParentId);
}
