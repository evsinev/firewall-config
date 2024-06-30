package com.payneteasy.firewall.redmine.messages;

import com.payneteasy.firewall.redmine.model.RedmineIssue;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Data
@FieldDefaults(makeFinal = true, level = PRIVATE)
@Builder
public class RedmineIssueCreateRequest {
    RedmineIssue issue;
}
