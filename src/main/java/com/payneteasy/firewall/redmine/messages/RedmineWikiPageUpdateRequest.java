package com.payneteasy.firewall.redmine.messages;

import com.google.gson.annotations.SerializedName;
import com.payneteasy.firewall.redmine.model.RedmineWikiPage;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Data
@FieldDefaults(makeFinal = true, level = PRIVATE)
@Builder
public class RedmineWikiPageUpdateRequest {

    @SerializedName("wiki_page")
    RedmineWikiPage wikiPage;
}
