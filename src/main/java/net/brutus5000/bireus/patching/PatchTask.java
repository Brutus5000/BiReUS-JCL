package net.brutus5000.bireus.patching;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.brutus5000.bireus.data.DiffHead;
import net.brutus5000.bireus.data.DiffItem;
import net.brutus5000.bireus.data.Repository;
import net.brutus5000.bireus.service.ArchiveService;
import net.brutus5000.bireus.service.DownloadService;
import net.brutus5000.bireus.service.NotificationService;
import net.brutus5000.bireus.service.RepositoryService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Objects;

@Slf4j
public abstract class PatchTask {
    protected RepositoryService repositoryService;
    protected NotificationService notificationService;
    protected DownloadService downloadService;

    protected String targetVersion;

    public abstract int getVersion();

    public void run(RepositoryService repositoryService, NotificationService notificationService, DownloadService downloadService, Path patchFile) throws IOException {
        log.debug("Started PatchTask run using {} (protocolVersion=`{}`)", getClass().getCanonicalName(), getVersion());

        this.repositoryService = repositoryService;
        this.notificationService = notificationService;
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
                notificationService.error(message);
                throw new IOException(message);
            }

            if (diffHead.getItems().size() != 1) {
                val message = "Invalid bireus file - the head is allowed to have one item only";
                notificationService.error(message);
                throw new IOException(message);
            }

            DiffItem rootItem = diffHead.getItems().stream().findFirst().get();
            patch(rootItem, repositoryService.getRepository().getAbsolutePath(), temporaryFolder);
        } finally {
            if (temporaryFolder != null) {
                temporaryFolder.toFile().deleteOnExit();
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
