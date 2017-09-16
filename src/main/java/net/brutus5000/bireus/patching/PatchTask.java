package net.brutus5000.bireus.patching;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.brutus5000.bireus.data.DiffHead;
import net.brutus5000.bireus.data.DiffItem;
import net.brutus5000.bireus.data.Repository;
import net.brutus5000.bireus.service.ArchiveService;
import net.brutus5000.bireus.service.DownloadService;
import net.brutus5000.bireus.service.PatchEventListener;
import net.brutus5000.bireus.service.RepositoryService;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.UUID;

@Slf4j
public abstract class PatchTask {
    protected RepositoryService repositoryService;
    protected PatchEventListener patchEventListener;
    protected DownloadService downloadService;

    protected String targetVersion;

    public abstract int getVersion();

    public void run(RepositoryService repositoryService, PatchEventListener patchEventListener, DownloadService downloadService, Path patchFile) throws IOException {
        log.debug("Started PatchTask run using {} (protocolVersion=`{}`)", getClass().getCanonicalName(), getVersion());

        this.repositoryService = repositoryService;
        this.patchEventListener = patchEventListener;
        this.downloadService = downloadService;

        Path temporaryFolder = null;
        try {
            temporaryFolder = createTemporaryFolder(patchFile.getFileName() + "_");

            log.info("Begin decompressing patch `{}` to `{}`", patchFile.getFileName(), temporaryFolder.getFileName());
            ArchiveService.extractTarXz(patchFile, temporaryFolder);
            log.info("Patch decompressed");

            log.trace("Loading {} from json to object", Repository.BIREUS_INFO_FILE);
            val infoFile = temporaryFolder
                    .resolve(Repository.BIREUS_INTERAL_FOLDER)
                    .toFile();
            val objectMapper = new ObjectMapper();
            val diffHead = objectMapper.readValue(infoFile, DiffHead.class);
            targetVersion = diffHead.getTargetVersion();

            if (!Objects.equals(diffHead.getProtocol(), this.getVersion())) {
                val message = MessageFormat.format("bireus protocol version `{0}` doesn't match patcher task version `{1}`",
                        diffHead.getProtocol(), this.getVersion());
                patchEventListener.error(message);
                throw new IOException(message);
            }

            if (diffHead.getItems().size() != 1) {
                val message = "Invalid bireus file - the head is allowed to have one item only";
                patchEventListener.error(message);
                throw new IOException(message);
            }

            DiffItem rootItem = diffHead.getItems().stream().findFirst().get();
            Path repositoryPath = repositoryService.getRepository().getAbsolutePath();
            patch(rootItem, repositoryPath, temporaryFolder);

            Files.delete(temporaryFolder.resolve(Repository.BIREUS_INTERAL_FOLDER)); // remove the patch descriptor

            // now, the temporary folder contains the checked out version that we want
            Path intermediateFolder = repositoryPath.getParent().resolve(repositoryService.getRepository().getName() + "_" + UUID.randomUUID().toString());
            Path relativeTemporaryFolder = repositoryPath.relativize(temporaryFolder);
            Files.move(repositoryPath, intermediateFolder); // make place for the new repository folder
            Files.move(intermediateFolder.resolve(relativeTemporaryFolder), repositoryPath); // make the temporaryFolder the new repository
            Files.move(intermediateFolder.resolve(Repository.BIREUS_INTERAL_FOLDER), repositoryPath.resolve(Repository.BIREUS_INTERAL_FOLDER)); // restore the old internal files
            FileUtils.deleteDirectory(intermediateFolder.toFile()); // get rid of the old repository version
        } finally {
            if (temporaryFolder != null) {
                FileUtils.deleteQuietly(temporaryFolder.toFile());
            }
        }
    }

    /***
     * Create a temporary directory inside the bireus internal folder
     * @return Path to temporary folder
     * @throws IOException when folder can't be created
     */
    protected Path createTemporaryFolder(String prefix) throws IOException {
        Path parentDirectoryPath = repositoryService.getRepository()
                .getAbsolutePath()
                .resolve(Repository.BIREUS_INTERAL_FOLDER)
                .resolve(Repository.BIREUS_TMP_SUBFOLDER);

        log.debug("Create temp folder in {}", parentDirectoryPath.getFileName());

        Files.createDirectories(parentDirectoryPath);
        return Files.createTempDirectory(parentDirectoryPath, prefix);
    }

    protected void patch(DiffItem diffItem, Path basePath, Path patchPath) throws IOException {
        patch(diffItem, basePath, patchPath, false);
    }

    protected abstract void patch(DiffItem diffItem, Path basePath, Path patchPath, boolean insideArchive) throws IOException;
}
