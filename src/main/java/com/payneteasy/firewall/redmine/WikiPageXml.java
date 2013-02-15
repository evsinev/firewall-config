package com.payneteasy.firewall.redmine;

import com.google.api.client.util.Key;

class WikiPageXml {

    @Key private String comments;
    @Key private String text;
    @Key private String title;
    @Key private Integer version;

    public String getComments() {
        return comments;
    }

    public String getText() {
        return text;
    }

    public String getTitle() {
        return title;
    }

    public Integer getVersion() {
        return version;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}

