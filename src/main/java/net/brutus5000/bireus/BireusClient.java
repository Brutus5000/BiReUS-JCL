package net.brutus5000.bireus;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.brutus5000.bireus.data.Repository;
import net.brutus5000.bireus.service.*;
import org.apache.commons.io.FileUtils;
import org.jgrapht.io.ImportException;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;

@Slf4j
public class BireusClient {
    private final RepositoryService repositoryService;

    public BireusClient(Path repositoryPath, PatchEventListener patchEventListener, DownloadService downloadService) throws BireusException {
        try {
            repositoryService = new RepositoryService(repositoryPath);
            repositoryService.setPatchEventListener(patchEventListener);
            repositoryService.setDownloadService(downloadService);
        } catch (IOException | ImportException e) {
            throw new BireusException("Error on initializing BiReUS repository", e);
        }
    }

    public static BireusClient getFromURL(URL url, Path path, PatchEventListener patchEventListener, DownloadService downloadService) throws BireusException {
        try {
            if (!Files.isDirectory(path)) {
                String message = MessageFormat.format("Path {0} is not a directory", path);
                throw new BireusException(message, null);
            }

            if (Files.exists(path) && Files.list(path).count() > 0) {
                String message = MessageFormat.format("Directory {0} is not empty", path);
                throw new BireusException(message, null);
            }

            URL infoJsonURL = new URL(url, "/" + Repository.BIREUS_INFO_FILE);
            Files.createDirectories(path.resolve(Repository.BIREUS_INTERAL_FOLDER));
            log.debug("Read {} from {}", Repository.BIREUS_INFO_FILE, infoJsonURL);
            byte[] infoJsonBytes = downloadService.read(infoJsonURL);

            ObjectMapper objectMapper = new ObjectMapper();
            Repository repository = objectMapper.readValue(infoJsonBytes, Repository.class);

            repository.setUrl(url);
            repository.setCurrentVersion(repository.getLatestVersion());

            log.debug("Write new {}", Repository.BIREUS_INFO_FILE);
            objectMapper.writeValue(path
                    .resolve(Repository.BIREUS_INTERAL_FOLDER)
                    .resolve(Repository.BIREUS_INFO_FILE)
                    .toFile(), repository);

            URL versionGmlURL = new URL(url, "/" + Repository.BIREUS_VERSIONS_FILE);
            log.debug("Read {} from {}", Repository.BIREUS_VERSIONS_FILE, versionGmlURL);
            downloadService.download(versionGmlURL, path
                    .resolve(Repository.BIREUS_INTERAL_FOLDER)
                    .resolve(Repository.BIREUS_VERSIONS_FILE));

            URL latestVersionURL = new URL(url, "/" + Repository.BIREUS_LATEST_VERSION_ARCHIVE);
            log.debug("Download latest version from {}", latestVersionURL);
            Path temporaryDirectory = Files.createTempDirectory("bireus_");
            downloadService.download(latestVersionURL, temporaryDirectory.resolve(Repository.BIREUS_LATEST_VERSION_ARCHIVE));

            ArchiveService.extractTarXz(temporaryDirectory.resolve(Repository.BIREUS_LATEST_VERSION_ARCHIVE), path);
            FileUtils.deleteDirectory(temporaryDirectory.toFile());
            return new BireusClient(path, patchEventListener, downloadService);
        } catch (IOException e) {
            FileUtils.deleteQuietly(path.toFile());
            throw new BireusException(MessageFormat.format("Error while loading repository from URL {0}", url), e);
        }
    }

    public boolean checkVersionExists(String version) {
        return repositoryService.checkVersionExists(version);
    }

    public void checkoutLatestVersion() throws BireusException {
        try {
            repositoryService.checkoutLatestVersion();
        } catch (CheckoutException e) {
            throw new BireusException("Error on checking out latest version", e);
        }
    }

    public void checkoutVersion(String version) throws BireusException {
        try {
            repositoryService.checkout(version);
        } catch (CheckoutException e) {
            throw new BireusException(MessageFormat.format("Error on checking out version {0}", version), e);
        }
    }
}
