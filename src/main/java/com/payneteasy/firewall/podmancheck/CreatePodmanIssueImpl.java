package com.payneteasy.firewall.podmancheck;

import com.google.gson.GsonBuilder;
import com.payneteasy.firewall.redmine.IRedmineIssueClient;
import com.payneteasy.firewall.redmine.impl.RedmineIssueClientImpl;
import com.payneteasy.firewall.redmine.messages.RedmineIssueCreateRequest;
import com.payneteasy.firewall.redmine.model.RedmineIssue;
import com.payneteasy.http.client.impl.HttpClientImpl;

public class CreatePodmanIssueImpl implements ICreatePodmanIssue {

    private final IRedmineIssueClient redmine;

    private final String project;

    public CreatePodmanIssueImpl(String aUrl, String aKey, String aProject) {
        redmine = new RedmineIssueClientImpl(
                aUrl
                , aKey
                , new GsonBuilder().setPrettyPrinting().create()
                , new HttpClientImpl()
        );
        project = aProject;
    }

    @Override
    public void createIssue(String aSubject, int aParent, String aDescription) {
        redmine.createIssue(RedmineIssueCreateRequest.builder()
                .issue(RedmineIssue.builder()
                        .subject(aSubject)
                        .projectId(project)
                        .parentIssueId(aParent)
                        .description(aDescription)
                        .assignedToId("me")
                        .build())
                .build());
    }
}
