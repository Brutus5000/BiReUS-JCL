package net.brutus5000.bireus.patching;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.io.FileUtils;

import jbsdiff.InvalidHeaderException;
import jbsdiff.Patch;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.brutus5000.bireus.data.DiffItem;
import net.brutus5000.bireus.data.Repository;
import net.brutus5000.bireus.service.ArchiveService;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

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
                Files.deleteIfExists(basePath);
                Files.copy(patchPath, basePath);
                break;
            case REMOVE:
                Files.delete(basePath);
                break;
            case ZIPDELTA:
                patchArchiveFile(item, basePath, patchPath, insideArchive);
                break;
            case BSDIFF:
                String crcBeforePatching = "0x" + Long.toHexString(FileUtils.checksumCRC32(basePath.toFile()));

                Path patchedPath = Paths.get(basePath + ".patched");

                try {
                    if (!Objects.equals(item.getBaseCrc(), crcBeforePatching)) {
                        val exception = new CrcMismatchException(basePath, item.getBaseCrc(), crcBeforePatching);
                        log.error("CRC mismatch in unpatched base file `{}` (expected={}, actual={}), patching aborted", basePath, item.getBaseCrc(), crcBeforePatching, exception);
                        notificationService.crcMismatch(basePath);
                        throw exception;
                    }

                    try {
                        Patch.patch(basePath.toFile(), patchedPath.toFile(), patchPath.toFile());
                    } catch (CompressorException | InvalidHeaderException e) {
                        log.error("Error on applying bsdiff4", e);
                        throw new IOException(e);
                    }

                    Files.delete(basePath);
                    Files.move(patchedPath, basePath);

                    String crcAfterPatching = Long.toHexString(FileUtils.checksumCRC32(basePath.toFile()));
                    if (!Objects.equals(item.getBaseCrc(), crcBeforePatching)) {
                        val exception = new CrcMismatchException(basePath, item.getBaseCrc(), crcBeforePatching);
                        log.error("CRC mismatch in patched base file `{}` (expected={}, actual={}), patching aborted", basePath, item.getBaseCrc(), crcBeforePatching, exception);
                        notificationService.crcMismatch(basePath);
                        throw exception;
                    }
                } catch (CrcMismatchException e) {
                    if (insideArchive)
                        throw e;

                    log.info("Emergency fallback: download {} from original source", basePath);
                    Repository repository = repositoryService.getRepository();
                    Files.delete(basePath);
                    URL fileUrl = new URL(repository.getUrl() + "/" + targetVersion + "/" + basePath.relativize(repository.getAbsolutePath()).toUri().toURL());
                    downloadService.download(fileUrl, basePath);
                }
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
                Files.deleteIfExists(basePath);
                Files.copy(patchPath, basePath);
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
        Path temporaryFolder = createTemporaryFolder("extracted_" + patchPath.getFileName() + "_");

        log.debug("Extracting files to `{}`", temporaryFolder);
        // extract the original files, attention: the patch files aren't zipped anymore
        ArchiveService.extractZip(basePath, temporaryFolder);

        patch(item, temporaryFolder, patchPath, true);

        log.debug("Re-compressing files from `{}`", temporaryFolder);
        ArchiveService.compressFolderToZip(temporaryFolder, basePath);

        FileUtils.deleteDirectory(temporaryFolder.toFile());
    }
}