package net.brutus5000.bireus.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum PatchAction {
    @JsonProperty("unchanged")
    UNCHANGED,
    @JsonProperty("add")
    ADD,
    @JsonProperty("remove")
    REMOVE,
    @JsonProperty("delta")
    DELTA,
    @JsonProperty("bsdiff")
    BSDIFF,
    @JsonProperty("zipdelta")
    ZIPDELTA;
}
