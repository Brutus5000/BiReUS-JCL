package net.brutus5000.bireus.data;

import lombok.Data;

import java.util.Collection;

@Data
public class DiffHead {
    Integer protocol;
    String baseVersion;
    String targetVersion;
    Collection<DiffItem> items;
}
