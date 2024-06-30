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
public class TPodmanSecurityGroup {
    @SerializedName("id")
    String groupId;
    @SerializedName("desc")
    String groupDescription;

    List<TPodmanSecurityResult> results;
}
