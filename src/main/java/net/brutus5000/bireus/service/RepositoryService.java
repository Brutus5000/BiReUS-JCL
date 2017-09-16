package net.brutus5000.bireus.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.brutus5000.bireus.data.Repository;
import net.brutus5000.bireus.patching.PatchTaskFactory;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.BidirectionalDijkstraShortestPath;
import org.jgrapht.graph.DirectedWeightedPseudograph;
import org.jgrapht.io.EdgeProvider;
import org.jgrapht.io.GmlImporter;
import org.jgrapht.io.ImportException;
import org.jgrapht.io.VertexProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Objects;

@Setter
@Getter
@Slf4j
public class RepositoryService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    PatchEventListener patchEventListener;
    DownloadService downloadService;

    Repository repository;

    /** Graph of ${code filename of patch file} to ${version name}. */
    Graph<String, String> versionGraph;

    public RepositoryService(Path absolutePath) throws IOException, ImportException {
        log.debug("Creating repository service for path `{}`", absolutePath);

        log.trace("Loading {}", Repository.BIREUS_INFO_FILE);
        repository = objectMapper.readValue(absolutePath.resolve(Repository.BIREUS_INTERAL_FOLDER).resolve(Repository.BIREUS_INFO_FILE).toFile(), Repository.class);
        repository.setAbsolutePath(absolutePath);

        log.trace("Loading {}", Repository.BIREUS_VERSIONS_FILE);

        VertexProvider<String> vertexProvider = (label, attributes) -> attributes.get("label");
        EdgeProvider<String, String> edgeProvider = (from, to, label, attributes) -> String.format("%s_to_%s", from, to);

        GmlImporter<String, String> gmlImporter = new GmlImporter<>(vertexProvider, edgeProvider);

        versionGraph = new DirectedWeightedPseudograph<>(String.class);

        gmlImporter.importGraph(versionGraph, repository.getVersionGraphPath().toFile());

        log.info("Repository `{}` loaded (currentVersion=`{}`, latestVersion=`{}`)", repository.getName(), repository.getCurrentVersion(), repository.getLatestVersion());
    }

    public void checkoutLatestVersion() throws CheckoutException {
        try {
            updateRepositoryFromRemote();
        } catch (IOException e) {
            log.warn("Update repository info from remote failed, use local instead", e);
        }

        checkout(repository.getLatestVersion());
    }

    public boolean checkVersionExists(String version) {
        if (versionGraph.containsVertex(version)) {
            return true;
        }
        try {
            updateRepositoryFromRemote();
        } catch (IOException e) {
            log.error("An error occurred during updating from remote repository", e);
            return false;
        }
        return versionGraph.containsVertex(version);
    }

    public void checkout(String version) throws CheckoutException {
        Objects.requireNonNull(downloadService);
        Objects.requireNonNull(patchEventListener);

        log.info("Checking out version `{}` from repository `{}`", version, repository.getName());
        patchEventListener.beginCheckoutVersion(version);

        String currentVersion = repository.getCurrentVersion();
        if (currentVersion.equals(version)) {
            log.info("Version `{}` is already checked out", version);
            patchEventListener.checkedOutAlready(version);
            return;
        }

        if (!checkVersionExists(version)) {
            log.error("Version `{}` is not listed on the server", version);
            patchEventListener.versionUnknown(version);
            throw new CheckoutException(MessageFormat.format("Version `{0}` is not listed on the server", version), repository, version);
        }

        BidirectionalDijkstraShortestPath<String, String> shortestPathAlgorithm = new BidirectionalDijkstraShortestPath<>(versionGraph);
        GraphPath<String, String> patchPath = shortestPathAlgorithm.getPath(currentVersion, version);

        if (patchPath == null) {
            log.error("No valid patch path from `{}` to `{}`", currentVersion, version);
            patchEventListener.noPatchPath(version);
            throw new CheckoutException(MessageFormat.format("No valid patch path from `{0}` to `{1}`", currentVersion, version), repository, version);
        }

        log.debug("Patch path found: {}", patchPath);
        patchEventListener.foundPatchPath(patchPath);

        applyPatchPath(patchPath);

        log.info("Version `{}` is now checked out", version);
        patchEventListener.finishCheckoutVersion(version);
    }

    private void applyPatchPath(GraphPath<String, String> patchPath) throws CheckoutException {
        String versionFrom = patchPath.getStartVertex();
        String versionTo;

        for (String intermediateVersion : patchPath.getVertexList()) {
            if (!Objects.equals(versionFrom, intermediateVersion)) {
                versionFrom = repository.getCurrentVersion();
                versionTo = intermediateVersion;

                try {
                    downloadPatchFile(versionFrom, versionTo);
                    applyPatch(versionFrom, versionTo);

                    repository.setCurrentVersion(versionTo);
                    log.trace("Delete and rewrite {}", Repository.BIREUS_INFO_FILE);
                    Files.delete(repository.getInfoPath());
                    objectMapper.writeValue(repository.getInfoPath().toFile(), repository);
                } catch (IOException e) {
                    log.error(e.getLocalizedMessage(), e);
                    throw new CheckoutException(e.getLocalizedMessage(), repository, versionTo, e);
                }
            }
        }
    }

    private void downloadPatchFile(String versionFrom, String versionTo) throws IOException {
        val patchDeltaFile = repository.getPatchPath(versionFrom, versionTo).toFile();
        if (!patchDeltaFile.exists()) {
            log.info("Download deltafile {} from server", patchDeltaFile.getName());
            downloadPatch(versionFrom, versionTo);

        } else {
            log.info("Deltafile `{}` is already on disk", patchDeltaFile.getName());
        }
    }

    private void applyPatch(String versionFrom, String versionTo) throws IOException {
        log.debug("Applying patch (from=`{}`, to=`{}`)", versionFrom, versionTo);
        patchEventListener.beginApplyPatch(versionFrom, versionTo);

        val patchTask = PatchTaskFactory.getInstance().create(repository.getProtocolVersion());
        patchTask.run(this, patchEventListener, downloadService, repository.getPatchPath(versionFrom, versionTo));

        log.debug("Patch applied");
        patchEventListener.finishApplyPatch(versionFrom, versionTo);
    }

    private void downloadPatch(String versionFrom, String versionTo) throws IOException {
        val url = repository.getRemotePatchURL(versionFrom, versionTo);
        val patchPath = repository.getPatchPath(versionFrom, versionTo);

        patchEventListener.beginDownloadPatch(url);
        Files.createDirectories(patchPath.getParent());

        try {
            downloadService.download(url, patchPath);
        } catch (DownloadException e) {
            patchEventListener.error(MessageFormat.format("Downloading patch-file failed from `{0}`", url));
            throw new DownloadException(e, url);
        }
        patchEventListener.finishDownloadPatch(url);
    }

    private void updateRepositoryFromRemote() throws IOException {
        log.debug("Download repository info from remote");
        val infoJsonBytes = downloadService.read(repository.getRemoteInfoURL());
        val newRepository = objectMapper.readValue(infoJsonBytes, Repository.class);

        if (!repository.getLatestVersion().equals(newRepository.getLatestVersion())) {
            log.debug("Latest version has changed (old=`{}`, new=`{}`), updating {}", repository.getLatestVersion(), newRepository.getLatestVersion(), Repository.BIREUS_INFO_FILE);
            repository.setLatestVersion(newRepository.getLatestVersion());

            log.trace("Delete and rewrite {}", Repository.BIREUS_INFO_FILE);
            Files.delete(repository.getInfoPath());
            objectMapper.writeValue(repository.getInfoPath().toFile(), repository);

            log.trace("Delete and rewrite {}", Repository.BIREUS_VERSIONS_FILE);
            Files.delete(repository.getVersionGraphPath());
            downloadService.download(repository.getRemoteInfoURL(), repository.getVersionGraphPath());
        } else {
            log.debug("Local repository info is up to date");
        }
    }
}
