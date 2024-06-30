package com.payneteasy.firewall.redmine.model;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Data
@FieldDefaults(makeFinal = true, level = PRIVATE)
@Builder
public class RedmineIssue {
    @SerializedName("project_id")
    String projectId;

    String subject;

    String description;

    @SerializedName("parent_issue_id")
    Integer parentIssueId;

    @SerializedName("estimated_hours")
    Integer estimatedHours;

    @SerializedName("assigned_to_id")
    String assignedToId;
}
