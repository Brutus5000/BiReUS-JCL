package net.brutus5000.bireus.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

import java.util.Collection;

@Data
public class DiffItem {
    @JsonProperty("name")
    String name;
    @JsonProperty("type")
    IoType ioType;
    @JsonProperty(value = "base_crc", required = false)
    String baseCrc;
    @JsonProperty(value = "target_crc", required = false)
    String targetCrc;
    @JsonProperty("action")
    PatchAction patchAction;
    @JsonProperty("items")
    Collection<DiffItem> items;
}
