package net.brutus5000.bireus.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

import java.util.Collection;

@Data
public class DiffItem {
    @JsonProperty("name")
    private String name;
    @JsonProperty("type")
    private IoType ioType;
    @JsonProperty(value = "base_crc", required = false)
    private String baseCrc;
    @JsonProperty(value = "target_crc", required = false)
    private String targetCrc;
    @JsonProperty("action")
    private PatchAction patchAction;
    @JsonProperty("items")
    private Collection<DiffItem> items;
}
