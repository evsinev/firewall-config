package com.payneteasy.firewall.redmine.model;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Data
@FieldDefaults(makeFinal = true, level = PRIVATE)
@Builder
public class RedmineWikiPage {

    String text;

    String comments;

    /** null on create; Redmine answers 409 if it does not match the current revision */
    Integer version;
}
