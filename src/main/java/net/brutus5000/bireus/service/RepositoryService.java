package net.brutus5000.bireus.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.BidirectionalDijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedWeightedPseudograph;
import org.jgrapht.io.EdgeProvider;
import org.jgrapht.io.GmlImporter;
import org.jgrapht.io.ImportException;
import org.jgrapht.io.VertexProvider;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.brutus5000.bireus.data.Repository;
import net.brutus5000.bireus.patching.PatchTaskFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Objects;

@Setter
@Getter
@Slf4j
public class RepositoryService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    NotificationService notificationService;
    DownloadService downloadService;

    Repository repository;
    Graph<String, DefaultEdge> versionGraph;

    public RepositoryService(Path absolutePath) throws IOException, ImportException {
        log.debug("Creating repository service for path `{}`", absolutePath);

        log.trace("Loading {}", Repository.BIREUS_INFO_FILE);
        repository = objectMapper.readValue(absolutePath.resolve(Repository.BIREUS_INTERAL_FOLDER).resolve(Repository.BIREUS_INFO_FILE).toFile(), Repository.class);
        repository.setAbsolutePath(absolutePath);

        log.trace("Loading {}", Repository.BIREUS_VERSIONS_FILE);

        VertexProvider<String> vertexProvider = (label, attributes) -> attributes.get("label");
        EdgeProvider<String, String> edgeProvider = (from, to, label, attributes) -> String.format("%s_to_%s", from, to);

        val gmlImporter = new GmlImporter(vertexProvider, edgeProvider);

        versionGraph = new DirectedWeightedPseudograph<>(DefaultEdge.class);


        gmlImporter.importGraph(versionGraph, repository.getVersionGraphPath().toFile());

        log.info("Repository `{}` loaded (currentVersion=`{}`, latestVersion=`{}`)", repository.getName(), repository.getCurrentVersion(), repository.getLatestVersion());
    }

    public void checkoutLatestVersion() throws CheckoutException {
        try {
            updateRepositoryFromRemote();
        } catch (Exception e) {
            log.warn("Update repository info from remote failed, use local instead", e);
        }

        checkout(repository.getLatestVersion());
    }

    public boolean checkVersionExists(String version) {
        if (versionGraph.containsVertex(version)) {
            return true;
        } else {
            try {
                updateRepositoryFromRemote();
                return versionGraph.containsVertex(version);
            } catch (Exception e) {
                log.error("An error occurred during updating from remote repository", e);
                return false;
            }
        }
    }

    public void checkout(String version) throws CheckoutException {
        assert Objects.nonNull(downloadService);
        assert Objects.nonNull(notificationService);

        log.info("Checking out version `{}` from repository `{}`", version, repository.getName());
        notificationService.beginCheckoutVersion(version);

        String currentVersion = repository.getCurrentVersion();
        if (currentVersion.equals(version)) {
            log.info("Version `{}` is already checked out", version);
            notificationService.checkedOutAlready(version);
            return;
        }

        if (!checkVersionExists(version)) {
            log.error("Version `{}` is not listed on the server", version);
            notificationService.versionUnknown(version);
            throw new CheckoutException(MessageFormat.format("Version `{0}` is not listed on the server", version), repository, version);
        }

        val shortestPathAlgorithm = new BidirectionalDijkstraShortestPath<String, DefaultEdge>(versionGraph);
        val patchPath = shortestPathAlgorithm.getPath(currentVersion, version);

        if (patchPath == null) {
            log.error("No valid patch path from `{}` to `{}`", currentVersion, version);
            notificationService.noPatchPath(version);
            throw new CheckoutException(MessageFormat.format("No valid patch path from `{0}` to `{1}`", currentVersion, version), repository, version);
        }

        log.debug("Patch path found: {}", patchPath);
        notificationService.foundPatchPath(patchPath);

        applyPatchPath(patchPath);

        log.info("Version `{}` is now checked out", version);
        notificationService.finishCheckoutVersion(version);
    }

    private void applyPatchPath(GraphPath<String, DefaultEdge> patchPath) throws CheckoutException {
        String versionFrom = patchPath.getStartVertex();
        String versionTo = null;

        for (String intermediateVersion : patchPath.getVertexList()) {
            if (!Objects.equals(versionFrom, intermediateVersion)) {
                versionFrom = repository.getCurrentVersion();
                versionTo = intermediateVersion;

                try {
                    downloadPatchFile(versionFrom, versionTo);
                    applyPatch(versionFrom, versionTo);

                    repository.setCurrentVersion(versionTo);
                    log.trace("Delete and rewrite {}", Repository.BIREUS_INFO_FILE);
                    repository.getInfoPath().toFile().delete();
                    objectMapper.writeValue(repository.getInfoPath().toFile(), repository);
                } catch (IOException e) {
                    log.error(e.getLocalizedMessage(), e);
                    throw new CheckoutException(e.getLocalizedMessage(), repository, versionTo, e);
                }
            }
        }
    }

    private void downloadPatchFile(String versionFrom, String versionTo) throws DownloadException {
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
        notificationService.beginApplyPatch(versionFrom, versionTo);

        val patchTask = PatchTaskFactory.getInstance().create(repository.getProtocolVersion());
        patchTask.run(this, notificationService, downloadService, repository.getPatchPath(versionFrom, versionTo));

        log.debug("Patch applied");
        notificationService.finishApplyPatch(versionFrom, versionTo);
    }

    private void downloadPatch(String versionFrom, String versionTo) throws DownloadException {
        val url = repository.getRemotePatchURL(versionFrom, versionTo);
        val patchPath = repository.getPatchPath(versionFrom, versionTo);

        try {
            notificationService.beginDownloadPatch(url);
            downloadService.download(url, patchPath);
            notificationService.finishDownloadPatch(url);
        } catch (DownloadException e) {
            notificationService.error(MessageFormat.format("Downloading patch-file failed from `{0}`", url));
            log.error("Downloading patch-file failed from `{}`", url, e);
            throw e;
        }
    }

    private void updateRepositoryFromRemote() throws Exception {
        log.debug("Download repository info from remote");
        val infoJsonBytes = downloadService.read(repository.getRemoteInfoURL());
        val newRepository = objectMapper.readValue(infoJsonBytes.array(), Repository.class);

        if (!repository.getLatestVersion().equals(newRepository.getLatestVersion())) {
            log.debug("Latest version has changed (old=`{}`, new=`{}`), updating {}", repository.getLatestVersion(), newRepository.getLatestVersion(), Repository.BIREUS_INFO_FILE);
            repository.setLatestVersion(newRepository.getLatestVersion());

            log.trace("Delete and rewrite {}", Repository.BIREUS_INFO_FILE);
            repository.getInfoPath().toFile().delete();
            objectMapper.writeValue(repository.getInfoPath().toFile(), repository);

            log.trace("Delete and rewrite {}", Repository.BIREUS_VERSIONS_FILE);
            repository.getVersionGraphPath().toFile().delete();
            downloadService.download(repository.getRemoteInfoURL(), repository.getVersionGraphPath());
        } else {
            log.debug("Local repository info is up to date");
        }
    }
}
