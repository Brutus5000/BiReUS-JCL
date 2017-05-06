package net.brutus5000.bireus.data;

import lombok.Data;

import java.nio.ByteBuffer;
import java.util.Collection;

@Data
public class DiffItem {
    String name;
    ByteBuffer baseCrc;
    ByteBuffer targetCrc;
    PatchAction patchAction;
    Collection<DiffItem> items;
}
