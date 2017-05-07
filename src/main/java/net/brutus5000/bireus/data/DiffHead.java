package net.brutus5000.bireus.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

import java.util.Collection;

@Data
public class DiffHead {
    @JsonProperty("repository")
    String repository;
    @JsonProperty("protocol")
    int protocol;
    @JsonProperty("base_version")
    String baseVersion;
    @JsonProperty("target_version")
    String targetVersion;
    @JsonProperty("items")
    Collection<DiffItem> items;
}
