package net.brutus5000.bireus.patching;

import net.brutus5000.bireus.data.DiffItem;

import java.nio.file.Path;

public class PatchTaskV1 extends PatchTask {
    @Override
    public String getVersion() {
        return "1";
    }

    @Override
    protected void patch(DiffItem item, Path basePath, Path patchPath, boolean insideArchive) {

    }
}
