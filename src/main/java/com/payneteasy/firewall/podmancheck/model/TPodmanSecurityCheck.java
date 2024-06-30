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
public class TPodmanSecurityCheck {
    @SerializedName("podmanbenchsecurity")
    String podmanBenchSecurityVersion;
    long                       start;
    List<TPodmanSecurityGroup> tests;
    int                        checks;
    int                        score;
    long                       end;
}
