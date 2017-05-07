package net.brutus5000.bireus.patching;

import org.apache.commons.io.FileUtils;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.brutus5000.bireus.data.DiffItem;
import net.brutus5000.bireus.service.ArchiveService;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
public class PatchTaskV1 extends PatchTask {
    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    protected void patch(DiffItem diffItem, Path basePath, Path patchPath, boolean insideArchive) throws IOException {
        for (val item : diffItem.getItems()) {
            switch (item.getIoType()) {
                case FILE:
                    patchFile(item,
                            basePath.resolve(item.getName()),
                            patchPath.resolve(item.getName()),
                            insideArchive);
                    break;
                case DIRECTORY:
                    patchDirectory(item,
                            basePath.resolve(item.getName()),
                            patchPath.resolve(item.getName()),
                            insideArchive);
                    break;
            }
        }
    }

    private void patchFile(DiffItem item, Path basePath, Path patchPath, boolean insideArchive) throws IOException {
        log.debug("Patching file (action=`{}`, file=`{}`, relative path=`{}`)", item.getPatchAction(), item.getName(), patchPath.getFileName());

        notificationService.beginPatchingFile(basePath);
        switch (item.getPatchAction()) {
            case ADD:
                if (basePath.toFile().exists())
                    basePath.toFile().delete();
                FileUtils.copyFile(patchPath.toFile(), basePath.toFile());
                break;
            case REMOVE:
                basePath.toFile().delete();
                break;
            case ZIPDELTA:
                patchArchiveFile(item, basePath, patchPath, insideArchive);
                break;
            case BSDIFF:
                // TODO: add implementation
                break;
            case UNCHANGED:
                break;
            default:
                log.error("Unexpected patch action `{}` on patching file", item.getPatchAction().toString());
        }
        notificationService.finishPatchingFile(basePath);
    }

    private void patchDirectory(DiffItem item, Path basePath, Path patchPath, boolean insideArchive) throws IOException {
        log.debug("Patching directory (action=`{}`, folder=`{}`, path=`{}`)", item.getPatchAction(), item.getName(), basePath.getFileName());

        notificationService.beginPatchingDirectory(basePath);
        switch (item.getPatchAction()) {
            case ADD:
                if (basePath.toFile().exists())
                    FileUtils.deleteDirectory(basePath.toFile());
                FileUtils.copyDirectory(patchPath.toFile(), basePath.toFile());
                break;
            case REMOVE:
                FileUtils.deleteDirectory(basePath.toFile());
                break;
            case DELTA:
                patch(item, basePath, patchPath, insideArchive);
                break;
            default:
                log.error("Unexpected patch action `{}` on patching directory", item.getPatchAction());
        }
        notificationService.finishPatchingDirectory(basePath);
    }

    private void patchArchiveFile(DiffItem item, Path basePath, Path patchPath, boolean insideArchive) throws IOException {
        Path temporaryFolder = createTemporaryFolder("extracted_" + patchPath.getFileName().toString() + "_");

        ArchiveService.extractZip(patchPath, temporaryFolder);
        patch(item, temporaryFolder, patchPath.resolve(item.getName()), true);
        ArchiveService.compressFolderToZip(temporaryFolder, basePath);

        temporaryFolder.toFile().deleteOnExit();
    }
}