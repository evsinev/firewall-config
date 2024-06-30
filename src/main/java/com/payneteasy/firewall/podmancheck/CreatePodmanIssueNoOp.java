package com.payneteasy.firewall.podmancheck;

public class CreatePodmanIssueNoOp implements ICreatePodmanIssue {

    @Override
    public void createIssue(String aSubject, int aParent, String aDescription) {
        System.out.println("aSubject = " + aSubject);
        System.out.println("aDescription = " + aDescription);
    }
}
