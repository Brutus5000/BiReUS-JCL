package net.brutus5000.bireus.patching;

import jbsdiff.InvalidHeaderException;
import jbsdiff.Patch;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.brutus5000.bireus.data.DiffItem;
import net.brutus5000.bireus.data.Repository;
import net.brutus5000.bireus.service.ArchiveService;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.io.FileUtils;

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
        /*
          Important information on the way of updating:
          The "new" or "patched" repository is created inside the patchPath, by replacing the bsdiff4 patches with the actual files.
          This way the original repository is intact in case of an error.
          After patching is finished, the patchPath contains the checked out version and needs to replace the original basePath.
         */

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

        patchEventListener.beginPatchingFile(basePath);
        switch (item.getPatchAction()) {
            case ADD:
                // do nothing: the new files are already in the patchPath
                break;
            case REMOVE:
                // do nothing: the files don't exist in the patchPath
                break;
            case ZIPDELTA:
                patchArchiveFile(item, basePath, patchPath);
                break;
            case BSDIFF:
                // apply the patch onto a temporary file and replace the file in patchPath
                // if checksum does not fit, load file from server and save in patchPath

                try {
                    // check the original file before patching (in basePath)
                    String crcBeforePatching = "0x" + Long.toHexString(FileUtils.checksumCRC32(basePath.toFile()));
                    if (!Objects.equals(item.getBaseCrc(), crcBeforePatching)) {
                        patchEventListener.crcMismatch(basePath);
                        throw new CrcMismatchException(basePath, item.getBaseCrc(), crcBeforePatching);
                    }

                    Path patchedTempPath = Paths.get(patchPath + ".patched");
                    try {
                        Patch.patch(basePath.toFile(), patchedTempPath.toFile(), patchPath.toFile());
                    } catch (CompressorException | InvalidHeaderException e) {
                        throw new IOException("Error on applying bsdiff4", e);
                    }

                    // the .patched-file replaces the original bsdiff4 file
                    Files.delete(patchPath);
                    Files.move(patchedTempPath, patchPath);

                    // check the final file after patching (in patchPath)
                    String crcAfterPatching = "0x" + Long.toHexString(FileUtils.checksumCRC32(patchPath.toFile()));
                    if (!Objects.equals(item.getTargetCrc(), crcAfterPatching)) {
                        val exception = new CrcMismatchException(patchPath, item.getTargetCrc(), crcAfterPatching);
                        log.error("CRC mismatch in patched file `{}` (expected={}, actual={}), patching aborted", patchPath, item.getTargetCrc(), crcAfterPatching, exception);
                        patchEventListener.crcMismatch(patchPath);
                        throw exception;
                    }
                } catch (CrcMismatchException e) {
                    if (insideArchive)
                        throw e;

                    log.info("Emergency fallback: download {} from original source", basePath);
                    Repository repository = repositoryService.getRepository();
                    Files.delete(patchPath);
                    URL fileUrl = new URL(repository.getUrl() + "/" + targetVersion + "/" + repository.getAbsolutePath().relativize(basePath).toUri().toURL());
                    downloadService.download(fileUrl, patchPath);
                }
                break;
            case UNCHANGED:
                // since unchanged, there is no file in the patchPath, we need to copy it from the basePath
                Files.copy(basePath, patchPath);
                break;
            default:
                log.error("Unexpected patch action `{}` on patching file", item.getPatchAction().toString());
        }
        patchEventListener.finishPatchingFile(basePath);
    }

    private void patchDirectory(DiffItem item, Path basePath, Path patchPath, boolean insideArchive) throws IOException {
        log.debug("Patching directory (action=`{}`, folder=`{}`, path=`{}`)", item.getPatchAction(), item.getName(), basePath.getFileName());

        patchEventListener.beginPatchingDirectory(basePath);
        switch (item.getPatchAction()) {
            case ADD:
                // do nothing: the new files are already in the patchPath
                break;
            case REMOVE:
                // do nothing: the files don't exist in the patchPath
                break;
            case DELTA:
                patch(item, basePath, patchPath, insideArchive);
                break;
            default:
                log.error("Unexpected patch action `{}` on patching directory", item.getPatchAction());
        }
        patchEventListener.finishPatchingDirectory(basePath);
    }

    private void patchArchiveFile(DiffItem item, Path basePath, Path patchPath) throws IOException {
        // we need a temporary folder to extract the zip content from the base files
        Path temporaryFolder = createTemporaryFolder("extracted_" + patchPath.getFileName() + "_");

        // extract the original files, attention: the patch files aren't zipped anymore
        log.debug("Extracting files to `{}`", temporaryFolder);
        ArchiveService.extractZip(basePath, temporaryFolder);

        // now we can start the patching
        patch(item, temporaryFolder, patchPath, true);

        // the patched files are now in patchPath
        // therefore we can remove the temporaryFolder
        log.debug("Removing the temporary folder {}", temporaryFolder);
        FileUtils.deleteDirectory(temporaryFolder.toFile());

        // patchPath is a folder with the patch files but is supposed to be the zip file,
        // therefore we rename the folder for a second before compressing
        log.debug("Re-compressing files at {}", patchPath);
        Path intermediateFolder = Files.move(patchPath, Paths.get(patchPath + ".patched"));
        ArchiveService.compressFolderToZip(intermediateFolder, patchPath);
        FileUtils.deleteDirectory(intermediateFolder.toFile());
    }
}
