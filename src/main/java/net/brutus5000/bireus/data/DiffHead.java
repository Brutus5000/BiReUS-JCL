package net.brutus5000.bireus.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collection;

@Data
public class DiffHead {
    @JsonProperty("repository")
    private String repository;
    @JsonProperty("protocol")
    private int protocol;
    @JsonProperty("base_version")
    private String baseVersion;
    @JsonProperty("target_version")
    private String targetVersion;
    @JsonProperty("items")
    private Collection<DiffItem> items;
}
