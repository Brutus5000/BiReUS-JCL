package net.brutus5000.bireus.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum IoType {
    @JsonProperty("file")
    FILE,
    @JsonProperty("directory")
    DIRECTORY;
}
