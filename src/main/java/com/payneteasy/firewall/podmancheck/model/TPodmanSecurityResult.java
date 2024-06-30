package com.payneteasy.firewall.podmancheck.model;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Data
@FieldDefaults(makeFinal = true, level = PRIVATE)
@Builder
public class TPodmanSecurityResult {
    @SerializedName("id")
    String resultId;

    @SerializedName("desc")
    String resultDescription;

    PodmanSecurityResultType result; // WARN, INFO
    String                   details; // File not found

    String remediation;
    @SerializedName("remediation-impact")
    String remediationImpact;

    List<String> items;

}
